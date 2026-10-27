package com.shadowrocket.android.core.vpn

import com.shadowrocket.android.core.model.ProxyNode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class ConnectionStatus {
    DISCONNECTED,
    CONNECTING,
    CONNECTED,
    STOPPING
}

data class TrafficStats(
    val uploadSpeed: Long = 0L,     // Bytes / sec
    val downloadSpeed: Long = 0L,   // Bytes / sec
    val totalUpload: Long = 0L,     // Bytes
    val totalDownload: Long = 0L    // Bytes
)

object VpnStateHolder {
    private val _status = MutableStateFlow(ConnectionStatus.DISCONNECTED)
    val status = _status.asStateFlow()

    private val _traffic = MutableStateFlow(TrafficStats())
    val traffic = _traffic.asStateFlow()

    private val _activeNode = MutableStateFlow<ProxyNode?>(null)
    val activeNode = _activeNode.asStateFlow()

    fun updateStatus(newStatus: ConnectionStatus) {
        _status.value = newStatus
    }

    fun updateTraffic(stats: TrafficStats) {
        _traffic.value = stats
    }

    fun setActiveNode(node: ProxyNode?) {
        _activeNode.value = node
    }
}

/**
 * JNI Bridge to the compiled Go sing-box / tun core.
 */
object NativeCoreBridge {
    init {
        try {
            System.loadLibrary("box")
        } catch (e: UnsatisfiedLinkError) {
            // Loaded in production when libbox.so is packaged
        }
    }

    external fun startCore(configJson: String): Int
    external fun stopCore(): Int
    external fun queryStats(): LongArray // [uplink, downlink, totalUp, totalDown]
}
