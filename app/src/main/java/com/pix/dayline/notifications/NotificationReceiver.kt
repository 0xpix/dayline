package com.pix.dayline.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class NotificationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val itemId = intent.getStringExtra(EXTRA_ITEM_ID) ?: return
        NotificationScheduler.showReminder(context.applicationContext, itemId)
    }

    companion object {
        const val EXTRA_ITEM_ID = "item_id"
    }
}
