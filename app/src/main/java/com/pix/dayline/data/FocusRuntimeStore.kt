package com.pix.dayline.data

import android.content.Context
import org.json.JSONObject
import java.time.LocalDate

data class FocusRuntimeState(
    val itemId: String,
    val occurrenceDate: LocalDate,
    val focus: Boolean,
    val phaseStartedEpochMillis: Long,
    val phaseEndEpochMillis: Long,
    val paused: Boolean = false,
    val pausedRemainingSeconds: Long = 0L,
    val sessionsCompleted: Int = 0,
    val focusedSeconds: Long = 0L,
    val finished: Boolean = false
)

class FocusRuntimeStore(context: Context) {
    private val prefs = context.getSharedPreferences("dayline_focus_runtime", Context.MODE_PRIVATE)

    fun load(itemId: String): FocusRuntimeState? {
        val raw = prefs.getString(itemId, null) ?: return null
        return runCatching {
            val json = JSONObject(raw)
            FocusRuntimeState(
                itemId = itemId,
                occurrenceDate = LocalDate.parse(json.getString("occurrenceDate")),
                focus = json.optBoolean("focus", true),
                phaseStartedEpochMillis = json.optLong(
                    "phaseStartedEpochMillis",
                    System.currentTimeMillis()
                ),
                phaseEndEpochMillis = json.optLong("phaseEndEpochMillis", 0L),
                paused = json.optBoolean("paused", false),
                pausedRemainingSeconds = json.optLong("pausedRemainingSeconds", 0L),
                sessionsCompleted = json.optInt("sessionsCompleted", 0),
                focusedSeconds = json.optLong("focusedSeconds", 0L),
                finished = json.optBoolean("finished", false)
            )
        }.getOrNull()
    }

    fun save(state: FocusRuntimeState) {
        prefs.edit().putString(
            state.itemId,
            JSONObject()
                .put("occurrenceDate", state.occurrenceDate.toString())
                .put("focus", state.focus)
                .put("phaseStartedEpochMillis", state.phaseStartedEpochMillis)
                .put("phaseEndEpochMillis", state.phaseEndEpochMillis)
                .put("paused", state.paused)
                .put("pausedRemainingSeconds", state.pausedRemainingSeconds)
                .put("sessionsCompleted", state.sessionsCompleted)
                .put("focusedSeconds", state.focusedSeconds)
                .put("finished", state.finished)
                .toString()
        ).apply()
    }

    fun clear(itemId: String) {
        prefs.edit().remove(itemId).apply()
    }
}
