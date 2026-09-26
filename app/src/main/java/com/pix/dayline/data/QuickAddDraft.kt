package com.pix.dayline.data

import com.pix.dayline.model.AgendaKind
import com.pix.dayline.model.DaylineItem
import com.pix.dayline.model.FocusCycle
import com.pix.dayline.model.ItemColor
import com.pix.dayline.model.Recurrence
import com.pix.dayline.model.TaskPriority
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID

/**
 * Pure Quick Add state used to materialize a DaylineItem.
 *
 * Keeping these invariants outside Compose makes editing behavior testable and
 * prevents UI refactors from silently dropping or retaining hidden metadata.
 */
data class QuickAddDraft(
    val title: String,
    val kind: AgendaKind,
    val date: LocalDate,
    val startTime: LocalTime?,
    val endTime: LocalTime?,
    val recurrence: Recurrence,
    val repeatDays: Set<Int> = emptySet(),
    val reminderMinutes: Int? = null,
    val focusCycle: FocusCycle = FocusCycle.OFF,
    val customFocusMinutes: Int = 25,
    val customBreakMinutes: Int = 5,
    val bufferBeforeMinutes: Int = 0,
    val bufferAfterMinutes: Int = 0,
    val priority: TaskPriority = TaskPriority.NORMAL,
    val estimatedDurationMinutes: Int? = null,
    val earliestDate: LocalDate? = null,
    val deadlineDate: LocalDate? = null,
    val color: ItemColor = ItemColor.MONO,
    val spaceId: String? = null
) {
    fun applyingShorthand(parsed: ParsedQuickAdd?): QuickAddDraft {
        if (parsed == null || !parsed.hasDirectives) return this

        val nextKind = if (parsed.kindExplicit) parsed.kind else kind
        val nextDate = if (parsed.dateExplicit) parsed.date else date
        val nextStart = parsed.startTime ?: startTime
        val nextDuration = parsed.durationMinutes
        val nextEnd = when {
            nextKind == AgendaKind.EVENT && nextStart != null && nextDuration != null ->
                nextStart.plusMinutes(nextDuration.toLong())
            else -> endTime
        }
        val shorthandFocus = parsed.focusMinutes
            ?.takeIf { nextKind == AgendaKind.EVENT && nextStart != null }

        return copy(
            title = parsed.title,
            kind = nextKind,
            date = nextDate,
            startTime = nextStart,
            endTime = nextEnd,
            recurrence = parsed.recurrence ?: recurrence,
            repeatDays = parsed.repeatDays.takeIf { parsed.recurrence == Recurrence.CUSTOM } ?: repeatDays,
            reminderMinutes = parsed.reminderMinutes ?: reminderMinutes,
            focusCycle = if (shorthandFocus != null) FocusCycle.CUSTOM else focusCycle,
            customFocusMinutes = shorthandFocus ?: customFocusMinutes,
            estimatedDurationMinutes = if (nextKind == AgendaKind.TASK) {
                nextDuration ?: estimatedDurationMinutes
            } else {
                estimatedDurationMinutes
            },
            deadlineDate = if (nextKind == AgendaKind.TASK) {
                parsed.deadlineDate ?: deadlineDate
            } else {
                deadlineDate
            }
        )
    }

    fun toItem(
        editing: DaylineItem? = null,
        idFactory: () -> String = { UUID.randomUUID().toString() }
    ): DaylineItem {
        val validEnd = endTime?.takeIf { startTime != null && it.isAfter(startTime) }
        val allDay = kind == AgendaKind.EVENT && startTime == null
        val preserveCalendarIdentity = kind == AgendaKind.EVENT && editing?.kind == AgendaKind.EVENT
        val preservedTimeZone = if (
            preserveCalendarIdentity &&
            startTime != null &&
            editing?.allDay != true
        ) {
            editing.timeZoneId
        } else {
            null
        }

        return DaylineItem(
            id = editing?.id ?: idFactory(),
            title = title.trim(),
            kind = kind,
            startDate = date,
            startTime = startTime,
            endTime = validEnd,
            recurrence = recurrence,
            repeatDays = if (recurrence == Recurrence.CUSTOM) {
                repeatDays.ifEmpty { setOf(date.dayOfWeek.value) }
            } else {
                emptySet()
            },
            recurrenceEndDate = editing?.recurrenceEndDate,
            excludedDates = editing?.excludedDates ?: emptySet(),
            seriesParentId = editing?.seriesParentId,
            reminderMinutes = reminderMinutes.takeIf { startTime != null },
            focusCycle = focusCycle.takeIf {
                kind == AgendaKind.EVENT && startTime != null && validEnd != null
            } ?: FocusCycle.OFF,
            customFocusMinutes = customFocusMinutes.coerceIn(5, 180),
            customBreakMinutes = customBreakMinutes.coerceIn(1, 60),
            focusSessionsCompleted = editing?.focusSessionsCompleted ?: 0,
            focusedMinutesCompleted = editing?.focusedMinutesCompleted ?: 0,
            bufferBeforeMinutes = bufferBeforeMinutes.coerceIn(0, 180),
            bufferAfterMinutes = bufferAfterMinutes.coerceIn(0, 180),
            priority = priority,
            estimatedDurationMinutes = (
                estimatedDurationMinutes ?: editing?.estimatedDurationMinutes ?: 30
            ).coerceIn(15, 8 * 60),
            earliestDate = earliestDate ?: editing?.earliestDate,
            deadlineDate = deadlineDate ?: editing?.deadlineDate,
            allDay = allDay,
            timeZoneId = preservedTimeZone,
            color = color,
            spaceId = spaceId,
            details = editing?.details ?: emptyList(),
            completedDates = editing?.completedDates ?: emptySet(),
            calendarEventId = editing?.calendarEventId.takeIf { preserveCalendarIdentity },
            calendarId = editing?.calendarId.takeIf { preserveCalendarIdentity },
            calendarName = editing?.calendarName.takeIf { preserveCalendarIdentity },
            calendarReadOnly = editing?.calendarReadOnly?.takeIf { preserveCalendarIdentity } ?: false
        )
    }
}
