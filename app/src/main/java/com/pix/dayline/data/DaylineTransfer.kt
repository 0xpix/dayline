package com.pix.dayline.data

import com.pix.dayline.model.*
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.UUID

object DaylineTransfer {
    private val dateTimeFormatter = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss")
    private val dateFormatter = DateTimeFormatter.BASIC_ISO_DATE

    fun exportIcs(items: List<DaylineItem>): String = buildString {
        appendLine("BEGIN:VCALENDAR")
        appendLine("VERSION:2.0")
        appendLine("PRODID:-//Dayline//Calendar//EN")
        appendLine("CALSCALE:GREGORIAN")

        items.filter { it.kind == AgendaKind.EVENT }.forEach { item ->
            appendLine("BEGIN:VEVENT")
            appendLine("UID:${escape(item.id)}@dayline")
            appendLine("SUMMARY:${escape(item.title)}")

            if (item.startTime == null) {
                appendLine("DTSTART;VALUE=DATE:${item.startDate.format(dateFormatter)}")
                appendLine("DTEND;VALUE=DATE:${item.startDate.plusDays(1).format(dateFormatter)}")
            } else {
                val start = LocalDateTime.of(item.startDate, item.startTime)
                val end = LocalDateTime.of(
                    item.startDate,
                    item.endTime?.takeIf { it.isAfter(item.startTime) }
                        ?: item.startTime.plusHours(1)
                )
                appendLine("DTSTART:${start.format(dateTimeFormatter)}")
                appendLine("DTEND:${end.format(dateTimeFormatter)}")
            }

            recurrenceRule(item)?.let { appendLine("RRULE:$it") }
            appendLine("END:VEVENT")
        }

        appendLine("END:VCALENDAR")
    }

    fun importIcs(raw: String): List<DaylineItem> {
        val unfolded = raw.replace("\r\n ", "").replace("\n ", "")
        val events = mutableListOf<List<String>>()
        var current: MutableList<String>? = null

        unfolded.lineSequence().forEach { rawLine ->
            val line = rawLine.trimEnd('\r')
            when (line) {
                "BEGIN:VEVENT" -> current = mutableListOf()
                "END:VEVENT" -> current?.let(events::add).also { current = null }
                else -> current?.add(line)
            }
        }

        return events.mapNotNull(::parseEvent)
    }

    private fun parseEvent(lines: List<String>): DaylineItem? {
        val summary = property(lines, "SUMMARY")?.let(::unescape)?.ifBlank { "Imported event" }
            ?: "Imported event"
        val startLine = lines.firstOrNull { it.startsWith("DTSTART") } ?: return null
        val endLine = lines.firstOrNull { it.startsWith("DTEND") }
        val rrule = property(lines, "RRULE")

        val dateOnly = startLine.substringBefore(':').contains("VALUE=DATE")
        val startValue = startLine.substringAfter(':').trim()
        val endValue = endLine?.substringAfter(':')?.trim()

        val date: LocalDate
        val startTime: LocalTime?
        val endTime: LocalTime?

        if (dateOnly || startValue.length == 8) {
            date = LocalDate.parse(startValue.take(8), dateFormatter)
            startTime = null
            endTime = null
        } else {
            val start = parseDateTime(startValue) ?: return null
            val end = endValue?.let(::parseDateTime)
            date = start.toLocalDate()
            startTime = start.toLocalTime()
            endTime = end?.toLocalTime()
        }

        val recurrence = when {
            rrule?.contains("FREQ=DAILY") == true -> Recurrence.DAILY
            rrule?.contains("BYDAY=MO,TU,WE,TH,FR,SA") == true -> Recurrence.EXCEPT_SUNDAY
            rrule?.contains("BYDAY=MO,TU,WE,TH,FR") == true -> Recurrence.WEEKDAYS
            rrule?.contains("BYDAY=SU") == true -> Recurrence.SUNDAYS
            rrule?.contains("FREQ=WEEKLY") == true -> Recurrence.WEEKLY
            rrule?.contains("FREQ=MONTHLY") == true -> Recurrence.MONTHLY
            else -> Recurrence.ONCE
        }

        return DaylineItem(
            id = UUID.randomUUID().toString(),
            title = summary,
            kind = AgendaKind.EVENT,
            startDate = date,
            startTime = startTime,
            endTime = endTime,
            recurrence = recurrence
        )
    }

    private fun property(lines: List<String>, name: String): String? =
        lines.firstOrNull { it.startsWith("$name:") || it.startsWith("$name;") }
            ?.substringAfter(':')

    private fun parseDateTime(value: String): LocalDateTime? {
        val clean = value.removeSuffix("Z")
        return runCatching {
            LocalDateTime.parse(clean.take(15), dateTimeFormatter)
        }.getOrNull()
    }

    private fun recurrenceRule(item: DaylineItem): String? = when (item.recurrence) {
        Recurrence.ONCE -> null
        Recurrence.DAILY -> "FREQ=DAILY"
        Recurrence.WEEKDAYS -> "FREQ=WEEKLY;BYDAY=MO,TU,WE,TH,FR"
        Recurrence.WEEKLY -> "FREQ=WEEKLY"
        Recurrence.MONTHLY -> "FREQ=MONTHLY;BYMONTHDAY=${item.startDate.dayOfMonth}"
    }

    private fun escape(value: String): String = value
        .replace("\\", "\\\\")
        .replace(";", "\\;")
        .replace(",", "\\,")
        .replace("\n", "\\n")

    private fun unescape(value: String): String = value
        .replace("\\n", "\n")
        .replace("\\,", ",")
        .replace("\\;", ";")
        .replace("\\\\", "\\")
}
