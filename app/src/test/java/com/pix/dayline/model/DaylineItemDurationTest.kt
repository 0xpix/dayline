package com.pix.dayline.model

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime

class DaylineItemDurationTest {
    private val item = DaylineItem(
        id = "event",
        title = "Long block",
        kind = AgendaKind.EVENT,
        startDate = LocalDate.of(2026, 9, 26),
        startTime = LocalTime.of(6, 0),
        endTime = LocalTime.of(9, 30),
        recurrence = Recurrence.ONCE
    )

    @Test
    fun movingStartPreservesExactDuration() {
        val moved = item.withStartPreservingDuration(LocalTime.of(8, 0))

        assertEquals(LocalTime.of(8, 0), moved.startTime)
        assertEquals(LocalTime.of(11, 30), moved.endTime)
        assertEquals(210L, moved.durationMinutes)
    }

    @Test
    fun topEdgeChangesStartButKeepsEnd() {
        val resized = item.withStartEdge(LocalTime.of(7, 15))

        assertEquals(LocalTime.of(7, 15), resized.startTime)
        assertEquals(LocalTime.of(9, 30), resized.endTime)
        assertEquals(135L, resized.durationMinutes)
    }

    @Test
    fun bottomEdgeChangesEndButKeepsStart() {
        val resized = item.withEndEdge(LocalTime.of(10, 45))

        assertEquals(LocalTime.of(6, 0), resized.startTime)
        assertEquals(LocalTime.of(10, 45), resized.endTime)
        assertEquals(285L, resized.durationMinutes)
    }

    @Test
    fun edgesEnforceFiveMinuteMinimum() {
        val top = item.withStartEdge(LocalTime.of(9, 30))
        assertEquals(LocalTime.of(9, 25), top.startTime)
        assertEquals(LocalTime.of(9, 30), top.endTime)

        val bottom = item.withEndEdge(LocalTime.of(6, 0))
        assertEquals(LocalTime.of(6, 0), bottom.startTime)
        assertEquals(LocalTime.of(6, 5), bottom.endTime)
    }
}
