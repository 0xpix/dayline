package com.pix.dayline.data

import com.pix.dayline.model.DaylineItem
import com.pix.dayline.model.Recurrence
import com.pix.dayline.model.RecurrenceEditScope
import java.time.LocalDate
import java.util.UUID

object SeriesEditor {
    fun apply(
        existingItems: List<DaylineItem>,
        original: DaylineItem,
        edited: DaylineItem,
        occurrenceDate: LocalDate,
        scope: RecurrenceEditScope
    ): List<DaylineItem> {
        if (original.recurrence == Recurrence.ONCE || scope == RecurrenceEditScope.ENTIRE_SERIES) {
            return existingItems.map { if (it.id == original.id) edited else it }
        }

        return when (scope) {
            RecurrenceEditScope.ENTIRE_SERIES ->
                existingItems.map { if (it.id == original.id) edited else it }

            RecurrenceEditScope.THIS_OCCURRENCE -> {
                val parent = original.copy(
                    excludedDates = original.excludedDates + occurrenceDate
                )
                val oneOff = edited.copy(
                    id = UUID.randomUUID().toString(),
                    startDate = occurrenceDate,
                    recurrence = Recurrence.ONCE,
                    recurrenceEndDate = null,
                    excludedDates = emptySet(),
                    seriesParentId = original.id,
                    calendarEventId = null,
                    calendarId = original.calendarId,
                    calendarReadOnly = false
                )
                existingItems
                    .map { if (it.id == original.id) parent else it } + oneOff
            }

            RecurrenceEditScope.THIS_AND_FOLLOWING -> {
                val before = original.copy(
                    recurrenceEndDate = occurrenceDate.minusDays(1)
                )
                val following = edited.copy(
                    id = UUID.randomUUID().toString(),
                    startDate = occurrenceDate,
                    recurrenceEndDate = original.recurrenceEndDate,
                    excludedDates = original.excludedDates.filterTo(mutableSetOf()) {
                        !it.isBefore(occurrenceDate)
                    },
                    seriesParentId = original.id,
                    calendarEventId = null,
                    calendarId = original.calendarId,
                    calendarReadOnly = false
                )
                existingItems
                    .map { if (it.id == original.id) before else it } + following
            }
        }
    }
}
