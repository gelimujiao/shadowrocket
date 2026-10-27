package com.shadowrocket.android.core.model

import kotlinx.serialization.Serializable

@Serializable
data class GeneralConfig(
    val bypassSystem: Boolean = true,
    val skipProxy: List<String> = listOf("192.168.0.0/16", "10.0.0.0/8", "172.16.0.0/12", "localhost", "*.local"),
    val tunExcludedRoutes: List<String> = emptyList(),
    val dnsServer: String = "system",
    val fallbackDnsServer: String = "1.1.1.1, 8.8.8.8",
    val ipv6: Boolean = true,
    val preferIpv6: Boolean = false,
    val icmpAutoReply: Boolean = true,
    val privateIpAnswer: Boolean = true,
    val udpPolicyNotSupportedBehaviour: String = "REJECT"
)

@Serializable
data class ShadowrocketConfig(
    val fileName: String = "default.conf",
    val general: GeneralConfig = GeneralConfig(),
    val rules: List<RuleItem> = emptyList(),
    val hosts: Map<String, String> = emptyMap(),
    val urlRewrites: List<String> = emptyList()
)

@Serializable
data class AppInfo(
    val packageName: String,
    val appName: String,
    val isSystemApp: Boolean = false,
    val isProxyAllowed: Boolean = true // Per-App Proxy flag
)
