package com.pix.dayline.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class NowActivityReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val itemId = intent.getStringExtra(EXTRA_ITEM_ID) ?: return
        val appContext = context.applicationContext

        when (intent.action) {
            ACTION_START -> NowActivityScheduler.handleStart(appContext, itemId)
            ACTION_PHASE -> NowActivityScheduler.handlePhase(appContext, itemId)
            ACTION_END -> NowActivityScheduler.handleEnd(appContext, itemId)
            ACTION_REFRESH -> NowActivityScheduler.handleRefresh(appContext, itemId)
            ACTION_PAUSE -> NowActivityScheduler.togglePause(appContext, itemId)
            ACTION_SKIP_REST -> NowActivityScheduler.skipRest(appContext, itemId)
            ACTION_PLUS_FIVE -> NowActivityScheduler.extendPhase(appContext, itemId, 5)
            ACTION_FINISH -> NowActivityScheduler.finishNow(appContext, itemId)
        }
    }

    companion object {
        const val ACTION_START = "com.pix.dayline.NOW_START"
        const val ACTION_PHASE = "com.pix.dayline.NOW_PHASE"
        const val ACTION_END = "com.pix.dayline.NOW_END"
        const val ACTION_REFRESH = "com.pix.dayline.NOW_REFRESH"
        const val ACTION_PAUSE = "com.pix.dayline.NOW_PAUSE"
        const val ACTION_SKIP_REST = "com.pix.dayline.NOW_SKIP_REST"
        const val ACTION_PLUS_FIVE = "com.pix.dayline.NOW_PLUS_FIVE"
        const val ACTION_FINISH = "com.pix.dayline.NOW_FINISH"
        const val EXTRA_ITEM_ID = "item_id"
    }
}
