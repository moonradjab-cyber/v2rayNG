package com.v2ray.ang.handler

import android.content.Context
import com.v2ray.ang.AppConfig
import com.v2ray.ang.core.CoreServiceManager
import com.v2ray.ang.core.LauncherManager
import com.v2ray.ang.util.LogUtil
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.net.HttpURLConnection
import java.net.URL

/**
 * Optional server failover. While the VPN is connected, periodically probes internet
 * reachability (traffic goes through the active server). If the current server stops
 * responding [FAIL_THRESHOLD] times in a row, switches to the next server in the same
 * group and restarts the tunnel.
 *
 * Gated behind the "pref_auto_failover" setting so it can be turned off if it misbehaves
 * on a given device/ROM. Started/stopped together with the core service.
 */
object FailoverMonitor {

    private const val PREF_KEY = "pref_auto_failover"
    private const val CHECK_INTERVAL_MS = 25_000L
    private const val SETTLE_DELAY_MS = 40_000L
    private const val FAIL_THRESHOLD = 3
    private const val TEST_URL = "http://cp.cloudflare.com/generate_204"
    private const val TIMEOUT_MS = 6000

    @Volatile
    private var job: Job? = null

    fun start(context: Context) {
        if (MmkvManager.decodeSettingsBool(PREF_KEY, true) != true) return
        if (job != null) return
        val appContext = context.applicationContext
        job = CoroutineScope(Dispatchers.IO).launch {
            // Let a freshly (re)started tunnel settle before judging it.
            delay(SETTLE_DELAY_MS)
            var fails = 0
            while (isActive && CoreServiceManager.isRunning()) {
                if (probe()) {
                    fails = 0
                } else {
                    fails++
                    if (fails >= FAIL_THRESHOLD) {
                        if (switchToNextServer(appContext)) {
                            // Service is restarting; this loop is replaced by a fresh one.
                            return@launch
                        }
                        fails = 0
                    }
                }
                delay(CHECK_INTERVAL_MS)
            }
        }
    }

    fun stop() {
        job?.cancel()
        job = null
    }

    private fun probe(): Boolean {
        var conn: HttpURLConnection? = null
        return try {
            conn = (URL(TEST_URL).openConnection() as HttpURLConnection).apply {
                connectTimeout = TIMEOUT_MS
                readTimeout = TIMEOUT_MS
                instanceFollowRedirects = false
                requestMethod = "GET"
                useCaches = false
            }
            val code = conn.responseCode
            code in 200..399
        } catch (e: Exception) {
            false
        } finally {
            conn?.disconnect()
        }
    }

    private fun switchToNextServer(context: Context): Boolean {
        val currentGuid = MmkvManager.getSelectServer() ?: return false
        val current = MmkvManager.decodeServerConfig(currentGuid) ?: return false
        val guids = MmkvManager.decodeServerList(current.subscriptionId)
        if (guids.size < 2) return false
        val idx = guids.indexOf(currentGuid)
        if (idx < 0) return false
        val next = guids[(idx + 1) % guids.size]
        if (next == currentGuid) return false

        MmkvManager.setSelectServer(next)
        LogUtil.i(AppConfig.TAG, "FailoverMonitor: current server unreachable, switching to next")
        LauncherManager.restartService(context)
        return true
    }
}
