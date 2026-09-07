package com.pix.dayline.model

import com.pix.dayline.planning.PlanningEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

class RecurrenceAndPlanningTest {
    private fun event(
        id: String,
        date: LocalDate,
        start: LocalTime? = LocalTime.of(9, 0),
        end: LocalTime? = LocalTime.of(10, 0),
        recurrence: Recurrence = Recurrence.ONCE
    ) = DaylineItem(id, id, AgendaKind.EVENT, date, start, end, recurrence)

    @Test
    fun monthly31stSkipsShortMonths() {
        val item = event("month", LocalDate.of(2026, 1, 31), recurrence = Recurrence.MONTHLY)
        assertFalse(item.occursOn(LocalDate.of(2026, 2, 28)))
        assertTrue(item.occursOn(LocalDate.of(2026, 3, 31)))
    }

    @Test
    fun recurrenceEndAndExclusionsAreHonored() {
        val item = event("daily", LocalDate.of(2026, 9, 1), recurrence = Recurrence.DAILY).copy(
            recurrenceEndDate = LocalDate.of(2026, 9, 10),
            excludedDates = setOf(LocalDate.of(2026, 9, 7))
        )
        assertFalse(item.occursOn(LocalDate.of(2026, 9, 7)))
        assertTrue(item.occursOn(LocalDate.of(2026, 9, 10)))
        assertFalse(item.occursOn(LocalDate.of(2026, 9, 11)))
    }

    @Test
    fun allDayDoesNotConsumeFreeTime() {
        val date = LocalDate.of(2026, 9, 7)
        val allDay = event("holiday", date, null, null).copy(allDay = true)
        val slots = PlanningEngine.freeSlots(listOf(allDay), date, LocalTime.of(7, 0), LocalTime.of(10, 0), 15)
        assertEquals(1, slots.size)
        assertEquals(LocalTime.of(7, 0), slots.first().start)
        assertEquals(LocalTime.of(10, 0), slots.first().end)
    }

    @Test
    fun buffersMergeBusyWindows() {
        val date = LocalDate.of(2026, 9, 7)
        val first = event("a", date, LocalTime.of(9, 0), LocalTime.of(10, 0)).copy(bufferAfterMinutes = 15)
        val second = event("b", date, LocalTime.of(10, 10), LocalTime.of(11, 0))
        val slots = PlanningEngine.freeSlots(listOf(first, second), date, LocalTime.of(8, 0), LocalTime.of(12, 0), 15)
        assertEquals(listOf(LocalTime.of(8, 0), LocalTime.of(11, 0)), slots.map { it.start })
    }

    @Test
    fun taskSuggestionsRespectEarliestAndDeadline() {
        val today = LocalDate.of(2026, 9, 7)
        val task = DaylineItem(
            id = "task", title = "task", kind = AgendaKind.TASK,
            startDate = today, startTime = null, endTime = null, recurrence = Recurrence.ONCE,
            estimatedDurationMinutes = 60,
            earliestDate = today.plusDays(2),
            deadlineDate = today.plusDays(3)
        )
        val suggestions = PlanningEngine.suggestionsForTask(
            items = listOf(task),
            task = task,
            now = LocalDateTime.of(today, LocalTime.NOON),
            maxResults = 5
        )
        assertTrue(suggestions.isNotEmpty())
        assertTrue(suggestions.all { !it.date.isBefore(today.plusDays(2)) && !it.date.isAfter(today.plusDays(3)) })
    }
}
