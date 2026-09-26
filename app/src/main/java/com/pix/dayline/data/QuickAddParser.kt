package com.pix.dayline.data

import com.pix.dayline.model.AgendaKind
import com.pix.dayline.model.Recurrence
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
    val focusMinutes: Int? = null,
    val recurrence: Recurrence? = null,
    val repeatDays: Set<Int> = emptySet(),
    val reminderMinutes: Int? = null,
    val kindExplicit: Boolean = false,
    val dateExplicit: Boolean = false
) {
    val hasDirectives: Boolean
        get() = kindExplicit ||
            dateExplicit ||
            startTime != null ||
            durationMinutes != null ||
            deadlineDate != null ||
            focusMinutes != null ||
            recurrence != null ||
            reminderMinutes != null
}

/**
 * Deterministic, on-device shorthand parser used by Quick Add.
 *
 * Supported examples:
 * - gym tomorrow 18:30-20:00
 * - work 6-9:30 every weekday
 * - dentist monday 14:00 remind 30m
 * - study 2h tonight
 * - task report 45m due monday
 */
object QuickAddParser {
    fun parse(input: String, today: LocalDate = LocalDate.now()): ParsedQuickAdd? {
        var working = input.trim()
        if (working.isBlank()) return null

        var kind = AgendaKind.EVENT
        var kindExplicit = false
        var focus = false

        when {
            working.startsWith("task ", ignoreCase = true) || working.equals("task", ignoreCase = true) -> {
                kind = AgendaKind.TASK
                kindExplicit = true
                working = working.removePrefixIgnoreCase("task").trim()
            }
            working.startsWith("focus ", ignoreCase = true) || working.equals("focus", ignoreCase = true) -> {
                kind = AgendaKind.EVENT
                kindExplicit = true
                focus = true
                working = working.removePrefixIgnoreCase("focus").trim()
            }
        }

        var deadlineDate: LocalDate? = null
        DUE_REGEX.find(working)?.let { match ->
            deadlineDate = resolveDateToken(match.groupValues[1], today)
            working = working.removeRange(match.range)
        }

        var reminderMinutes: Int? = null
        REMINDER_REGEX.find(working)?.let { match ->
            val amount = match.groupValues[1].toIntOrNull() ?: 0
            val multiplier = if (match.groupValues[2].equals("h", true)) 60 else 1
            reminderMinutes = (amount * multiplier).takeIf { it > 0 }?.coerceAtMost(24 * 60)
            working = working.removeRange(match.range)
        }

        var recurrence: Recurrence? = null
        var repeatDays = emptySet<Int>()
        RECURRENCE_REGEX.find(working)?.let { match ->
            val token = match.groupValues[1].lowercase(Locale.ROOT)
            recurrence = when {
                token == "day" || token == "daily" -> Recurrence.DAILY
                token == "weekday" || token == "weekdays" -> Recurrence.WEEKDAYS
                token == "weekend" || token == "weekends" -> Recurrence.WEEKENDS
                token == "week" || token == "weekly" -> Recurrence.WEEKLY
                token == "month" || token == "monthly" -> Recurrence.MONTHLY
                weekday(token) != null -> {
                    repeatDays = setOf(weekday(token)!!.value)
                    Recurrence.CUSTOM
                }
                else -> null
            }
            working = working.removeRange(match.range)
        }

        var date = today
        var dateExplicit = false
        DATE_REGEX.find(working)?.let { match ->
            resolveDateToken(match.value, today)?.let { resolved ->
                date = resolved
                dateExplicit = true
                working = working.removeRange(match.range)
            }
        }

        var daypartTime: LocalTime? = null
        DAYPART_REGEX.find(working)?.let { match ->
            daypartTime = when (match.value.lowercase(Locale.ROOT)) {
                "morning" -> LocalTime.of(9, 0)
                "afternoon" -> LocalTime.of(15, 0)
                "evening", "tonight" -> LocalTime.of(19, 0)
                else -> null
            }
            dateExplicit = true
            working = working.removeRange(match.range)
        }

        var startTime: LocalTime? = null
        var durationMinutes: Int? = null

        TIME_RANGE_REGEX.find(working)?.let { match ->
            val start = parseClock(match.groupValues[1], match.groupValues[2])
            val end = parseClock(match.groupValues[3], match.groupValues[4])
            if (start != null && end != null && end.isAfter(start)) {
                startTime = start
                durationMinutes = java.time.Duration.between(start, end).toMinutes().toInt()
                working = working.removeRange(match.range)
            }
        }

        if (startTime == null) {
            TIME_REGEX.find(working)?.let { match ->
                val hour = match.groupValues[1].toIntOrNull()
                val minute = match.groupValues[2].toIntOrNull()
                if (hour != null && minute != null && hour in 0..23 && minute in 0..59) {
                    startTime = LocalTime.of(hour, minute)
                    working = working.removeRange(match.range)
                }
            }
        }

        if (durationMinutes == null) {
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
        }

        if (startTime == null) startTime = daypartTime

        val title = working
            .replace(Regex("""\s+"""), " ")
            .trim(' ', ',', '-', '–', '—', '·')
            .ifBlank { if (focus) "Focus" else "" }

        if (title.isBlank()) return null

        return ParsedQuickAdd(
            title = title,
            kind = kind,
            date = date,
            startTime = startTime,
            durationMinutes = durationMinutes,
            deadlineDate = deadlineDate,
            focusMinutes = durationMinutes.takeIf { focus },
            recurrence = recurrence,
            repeatDays = repeatDays,
            reminderMinutes = reminderMinutes,
            kindExplicit = kindExplicit,
            dateExplicit = dateExplicit
        )
    }

    private fun parseClock(hourRaw: String, minuteRaw: String): LocalTime? {
        val hour = hourRaw.toIntOrNull() ?: return null
        val minute = minuteRaw.takeIf { it.isNotBlank() }?.toIntOrNull() ?: 0
        if (hour !in 0..23 || minute !in 0..59) return null
        return LocalTime.of(hour, minute)
    }

    private fun resolveDateToken(token: String, today: LocalDate): LocalDate? =
        when (token.lowercase(Locale.ROOT)) {
            "today" -> today
            "tomorrow" -> today.plusDays(1)
            else -> weekday(token)?.let { day ->
                if (today.dayOfWeek == day) today else today.with(TemporalAdjusters.next(day))
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
    private val REMINDER_REGEX = Regex("""(?i)\bremind(?:\s+me)?\s+(\d{1,3})\s*([mh])\b""")
    private val RECURRENCE_REGEX = Regex(
        """(?i)\bevery\s+(day|daily|weekday|weekdays|weekend|weekends|week|weekly|month|monthly|monday|mon|tuesday|tue|wednesday|wed|thursday|thu|friday|fri|saturday|sat|sunday|sun)\b"""
    )
    private val DATE_REGEX = Regex("""(?i)\b$DATE_TOKEN\b""")
    private val DAYPART_REGEX = Regex("""(?i)\b(?:morning|afternoon|evening|tonight)\b""")
    private val TIME_RANGE_REGEX = Regex(
        """(?i)(?:\bat\s+)?\b([01]?\d|2[0-3])(?::([0-5]\d))?\s*(?:-|–|—|to)\s*([01]?\d|2[0-3])(?::([0-5]\d))?\b"""
    )
    private val TIME_REGEX = Regex("""(?i)(?:\bat\s+)?\b([01]?\d|2[0-3]):([0-5]\d)\b""")
    private val DURATION_REGEX = Regex(
        """(?i)\b(?:(?<hours>\d{1,2})h(?:(?<minutes>\d{1,2})m)?|(?<onlyMinutes>\d{1,3})m)\b"""
    )
}
