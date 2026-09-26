package com.pix.dayline.data

import java.time.Duration
import java.time.LocalTime

object CalendarProviderTimePolicy {
    private val rfc2445Duration = Regex(
        """^([+-])?P(?:(\d+)W|(?:(\d+)D)?(?:T(?:(\d+)H)?(?:(\d+)M)?(?:(\d+)S)?)?)$"""
    )

    fun durationMillis(raw: String?): Long? {
        val value = raw?.trim()?.uppercase()?.takeIf { it.isNotEmpty() } ?: return null

        runCatching { Duration.parse(value).toMillis() }
            .getOrNull()
            ?.takeIf { it > 0L }
            ?.let { return it }

        val match = rfc2445Duration.matchEntire(value) ?: return null
        if (match.groupValues[1] == "-") return null

        val weeks = match.groupValues[2].toLongOrNull() ?: 0L
        val days = match.groupValues[3].toLongOrNull() ?: 0L
        val hours = match.groupValues[4].toLongOrNull() ?: 0L
        val minutes = match.groupValues[5].toLongOrNull() ?: 0L
        val seconds = match.groupValues[6].toLongOrNull() ?: 0L

        val totalSeconds = weeks * 7L * 24L * 3600L +
            days * 24L * 3600L +
            hours * 3600L +
            minutes * 60L +
            seconds

        return totalSeconds
            .takeIf { it > 0L }
            ?.let { seconds ->
                runCatching { Math.multiplyExact(seconds, 1000L) }.getOrNull()
            }
    }

    fun reconciledEndTime(
        localStart: LocalTime?,
        localEnd: LocalTime?,
        providerStart: LocalTime?,
        providerEnd: LocalTime?,
        allDay: Boolean
    ): LocalTime? {
        if (allDay) return null

        if (
            providerStart != null &&
            providerEnd != null &&
            providerEnd.isAfter(providerStart)
        ) {
            return providerEnd
        }

        if (
            localStart != null &&
            localEnd != null &&
            localEnd.isAfter(localStart) &&
            providerStart != null
        ) {
            val duration = Duration.between(localStart, localEnd)
            val shifted = providerStart.plus(duration)
            if (shifted.isAfter(providerStart)) return shifted
        }

        return localEnd
    }
}
