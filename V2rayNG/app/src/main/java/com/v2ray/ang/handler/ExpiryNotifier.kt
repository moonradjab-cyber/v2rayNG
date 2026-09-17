package com.v2ray.ang.handler

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.v2ray.ang.AngApplication
import com.v2ray.ang.AppConfig
import com.v2ray.ang.R
import com.v2ray.ang.ui.main.SubInfoFetcher
import com.v2ray.ang.util.LogUtil
import java.util.concurrent.TimeUnit

/**
 * Periodically checks subscription expiry and posts a reminder notification
 * when a subscription is about to expire (within [WARN_DAYS] days).
 */
object ExpiryNotifier {

    private const val TASK = "maxachkala_expiry_check"
    private const val CHANNEL = "maxachkala_expiry"
    private const val NOTIF_ID = 91234
    private const val WARN_DAYS = 3L
    private const val BOT_URL = "https://t.me/maxachkalavpn_bot"

    /**
     * Schedule the periodic expiry check. Safe to call on every launch (KEEP policy).
     */
    fun schedule(context: Context = AngApplication.application) {
        val request = PeriodicWorkRequestBuilder<ExpiryWorker>(12, TimeUnit.HOURS)
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .setInitialDelay(1, TimeUnit.MINUTES)
            .build()
        WorkManager.getInstance(context)
            .enqueueUniquePeriodicWork(TASK, ExistingPeriodicWorkPolicy.KEEP, request)
    }

    class ExpiryWorker(context: Context, params: WorkerParameters) :
        CoroutineWorker(context, params) {

        override suspend fun doWork(): Result {
            try {
                var minDays = Long.MAX_VALUE
                MmkvManager.decodeSubscriptions().forEach { sub ->
                    val url = sub.subscription.url
                    if (url.isEmpty()) return@forEach
                    val info = SubInfoFetcher.fetch(url) ?: return@forEach
                    if (info.expireEpochSec > 0L) {
                        val days = (info.expireEpochSec - System.currentTimeMillis() / 1000L) / 86400L
                        if (days in 0L..WARN_DAYS && days < minDays) {
                            minDays = days
                        }
                    }
                }
                if (minDays in 0L..WARN_DAYS) {
                    notifyExpiry(applicationContext, minDays)
                }
            } catch (e: Exception) {
                LogUtil.e(AppConfig.TAG, "ExpiryNotifier: check failed", e)
            }
            return Result.success()
        }

        private fun notifyExpiry(context: Context, days: Long) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(
                    CHANNEL,
                    "Подписка",
                    NotificationManager.IMPORTANCE_HIGH
                )
                manager.createNotificationChannel(channel)
            }

            val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            val pendingIntent = PendingIntent.getActivity(
                context,
                0,
                Intent(Intent.ACTION_VIEW, Uri.parse(BOT_URL)),
                flags
            )

            val text = if (days <= 0L) {
                "Подписка истекает сегодня. Продлите в боте."
            } else {
                "Подписка истекает через $days дн. Продлите в боте."
            }

            val notification = NotificationCompat.Builder(context, CHANNEL)
                .setSmallIcon(R.drawable.ic_stat_logo)
                .setContentTitle("Maxachkala VPN")
                .setContentText(text)
                .setStyle(NotificationCompat.BigTextStyle().bigText(text))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .build()

            try {
                NotificationManagerCompat.from(context).notify(NOTIF_ID, notification)
            } catch (e: SecurityException) {
                LogUtil.e(AppConfig.TAG, "ExpiryNotifier: notification permission missing", e)
            }
        }
    }
}
