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

/**
 * "Auto" server mode. While enabled (pref_auto_failover) and the VPN is connected,
 * periodically measures the real ping of the active server. If the server is
 * unreachable (ping < 0) or too slow (ping > [PING_THRESHOLD_MS]) for a few checks
 * in a row, switches to the next server in the same group and restarts the tunnel.
 *
 * Enabled by the "Авто" item in the server list (and the settings toggle). Off by default.
 */
object FailoverMonitor {

    private const val PREF_KEY = "pref_auto_failover"
    private const val CHECK_INTERVAL_MS = 25_000L
    private const val SETTLE_DELAY_MS = 40_000L
    private const val FAIL_THRESHOLD = 2
    private const val PING_THRESHOLD_MS = 500L

    @Volatile
    private var job: Job? = null

    fun start(context: Context) {
        if (MmkvManager.decodeSettingsBool(PREF_KEY, false) != true) return
        if (!CoreServiceManager.isRunning()) return
        if (job != null) return
        val appContext = context.applicationContext
        job = CoroutineScope(Dispatchers.IO).launch {
            // Let a freshly (re)started tunnel settle before judging it.
            delay(SETTLE_DELAY_MS)
            var fails = 0
            while (isActive && CoreServiceManager.isRunning()) {
                val ping = CoreServiceManager.measureCurrentDelay()
                val bad = ping < 0L || ping > PING_THRESHOLD_MS
                if (!bad) {
                    fails = 0
                } else {
                    fails++
                    LogUtil.i(AppConfig.TAG, "FailoverMonitor: bad ping=$ping ($fails/$FAIL_THRESHOLD)")
                    if (fails >= FAIL_THRESHOLD) {
                        if (switchToNextServer(appContext)) {
                            // Service is restarting; this loop is replaced by a fresh one.
                            job = null
                            return@launch
                        }
                        fails = 0
                    }
                }
                delay(CHECK_INTERVAL_MS)
            }
            job = null
        }
    }

    fun stop() {
        job?.cancel()
        job = null
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
        LogUtil.i(AppConfig.TAG, "FailoverMonitor: switching to next server")
        LauncherManager.restartService(context)
        return true
    }
}
