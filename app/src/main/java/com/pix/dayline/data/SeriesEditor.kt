package com.pix.dayline.data

import com.pix.dayline.model.DaylineItem
import com.pix.dayline.model.Recurrence
import com.pix.dayline.model.RecurrenceEditScope
import java.time.LocalDate
import java.util.UUID

data class SeriesEditResult(
    val items: List<DaylineItem>,
    val editedItem: DaylineItem
)

object SeriesEditor {
    fun editForGestureScope(
        original: DaylineItem,
        editedOccurrence: DaylineItem,
        occurrenceDate: LocalDate,
        scope: RecurrenceEditScope
    ): DaylineItem = when (scope) {
        RecurrenceEditScope.ENTIRE_SERIES -> editedOccurrence.copy(startDate = original.startDate)
        RecurrenceEditScope.THIS_OCCURRENCE,
        RecurrenceEditScope.THIS_AND_FOLLOWING -> editedOccurrence.copy(startDate = occurrenceDate)
    }

    fun apply(
        existingItems: List<DaylineItem>,
        original: DaylineItem,
        edited: DaylineItem,
        occurrenceDate: LocalDate,
        scope: RecurrenceEditScope
    ): List<DaylineItem> = applyWithSelection(
        existingItems = existingItems,
        original = original,
        edited = edited,
        occurrenceDate = occurrenceDate,
        scope = scope
    ).items

    fun applyWithSelection(
        existingItems: List<DaylineItem>,
        original: DaylineItem,
        edited: DaylineItem,
        occurrenceDate: LocalDate,
        scope: RecurrenceEditScope
    ): SeriesEditResult {
        if (original.recurrence == Recurrence.ONCE) {
            val updated = edited.copy(id = original.id)
            return SeriesEditResult(
                items = existingItems.map { if (it.id == original.id) updated else it },
                editedItem = updated
            )
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
                SeriesEditResult(
                    items = existingItems.map { if (it.id == original.id) wholeSeries else it },
                    editedItem = wholeSeries
                )
            }

            RecurrenceEditScope.THIS_OCCURRENCE -> {
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
                SeriesEditResult(
                    items = existingItems.map { if (it.id == original.id) parent else it } + oneOff,
                    editedItem = oneOff
                )
            }

            RecurrenceEditScope.THIS_AND_FOLLOWING -> {
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
                SeriesEditResult(
                    items = existingItems.map { if (it.id == original.id) before else it } + following,
                    editedItem = following
                )
            }
        }
    }
}
