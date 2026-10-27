package com.shadowrocket.android.core.parser

import com.shadowrocket.android.core.model.*

/**
 * Parser for Shadowrocket / Surge `.conf` configuration files.
 * Accurately parses [General], [Rule], [Host], and [URL Rewrite] sections.
 */
object ShadowrocketConfParser {

    fun parse(content: String, fileName: String = "default.conf"): ShadowrocketConfig {
        var currentSection = ""
        val generalMap = mutableMapOf<String, String>()
        val rules = mutableListOf<RuleItem>()
        val hosts = mutableMapOf<String, String>()
        val urlRewrites = mutableListOf<String>()

        content.lines().forEach { rawLine ->
            val line = rawLine.trim()
            if (line.isEmpty() || line.startsWith("#") || line.startsWith(";")) {
                return@forEach
            }

            if (line.startsWith("[") && line.endsWith("]")) {
                currentSection = line.substring(1, line.length - 1).trim()
                return@forEach
            }

            when (currentSection.lowercase()) {
                "general" -> {
                    val parts = line.split("=", limit = 2)
                    if (parts.size == 2) {
                        generalMap[parts[0].trim()] = parts[1].trim()
                    }
                }
                "rule" -> {
                    parseRuleLine(line)?.let { rules.add(it) }
                }
                "host" -> {
                    val parts = line.split("=", limit = 2)
                    if (parts.size == 2) {
                        hosts[parts[0].trim()] = parts[1].trim()
                    }
                }
                "url rewrite" -> {
                    urlRewrites.add(line)
                }
            }
        }

        val generalConfig = GeneralConfig(
            bypassSystem = generalMap["bypass-system"]?.toBooleanStrictOrNull() ?: true,
            skipProxy = generalMap["skip-proxy"]?.split(",")?.map { it.trim() }
                ?: listOf("192.168.0.0/16", "10.0.0.0/8", "172.16.0.0/12", "localhost", "*.local"),
            tunExcludedRoutes = generalMap["tun-excluded-routes"]?.split(",")?.map { it.trim() } ?: emptyList(),
            dnsServer = generalMap["dns-server"] ?: "system",
            fallbackDnsServer = generalMap["fallback-dns-server"] ?: "1.1.1.1, 8.8.8.8",
            ipv6 = generalMap["ipv6"]?.toBooleanStrictOrNull() ?: true,
            preferIpv6 = generalMap["prefer-ipv6"]?.toBooleanStrictOrNull() ?: false,
            icmpAutoReply = generalMap["icmp-auto-reply"]?.toBooleanStrictOrNull() ?: true,
            privateIpAnswer = generalMap["private-ip-answer"]?.toBooleanStrictOrNull() ?: true,
            udpPolicyNotSupportedBehaviour = generalMap["udp-policy-not-supported-behaviour"] ?: "REJECT"
        )

        return ShadowrocketConfig(
            fileName = fileName,
            general = generalConfig,
            rules = rules,
            hosts = hosts,
            urlRewrites = urlRewrites
        )
    }

    private fun parseRuleLine(line: String): RuleItem? {
        val parts = line.split(",").map { it.trim() }
        if (parts.size < 2) return null

        val typeStr = parts[0]
        val type = RuleType.fromString(typeStr) ?: return null

        return if (type == RuleType.FINAL) {
            val policy = RulePolicy.fromString(parts[1])
            RuleItem(type = type, pattern = "", policy = policy)
        } else {
            if (parts.size < 3) return null
            val pattern = parts[1]
            val policy = RulePolicy.fromString(parts[2])
            val noResolve = parts.contains("no-resolve")
            RuleItem(type = type, pattern = pattern, policy = policy, noResolve = noResolve)
        }
    }

    fun serialize(config: ShadowrocketConfig): String {
        val sb = StringBuilder()
        sb.append("[General]\n")
        sb.append("bypass-system = ${config.general.bypassSystem}\n")
        sb.append("skip-proxy = ${config.general.skipProxy.joinToString(", ")}\n")
        sb.append("dns-server = ${config.general.dnsServer}\n")
        sb.append("ipv6 = ${config.general.ipv6}\n\n")

        sb.append("[Rule]\n")
        config.rules.forEach { r ->
            if (r.type == RuleType.FINAL) {
                sb.append("FINAL,${r.policy}\n")
            } else {
                val noRes = if (r.noResolve) ",no-resolve" else ""
                sb.append("${r.type.name.replace('_', '-')},${r.pattern},${r.policy}$noRes\n")
            }
        }
        sb.append("\n[Host]\n")
        config.hosts.forEach { (k, v) -> sb.append("$k = $v\n") }

        return sb.toString()
    }
}
