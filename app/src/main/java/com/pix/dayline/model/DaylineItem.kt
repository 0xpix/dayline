package com.pix.dayline.model

import java.time.Duration
import java.time.LocalDate
import java.time.LocalTime

enum class AgendaKind { EVENT, TASK }
enum class Recurrence { ONCE, DAILY, WEEKDAYS, WEEKLY, MONTHLY }
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
        Recurrence.WEEKLY -> date.dayOfWeek == startDate.dayOfWeek
        Recurrence.MONTHLY -> date.dayOfMonth == startDate.dayOfMonth
    }
}

fun DaylineItem.isCompletedOn(date: LocalDate): Boolean = date in completedDates

fun DaylineItem.overlaps(other: DaylineItem): Boolean {
    val aStart = startTime ?: return false
    val bStart = other.startTime ?: return false
    val aEnd = endTime?.takeIf { it.isAfter(aStart) } ?: aStart.plusHours(1)
    val bEnd = other.endTime?.takeIf { it.isAfter(bStart) } ?: bStart.plusHours(1)
    return aStart < bEnd && bStart < aEnd
}
