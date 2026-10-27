package com.shadowrocket.android.core.model

import kotlinx.serialization.Serializable

@Serializable
enum class RoutingMode(val title: String, val subtitle: String) {
    CONFIG("配置 (Config)", "根据规则列表进行智能路由分流"),
    PROXY("全局代理 (Proxy)", "所有网络流量强制走当前选中的代理节点"),
    DIRECT("全局直连 (Direct)", "所有网络流量直接连接，不通过代理"),
    SCENE("场景分流 (Scene)", "根据当前所连 Wi-Fi 或移动网络自动切换规则")
}

@Serializable
enum class RuleType {
    DOMAIN,
    DOMAIN_SUFFIX,
    DOMAIN_KEYWORD,
    IP_CIDR,
    IP_CIDR6,
    GEOIP,
    USER_AGENT,
    FINAL,
    RULE_SET;

    companion object {
        fun fromString(str: String): RuleType? = when (str.trim().uppercase()) {
            "DOMAIN" -> DOMAIN
            "DOMAIN-SUFFIX" -> DOMAIN_SUFFIX
            "DOMAIN-KEYWORD" -> DOMAIN_KEYWORD
            "IP-CIDR" -> IP_CIDR
            "IP-CIDR6" -> IP_CIDR6
            "GEOIP" -> GEOIP
            "USER-AGENT" -> USER_AGENT
            "FINAL" -> FINAL
            "RULE-SET" -> RULE_SET
            else -> null
        }
    }
}

@Serializable
enum class RulePolicy {
    DIRECT,
    PROXY,
    REJECT;

    companion object {
        fun fromString(str: String): RulePolicy = when (str.trim().uppercase()) {
            "DIRECT" -> DIRECT
            "REJECT", "REJECT-TINYGIF", "REJECT-DROP" -> REJECT
            else -> PROXY
        }
    }
}

@Serializable
data class RuleItem(
    val type: RuleType,
    val pattern: String,
    val policy: RulePolicy,
    val noResolve: Boolean = false,
    val comment: String? = null
)
