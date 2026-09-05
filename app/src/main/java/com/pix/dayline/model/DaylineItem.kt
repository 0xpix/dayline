package com.pix.dayline.model

import java.time.Duration
import java.time.LocalDate
import java.time.LocalTime

enum class AgendaKind { EVENT, TASK }
enum class Recurrence { ONCE, DAILY, WEEKDAYS, WEEKLY, MONTHLY }
enum class ItemColor { MONO, BLUE, SAGE, AMBER, ROSE, VIOLET }

data class TaskDetail(val id: String, val text: String, val done: Boolean = false)

data class DaylineItem(
    val id: String,
    val title: String,
    val kind: AgendaKind,
    val startDate: LocalDate,
    val startTime: LocalTime?,
    val endTime: LocalTime?,
    val recurrence: Recurrence,
    val reminderMinutes: Int? = null,
    val color: ItemColor = ItemColor.MONO,
    val spaceId: String? = null,
    val details: List<TaskDetail> = emptyList(),
    val completedDates: Set<LocalDate> = emptySet()
) {
    val time: LocalTime? get() = startTime
    val durationMinutes: Long?
        get() = if (startTime != null && endTime != null && endTime.isAfter(startTime)) Duration.between(startTime, endTime).toMinutes() else null
}

fun DaylineItem.occursOn(date: LocalDate): Boolean {
    if (date.isBefore(startDate)) return false
    return when (recurrence) {
        Recurrence.ONCE -> date == startDate
        Recurrence.DAILY -> true
        Recurrence.WEEKDAYS -> date.dayOfWeek.value in 1..5
        Recurrence.WEEKLY -> date.dayOfWeek == startDate.dayOfWeek
        Recurrence.MONTHLY -> date.dayOfMonth == startDate.dayOfMonth
    }
}
fun DaylineItem.isCompletedOn(date: LocalDate): Boolean = date in completedDates
