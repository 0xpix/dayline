package com.pix.dayline.planning

import com.pix.dayline.model.AgendaKind
import com.pix.dayline.model.DaylineItem
import com.pix.dayline.model.occursOn
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

/** Pure, local planning helpers. No network, AI, or account state is involved. */
data class FreeSlot(
    val date: LocalDate,
    val start: LocalTime,
    val end: LocalTime
) {
    val durationMinutes: Int
        get() = Duration.between(start, end).toMinutes().toInt().coerceAtLeast(0)
}

object PlanningEngine {
    private val defaultDayStart = LocalTime.of(7, 0)
    private val defaultDayEnd = LocalTime.of(22, 0)

    fun freeSlots(
        items: List<DaylineItem>,
        date: LocalDate,
        dayStart: LocalTime = defaultDayStart,
        dayEnd: LocalTime = defaultDayEnd,
        minMinutes: Int = 15
    ): List<FreeSlot> {
        if (!dayEnd.isAfter(dayStart)) return emptyList()

        val busy = items
            .filter { it.occursOn(date) && it.startTime != null && !it.allDay }
            .mapNotNull { item -> busyInterval(item, dayStart, dayEnd) }
            .sortedBy { it.first }

        val merged = mutableListOf<Pair<LocalTime, LocalTime>>()
        busy.forEach { interval ->
            val last = merged.lastOrNull()
            if (last == null || interval.first.isAfter(last.second)) {
                merged += interval
            } else if (interval.second.isAfter(last.second)) {
                merged[merged.lastIndex] = last.first to interval.second
            }
        }

        val result = mutableListOf<FreeSlot>()
        var cursor = dayStart
        merged.forEach { (start, end) ->
            if (start.isAfter(cursor)) addIfLongEnough(result, date, cursor, start, minMinutes)
            if (end.isAfter(cursor)) cursor = end
        }
        if (dayEnd.isAfter(cursor)) addIfLongEnough(result, date, cursor, dayEnd, minMinutes)
        return result
    }

    fun fittingSlots(
        items: List<DaylineItem>,
        date: LocalDate,
        durationMinutes: Int,
        earliest: LocalTime? = null,
        maxResults: Int = 4,
        dayStart: LocalTime = defaultDayStart,
        dayEnd: LocalTime = defaultDayEnd
    ): List<FreeSlot> {
        val wanted = durationMinutes.coerceIn(15, 8 * 60)
        return freeSlots(items, date, dayStart, dayEnd, wanted)
            .mapNotNull { slot ->
                var start = slot.start
                if (earliest != null && earliest.isAfter(start)) start = roundUpQuarter(earliest)
                if (!slot.end.isAfter(start)) return@mapNotNull null
                val end = start.plusMinutes(wanted.toLong())
                if (end.isAfter(slot.end) || !end.isAfter(start)) null else FreeSlot(date, start, end)
            }
            .take(maxResults)
    }

    fun suggestions(
        items: List<DaylineItem>,
        fromDate: LocalDate,
        durationMinutes: Int,
        horizonDays: Int = 7,
        now: LocalDateTime = LocalDateTime.now(),
        maxResults: Int = 5,
        untilDate: LocalDate? = null
    ): List<FreeSlot> {
        val results = mutableListOf<FreeSlot>()
        for (offset in 0 until horizonDays.coerceAtLeast(1)) {
            val date = fromDate.plusDays(offset.toLong())
            if (untilDate != null && date.isAfter(untilDate)) break
            val earliest = if (date == now.toLocalDate()) now.toLocalTime() else null
            results += fittingSlots(
                items = items,
                date = date,
                durationMinutes = durationMinutes,
                earliest = earliest,
                maxResults = maxResults - results.size
            )
            if (results.size >= maxResults) break
        }
        return results.take(maxResults)
    }

    fun suggestionsForTask(
        items: List<DaylineItem>,
        task: DaylineItem,
        now: LocalDateTime = LocalDateTime.now(),
        maxResults: Int = 5
    ): List<FreeSlot> {
        val start = maxOf(task.earliestDate ?: task.startDate, now.toLocalDate())
        val deadline = task.deadlineDate?.takeIf { !it.isBefore(start) }
        val horizon = if (deadline != null) {
            Duration.between(start.atStartOfDay(), deadline.plusDays(1).atStartOfDay()).toDays().toInt().coerceAtLeast(1)
        } else 7
        return suggestions(
            items = items.filterNot { it.id == task.id },
            fromDate = start,
            durationMinutes = task.estimatedDurationMinutes,
            horizonDays = horizon.coerceAtMost(31),
            now = now,
            maxResults = maxResults,
            untilDate = deadline
        )
    }

    fun nextSlotAfter(
        items: List<DaylineItem>,
        date: LocalDate,
        after: LocalTime,
        durationMinutes: Int,
        dayEnd: LocalTime = defaultDayEnd
    ): FreeSlot? = fittingSlots(
        items = items,
        date = date,
        durationMinutes = durationMinutes,
        earliest = after,
        maxResults = 1,
        dayEnd = dayEnd
    ).firstOrNull()

    fun previousSlotBefore(
        items: List<DaylineItem>,
        date: LocalDate,
        before: LocalTime,
        durationMinutes: Int,
        dayStart: LocalTime = defaultDayStart
    ): FreeSlot? {
        val wanted = durationMinutes.coerceIn(15, 8 * 60)
        return freeSlots(items, date, dayStart = dayStart, dayEnd = before, minMinutes = wanted)
            .asReversed()
            .firstNotNullOfOrNull { slot ->
                val end = slot.end
                val start = end.minusMinutes(wanted.toLong())
                if (start.isBefore(slot.start)) null else FreeSlot(date, start, end)
            }
    }

    fun overlapMinutes(first: DaylineItem, second: DaylineItem): Int {
        if (first.allDay || second.allDay) return 0
        val aStart = first.startTime ?: return 0
        val bStart = second.startTime ?: return 0
        val aEnd = first.endTime?.takeIf { it.isAfter(aStart) }
            ?: aStart.plusMinutes(first.planningDurationMinutes.toLong())
        val bEnd = second.endTime?.takeIf { it.isAfter(bStart) }
            ?: bStart.plusMinutes(second.planningDurationMinutes.toLong())
        val start = maxOf(aStart, bStart)
        val end = minOf(aEnd, bEnd)
        return if (end.isAfter(start)) Duration.between(start, end).toMinutes().toInt() else 0
    }

    private fun busyInterval(
        item: DaylineItem,
        dayStart: LocalTime,
        dayEnd: LocalTime
    ): Pair<LocalTime, LocalTime>? {
        if (item.allDay) return null
        val rawStart = item.startTime ?: return null
        val rawEnd = when {
            item.endTime != null && item.endTime.isAfter(rawStart) -> item.endTime
            item.kind == AgendaKind.TASK -> rawStart.plusMinutes(item.estimatedDurationMinutes.toLong())
            else -> rawStart.plusHours(1)
        }
        if (!rawEnd.isAfter(rawStart)) return null

        val bufferedStart = rawStart.minusMinutes(item.bufferBeforeMinutes.toLong())
        val bufferedEnd = rawEnd.plusMinutes(item.bufferAfterMinutes.toLong())
        val start = if (bufferedStart.isBefore(dayStart)) dayStart else bufferedStart
        val end = if (bufferedEnd.isAfter(dayEnd)) dayEnd else bufferedEnd
        return if (end.isAfter(start)) start to end else null
    }

    private fun addIfLongEnough(
        target: MutableList<FreeSlot>,
        date: LocalDate,
        start: LocalTime,
        end: LocalTime,
        minMinutes: Int
    ) {
        if (Duration.between(start, end).toMinutes() >= minMinutes) target += FreeSlot(date, start, end)
    }

    private fun roundUpQuarter(time: LocalTime): LocalTime {
        val minutes = time.hour * 60 + time.minute
        val rounded = ((minutes + 14) / 15) * 15
        return if (rounded >= 24 * 60) LocalTime.of(23, 45) else LocalTime.of(rounded / 60, rounded % 60)
    }
}
