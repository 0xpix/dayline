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
            .filter { it.occursOn(date) && it.startTime != null }
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
            if (start.isAfter(cursor)) {
                addIfLongEnough(result, date, cursor, start, minMinutes)
            }
            if (end.isAfter(cursor)) cursor = end
        }
        if (dayEnd.isAfter(cursor)) {
            addIfLongEnough(result, date, cursor, dayEnd, minMinutes)
        }
        return result
    }

    fun fittingSlots(
        items: List<DaylineItem>,
        date: LocalDate,
        durationMinutes: Int,
        earliest: LocalTime? = null,
        maxResults: Int = 4
    ): List<FreeSlot> {
        val wanted = durationMinutes.coerceIn(15, 8 * 60)
        return freeSlots(items, date, minMinutes = wanted)
            .mapNotNull { slot ->
                var start = slot.start
                if (earliest != null && earliest.isAfter(start)) start = roundUpQuarter(earliest)
                if (!slot.end.isAfter(start)) return@mapNotNull null
                val end = start.plusMinutes(wanted.toLong())
                if (end.isAfter(slot.end) || !end.isAfter(start)) null
                else FreeSlot(date, start, end)
            }
            .take(maxResults)
    }

    fun suggestions(
        items: List<DaylineItem>,
        fromDate: LocalDate,
        durationMinutes: Int,
        horizonDays: Int = 7,
        now: LocalDateTime = LocalDateTime.now(),
        maxResults: Int = 5
    ): List<FreeSlot> {
        val results = mutableListOf<FreeSlot>()
        for (offset in 0 until horizonDays.coerceAtLeast(1)) {
            val date = fromDate.plusDays(offset.toLong())
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

    fun nextSlotAfter(
        items: List<DaylineItem>,
        date: LocalDate,
        after: LocalTime,
        durationMinutes: Int
    ): FreeSlot? = fittingSlots(
        items = items,
        date = date,
        durationMinutes = durationMinutes,
        earliest = after,
        maxResults = 1
    ).firstOrNull()

    private fun busyInterval(
        item: DaylineItem,
        dayStart: LocalTime,
        dayEnd: LocalTime
    ): Pair<LocalTime, LocalTime>? {
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
        if (Duration.between(start, end).toMinutes() >= minMinutes) {
            target += FreeSlot(date, start, end)
        }
    }

    private fun roundUpQuarter(time: LocalTime): LocalTime {
        val minutes = time.hour * 60 + time.minute
        val rounded = ((minutes + 14) / 15) * 15
        return LocalTime.of((rounded / 60).coerceAtMost(23), rounded % 60)
    }
}
