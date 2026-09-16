package com.v2ray.ang.ui.main

import android.util.Base64
import com.v2ray.ang.handler.MmkvManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID

/**
 * Traffic / expiry / announce / title parsed from the subscription response headers.
 */
data class SubInfo(
    val used: Long,
    val total: Long,
    val expireEpochSec: Long,
    val announce: String?,
    val profileTitle: String?,
    val userId: String?
)

object SubInfoFetcher {
    private fun deviceHwid(): String {
        var hwid = MmkvManager.decodeSettingsString("device_hwid") ?: ""
        if (hwid.isBlank()) {
            hwid = UUID.randomUUID().toString()
            MmkvManager.encodeSettings("device_hwid", hwid)
        }
        return hwid
    }

    suspend fun fetch(url: String): SubInfo? = withContext(Dispatchers.IO) {
        var conn: HttpURLConnection? = null
        try {
            conn = (URL(url).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 8000
                readTimeout = 8000
                instanceFollowRedirects = true
                setRequestProperty("User-Agent", "v2rayNG")
                setRequestProperty("x-hwid", deviceHwid())
                setRequestProperty("x-device-os", "Android")
            }
            conn.connect()

            val userInfo = conn.getHeaderField("Subscription-Userinfo")
            val announce = decodeHeaderText(conn.getHeaderField("announce"))
            val profileTitle = decodeHeaderText(conn.getHeaderField("profile-title"))
            val userId = extractUserId(conn, url)

            if (userInfo.isNullOrBlank() && announce.isNullOrBlank() && profileTitle.isNullOrBlank() && userId.isNullOrBlank()) {
                return@withContext null
            }

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
                announce = announce,
                profileTitle = profileTitle,
                userId = userId
            )
        } catch (e: Exception) {
            null
        } finally {
            conn?.disconnect()
        }
    }

    private fun extractUserId(conn: HttpURLConnection, url: String): String? {
        val header = sequenceOf("x-user-id", "subscription-userid", "profile-id", "x-account")
            .map { conn.getHeaderField(it) }
            .firstOrNull { !it.isNullOrBlank() }
        if (!header.isNullOrBlank()) return header.trim()

        val cd = conn.getHeaderField("Content-Disposition")
        if (!cd.isNullOrBlank()) {
            val m = Regex("filename\\*?=\"?([^\";]+)\"?", RegexOption.IGNORE_CASE).find(cd)
            val name = m?.groupValues?.getOrNull(1)?.trim()
            if (!name.isNullOrBlank()) return name
        }

        return try {
            val path = URL(url).path.trimEnd('/')
            val seg = path.substringAfterLast('/')
            val decoded = java.net.URLDecoder.decode(seg, "UTF-8").trim()
            decoded.ifBlank { null }
        } catch (e: Exception) {
            null
        }
    }

    private fun isClean(text: String): Boolean =
        text.isNotBlank() && !text.contains('\uFFFD')

    private fun decodeHeaderText(raw: String?): String? {
        if (raw.isNullOrBlank()) return null
        var s = raw.trim()
        if (s.startsWith("rwEncodeBase64:")) {
            s = s.removePrefix("rwEncodeBase64:").trim()
        }

        val cleaned = s.replace(Regex("\\s"), "")
        for (flags in intArrayOf(Base64.DEFAULT, Base64.URL_SAFE)) {
            val text = try {
                String(Base64.decode(cleaned, flags or Base64.NO_WRAP), Charsets.UTF_8).trim()
            } catch (e: Exception) {
                ""
            }
            if (isClean(text)) return text
        }

        val recovered = try {
            String(raw.toByteArray(Charsets.ISO_8859_1), Charsets.UTF_8).trim()
        } catch (e: Exception) {
            ""
        }
        if (isClean(recovered) && recovered != raw.trim()) {
            return recovered.removePrefix("rwEncodeBase64:").trim()
        }

        return null
    }
}
