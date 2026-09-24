package com.pix.dayline.planning

import com.pix.dayline.model.AgendaKind
import com.pix.dayline.model.DaylineItem
import com.pix.dayline.model.Recurrence
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime

class PlanningEngineEdgeCasesTest {
    private val date = LocalDate.of(2026, 9, 24)

    private fun event(
        id: String,
        start: LocalTime,
        end: LocalTime,
        allDay: Boolean = false
    ) = DaylineItem(
        id = id,
        title = id,
        kind = AgendaKind.EVENT,
        startDate = date,
        startTime = start,
        endTime = end,
        recurrence = Recurrence.ONCE,
        allDay = allDay
    )

    @Test
    fun invalidDayWindowHasNoFreeSlots() {
        assertTrue(
            PlanningEngine.freeSlots(
                items = emptyList(),
                date = date,
                dayStart = LocalTime.of(18, 0),
                dayEnd = LocalTime.of(8, 0)
            ).isEmpty()
        )
    }

    @Test
    fun adjacentBusyIntervalsMergeWithoutPhantomGap() {
        val first = event("a", LocalTime.of(9, 0), LocalTime.of(10, 0))
        val second = event("b", LocalTime.of(10, 0), LocalTime.of(11, 0))

        val slots = PlanningEngine.freeSlots(
            items = listOf(first, second),
            date = date,
            dayStart = LocalTime.of(8, 0),
            dayEnd = LocalTime.of(12, 0),
            minMinutes = 15
        )

        assertEquals(2, slots.size)
        assertEquals(LocalTime.of(8, 0), slots[0].start)
        assertEquals(LocalTime.of(9, 0), slots[0].end)
        assertEquals(LocalTime.of(11, 0), slots[1].start)
        assertEquals(LocalTime.of(12, 0), slots[1].end)
    }

    @Test
    fun fittingSlotRoundsEarliestUpToQuarterHour() {
        val slots = PlanningEngine.fittingSlots(
            items = emptyList(),
            date = date,
            durationMinutes = 30,
            earliest = LocalTime.of(9, 7),
            dayStart = LocalTime.of(8, 0),
            dayEnd = LocalTime.of(12, 0)
        )

        assertEquals(LocalTime.of(9, 15), slots.first().start)
        assertEquals(LocalTime.of(9, 45), slots.first().end)
    }

    @Test
    fun previousSlotBeforeUsesLatestAvailableWindow() {
        val busy = event("busy", LocalTime.of(10, 0), LocalTime.of(11, 0))

        val slot = PlanningEngine.previousSlotBefore(
            items = listOf(busy),
            date = date,
            before = LocalTime.of(13, 0),
            durationMinutes = 60,
            dayStart = LocalTime.of(8, 0)
        )

        requireNotNull(slot)
        assertEquals(LocalTime.of(12, 0), slot.start)
        assertEquals(LocalTime.of(13, 0), slot.end)
    }

    @Test
    fun overlapMinutesReturnsOnlyActualIntersection() {
        val first = event("a", LocalTime.of(9, 0), LocalTime.of(10, 30))
        val second = event("b", LocalTime.of(10, 0), LocalTime.of(11, 0))

        assertEquals(30, PlanningEngine.overlapMinutes(first, second))
    }

    @Test
    fun allDayItemsNeverCreateTimedOverlap() {
        val timed = event("timed", LocalTime.of(9, 0), LocalTime.of(10, 0))
        val allDay = event("all-day", LocalTime.of(9, 0), LocalTime.of(10, 0), allDay = true)

        assertEquals(0, PlanningEngine.overlapMinutes(timed, allDay))
    }
}
