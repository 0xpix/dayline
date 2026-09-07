package com.pix.dayline.glyph

import android.content.Context
import com.pix.dayline.data.FocusRuntimeStore
import com.pix.dayline.model.*
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

object GlyphStateResolver {
    fun resolve(
        context: Context,
        items: List<DaylineItem>,
        preferences: GlyphPreferences,
        now: LocalDateTime = LocalDateTime.now()
    ): DaylineGlyphSignal {
        if (!preferences.enabled) return DaylineGlyphSignal.IDLE

        val today = now.toLocalDate()
        val time = now.toLocalTime()
        if (preferences.quietHoursEnabled && inQuietHours(time, preferences.quietStart, preferences.quietEnd)) {
            return DaylineGlyphSignal.SLEEPY
        }

        if (preferences.mode == GlyphMode.EYES_ONLY || !preferences.showAppStates) {
            return idleSignal(preferences)
        }

        val todays = items
            .filter { it.occursOn(today) && it.kind == AgendaKind.EVENT && it.startTime != null }
            .sortedBy { it.startTime }

        // Conflict is the highest-priority automatically derived state.
        for (i in todays.indices) {
            for (j in i + 1 until todays.size) {
                if (todays[i].overlaps(todays[j])) {
                    val aStart = todays[i].startTime ?: continue
                    val aEnd = todays[i].endTime ?: aStart.plusHours(1)
                    val bStart = todays[j].startTime ?: continue
                    val bEnd = todays[j].endTime ?: bStart.plusHours(1)
                    if (time >= maxOf(aStart, bStart).minusMinutes(15) && time <= minOf(aEnd, bEnd)) {
                        return DaylineGlyphSignal.CONFLICT
                    }
                }
            }
        }

        val missedTask = items
            .asSequence()
            .filter { it.kind == AgendaKind.TASK && it.occursOn(today) && !it.isCompletedOn(today) }
            .mapNotNull { task -> task.startTime?.let { task to it } }
            .firstOrNull { (_, due) -> Duration.between(due, time).toMinutes() >= 15 }
        if (missedTask != null) return DaylineGlyphSignal.MISSED

        val active = todays.firstOrNull { item ->
            val start = item.startTime ?: return@firstOrNull false
            val end = item.endTime?.takeIf { it.isAfter(start) } ?: start.plusHours(1)
            time >= start && time < end
        }

        if (active != null && active.focusCycle != FocusCycle.OFF) {
            val runtime = FocusRuntimeStore(context).load(active.id)
            if (runtime != null && runtime.occurrenceDate == today && !runtime.finished) {
                if (runtime.paused) return DaylineGlyphSignal.SLEEPY
                return if (runtime.focus) DaylineGlyphSignal.FOCUS else DaylineGlyphSignal.REST
            }
            // Fallback if the focus runtime has not been created yet.
            val start = active.startTime!!
            val elapsedMinutes = Duration.between(start, time).toMinutes().coerceAtLeast(0)
            val cycle = (active.focusMinutes + active.breakMinutes).coerceAtLeast(1)
            return if ((elapsedMinutes % cycle) < active.focusMinutes) {
                DaylineGlyphSignal.FOCUS
            } else {
                DaylineGlyphSignal.REST
            }
        }

        val justStarted = todays.firstOrNull {
            val start = it.startTime ?: return@firstOrNull false
            val delta = Duration.between(start, time).toMinutes()
            delta in 0..2
        }
        if (justStarted != null) return DaylineGlyphSignal.EVENT_STARTED

        val next = todays.firstOrNull { (it.startTime ?: LocalTime.MIN) > time }
        if (next != null) {
            val nextStart = next.startTime ?: return idleSignal(preferences)
            val minutes = Duration.between(time, nextStart).toMinutes()
            if (next.bufferBeforeMinutes > 0 && minutes in 0..next.bufferBeforeMinutes) {
                return DaylineGlyphSignal.GO
            }
            if (minutes in 0..5) return DaylineGlyphSignal.REMINDER_SOON
            if (minutes in 6..15) return DaylineGlyphSignal.NEXT_EVENT
            if (active == null && minutes >= 60) return DaylineGlyphSignal.FREE_NOW
        }

        if (todays.isEmpty()) return DaylineGlyphSignal.NO_PLANS
        return idleSignal(preferences)
    }

    fun idleSignal(preferences: GlyphPreferences): DaylineGlyphSignal = when (preferences.idleExpression) {
        GlyphIdleExpression.CENTER -> DaylineGlyphSignal.CENTER
        GlyphIdleExpression.CURIOUS -> DaylineGlyphSignal.CURIOUS
        GlyphIdleExpression.SLEEPY -> DaylineGlyphSignal.SLEEPY
        GlyphIdleExpression.HAPPY -> DaylineGlyphSignal.HAPPY
    }

    fun inQuietHours(now: LocalTime, start: LocalTime, end: LocalTime): Boolean = when {
        start == end -> true
        start < end -> now >= start && now < end
        else -> now >= start || now < end
    }
}
