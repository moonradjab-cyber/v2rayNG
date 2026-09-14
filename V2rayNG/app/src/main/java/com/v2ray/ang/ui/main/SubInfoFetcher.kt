package com.v2ray.ang.ui.main

import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

/**
 * Traffic / expiry / announce info parsed from the subscription response headers.
 * - used = upload + download, total, expire (epoch seconds; 0 = unlimited) come
 *   from the standard "Subscription-Userinfo" header.
 * - announce is the Remnawave "announce" header (base64), already filled with the
 *   user's real days left and ID by the panel; shown as-is.
 */
data class SubInfo(
    val used: Long,
    val total: Long,
    val expireEpochSec: Long,
    val announce: String?
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

            val userInfo = conn.getHeaderField("Subscription-Userinfo")
            val announce = decodeAnnounce(conn.getHeaderField("announce"))

            if (userInfo.isNullOrBlank() && announce.isNullOrBlank()) return@withContext null

            var upload = 0L
            var download = 0L
            var total = 0L
            var expire = 0L
            userInfo?.split(";")?.forEach { part ->
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
            SubInfo(
                used = upload + download,
                total = total,
                expireEpochSec = expire,
                announce = announce
            )
        } catch (e: Exception) {
            null
        } finally {
            conn?.disconnect()
        }
    }

    private fun decodeAnnounce(raw: String?): String? {
        if (raw.isNullOrBlank()) return null
        var s = raw.trim()
        if (s.startsWith("rwEncodeBase64:")) {
            s = s.substring("rwEncodeBase64:".length).trim()
        }
        return try {
            val bytes = Base64.decode(s, Base64.DEFAULT)
            val text = String(bytes, Charsets.UTF_8).trim()
            text.ifBlank { null }
        } catch (e: Exception) {
            raw.trim().ifBlank { null }
        }
    }
}
