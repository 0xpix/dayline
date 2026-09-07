package com.pix.dayline.model

import java.time.Duration
import java.time.LocalDate
import java.time.LocalTime

enum class AgendaKind { EVENT, TASK }
enum class Recurrence { ONCE, DAILY, WEEKDAYS, WEEKENDS, CUSTOM, WEEKLY, MONTHLY }
enum class RecurrenceEditScope { THIS_OCCURRENCE, THIS_AND_FOLLOWING, ENTIRE_SERIES }
enum class ItemColor { MONO, BLUE, SAGE, AMBER, ROSE, VIOLET }
enum class FocusCycle { OFF, POMODORO_25_5, FOCUS_50_10, CUSTOM }
enum class TaskPriority { LOW, NORMAL, HIGH }

data class TaskDetail(
    val id: String,
    val text: String,
    val done: Boolean = false
)

data class DaylineItem(
    val id: String,
    val title: String,
    val kind: AgendaKind,
    val startDate: LocalDate,
    val startTime: LocalTime?,
    val endTime: LocalTime?,
    val recurrence: Recurrence,
    val repeatDays: Set<Int> = emptySet(),
    val recurrenceEndDate: LocalDate? = null,
    val excludedDates: Set<LocalDate> = emptySet(),
    val seriesParentId: String? = null,
    val reminderMinutes: Int? = null,
    val focusCycle: FocusCycle = FocusCycle.OFF,
    val customFocusMinutes: Int = 25,
    val customBreakMinutes: Int = 5,
    val focusSessionsCompleted: Int = 0,
    val focusedMinutesCompleted: Int = 0,
    val bufferBeforeMinutes: Int = 0,
    val bufferAfterMinutes: Int = 0,
    val priority: TaskPriority = TaskPriority.NORMAL,
    val estimatedDurationMinutes: Int = 30,
    val earliestDate: LocalDate? = null,
    val deadlineDate: LocalDate? = null,
    val allDay: Boolean = false,
    val timeZoneId: String? = null,
    val color: ItemColor = ItemColor.MONO,
    val spaceId: String? = null,
    val details: List<TaskDetail> = emptyList(),
    val completedDates: Set<LocalDate> = emptySet(),
    val calendarEventId: Long? = null,
    val calendarId: Long? = null,
    val calendarName: String? = null,
    val calendarReadOnly: Boolean = false
) {
    val time: LocalTime? get() = startTime

    val durationMinutes: Long?
        get() = if (
            startTime != null &&
            endTime != null &&
            endTime.isAfter(startTime)
        ) {
            Duration.between(startTime, endTime).toMinutes()
        } else {
            null
        }

    /** Duration Dayline should reserve when fitting this item into a free slot. */
    val planningDurationMinutes: Int
        get() = if (kind == AgendaKind.TASK) {
            estimatedDurationMinutes.coerceIn(15, 8 * 60)
        } else {
            (durationMinutes ?: 60L).toInt().coerceIn(15, 24 * 60)
        }

    val focusMinutes: Int
        get() = when (focusCycle) {
            FocusCycle.OFF -> 0
            FocusCycle.POMODORO_25_5 -> 25
            FocusCycle.FOCUS_50_10 -> 50
            FocusCycle.CUSTOM -> customFocusMinutes.coerceIn(5, 180)
        }

    val breakMinutes: Int
        get() = when (focusCycle) {
            FocusCycle.OFF -> 0
            FocusCycle.POMODORO_25_5 -> 5
            FocusCycle.FOCUS_50_10 -> 10
            FocusCycle.CUSTOM -> customBreakMinutes.coerceIn(1, 60)
        }
}

fun DaylineItem.occursOn(date: LocalDate): Boolean {
    if (date.isBefore(startDate)) return false
    if (recurrenceEndDate != null && date.isAfter(recurrenceEndDate)) return false
    if (date in excludedDates) return false

    return when (recurrence) {
        Recurrence.ONCE -> date == startDate
        Recurrence.DAILY -> true
        Recurrence.WEEKDAYS -> date.dayOfWeek.value in 1..5
        Recurrence.WEEKENDS -> date.dayOfWeek.value in 6..7
        Recurrence.CUSTOM -> date.dayOfWeek.value in repeatDays
        Recurrence.WEEKLY -> date.dayOfWeek == startDate.dayOfWeek
        // Keep RFC-style monthly semantics: a series created on the 31st does
        // not silently move to the 30th/28th in shorter months.
        Recurrence.MONTHLY -> date.dayOfMonth == startDate.dayOfMonth
    }
}

fun DaylineItem.isCompletedOn(date: LocalDate): Boolean = date in completedDates

fun DaylineItem.overlaps(other: DaylineItem): Boolean {
    if (allDay || other.allDay) return false
    val aStart = startTime ?: return false
    val bStart = other.startTime ?: return false
    val aEnd = endTime?.takeIf { it.isAfter(aStart) }
        ?: aStart.plusMinutes(if (kind == AgendaKind.TASK) estimatedDurationMinutes.toLong() else 60L)
    val bEnd = other.endTime?.takeIf { it.isAfter(bStart) }
        ?: bStart.plusMinutes(if (other.kind == AgendaKind.TASK) other.estimatedDurationMinutes.toLong() else 60L)
    return aStart < bEnd && bStart < aEnd
}
