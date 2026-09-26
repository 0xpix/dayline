package com.pix.dayline.data

import com.pix.dayline.model.AgendaKind
import com.pix.dayline.model.DaylineSpace
import com.pix.dayline.model.Recurrence
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
        assertEquals(true, parsed.dateExplicit)
        assertEquals(true, parsed.hasDirectives)
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
    fun plainTitleHasNoDirectivesAndTaskKeywordIsExplicit() {
        val plain = QuickAddParser.parse("write report", today = thursday)!!
        assertEquals(false, plain.hasDirectives)
        assertEquals(false, plain.kindExplicit)
        assertEquals(false, plain.dateExplicit)

        val task = QuickAddParser.parse("task write report", today = thursday)!!
        assertEquals(true, task.kindExplicit)
        assertEquals(true, task.hasDirectives)
        assertEquals(AgendaKind.TASK, task.kind)
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
    @Test
    fun parsesDotSeparatedTimeSpaceRepeatReminderAndCleanTitle() {
        val personal = DaylineSpace(id = "personal", name = "Personal")
        val parsed = QuickAddParser.parse(
            "Lumen project.11:00-14:00.personal.daily.5min",
            today = thursday,
            spaces = listOf(personal)
        )!!

        assertEquals("Lumen project", parsed.title)
        assertEquals(LocalTime.of(11, 0), parsed.startTime)
        assertEquals(180, parsed.durationMinutes)
        assertEquals(Recurrence.DAILY, parsed.recurrence)
        assertEquals(5, parsed.reminderMinutes)
        assertEquals("personal", parsed.spaceId)
        assertEquals("Personal", parsed.spaceName)
    }

    @Test
    fun dotShorthandPreservesPeriodsInEventNames() {
        val personal = DaylineSpace(id = "personal", name = "Personal")
        val parsed = QuickAddParser.parse(
            "Dr. appointment.14:00.personal.daily.10min",
            today = thursday,
            spaces = listOf(personal)
        )!!

        assertEquals("Dr. appointment", parsed.title)
        assertEquals(LocalTime.of(14, 0), parsed.startTime)
        assertEquals(Recurrence.DAILY, parsed.recurrence)
        assertEquals(10, parsed.reminderMinutes)
        assertEquals("personal", parsed.spaceId)
    }

    @Test
    fun plainDailyTitleIsNotTreatedAsDotDirective() {
        val parsed = QuickAddParser.parse("daily", today = thursday)!!
        assertEquals("daily", parsed.title)
        assertNull(parsed.recurrence)
        assertEquals(false, parsed.hasDirectives)
    }

    @Test
    fun parsesTimeRangeRecurrenceReminderAndTonight() {
        val work = QuickAddParser.parse("work 6-9:30 every weekday", today = thursday)!!
        assertEquals("work", work.title)
        assertEquals(LocalTime.of(6, 0), work.startTime)
        assertEquals(210, work.durationMinutes)
        assertEquals(com.pix.dayline.model.Recurrence.WEEKDAYS, work.recurrence)

        val dentist = QuickAddParser.parse("dentist Monday 14:00 remind 30m", today = thursday)!!
        assertEquals(LocalDate.of(2026, 9, 28), dentist.date)
        assertEquals(LocalTime.of(14, 0), dentist.startTime)
        assertEquals(30, dentist.reminderMinutes)

        val study = QuickAddParser.parse("study 2h tonight", today = thursday)!!
        assertEquals("study", study.title)
        assertEquals(LocalTime.of(19, 0), study.startTime)
        assertEquals(120, study.durationMinutes)
    }

}
