package com.v2ray.ang.ui.main

import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

/**
 * Traffic / expiry / announce info parsed from the subscription response headers.
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

    private fun isClean(text: String): Boolean =
        text.isNotBlank() && !text.contains('\uFFFD')

    private fun decodeAnnounce(raw: String?): String? {
        if (raw.isNullOrBlank()) return null
        var s = raw.trim()
        if (s.startsWith("rwEncodeBase64:")) {
            s = s.removePrefix("rwEncodeBase64:").trim()
        }

        // 1) Try base64 (standard, then URL-safe). Accept only clean UTF-8 text.
        val cleaned = s.replace(Regex("\\s"), "")
        for (flags in intArrayOf(Base64.DEFAULT, Base64.URL_SAFE)) {
            val text = try {
                String(Base64.decode(cleaned, flags or Base64.NO_WRAP), Charsets.UTF_8).trim()
            } catch (e: Exception) {
                ""
            }
            if (isClean(text)) return text
        }

        // 2) Maybe the header carried raw UTF-8 read as ISO-8859-1 — recover it.
        val recovered = try {
            String(raw.toByteArray(Charsets.ISO_8859_1), Charsets.UTF_8).trim()
        } catch (e: Exception) {
            ""
        }
        if (isClean(recovered) && recovered != raw.trim()) {
            return recovered.removePrefix("rwEncodeBase64:").trim()
        }

        // 3) Could not decode reliably — let the UI show its static fallback text.
        return null
    }
}
