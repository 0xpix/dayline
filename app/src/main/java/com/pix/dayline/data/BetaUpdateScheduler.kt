package com.pix.dayline.data

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.pix.dayline.BuildConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

object BetaUpdateScheduler {
    private const val INTERVAL_MILLIS = 24L * 60L * 60L * 1000L

    fun sync(context: Context) {
        if (BuildConfig.UPDATE_CHANNEL != "GitHub beta") {
            cancel(context)
            return
        }
        val store = DaylineStore(context.applicationContext)
        if (!store.loadAutoBetaUpdates()) {
            cancel(context)
            return
        }

        val manager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val first = System.currentTimeMillis() + INTERVAL_MILLIS
        manager.setInexactRepeating(
            AlarmManager.RTC,
            first,
            INTERVAL_MILLIS,
            pendingIntent(context)
        )
    }

    fun cancel(context: Context) {
        val manager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        manager.cancel(pendingIntent(context))
    }

    private fun pendingIntent(context: Context): PendingIntent = PendingIntent.getBroadcast(
        context,
        130013,
        Intent(context, BetaUpdateReceiver::class.java),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
}

class BetaUpdateReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (BuildConfig.UPDATE_CHANNEL != "GitHub beta") return
        val appContext = context.applicationContext
        val store = DaylineStore(appContext)
        if (!store.loadAutoBetaUpdates()) return

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val result = BetaUpdateChecker.check(BuildConfig.VERSION_NAME)
                result.checkedAtMillis?.let { store.saveUpdateCheckResult(it, result.error) }
                store.saveAvailableBetaRelease(
                    if (result.status == UpdateStatus.AVAILABLE) result.release else null
                )
            } finally {
                pendingResult.finish()
            }
        }
    }
}
