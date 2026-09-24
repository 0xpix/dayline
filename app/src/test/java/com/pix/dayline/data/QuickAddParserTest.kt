package com.pix.dayline.data

import com.pix.dayline.model.AgendaKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime

class QuickAddParserTest {
    private val thursday = LocalDate.of(2026, 9, 24)

    @Test
    fun parsesEventTomorrowWithTimeAndDuration() {
        val parsed = QuickAddParser.parse(
            "gym tomorrow 7:30 1h",
            today = thursday
        )!!

        assertEquals("gym", parsed.title)
        assertEquals(AgendaKind.EVENT, parsed.kind)
        assertEquals(LocalDate.of(2026, 9, 25), parsed.date)
        assertEquals(LocalTime.of(7, 30), parsed.startTime)
        assertEquals(60, parsed.durationMinutes)
        assertNull(parsed.deadlineDate)
    }

    @Test
    fun parsesNamedWeekdayEvent() {
        val parsed = QuickAddParser.parse(
            "dentist Friday 14:00",
            today = thursday
        )!!

        assertEquals("dentist", parsed.title)
        assertEquals(LocalDate.of(2026, 9, 25), parsed.date)
        assertEquals(LocalTime.of(14, 0), parsed.startTime)
        assertNull(parsed.durationMinutes)
    }

    @Test
    fun parsesTaskDurationAndDeadline() {
        val parsed = QuickAddParser.parse(
            "task report 45m due Monday",
            today = thursday
        )!!

        assertEquals("report", parsed.title)
        assertEquals(AgendaKind.TASK, parsed.kind)
        assertEquals(thursday, parsed.date)
        assertEquals(45, parsed.durationMinutes)
        assertEquals(LocalDate.of(2026, 9, 28), parsed.deadlineDate)
        assertNull(parsed.startTime)
    }

    @Test
    fun parsesFocusShortcutWithoutInventingATitle() {
        val parsed = QuickAddParser.parse(
            "focus 50m at 18:00",
            today = thursday
        )!!

        assertEquals("Focus", parsed.title)
        assertEquals(AgendaKind.EVENT, parsed.kind)
        assertEquals(thursday, parsed.date)
        assertEquals(LocalTime.of(18, 0), parsed.startTime)
        assertEquals(50, parsed.durationMinutes)
        assertEquals(50, parsed.focusMinutes)
    }

    @Test
    fun sameWeekdayResolvesToTodayAndInvalidInputReturnsNull() {
        val parsed = QuickAddParser.parse("dentist Thursday 09:00", today = thursday)!!
        assertEquals(thursday, parsed.date)
        assertNull(QuickAddParser.parse("   ", today = thursday))
        assertNull(QuickAddParser.parse("task tomorrow", today = thursday))
    }

    @Test
    fun durationSupportsHoursAndMinutesAndClampsAtOneDay() {
        assertEquals(
            90,
            QuickAddParser.parse("meeting 1h30m", today = thursday)!!.durationMinutes
        )
        assertEquals(
            24 * 60,
            QuickAddParser.parse("marathon 99h", today = thursday)!!.durationMinutes
        )
    }
}
