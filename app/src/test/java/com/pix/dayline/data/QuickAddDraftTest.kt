package com.pix.dayline.data

import com.pix.dayline.model.AgendaKind
import com.pix.dayline.model.DaylineItem
import com.pix.dayline.model.FocusCycle
import com.pix.dayline.model.Recurrence
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime

class QuickAddDraftTest {
    private val date = LocalDate.of(2026, 9, 24)

    private fun providerEvent() = DaylineItem(
        id = "event-1",
        title = "Mapped event",
        kind = AgendaKind.EVENT,
        startDate = date,
        startTime = LocalTime.of(9, 0),
        endTime = LocalTime.of(10, 0),
        recurrence = Recurrence.ONCE,
        focusSessionsCompleted = 2,
        focusedMinutesCompleted = 75,
        calendarEventId = 42L,
        calendarId = 7L,
        calendarName = "Work",
        calendarReadOnly = true,
        timeZoneId = "Europe/Berlin"
    )

    @Test
    fun eventEditPreservesProviderIdentityAndHiddenProgress() {
        val editing = providerEvent()
        val saved = QuickAddDraft(
            title = "Renamed event",
            kind = AgendaKind.EVENT,
            date = date,
            startTime = LocalTime.of(9, 30),
            endTime = LocalTime.of(10, 30),
            recurrence = Recurrence.ONCE
        ).toItem(editing = editing)

        assertEquals(editing.id, saved.id)
        assertEquals(42L, saved.calendarEventId)
        assertEquals(7L, saved.calendarId)
        assertEquals("Work", saved.calendarName)
        assertTrue(saved.calendarReadOnly)
        assertEquals("Europe/Berlin", saved.timeZoneId)
        assertEquals(2, saved.focusSessionsCompleted)
        assertEquals(75, saved.focusedMinutesCompleted)
    }

    @Test
    fun convertingProviderEventToTaskClearsCalendarIdentity() {
        val saved = QuickAddDraft(
            title = "Turn into task",
            kind = AgendaKind.TASK,
            date = date,
            startTime = LocalTime.of(9, 30),
            endTime = LocalTime.of(10, 30),
            recurrence = Recurrence.ONCE,
            focusCycle = FocusCycle.POMODORO_25_5
        ).toItem(editing = providerEvent())

        assertNull(saved.calendarEventId)
        assertNull(saved.calendarId)
        assertNull(saved.calendarName)
        assertFalse(saved.calendarReadOnly)
        assertNull(saved.timeZoneId)
        assertEquals(FocusCycle.OFF, saved.focusCycle)
    }

    @Test
    fun convertingTimedEventToAllDayKeepsMappingButDropsTimedMetadata() {
        val saved = QuickAddDraft(
            title = "All day",
            kind = AgendaKind.EVENT,
            date = date,
            startTime = null,
            endTime = null,
            recurrence = Recurrence.ONCE,
            reminderMinutes = 15,
            focusCycle = FocusCycle.POMODORO_25_5
        ).toItem(editing = providerEvent())

        assertTrue(saved.allDay)
        assertEquals(42L, saved.calendarEventId)
        assertNull(saved.timeZoneId)
        assertNull(saved.reminderMinutes)
        assertEquals(FocusCycle.OFF, saved.focusCycle)
    }

    @Test
    fun shorthandOnlyOverridesExplicitFields() {
        val manual = QuickAddDraft(
            title = "report tomorrow 45m",
            kind = AgendaKind.TASK,
            date = date,
            startTime = null,
            endTime = null,
            recurrence = Recurrence.ONCE,
            estimatedDurationMinutes = 30
        )
        val parsed = QuickAddParser.parse(manual.title, today = date)!!

        val resolved = manual.applyingShorthand(parsed)

        assertEquals(AgendaKind.TASK, resolved.kind)
        assertEquals(date.plusDays(1), resolved.date)
        assertEquals(45, resolved.estimatedDurationMinutes)
        assertEquals("report", resolved.title)
    }

    @Test
    fun explicitTaskDirectiveOverridesManualEventType() {
        val manual = QuickAddDraft(
            title = "task report 45m due Monday",
            kind = AgendaKind.EVENT,
            date = date,
            startTime = null,
            endTime = null,
            recurrence = Recurrence.ONCE
        )
        val parsed = QuickAddParser.parse(manual.title, today = date)!!

        val resolved = manual.applyingShorthand(parsed)

        assertEquals(AgendaKind.TASK, resolved.kind)
        assertEquals(45, resolved.estimatedDurationMinutes)
        assertEquals(LocalDate.of(2026, 9, 28), resolved.deadlineDate)
        assertEquals("report", resolved.title)
    }

    @Test
    fun eventDurationBuildsEndTimeAndFocusShortcutBuildsCustomCycle() {
        val eventDraft = QuickAddDraft(
            title = "gym tomorrow 7:30 1h",
            kind = AgendaKind.EVENT,
            date = date,
            startTime = null,
            endTime = null,
            recurrence = Recurrence.ONCE
        ).applyingShorthand(
            QuickAddParser.parse("gym tomorrow 7:30 1h", today = date)
        )

        assertEquals(date.plusDays(1), eventDraft.date)
        assertEquals(LocalTime.of(7, 30), eventDraft.startTime)
        assertEquals(LocalTime.of(8, 30), eventDraft.endTime)

        val focusDraft = QuickAddDraft(
            title = "focus 50m at 18:00",
            kind = AgendaKind.TASK,
            date = date,
            startTime = null,
            endTime = null,
            recurrence = Recurrence.ONCE
        ).applyingShorthand(
            QuickAddParser.parse("focus 50m at 18:00", today = date)
        )

        assertEquals(AgendaKind.EVENT, focusDraft.kind)
        assertEquals(FocusCycle.CUSTOM, focusDraft.focusCycle)
        assertEquals(50, focusDraft.customFocusMinutes)
        assertEquals(LocalTime.of(18, 0), focusDraft.startTime)
        assertEquals(LocalTime.of(18, 50), focusDraft.endTime)
        assertEquals("Focus", focusDraft.title)
    }

    @Test
    fun newCustomItemUsesDeterministicIdAndDateDayFallback() {
        val saved = QuickAddDraft(
            title = "  New item  ",
            kind = AgendaKind.EVENT,
            date = date,
            startTime = LocalTime.of(14, 0),
            endTime = LocalTime.of(13, 0),
            recurrence = Recurrence.CUSTOM,
            repeatDays = emptySet(),
            customFocusMinutes = 999,
            customBreakMinutes = 0
        ).toItem(idFactory = { "new-id" })

        assertEquals("new-id", saved.id)
        assertEquals("New item", saved.title)
        assertEquals(setOf(date.dayOfWeek.value), saved.repeatDays)
        assertNull(saved.endTime)
        assertEquals(180, saved.customFocusMinutes)
        assertEquals(1, saved.customBreakMinutes)
    }
    @Test
    fun shorthandAppliesRepeatAndReminder() {
        val resolved = QuickAddDraft(
            title = "work 6-9:30 every weekday remind 15m",
            kind = AgendaKind.EVENT,
            date = date,
            startTime = null,
            endTime = null,
            recurrence = Recurrence.ONCE
        ).applyingShorthand(
            QuickAddParser.parse("work 6-9:30 every weekday remind 15m", today = date)
        )

        assertEquals(LocalTime.of(6, 0), resolved.startTime)
        assertEquals(LocalTime.of(9, 30), resolved.endTime)
        assertEquals(Recurrence.WEEKDAYS, resolved.recurrence)
        assertEquals(15, resolved.reminderMinutes)
    }

}
