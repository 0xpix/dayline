package com.pix.dayline.data

import com.pix.dayline.model.AgendaKind
import com.pix.dayline.model.DaylineSpace
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime
import java.util.Locale

class QuickAddPreviewTest {
    private val thursday = LocalDate.of(2026, 9, 24)

    @Test
    fun plainTitlesStayQuiet() {
        val parsed = QuickAddParser.parse("write report", today = thursday)!!
        assertNull(parsed.previewLabel(today = thursday, locale = Locale.US))
    }

    @Test
    fun eventPreviewShowsOnlyRecognizedDirectives() {
        val parsed = QuickAddParser.parse(
            "gym tomorrow 7:30 1h",
            today = thursday
        )!!

        assertEquals(
            "Tomorrow · 07:30 · 1h",
            parsed.previewLabel(today = thursday, locale = Locale.US)
        )
    }

    @Test
    fun dotShorthandPreviewShowsEverythingRecognized() {
        val parsed = QuickAddParser.parse(
            "Lumen project.11:00-14:00.personal.daily.5min",
            today = thursday,
            spaces = listOf(DaylineSpace(id = "personal", name = "Personal"))
        )!!

        assertEquals(
            "11:00 · 3h · Personal · Every day · Remind 5m before",
            parsed.previewLabel(today = thursday, locale = Locale.US)
        )
    }

    @Test
    fun taskPreviewIncludesExplicitTypeDurationAndDeadline() {
        val parsed = QuickAddParser.parse(
            "task report 45m due Monday",
            today = thursday
        )!!

        assertEquals(AgendaKind.TASK, parsed.kind)
        assertEquals(
            "Task · 45m · Due Mon, Sep 28",
            parsed.previewLabel(today = thursday, locale = Locale.US)
        )
    }

    @Test
    fun focusPreviewIsCompactAndReadable() {
        val parsed = QuickAddParser.parse(
            "focus 50m at 18:00",
            today = thursday
        )!!

        assertEquals(LocalTime.of(18, 0), parsed.startTime)
        assertEquals(
            "Focus · 18:00 · 50m",
            parsed.previewLabel(today = thursday, locale = Locale.US)
        )
    }

    @Test
    fun focusPreviewSignalsWhenATimeIsStillMissing() {
        val parsed = QuickAddParser.parse(
            "focus 50m",
            today = thursday
        )!!

        assertEquals(
            "Focus · 50m · Needs time",
            parsed.previewLabel(today = thursday, locale = Locale.US)
        )
    }

    @Test
    fun manuallySelectedTimeCompletesFocusPreview() {
        val parsed = QuickAddParser.parse(
            "focus 50m",
            today = thursday
        )!!

        assertEquals(
            "Focus · 50m",
            parsed.previewLabel(
                today = thursday,
                locale = Locale.US,
                currentStartTime = LocalTime.of(18, 0)
            )
        )
    }

    @Test
    fun focusCompletenessUsesParsedOrManualStartTime() {
        val untimed = QuickAddParser.parse(
            "focus 50m",
            today = thursday
        )!!
        val timed = QuickAddParser.parse(
            "focus 50m at 18:00",
            today = thursday
        )!!

        assertEquals(true, untimed.needsFocusStartTime())
        assertEquals(
            false,
            untimed.needsFocusStartTime(currentStartTime = LocalTime.of(18, 0))
        )
        assertEquals(false, timed.needsFocusStartTime())
    }

    @Test
    fun mixedDurationUsesCompactHourMinuteForm() {
        val parsed = QuickAddParser.parse(
            "meeting Friday 09:00 1h30m",
            today = thursday
        )!!

        assertEquals(
            "Tomorrow · 09:00 · 1h30m",
            parsed.previewLabel(today = thursday, locale = Locale.US)
        )
    }
}
