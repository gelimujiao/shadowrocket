package com.shadowrocket.android.core.model

import kotlinx.serialization.Serializable

@Serializable
enum class ProxyProtocol(val scheme: String, val displayName: String) {
    SHADOWSOCKS("ss", "Shadowsocks"),
    SHADOWSOCKS_R("ssr", "ShadowsocksR"),
    VMESS("vmess", "VMess"),
    VLESS("vless", "VLESS"),
    TROJAN("trojan", "Trojan"),
    HYSTERIA2("hysteria2", "Hysteria 2"),
    TUIC("tuic", "TUIC v5"),
    WIREGUARD("wireguard", "WireGuard"),
    SNELL("snell", "Snell"),
    HTTP("http", "HTTP(S)"),
    SOCKS5("socks5", "SOCKS5");

    companion object {
        fun fromScheme(scheme: String): ProxyProtocol? {
            val lower = scheme.lowercase().removeSuffix("://")
            return entries.firstOrNull { it.scheme == lower }
                ?: when (lower) {
                    "hy2" -> HYSTERIA2
                    "hysteria" -> HYSTERIA2
                    "socks" -> SOCKS5
                    "https" -> HTTP
                    else -> null
                }
        }
    }
}

@Serializable
data class ProxyNode(
    val id: String,
    val name: String,
    val protocol: ProxyProtocol,
    val server: String,
    val port: Int,
    val password: String = "",
    val method: String = "", // encryption method (e.g., aes-128-gcm, chacha20-poly1305)
    val uuid: String = "", // for vmess / vless
    val alterId: Int = 0,
    val network: String = "tcp", // tcp, ws, grpc, h2, quic
    val wsPath: String = "",
    val wsHost: String = "",
    val tlsEnabled: Boolean = false,
    val sni: String = "",
    val alpn: List<String> = emptyList(),
    val flow: String = "", // xtls-rprx-vision
    val realityPublicKey: String = "",
    val realityShortId: String = "",
    val obfs: String = "", // http, tls, simple-obfs
    val obfsHost: String = "",
    val pingMs: Long = -1L, // -1 means untested, -2 timeout
    val subscriptionUrl: String? = null,
    val group: String = "Default"
)
