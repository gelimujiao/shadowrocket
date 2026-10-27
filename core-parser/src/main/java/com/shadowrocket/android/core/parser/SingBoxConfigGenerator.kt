package com.shadowrocket.android.core.parser

import com.shadowrocket.android.core.model.*
import kotlinx.serialization.json.*

/**
 * Generates runtime JSON configuration for sing-box core
 * based on selected ProxyNode, RoutingMode, and ShadowrocketConfig.
 */
object SingBoxConfigGenerator {

    fun generate(
        selectedNode: ProxyNode,
        routingMode: RoutingMode,
        config: ShadowrocketConfig,
        allowedApps: List<String> = emptyList(),
        disallowedApps: List<String> = emptyList()
    ): String {
        val root = buildJsonObject {
            putJsonObject("log") {
                put("level", "warn")
                put("timestamp", true)
            }

            // Inbound: TUN device
            putJsonArray("inbounds") {
                addJsonObject {
                    put("type", "tun")
                    put("tag", "tun-in")
                    put("interface_name", "tun0")
                    put("inet4_address", "172.19.0.1/30")
                    put("inet6_address", "fdfe:dcba:9876::1/126")
                    put("auto_route", true)
                    put("strict_route", false)
                    put("stack", "gvisor")
                    put("sniff", true)

                    // Per-App Proxy configuration (Android-exclusive)
                    if (allowedApps.isNotEmpty()) {
                        putJsonArray("include_package") {
                            allowedApps.forEach { add(it) }
                        }
                    } else if (disallowedApps.isNotEmpty()) {
                        putJsonArray("exclude_package") {
                            disallowedApps.forEach { add(it) }
                        }
                    }
                }
            }

            // Outbounds: Proxy node, Direct, Block/Reject
            putJsonArray("outbounds") {
                add(buildOutbound(selectedNode))
                addJsonObject {
                    put("type", "direct")
                    put("tag", "direct")
                }
                addJsonObject {
                    put("type", "block")
                    put("tag", "block")
                }
                addJsonObject {
                    put("type", "dns")
                    put("tag", "dns-out")
                }
            }

            // DNS Configuration
            putJsonObject("dns") {
                putJsonArray("servers") {
                    addJsonObject {
                        put("tag", "remote-dns")
                        put("address", "https://1.1.1.1/dns-query")
                        put("detour", "proxy")
                    }
                    addJsonObject {
                        put("tag", "local-dns")
                        put("address", "223.5.5.5")
                        put("detour", "direct")
                    }
                }
            }

            // Route rules
            putJsonObject("route") {
                putJsonArray("rules") {
                    // Protocol specific sniffs
                    addJsonObject {
                        put("protocol", "dns")
                        put("outbound", "dns-out")
                    }

                    when (routingMode) {
                        RoutingMode.PROXY -> {
                            addJsonObject {
                                put("outbound", "proxy")
                            }
                        }
                        RoutingMode.DIRECT -> {
                            addJsonObject {
                                put("outbound", "direct")
                            }
                        }
                        RoutingMode.CONFIG, RoutingMode.SCENE -> {
                            // Translate Shadowrocket rules to sing-box route rules
                            config.rules.forEach { rule ->
                                val targetOutbound = when (rule.policy) {
                                    RulePolicy.DIRECT -> "direct"
                                    RulePolicy.PROXY -> "proxy"
                                    RulePolicy.REJECT -> "block"
                                }

                                when (rule.type) {
                                    RuleType.DOMAIN -> addJsonObject {
                                        putJsonArray("domain") { add(rule.pattern) }
                                        put("outbound", targetOutbound)
                                    }
                                    RuleType.DOMAIN_SUFFIX -> addJsonObject {
                                        putJsonArray("domain_suffix") { add(rule.pattern) }
                                        put("outbound", targetOutbound)
                                    }
                                    RuleType.DOMAIN_KEYWORD -> addJsonObject {
                                        putJsonArray("domain_keyword") { add(rule.pattern) }
                                        put("outbound", targetOutbound)
                                    }
                                    RuleType.IP_CIDR, RuleType.IP_CIDR6 -> addJsonObject {
                                        putJsonArray("ip_cidr") { add(rule.pattern) }
                                        put("outbound", targetOutbound)
                                    }
                                    RuleType.GEOIP -> addJsonObject {
                                        putJsonArray("geoip") { add(rule.pattern.lowercase()) }
                                        put("outbound", targetOutbound)
                                    }
                                    RuleType.FINAL -> addJsonObject {
                                        put("outbound", targetOutbound)
                                    }
                                    else -> {}
                                }
                            }
                        }
                    }
                }
                put("final", if (routingMode == RoutingMode.PROXY) "proxy" else "direct")
                put("auto_detect_interface", true)
            }
        }

        return root.toString()
    }

    private fun buildOutbound(node: ProxyNode): JsonObject {
        return buildJsonObject {
            put("tag", "proxy")
            when (node.protocol) {
                ProxyProtocol.SHADOWSOCKS -> {
                    put("type", "shadowsocks")
                    put("server", node.server)
                    put("server_port", node.port)
                    put("method", node.method)
                    put("password", node.password)
                }
                ProxyProtocol.VMESS -> {
                    put("type", "vmess")
                    put("server", node.server)
                    put("server_port", node.port)
                    put("uuid", node.uuid)
                    put("alter_id", node.alterId)
                    put("security", "auto")
                    if (node.tlsEnabled) {
                        putJsonObject("tls") {
                            put("enabled", true)
                            put("server_name", node.sni)
                        }
                    }
                    if (node.network == "ws") {
                        putJsonObject("transport") {
                            put("type", "ws")
                            put("path", node.wsPath)
                            if (node.wsHost.isNotEmpty()) {
                                putJsonObject("headers") {
                                    put("Host", node.wsHost)
                                }
                            }
                        }
                    }
                }
                ProxyProtocol.VLESS -> {
                    put("type", "vless")
                    put("server", node.server)
                    put("server_port", node.port)
                    put("uuid", node.uuid)
                    if (node.flow.isNotEmpty()) {
                        put("flow", node.flow)
                    }
                    if (node.tlsEnabled) {
                        putJsonObject("tls") {
                            put("enabled", true)
                            put("server_name", node.sni)
                            if (node.realityPublicKey.isNotEmpty()) {
                                putJsonObject("reality") {
                                    put("enabled", true)
                                    put("public_key", node.realityPublicKey)
                                    put("short_id", node.realityShortId)
                                }
                            }
                        }
                    }
                }
                ProxyProtocol.TROJAN -> {
                    put("type", "trojan")
                    put("server", node.server)
                    put("server_port", node.port)
                    put("password", node.password)
                    putJsonObject("tls") {
                        put("enabled", true)
                        put("server_name", node.sni)
                    }
                }
                ProxyProtocol.HYSTERIA2 -> {
                    put("type", "hysteria2")
                    put("server", node.server)
                    put("server_port", node.port)
                    put("password", node.password)
                    putJsonObject("tls") {
                        put("enabled", true)
                        put("server_name", node.sni)
                    }
                }
                ProxyProtocol.TUIC -> {
                    put("type", "tuic")
                    put("server", node.server)
                    put("server_port", node.port)
                    put("uuid", node.uuid)
                    put("password", node.password)
                    putJsonObject("tls") {
                        put("enabled", true)
                        put("server_name", node.sni)
                    }
                }
                else -> {
                    put("type", "socks")
                    put("server", node.server)
                    put("server_port", node.port)
                }
            }
        }
    }
}
