package com.shadowrocket.android.core

import android.content.Context
import com.shadowrocket.android.core.model.ShadowrocketConfig
import com.shadowrocket.android.core.parser.ShadowrocketConfParser
import java.io.File
import java.io.FileOutputStream

object AssetLoader {

    private const val DEFAULT_CONF_NAME = "default.conf"
    private const val MMDB_NAME = "GeoLite2-Country.mmdb"

    /**
     * Loads the default Shadowrocket configuration from assets.
     */
    fun loadDefaultConfig(context: Context): ShadowrocketConfig {
        return try {
            val content = context.assets.open(DEFAULT_CONF_NAME).bufferedReader().use { it.readText() }
            ShadowrocketConfParser.parse(content, DEFAULT_CONF_NAME)
        } catch (e: Exception) {
            e.printStackTrace()
            ShadowrocketConfig()
        }
    }

    /**
     * Extracts GeoLite2-Country.mmdb from assets to the app's files directory for local lookup.
     */
    fun ensureGeoIpDatabase(context: Context): File {
        val destFile = File(context.filesDir, MMDB_NAME)
        if (!destFile.exists() || destFile.length() == 0L) {
            try {
                context.assets.open(MMDB_NAME).use { input ->
                    FileOutputStream(destFile).use { output ->
                        input.copyTo(output)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        return destFile
    }
}
