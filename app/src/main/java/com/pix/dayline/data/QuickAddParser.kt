package com.pix.dayline.data

import com.pix.dayline.model.AgendaKind
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.temporal.TemporalAdjusters
import java.util.Locale

data class ParsedQuickAdd(
    val title: String,
    val kind: AgendaKind,
    val date: LocalDate,
    val startTime: LocalTime? = null,
    val durationMinutes: Int? = null,
    val deadlineDate: LocalDate? = null,
    val focusMinutes: Int? = null
)

/**
 * Small deterministic parser for Dayline's local shorthand entry.
 *
 * It intentionally handles a narrow, documented grammar instead of pretending
 * to understand arbitrary natural language. No network or model is involved.
 */
object QuickAddParser {
    fun parse(input: String, today: LocalDate = LocalDate.now()): ParsedQuickAdd? {
        var working = input.trim()
        if (working.isBlank()) return null

        var kind = AgendaKind.EVENT
        var focus = false

        when {
            working.startsWith("task ", ignoreCase = true) || working.equals("task", ignoreCase = true) -> {
                kind = AgendaKind.TASK
                working = working.removePrefixIgnoreCase("task").trim()
            }
            working.startsWith("focus ", ignoreCase = true) || working.equals("focus", ignoreCase = true) -> {
                kind = AgendaKind.EVENT
                focus = true
                working = working.removePrefixIgnoreCase("focus").trim()
            }
        }

        var deadlineDate: LocalDate? = null
        DUE_REGEX.find(working)?.let { match ->
            deadlineDate = resolveDateToken(match.groupValues[1], today)
            working = working.removeRange(match.range)
        }

        var date = today
        DATE_REGEX.find(working)?.let { match ->
            resolveDateToken(match.value, today)?.let { resolved ->
                date = resolved
                working = working.removeRange(match.range)
            }
        }

        var startTime: LocalTime? = null
        TIME_REGEX.find(working)?.let { match ->
            val hour = match.groupValues[1].toIntOrNull()
            val minute = match.groupValues[2].toIntOrNull()
            if (hour != null && minute != null && hour in 0..23 && minute in 0..59) {
                startTime = LocalTime.of(hour, minute)
                working = working.removeRange(match.range)
            }
        }

        var durationMinutes: Int? = null
        DURATION_REGEX.find(working)?.let { match ->
            val hours = match.groups["hours"]?.value?.toIntOrNull() ?: 0
            val minutes = match.groups["minutes"]?.value?.toIntOrNull()
                ?: match.groups["onlyMinutes"]?.value?.toIntOrNull()
                ?: 0
            val total = hours * 60 + minutes
            if (total > 0) {
                durationMinutes = total.coerceAtMost(24 * 60)
                working = working.removeRange(match.range)
            }
        }

        val title = working
            .replace(Regex("""\s+"""), " ")
            .trim(' ', ',', '-', '·')
            .ifBlank { if (focus) "Focus" else "" }

        if (title.isBlank()) return null

        return ParsedQuickAdd(
            title = title,
            kind = kind,
            date = date,
            startTime = startTime,
            durationMinutes = durationMinutes,
            deadlineDate = deadlineDate,
            focusMinutes = durationMinutes.takeIf { focus }
        )
    }

    private fun resolveDateToken(token: String, today: LocalDate): LocalDate? {
        return when (token.lowercase(Locale.ROOT)) {
            "today" -> today
            "tomorrow" -> today.plusDays(1)
            else -> weekday(token)?.let { day ->
                if (today.dayOfWeek == day) today
                else today.with(TemporalAdjusters.next(day))
            }
        }
    }

    private fun weekday(token: String): DayOfWeek? = when (token.lowercase(Locale.ROOT).take(3)) {
        "mon" -> DayOfWeek.MONDAY
        "tue" -> DayOfWeek.TUESDAY
        "wed" -> DayOfWeek.WEDNESDAY
        "thu" -> DayOfWeek.THURSDAY
        "fri" -> DayOfWeek.FRIDAY
        "sat" -> DayOfWeek.SATURDAY
        "sun" -> DayOfWeek.SUNDAY
        else -> null
    }

    private fun String.removePrefixIgnoreCase(prefix: String): String =
        if (startsWith(prefix, ignoreCase = true)) substring(prefix.length) else this

    private val DATE_TOKEN =
        """(?:today|tomorrow|monday|mon|tuesday|tue|wednesday|wed|thursday|thu|friday|fri|saturday|sat|sunday|sun)"""

    private val DUE_REGEX = Regex("""(?i)\bdue\s+($DATE_TOKEN)\b""")
    private val DATE_REGEX = Regex("""(?i)\b$DATE_TOKEN\b""")
    private val TIME_REGEX = Regex("""(?i)(?:\bat\s+)?\b([01]?\d|2[0-3]):([0-5]\d)\b""")
    private val DURATION_REGEX = Regex(
        """(?i)\b(?:(?<hours>\d{1,2})h(?:(?<minutes>\d{1,2})m)?|(?<onlyMinutes>\d{1,3})m)\b"""
    )
}
