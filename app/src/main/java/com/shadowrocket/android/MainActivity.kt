package com.shadowrocket.android

import android.app.UiModeManager
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.net.VpnService
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import com.shadowrocket.android.core.AssetLoader
import com.shadowrocket.android.core.model.*
import com.shadowrocket.android.core.parser.ShadowrocketConfParser
import com.shadowrocket.android.core.parser.SingBoxConfigGenerator
import com.shadowrocket.android.core.vpn.ConnectionStatus
import com.shadowrocket.android.core.vpn.ShadowrocketVpnService
import com.shadowrocket.android.core.vpn.VpnStateHolder
import com.shadowrocket.android.ui.screens.*
import com.shadowrocket.android.ui.theme.ShadowrocketTheme
import com.shadowrocket.android.ui.tv.TvHomeScreen

enum class Screen(val title: String, val icon: ImageVector) {
    HOME("首页", Icons.Default.RocketLaunch),
    CONFIG("配置", Icons.Default.Tune),
    PER_APP("分应用", Icons.Default.Apps),
    SETTINGS("设置", Icons.Default.Settings)
}

class MainActivity : ComponentActivity() {

    private val vpnPrepareLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            launchVpnService()
        }
    }

    private var pendingConfigJson: String = ""
    private var pendingNodeName: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val isTv = isRunningOnTv()

        setContent {
            ShadowrocketTheme {
                val connectionStatus by VpnStateHolder.status.collectAsState()
                val trafficStats by VpnStateHolder.traffic.collectAsState()

                // Load real default.conf extracted from Shadowrocket assets
                val sampleConfig = remember {
                    AssetLoader.ensureGeoIpDatabase(this@MainActivity)
                    AssetLoader.loadDefaultConfig(this@MainActivity)
                }

                var nodes by remember {
                    mutableStateOf(
                        listOf(
                            ProxyNode(
                                id = "1",
                                name = "香港 01 | 高速专线",
                                protocol = ProxyProtocol.VLESS,
                                server = "hk.example.com",
                                port = 443,
                                uuid = "a1b2c3d4-e5f6-7890-1234-56789abcdef0",
                                tlsEnabled = true,
                                sni = "hk.example.com",
                                pingMs = 38L
                            ),
                            ProxyNode(
                                id = "2",
                                name = "日本 02 | 4K流媒体",
                                protocol = ProxyProtocol.HYSTERIA2,
                                server = "jp.example.com",
                                port = 8443,
                                password = "securepassword",
                                tlsEnabled = true,
                                sni = "jp.example.com",
                                pingMs = 62L
                            ),
                            ProxyNode(
                                id = "3",
                                name = "美国 01 | AI解锁专线",
                                protocol = ProxyProtocol.TROJAN,
                                server = "us.example.com",
                                port = 443,
                                password = "trojanpassword",
                                tlsEnabled = true,
                                sni = "us.example.com",
                                pingMs = 158L
                            ),
                            ProxyNode(
                                id = "4",
                                name = "新加坡 01 | 直连优化",
                                protocol = ProxyProtocol.SHADOWSOCKS,
                                server = "sg.example.com",
                                port = 8388,
                                method = "2022-blake3-aes-128-gcm",
                                password = "secretpassword",
                                pingMs = 75L
                            )
                        )
                    )
                }

                var selectedNode by remember { mutableStateOf(nodes.firstOrNull()) }
                var routingMode by remember { mutableStateOf(RoutingMode.CONFIG) }
                var currentScreen by remember { mutableStateOf(Screen.HOME) }

                // Per-app sample list
                var appList by remember {
                    mutableStateOf(
                        listOf(
                            AppInfo("com.google.android.youtube", "YouTube", false, true),
                            AppInfo("org.telegram.messenger", "Telegram", false, true),
                            AppInfo("com.twitter.android", "X (Twitter)", false, true),
                            AppInfo("com.tencent.mm", "微信", false, false),
                            AppInfo("com.eg.android.AlipayGphone", "支付宝", false, false),
                            AppInfo("com.netease.cloudmusic", "网易云音乐", false, false),
                            AppInfo("tv.danmaku.bili", "哔哩哔哩", false, false)
                        )
                    )
                }

                fun toggleVpn() {
                    val active = selectedNode ?: return
                    if (connectionStatus == ConnectionStatus.CONNECTED || connectionStatus == ConnectionStatus.CONNECTING) {
                        val stopIntent = Intent(this@MainActivity, ShadowrocketVpnService::class.java).apply {
                            action = ShadowrocketVpnService.ACTION_STOP
                        }
                        startService(stopIntent)
                    } else {
                        VpnStateHolder.setActiveNode(active)
                        val allowed = appList.filter { it.isProxyAllowed }.map { it.packageName }
                        pendingConfigJson = SingBoxConfigGenerator.generate(
                            selectedNode = active,
                            routingMode = routingMode,
                            config = sampleConfig,
                            allowedApps = allowed
                        )
                        pendingNodeName = active.name

                        val prepareIntent = VpnService.prepare(this@MainActivity)
                        if (prepareIntent != null) {
                            vpnPrepareLauncher.launch(prepareIntent)
                        } else {
                            launchVpnService()
                        }
                    }
                }

                if (isTv) {
                    // Android TV Dedicated Interface
                    TvHomeScreen(
                        connectionStatus = connectionStatus,
                        selectedNode = selectedNode,
                        nodes = nodes,
                        onToggleVpn = { toggleVpn() },
                        onSelectNode = { selectedNode = it }
                    )
                } else {
                    // Responsive Phone & Tablet Navigation
                    val configuration = LocalConfiguration.current
                    val isTablet = configuration.screenWidthDp >= 600

                    Scaffold(
                        bottomBar = {
                            if (!isTablet) {
                                NavigationBar {
                                    Screen.entries.forEach { screen ->
                                        NavigationBarItem(
                                            icon = { Icon(screen.icon, contentDescription = screen.title) },
                                            label = { Text(screen.title) },
                                            selected = currentScreen == screen,
                                            onClick = { currentScreen = screen }
                                        )
                                    }
                                }
                            }
                        }
                    ) { innerPadding ->
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(innerPadding)
                        ) {
                            if (isTablet) {
                                NavigationRail {
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Screen.entries.forEach { screen ->
                                        NavigationRailItem(
                                            icon = { Icon(screen.icon, contentDescription = screen.title) },
                                            label = { Text(screen.title) },
                                            selected = currentScreen == screen,
                                            onClick = { currentScreen = screen }
                                        )
                                    }
                                }
                            }

                            Box(modifier = Modifier.weight(1f)) {
                                when (currentScreen) {
                                    Screen.HOME -> HomeScreen(
                                        connectionStatus = connectionStatus,
                                        trafficStats = trafficStats,
                                        selectedNode = selectedNode,
                                        nodes = nodes,
                                        routingMode = routingMode,
                                        onToggleVpn = { toggleVpn() },
                                        onSelectNode = { selectedNode = it },
                                        onSelectMode = { routingMode = it },
                                        onTestAllPing = {
                                            nodes = nodes.map { it.copy(pingMs = (30L..180L).random()) }
                                        }
                                    )
                                    Screen.CONFIG -> ConfigScreen(
                                        currentConfig = sampleConfig,
                                        onEditRules = { /* Open rule editor */ }
                                    )
                                    Screen.PER_APP -> PerAppProxyScreen(
                                        appList = appList,
                                        onToggleAppProxy = { pkg, allowed ->
                                            appList = appList.map {
                                                if (it.packageName == pkg) it.copy(isProxyAllowed = allowed) else it
                                            }
                                        },
                                        onSelectAll = { allowed ->
                                            appList = appList.map { it.copy(isProxyAllowed = allowed) }
                                        }
                                    )
                                    Screen.SETTINGS -> SettingsScreen()
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun launchVpnService() {
        val startIntent = Intent(this, ShadowrocketVpnService::class.java).apply {
            action = ShadowrocketVpnService.ACTION_START
            putExtra(ShadowrocketVpnService.EXTRA_CONFIG_JSON, pendingConfigJson)
            putExtra(ShadowrocketVpnService.EXTRA_NODE_NAME, pendingNodeName)
        }
        startService(startIntent)
    }

    private fun isRunningOnTv(): Boolean {
        val uiModeManager = getSystemService(Context.UI_MODE_SERVICE) as UiModeManager
        return uiModeManager.currentModeType == Configuration.UI_MODE_TYPE_TELEVISION
    }
}
