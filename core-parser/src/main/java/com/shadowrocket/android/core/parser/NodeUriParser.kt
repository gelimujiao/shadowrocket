package com.shadowrocket.android.core.parser

import android.net.Uri
import android.util.Base64
import com.shadowrocket.android.core.model.ProxyNode
import com.shadowrocket.android.core.model.ProxyProtocol
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.net.URLDecoder
import java.util.UUID

object NodeUriParser {

    private val json = Json { ignoreUnknownKeys = true }

    fun parse(uriString: String): ProxyNode? {
        val trimmed = uriString.trim()
        val colonIndex = trimmed.indexOf("://")
        if (colonIndex == -1) return null

        val scheme = trimmed.substring(0, colonIndex).lowercase()
        val protocol = ProxyProtocol.fromScheme(scheme) ?: return null

        return try {
            when (protocol) {
                ProxyProtocol.SHADOWSOCKS -> parseShadowsocks(trimmed)
                ProxyProtocol.VMESS -> parseVmess(trimmed)
                ProxyProtocol.VLESS -> parseVless(trimmed)
                ProxyProtocol.TROJAN -> parseTrojan(trimmed)
                ProxyProtocol.HYSTERIA2 -> parseHysteria2(trimmed)
                ProxyProtocol.TUIC -> parseTuic(trimmed)
                else -> null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun parseShadowsocks(uriString: String): ProxyNode {
        val raw = uriString.substring("ss://".length)
        val hashIdx = raw.indexOf("#")
        val name = if (hashIdx != -1) URLDecoder.decode(raw.substring(hashIdx + 1), "UTF-8") else "Shadowsocks"
        val mainPart = if (hashIdx != -1) raw.substring(0, hashIdx) else raw

        // SIP002: ss://base64(method:password)@server:port
        if (mainPart.contains("@")) {
            val atIdx = mainPart.indexOf("@")
            val userInfo = decodeBase64Safe(mainPart.substring(0, atIdx))
            val hostPort = mainPart.substring(atIdx + 1)
            val credParts = userInfo.split(":", limit = 2)
            val hpParts = hostPort.split(":", limit = 2)
            return ProxyNode(
                id = UUID.randomUUID().toString(),
                name = name,
                protocol = ProxyProtocol.SHADOWSOCKS,
                server = hpParts[0],
                port = hpParts[1].toInt(),
                method = credParts.getOrNull(0) ?: "",
                password = credParts.getOrNull(1) ?: ""
            )
        } else {
            // Legacy base64
            val decoded = decodeBase64Safe(mainPart)
            val atIdx = decoded.indexOf("@")
            val credParts = decoded.substring(0, atIdx).split(":", limit = 2)
            val hpParts = decoded.substring(atIdx + 1).split(":", limit = 2)
            return ProxyNode(
                id = UUID.randomUUID().toString(),
                name = name,
                protocol = ProxyProtocol.SHADOWSOCKS,
                server = hpParts[0],
                port = hpParts[1].toInt(),
                method = credParts[0],
                password = credParts[1]
            )
        }
    }

    private fun parseVmess(uriString: String): ProxyNode {
        val base64Data = uriString.substring("vmess://".length).trim()
        val jsonStr = decodeBase64Safe(base64Data)
        val obj = json.parseToJsonElement(jsonStr).jsonObject

        val name = obj["ps"]?.jsonPrimitive?.content ?: "VMess"
        val server = obj["add"]?.jsonPrimitive?.content ?: ""
        val port = obj["port"]?.jsonPrimitive?.content?.toIntOrNull() ?: 443
        val uuid = obj["id"]?.jsonPrimitive?.content ?: ""
        val aid = obj["aid"]?.jsonPrimitive?.content?.toIntOrNull() ?: 0
        val net = obj["net"]?.jsonPrimitive?.content ?: "tcp"
        val path = obj["path"]?.jsonPrimitive?.content ?: ""
        val host = obj["host"]?.jsonPrimitive?.content ?: ""
        val tls = obj["tls"]?.jsonPrimitive?.content?.equals("tls", ignoreCase = true) ?: false
        val sni = obj["sni"]?.jsonPrimitive?.content ?: host

        return ProxyNode(
            id = UUID.randomUUID().toString(),
            name = name,
            protocol = ProxyProtocol.VMESS,
            server = server,
            port = port,
            uuid = uuid,
            alterId = aid,
            network = net,
            wsPath = path,
            wsHost = host,
            tlsEnabled = tls,
            sni = sni
        )
    }

    private fun parseVless(uriString: String): ProxyNode {
        val uri = Uri.parse(uriString)
        val name = uri.fragment?.let { URLDecoder.decode(it, "UTF-8") } ?: "VLESS"
        val uuid = uri.userInfo ?: ""
        val server = uri.host ?: ""
        val port = if (uri.port != -1) uri.port else 443

        val security = uri.getQueryParameter("security") ?: "none"
        val tlsEnabled = security in listOf("tls", "reality")
        val flow = uri.getQueryParameter("flow") ?: ""
        val sni = uri.getQueryParameter("sni") ?: ""
        val pbk = uri.getQueryParameter("pbk") ?: ""
        val sid = uri.getQueryParameter("sid") ?: ""
        val net = uri.getQueryParameter("type") ?: "tcp"
        val path = uri.getQueryParameter("path") ?: ""

        return ProxyNode(
            id = UUID.randomUUID().toString(),
            name = name,
            protocol = ProxyProtocol.VLESS,
            server = server,
            port = port,
            uuid = uuid,
            flow = flow,
            tlsEnabled = tlsEnabled,
            sni = sni,
            realityPublicKey = pbk,
            realityShortId = sid,
            network = net,
            wsPath = path
        )
    }

    private fun parseTrojan(uriString: String): ProxyNode {
        val uri = Uri.parse(uriString)
        val name = uri.fragment?.let { URLDecoder.decode(it, "UTF-8") } ?: "Trojan"
        val password = uri.userInfo ?: ""
        val server = uri.host ?: ""
        val port = if (uri.port != -1) uri.port else 443
        val sni = uri.getQueryParameter("sni") ?: server

        return ProxyNode(
            id = UUID.randomUUID().toString(),
            name = name,
            protocol = ProxyProtocol.TROJAN,
            server = server,
            port = port,
            password = password,
            tlsEnabled = true,
            sni = sni
        )
    }

    private fun parseHysteria2(uriString: String): ProxyNode {
        val uri = Uri.parse(uriString)
        val name = uri.fragment?.let { URLDecoder.decode(it, "UTF-8") } ?: "Hysteria 2"
        val password = uri.userInfo ?: ""
        val server = uri.host ?: ""
        val port = if (uri.port != -1) uri.port else 443
        val sni = uri.getQueryParameter("sni") ?: server

        return ProxyNode(
            id = UUID.randomUUID().toString(),
            name = name,
            protocol = ProxyProtocol.HYSTERIA2,
            server = server,
            port = port,
            password = password,
            tlsEnabled = true,
            sni = sni
        )
    }

    private fun parseTuic(uriString: String): ProxyNode {
        val uri = Uri.parse(uriString)
        val name = uri.fragment?.let { URLDecoder.decode(it, "UTF-8") } ?: "TUIC v5"
        val parts = (uri.userInfo ?: "").split(":", limit = 2)
        val uuid = parts.getOrNull(0) ?: ""
        val password = parts.getOrNull(1) ?: ""
        val server = uri.host ?: ""
        val port = if (uri.port != -1) uri.port else 443
        val sni = uri.getQueryParameter("sni") ?: server

        return ProxyNode(
            id = UUID.randomUUID().toString(),
            name = name,
            protocol = ProxyProtocol.TUIC,
            server = server,
            port = port,
            uuid = uuid,
            password = password,
            tlsEnabled = true,
            sni = sni
        )
    }

    private fun decodeBase64Safe(input: String): String {
        var str = input.replace("-", "+").replace("_", "/")
        val pad = str.length % 4
        if (pad != 0) str += "=".repeat(4 - pad)
        return String(Base64.decode(str, Base64.DEFAULT), Charsets.UTF_8)
    }
}
