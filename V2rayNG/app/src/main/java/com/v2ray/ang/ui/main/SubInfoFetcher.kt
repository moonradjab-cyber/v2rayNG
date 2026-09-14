package com.v2ray.ang.ui.main

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

/**
 * Traffic / expiry info parsed from the standard "Subscription-Userinfo" response
 * header (used=upload+download, total, expire as epoch seconds; 0 = unlimited).
 */
data class SubInfo(
    val used: Long,
    val total: Long,
    val expireEpochSec: Long
)

object SubInfoFetcher {
    suspend fun fetch(url: String): SubInfo? = withContext(Dispatchers.IO) {
        var conn: HttpURLConnection? = null
        try {
            conn = (URL(url).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 8000
                readTimeout = 8000
                instanceFollowRedirects = true
                setRequestProperty("User-Agent", "v2rayNG")
            }
            conn.connect()
            val header = conn.getHeaderField("Subscription-Userinfo") ?: return@withContext null
            if (header.isBlank()) return@withContext null

            var upload = 0L
            var download = 0L
            var total = 0L
            var expire = 0L
            header.split(";").forEach { part ->
                val kv = part.trim().split("=", limit = 2)
                if (kv.size == 2) {
                    val value = kv[1].trim().toLongOrNull() ?: 0L
                    when (kv[0].trim().lowercase()) {
                        "upload" -> upload = value
                        "download" -> download = value
                        "total" -> total = value
                        "expire" -> expire = value
                    }
                }
            }
            SubInfo(used = upload + download, total = total, expireEpochSec = expire)
        } catch (e: Exception) {
            null
        } finally {
            conn?.disconnect()
        }
    }
}
