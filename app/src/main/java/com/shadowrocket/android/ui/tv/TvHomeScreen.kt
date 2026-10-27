package com.shadowrocket.android.ui.tv

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shadowrocket.android.core.model.ProxyNode
import com.shadowrocket.android.core.vpn.ConnectionStatus
import com.shadowrocket.android.ui.theme.ShadowrocketGreen

/**
 * Dedicated 10-foot UI optimized for Android TV and Set-top Boxes.
 * Full D-Pad remote control focus support.
 */
@Composable
fun TvHomeScreen(
    connectionStatus: ConnectionStatus,
    selectedNode: ProxyNode?,
    nodes: List<ProxyNode>,
    onToggleVpn: () -> Unit,
    onSelectNode: (ProxyNode) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0F0F12))
            .padding(32.dp)
    ) {
        // Left Column: Big Master Switch & Status (Remote focused)
        Column(
            modifier = Modifier
                .weight(0.4f)
                .fillMaxHeight(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Shadowrocket TV",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            Spacer(modifier = Modifier.height(24.dp))

            var buttonFocused by remember { mutableStateOf(false) }

            Button(
                onClick = onToggleVpn,
                modifier = Modifier
                    .size(140.dp)
                    .onFocusChanged { buttonFocused = it.isFocused }
                    .border(
                        BorderStroke(
                            if (buttonFocused) 4.dp else 0.dp,
                            if (buttonFocused) Color.White else Color.Transparent
                        ),
                        RoundedCornerShape(70.dp)
                    ),
                shape = RoundedCornerShape(70.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = when (connectionStatus) {
                        ConnectionStatus.CONNECTED -> ShadowrocketGreen
                        ConnectionStatus.CONNECTING -> MaterialTheme.colorScheme.primary
                        else -> Color(0xFF2C2C2E)
                    }
                )
            ) {
                Icon(
                    imageVector = Icons.Default.PowerSettingsNew,
                    contentDescription = null,
                    modifier = Modifier.size(54.dp),
                    tint = Color.White
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = when (connectionStatus) {
                    ConnectionStatus.CONNECTED -> "已连接"
                    ConnectionStatus.CONNECTING -> "正在连接..."
                    else -> "未连接 (按 OK 开启)"
                },
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold,
                color = when (connectionStatus) {
                    ConnectionStatus.CONNECTED -> ShadowrocketGreen
                    ConnectionStatus.CONNECTING -> MaterialTheme.colorScheme.primary
                    else -> Color.Gray
                }
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = selectedNode?.name ?: "未选节点",
                fontSize = 15.sp,
                color = Color.LightGray
            )
        }

        Spacer(modifier = Modifier.width(32.dp))

        // Right Column: Server Node List (D-Pad Navigable)
        Column(
            modifier = Modifier
                .weight(0.6f)
                .fillMaxHeight()
        ) {
            Text(
                text = "选择服务器节点 (${nodes.size})",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(nodes, key = { it.id }) { node ->
                    var isFocused by remember { mutableStateOf(false) }
                    val isSelected = selectedNode?.id == node.id

                    Card(
                        onClick = { onSelectNode(node) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusable()
                            .onFocusChanged { isFocused = it.isFocused }
                            .border(
                                BorderStroke(
                                    if (isFocused) 3.dp else 0.dp,
                                    if (isFocused) Color.White else Color.Transparent
                                ),
                                RoundedCornerShape(12.dp)
                            ),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = when {
                                isSelected -> MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
                                isFocused -> Color(0xFF3A3A3C)
                                else -> Color(0xFF1C1C1E)
                            }
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = node.name,
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.White
                                )
                                Text(
                                    text = "${node.protocol.displayName} • ${node.server}:${node.port}",
                                    fontSize = 12.sp,
                                    color = Color.Gray
                                )
                            }

                            if (isSelected) {
                                Text(
                                    text = "当前使用",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
