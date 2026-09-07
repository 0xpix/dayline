package com.pix.dayline.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.pix.dayline.data.BetaUpdateScheduler
import com.pix.dayline.data.DaylineStore
import com.pix.dayline.widgets.DaylineWidgetUpdater
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val supported = intent.action in setOf(
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED,
            Intent.ACTION_DATE_CHANGED
        )
        if (!supported) return

        val appContext = context.applicationContext
        val store = DaylineStore(appContext)
        val items = store.loadItems()

        // Rebuild every time-based surface after reboot, app replacement,
        // timezone/date changes or a manual clock adjustment. This prevents
        // reminders, Now Activity and Glance widgets from staying anchored to stale time.
        NotificationScheduler.syncAll(appContext, items)

        if (store.loadNowActivityEnabled()) {
            NowActivityScheduler.syncAll(appContext, items)
        } else {
            NowActivityScheduler.cancelAll(appContext, items)
        }

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.Default).launch {
            try {
                DaylineWidgetUpdater.updateAll(appContext)
                store.saveWidgetRefreshAt()
            } finally {
                pendingResult.finish()
            }
        }

        BetaUpdateScheduler.sync(appContext)
    }
}
