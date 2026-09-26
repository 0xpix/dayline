package com.pix.dayline.data

import android.content.Context

/**
 * Tracks the last calendar state that Dayline and Android Calendar agreed on.
 *
 * This gives reconciliation a three-way baseline: provider, current Dayline
 * row and last-known shared state. If both sides changed independently, Dayline
 * can keep the local edit instead of silently overwriting it.
 */
class CalendarSyncBaselineStore(context: Context) {
    private val prefs = context.getSharedPreferences(
        "dayline_calendar_sync_baselines",
        Context.MODE_PRIVATE
    )

    fun load(eventId: Long): String? =
        prefs.getString(baselineKey(eventId), null)?.takeIf { it.isNotBlank() }

    fun save(eventId: Long, fingerprint: String) {
        prefs.edit()
            .putString(baselineKey(eventId), fingerprint)
            .remove(conflictKey(eventId))
            .apply()
    }

    fun remove(eventId: Long) {
        prefs.edit()
            .remove(baselineKey(eventId))
            .remove(conflictKey(eventId))
            .apply()
    }

    fun markConflict(eventId: Long) {
        prefs.edit()
            .putLong(conflictKey(eventId), System.currentTimeMillis())
            .apply()
    }

    fun conflictCount(): Int =
        prefs.all.keys.count { it.startsWith(CONFLICT_PREFIX) }

    fun lastConflictAt(): Long? =
        prefs.all
            .filterKeys { it.startsWith(CONFLICT_PREFIX) }
            .values
            .mapNotNull { it as? Long }
            .maxOrNull()

    private fun baselineKey(eventId: Long) = "$BASELINE_PREFIX$eventId"
    private fun conflictKey(eventId: Long) = "$CONFLICT_PREFIX$eventId"

    private companion object {
        const val BASELINE_PREFIX = "baseline_"
        const val CONFLICT_PREFIX = "conflict_"
    }
}
