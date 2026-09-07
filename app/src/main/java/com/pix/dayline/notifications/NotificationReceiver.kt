package com.pix.dayline.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.pix.dayline.data.DaylineStore
import com.pix.dayline.glyph.GlyphRuntimeStore
import com.pix.dayline.model.DaylineGlyphSignal
import com.pix.dayline.model.GlyphMode

class NotificationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val itemId = intent.getStringExtra(EXTRA_ITEM_ID) ?: return
        val appContext = context.applicationContext
        NotificationScheduler.showReminder(appContext, itemId)

        val prefs = DaylineStore(appContext).loadGlyphPreferences()
        if (prefs.enabled && prefs.mode == GlyphMode.EYES_AND_STATES && prefs.showAppStates) {
            GlyphRuntimeStore(appContext).enqueue(
                DaylineGlyphSignal.REMINDER_SOON,
                prefs.reminderFlashSeconds.coerceIn(2, 10) * 1_000L
            )
        }
    }

    companion object {
        const val EXTRA_ITEM_ID = "item_id"
    }
}
