package com.zivpn.custom

import android.net.Uri

object ConfigUtils {
    fun parseZivpnUrl(url: String): VpnConfig? {
        if (!url.startsWith("zivpn://")) return null

        try {
            // Format: zivpn://server@auth
            val content = url.substring(8)
            val parts = content.split("@")
            if (parts.size < 2) return null

            return VpnConfig(
                serverHost = parts[0],
                authUser = parts[1]
            )
        } catch (e: Exception) {
            return null
        }
    }

    fun parseMultiLineImport(input: String): List<VpnConfig> {
        val configs = mutableListOf<VpnConfig>()
        val lines = input.split("\n")
        for (line in lines) {
            val trimmed = line.trim()
            if (trimmed.startsWith("zivpn://")) {
                parseZivpnUrl(trimmed)?.let { configs.add(it) }
            }
        }
        return configs
    }

    fun serializeConfig(config: VpnConfig): String {
        return "zivpn://${config.serverHost}@${config.authUser}"
    }
}
