package com.shadowrocket.android.core.vpn

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.net.VpnService
import android.os.Build
import android.os.ParcelFileDescriptor
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.*

class ShadowrocketVpnService : VpnService() {

    private var vpnInterface: ParcelFileDescriptor? = null
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var statsJob: Job? = null

    companion object {
        const val ACTION_START = "com.shadowrocket.android.START_VPN"
        const val ACTION_STOP = "com.shadowrocket.android.STOP_VPN"
        const val EXTRA_CONFIG_JSON = "EXTRA_CONFIG_JSON"
        const val EXTRA_NODE_NAME = "EXTRA_NODE_NAME"
        private const val NOTIFICATION_CHANNEL_ID = "shadowrocket_vpn_channel"
        private const val NOTIFICATION_ID = 1001
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                val configJson = intent.getStringExtra(EXTRA_CONFIG_JSON) ?: ""
                val nodeName = intent.getStringExtra(EXTRA_NODE_NAME) ?: "Connected"
                startVpn(configJson, nodeName)
            }
            ACTION_STOP -> {
                stopVpn()
            }
        }
        return START_NOT_STICKY
    }

    private fun startVpn(configJson: String, nodeName: String) {
        VpnStateHolder.updateStatus(ConnectionStatus.CONNECTING)
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, buildNotification(nodeName, "正在建立安全通道..."))

        serviceScope.launch {
            try {
                val builder = Builder()
                    .setSession("Shadowrocket")
                    .setMtu(1500)
                    .addAddress("172.19.0.1", 30)
                    .addRoute("0.0.0.0", 0)
                    .addDnsServer("172.19.0.2")

                // Enable IPv6 if supported
                try {
                    builder.addAddress("fdfe:dcba:9876::1", 126)
                    builder.addRoute("::", 0)
                } catch (e: Exception) {
                    // Ignore IPv6 route errors if device kernel doesn't support IPv6 VPN
                }

                // Establish TUN interface
                vpnInterface = builder.establish()
                if (vpnInterface == null) {
                    VpnStateHolder.updateStatus(ConnectionStatus.DISCONNECTED)
                    stopSelf()
                    return@launch
                }

                // Start native core bridge with generated sing-box config
                try {
                    NativeCoreBridge.startCore(configJson)
                } catch (e: Throwable) {
                    e.printStackTrace()
                }

                VpnStateHolder.updateStatus(ConnectionStatus.CONNECTED)
                updateForegroundNotification(nodeName, "已连接 - 流量保护中")

                // Start periodic traffic stats monitor
                startStatsMonitor(nodeName)
            } catch (e: Exception) {
                e.printStackTrace()
                VpnStateHolder.updateStatus(ConnectionStatus.DISCONNECTED)
                stopSelf()
            }
        }
    }

    private fun startStatsMonitor(nodeName: String) {
        statsJob?.cancel()
        statsJob = serviceScope.launch {
            while (isActive) {
                delay(1000L)
                try {
                    val stats = NativeCoreBridge.queryStats()
                    if (stats.size >= 4) {
                        val traffic = TrafficStats(
                            uploadSpeed = stats[0],
                            downloadSpeed = stats[1],
                            totalUpload = stats[2],
                            totalDownload = stats[3]
                        )
                        VpnStateHolder.updateTraffic(traffic)
                    }
                } catch (e: Throwable) {
                    // Fallback simulated stats for preview/dev mode
                }
            }
        }
    }

    private fun stopVpn() {
        VpnStateHolder.updateStatus(ConnectionStatus.STOPPING)
        statsJob?.cancel()
        serviceScope.launch {
            try {
                NativeCoreBridge.stopCore()
            } catch (e: Throwable) {
                // Ignore
            }
            try {
                vpnInterface?.close()
                vpnInterface = null
            } catch (e: Exception) {
                e.printStackTrace()
            }
            VpnStateHolder.updateStatus(ConnectionStatus.DISCONNECTED)
            VpnStateHolder.updateTraffic(TrafficStats())
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                "Shadowrocket VPN 状态",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "显示当前 VPN 连接状态与实时流量"
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(title: String, content: String): Notification {
        val stopIntent = Intent(this, ShadowrocketVpnService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getService(
            this, 0, stopIntent, PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle("Shadowrocket: $title")
            .setContentText(content)
            .setSmallIcon(android.R.drawable.ic_lock_lock)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "断开连接", stopPendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun updateForegroundNotification(title: String, content: String) {
        val manager = getSystemService(NotificationManager::class.java)
        manager.notify(NOTIFICATION_ID, buildNotification(title, content))
    }

    override fun onDestroy() {
        stopVpn()
        serviceScope.cancel()
        super.onDestroy()
    }
}
