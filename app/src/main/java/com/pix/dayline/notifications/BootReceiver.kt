package com.pix.dayline.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.pix.dayline.data.DaylineStore

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED || intent.action == Intent.ACTION_MY_PACKAGE_REPLACED) {
            val store = DaylineStore(context.applicationContext)
            NotificationScheduler.syncAll(context.applicationContext, store.loadItems())
        }
    }
}
