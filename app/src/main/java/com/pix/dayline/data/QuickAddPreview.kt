package com.pix.dayline.data

import com.pix.dayline.model.Recurrence
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Short, deterministic description of the directives Quick Add recognized.
 *
 * The title itself is intentionally omitted: the user can already see what
 * they typed. Only explicit shorthand is surfaced so manual form choices are
 * never misrepresented by the preview.
 */
fun ParsedQuickAdd.previewLabel(
    today: LocalDate,
    locale: Locale = Locale.getDefault(),
    currentStartTime: LocalTime? = null
): String? {
    if (!hasDirectives) return null

    val focusNeedsTime = needsFocusStartTime(currentStartTime)

    val parts = buildList {
        if (kindExplicit) {
            add(
                if (focusMinutes != null) {
                    "Focus"
                } else {
                    kind.name.lowercase().replaceFirstChar { it.titlecase(locale) }
                }
            )
        }

        if (dateExplicit) {
            add(
                when (date) {
                    today -> "Today"
                    today.plusDays(1) -> "Tomorrow"
                    else -> date.format(DateTimeFormatter.ofPattern("EEE, MMM d", locale))
                }
            )
        }

        startTime?.let {
            add(it.format(DateTimeFormatter.ofPattern("HH:mm", locale)))
        }

        durationMinutes?.let { add(formatQuickAddDuration(it)) }

        spaceName?.let { add(it) }

        if (focusNeedsTime) add("Needs time")

        recurrence?.let { repeat ->
            add(
                when (repeat) {
                    Recurrence.DAILY -> "Every day"
                    Recurrence.WEEKDAYS -> "Weekdays"
                    Recurrence.WEEKENDS -> "Weekends"
                    Recurrence.WEEKLY -> "Weekly"
                    Recurrence.MONTHLY -> "Monthly"
                    Recurrence.CUSTOM -> repeatDays
                        .sorted()
                        .joinToString("/") { java.time.DayOfWeek.of(it).name.take(3).lowercase().replaceFirstChar(Char::titlecase) }
                    Recurrence.ONCE -> "Once"
                }
            )
        }

        reminderMinutes?.let { add("Remind ${formatQuickAddDuration(it)} before") }

        deadlineDate?.let { deadline ->
            add("Due ${deadline.format(DateTimeFormatter.ofPattern("EEE, MMM d", locale))}")
        }
    }

    return parts.takeIf { it.isNotEmpty() }?.joinToString(" · ")
}


fun ParsedQuickAdd.needsFocusStartTime(
    currentStartTime: LocalTime? = null
): Boolean =
    focusMinutes != null && (startTime ?: currentStartTime) == null

private fun formatQuickAddDuration(minutes: Int): String = when {
    minutes < 60 -> "${minutes}m"
    minutes % 60 == 0 -> "${minutes / 60}h"
    else -> "${minutes / 60}h${minutes % 60}m"
}
