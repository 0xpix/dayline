package com.pix.dayline.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class NowActivityReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val itemId = intent.getStringExtra(EXTRA_ITEM_ID) ?: return

        when (intent.action) {
            ACTION_START -> NowActivityScheduler.handleStart(
                context.applicationContext,
                itemId
            )

            ACTION_END -> NowActivityScheduler.handleEnd(
                context.applicationContext,
                itemId
            )
        }
    }

    companion object {
        const val ACTION_START = "com.pix.dayline.NOW_START"
        const val ACTION_END = "com.pix.dayline.NOW_END"
        const val EXTRA_ITEM_ID = "item_id"
    }
}
