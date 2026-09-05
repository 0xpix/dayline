package com.pix.dayline.model

import java.time.LocalDate
import java.time.LocalTime

enum class AgendaKind {
    EVENT,
    TASK
}

enum class Recurrence {
    ONCE,
    DAILY,
    WEEKDAYS,
    WEEKLY,
    MONTHLY
}

data class DaylineItem(
    val id: String,
    val title: String,
    val kind: AgendaKind,
    val startDate: LocalDate,
    val time: LocalTime?,
    val recurrence: Recurrence,
    val completedDates: Set<LocalDate> = emptySet()
)

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
