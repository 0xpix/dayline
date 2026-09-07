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
        if (original.recurrence == Recurrence.ONCE) {
            return existingItems.map { if (it.id == original.id) edited.copy(id = original.id) else it }
        }

        return when (scope) {
            RecurrenceEditScope.ENTIRE_SERIES -> {
                val wholeSeries = edited.copy(
                    id = original.id,
                    seriesParentId = original.seriesParentId,
                    calendarEventId = original.calendarEventId,
                    calendarId = original.calendarId,
                    calendarName = original.calendarName,
                    calendarReadOnly = original.calendarReadOnly
                )
                existingItems.map { if (it.id == original.id) wholeSeries else it }
            }

            RecurrenceEditScope.THIS_OCCURRENCE -> {
                // The master keeps its original cadence and skips only the
                // occurrence being edited. The detached item is a real one-off,
                // so changing its date/time never shifts the whole series.
                val parent = original.copy(
                    excludedDates = original.excludedDates + occurrenceDate
                )
                val oneOff = edited.copy(
                    id = UUID.randomUUID().toString(),
                    recurrence = Recurrence.ONCE,
                    repeatDays = emptySet(),
                    recurrenceEndDate = null,
                    excludedDates = emptySet(),
                    seriesParentId = original.seriesParentId ?: original.id,
                    calendarEventId = null,
                    calendarId = original.calendarId,
                    calendarName = original.calendarName,
                    calendarReadOnly = false
                )
                existingItems
                    .map { if (it.id == original.id) parent else it } + oneOff
            }

            RecurrenceEditScope.THIS_AND_FOLLOWING -> {
                // Close the old master immediately before the selected
                // occurrence, then start a new linked series using the edited
                // values. Past exclusions stay with the old segment; future
                // exclusions follow the new segment.
                val pastExclusions = original.excludedDates.filterTo(mutableSetOf()) {
                    it.isBefore(occurrenceDate)
                }
                val futureExclusions = original.excludedDates.filterTo(mutableSetOf()) {
                    !it.isBefore(occurrenceDate)
                }
                val before = original.copy(
                    recurrenceEndDate = occurrenceDate.minusDays(1),
                    excludedDates = pastExclusions
                )
                val following = edited.copy(
                    id = UUID.randomUUID().toString(),
                    recurrenceEndDate = original.recurrenceEndDate,
                    excludedDates = futureExclusions,
                    seriesParentId = original.seriesParentId ?: original.id,
                    calendarEventId = null,
                    calendarId = original.calendarId,
                    calendarName = original.calendarName,
                    calendarReadOnly = false
                )
                existingItems
                    .map { if (it.id == original.id) before else it } + following
            }
        }
    }
}
