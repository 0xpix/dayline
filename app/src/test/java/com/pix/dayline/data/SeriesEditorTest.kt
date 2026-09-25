package com.pix.dayline.data

import com.pix.dayline.model.AgendaKind
import com.pix.dayline.model.DaylineItem
import com.pix.dayline.model.Recurrence
import com.pix.dayline.model.RecurrenceEditScope
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime

class SeriesEditorTest {
    private val startDate = LocalDate.of(2026, 9, 1)

    private fun series() = DaylineItem(
        id = "series",
        title = "Stand-up",
        kind = AgendaKind.EVENT,
        startDate = startDate,
        startTime = LocalTime.of(9, 0),
        endTime = LocalTime.of(9, 30),
        recurrence = Recurrence.DAILY,
        recurrenceEndDate = LocalDate.of(2026, 9, 30),
        calendarEventId = 42L,
        calendarId = 7L,
        calendarName = "Work"
    )

    @Test
    fun thisOccurrenceDetachesOnlySelectedDate() {
        val original = series()
        val occurrence = LocalDate.of(2026, 9, 10)
        val edited = original.copy(
            startDate = occurrence,
            startTime = LocalTime.of(11, 0),
            endTime = LocalTime.of(11, 30)
        )

        val result = SeriesEditor.apply(
            existingItems = listOf(original),
            original = original,
            edited = edited,
            occurrenceDate = occurrence,
            scope = RecurrenceEditScope.THIS_OCCURRENCE
        )

        assertEquals(2, result.size)
        val parent = result.first { it.id == original.id }
        val detached = result.first { it.id != original.id }

        assertTrue(occurrence in parent.excludedDates)
        assertEquals(Recurrence.DAILY, parent.recurrence)
        assertEquals(Recurrence.ONCE, detached.recurrence)
        assertEquals(occurrence, detached.startDate)
        assertEquals(LocalTime.of(11, 0), detached.startTime)
        assertEquals(original.id, detached.seriesParentId)
        assertEquals(original.calendarId, detached.calendarId)
        assertNull(detached.calendarEventId)
        assertFalse(detached.calendarReadOnly)
    }

    @Test
    fun thisOccurrenceReturnsDetachedOccurrenceAsEditedItem() {
        val original = series()
        val occurrence = LocalDate.of(2026, 9, 10)
        val edited = original.copy(
            startDate = occurrence,
            startTime = LocalTime.of(11, 0),
            endTime = LocalTime.of(11, 30)
        )

        val result = SeriesEditor.applyWithSelection(
            existingItems = listOf(original),
            original = original,
            edited = edited,
            occurrenceDate = occurrence,
            scope = RecurrenceEditScope.THIS_OCCURRENCE
        )

        val parent = result.items.first { it.id == original.id }
        assertTrue(occurrence in parent.excludedDates)
        assertNotEquals(original.id, result.editedItem.id)
        assertEquals(Recurrence.ONCE, result.editedItem.recurrence)
        assertEquals(occurrence, result.editedItem.startDate)
        assertEquals(LocalTime.of(11, 0), result.editedItem.startTime)
        assertEquals(original.id, result.editedItem.seriesParentId)
    }

    @Test
    fun entireSeriesReturnsUpdatedMasterAsEditedItem() {
        val original = series()
        val edited = original.copy(
            title = "Daily sync",
            startTime = LocalTime.of(8, 30),
            endTime = LocalTime.of(9, 0)
        )

        val result = SeriesEditor.applyWithSelection(
            existingItems = listOf(original),
            original = original,
            edited = edited,
            occurrenceDate = LocalDate.of(2026, 9, 10),
            scope = RecurrenceEditScope.ENTIRE_SERIES
        )

        assertEquals(original.id, result.editedItem.id)
        assertEquals("Daily sync", result.editedItem.title)
        assertEquals(LocalTime.of(8, 30), result.editedItem.startTime)
        assertEquals(result.items.single(), result.editedItem)
    }

    @Test
    fun thisAndFollowingSplitsSeriesAndKeepsExclusionsOnCorrectSide() {
        val occurrence = LocalDate.of(2026, 9, 15)
        val original = series().copy(
            excludedDates = setOf(
                LocalDate.of(2026, 9, 5),
                LocalDate.of(2026, 9, 20)
            )
        )
        val edited = original.copy(
            startDate = occurrence,
            startTime = LocalTime.of(10, 0),
            endTime = LocalTime.of(10, 30)
        )

        val result = SeriesEditor.apply(
            existingItems = listOf(original),
            original = original,
            edited = edited,
            occurrenceDate = occurrence,
            scope = RecurrenceEditScope.THIS_AND_FOLLOWING
        )

        assertEquals(2, result.size)
        val before = result.first { it.id == original.id }
        val following = result.first { it.id != original.id }

        assertEquals(occurrence.minusDays(1), before.recurrenceEndDate)
        assertEquals(setOf(LocalDate.of(2026, 9, 5)), before.excludedDates)

        assertNotEquals(original.id, following.id)
        assertEquals(occurrence, following.startDate)
        assertEquals(original.recurrenceEndDate, following.recurrenceEndDate)
        assertEquals(setOf(LocalDate.of(2026, 9, 20)), following.excludedDates)
        assertEquals(original.id, following.seriesParentId)
        assertNull(following.calendarEventId)
    }

    @Test
    fun entireSeriesPreservesProviderIdentity() {
        val original = series()
        val edited = original.copy(
            title = "Daily sync",
            startTime = LocalTime.of(8, 30),
            endTime = LocalTime.of(9, 0),
            calendarEventId = null,
            calendarId = null,
            calendarName = null
        )

        val result = SeriesEditor.apply(
            existingItems = listOf(original),
            original = original,
            edited = edited,
            occurrenceDate = LocalDate.of(2026, 9, 10),
            scope = RecurrenceEditScope.ENTIRE_SERIES
        )

        val updated = result.single()
        assertEquals("Daily sync", updated.title)
        assertEquals(LocalTime.of(8, 30), updated.startTime)
        assertEquals(42L, updated.calendarEventId)
        assertEquals(7L, updated.calendarId)
        assertEquals("Work", updated.calendarName)
    }
}
