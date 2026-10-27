package com.shadowrocket.android.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shadowrocket.android.core.model.ProxyNode
import com.shadowrocket.android.core.model.RoutingMode
import com.shadowrocket.android.core.vpn.ConnectionStatus
import com.shadowrocket.android.core.vpn.TrafficStats
import com.shadowrocket.android.ui.components.NodeCard
import com.shadowrocket.android.ui.components.RoutingModeSelector
import com.shadowrocket.android.ui.components.TrafficStatsCard
import com.shadowrocket.android.ui.theme.ShadowrocketGreen

@Composable
fun HomeScreen(
    connectionStatus: ConnectionStatus,
    trafficStats: TrafficStats,
    selectedNode: ProxyNode?,
    nodes: List<ProxyNode>,
    routingMode: RoutingMode,
    onToggleVpn: () -> Unit,
    onSelectNode: (ProxyNode) -> Unit,
    onSelectMode: (RoutingMode) -> Unit,
    onTestAllPing: () -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 24.dp)
    ) {
        // Master VPN Toggle Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = when (connectionStatus) {
                        ConnectionStatus.CONNECTED -> ShadowrocketGreen.copy(alpha = 0.12f)
                        ConnectionStatus.CONNECTING -> MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                        else -> MaterialTheme.colorScheme.surface
                    }
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = when (connectionStatus) {
                                ConnectionStatus.CONNECTED -> "已连接"
                                ConnectionStatus.CONNECTING -> "正在连接..."
                                ConnectionStatus.STOPPING -> "正在断开..."
                                ConnectionStatus.DISCONNECTED -> "未连接"
                            },
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = when (connectionStatus) {
                                ConnectionStatus.CONNECTED -> ShadowrocketGreen
                                ConnectionStatus.CONNECTING -> MaterialTheme.colorScheme.primary
                                else -> MaterialTheme.colorScheme.onSurface
                            }
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = selectedNode?.let { "${it.name} (${it.protocol.displayName})" } ?: "未选择节点",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }

                    FilledIconButton(
                        onClick = onToggleVpn,
                        modifier = Modifier.size(54.dp),
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = when (connectionStatus) {
                                ConnectionStatus.CONNECTED -> ShadowrocketGreen
                                ConnectionStatus.CONNECTING -> MaterialTheme.colorScheme.primary
                                else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f)
                            },
                            contentColor = if (connectionStatus == ConnectionStatus.DISCONNECTED) {
                                MaterialTheme.colorScheme.onSurface
                            } else Color.White
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.PowerSettingsNew,
                            contentDescription = "Toggle VPN",
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
            }
        }

        // Live Traffic speedometer
        if (connectionStatus == ConnectionStatus.CONNECTED) {
            item {
                TrafficStatsCard(stats = trafficStats)
            }
        }

        // Global Routing Mode Selector
        item {
            RoutingModeSelector(
                selectedMode = routingMode,
                onModeSelect = onSelectMode
            )
        }

        // Node list header with "Test Ping All"
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "服务器节点 (${nodes.size})",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )

                TextButton(
                    onClick = onTestAllPing,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Speed,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("一键测速", fontSize = 13.sp)
                }
            }
        }

        // List of proxy nodes
        items(nodes, key = { it.id }) { node ->
            NodeCard(
                node = node,
                isSelected = selectedNode?.id == node.id,
                onSelect = { onSelectNode(node) }
            )
        }
    }
}
