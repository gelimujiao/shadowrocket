package com.shadowrocket.android.core.vpn

import android.content.Intent
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import androidx.annotation.RequiresApi
import kotlinx.coroutines.*

@RequiresApi(Build.VERSION_CODES.N)
class ShadowrocketTileService : TileService() {

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var job: Job? = null

    override fun onStartListening() {
        super.onStartListening()
        job = scope.launch {
            VpnStateHolder.status.collect { status ->
                val tile = qsTile ?: return@collect
                when (status) {
                    ConnectionStatus.CONNECTED -> {
                        tile.state = Tile.STATE_ACTIVE
                        tile.label = VpnStateHolder.activeNode.value?.name ?: "已连接"
                    }
                    ConnectionStatus.CONNECTING -> {
                        tile.state = Tile.STATE_ACTIVE
                        tile.label = "正在连接..."
                    }
                    ConnectionStatus.DISCONNECTED, ConnectionStatus.STOPPING -> {
                        tile.state = Tile.STATE_INACTIVE
                        tile.label = "Shadowrocket"
                    }
                }
                tile.updateTile()
            }
        }
    }

    override fun onStopListening() {
        job?.cancel()
        super.onStopListening()
    }

    override fun onClick() {
        super.onClick()
        val currentStatus = VpnStateHolder.status.value
        val intent = Intent(this, ShadowrocketVpnService::class.java)
        if (currentStatus == ConnectionStatus.CONNECTED || currentStatus == ConnectionStatus.CONNECTING) {
            intent.action = ShadowrocketVpnService.ACTION_STOP
        } else {
            // In real app, launch MainActivity if no node selected or start with active node
            intent.action = ShadowrocketVpnService.ACTION_START
            intent.putExtra(ShadowrocketVpnService.EXTRA_NODE_NAME, VpnStateHolder.activeNode.value?.name ?: "默认节点")
        }
        startService(intent)
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }
}
