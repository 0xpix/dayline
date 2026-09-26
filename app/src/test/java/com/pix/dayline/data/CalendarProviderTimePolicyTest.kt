package com.pix.dayline.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalTime

class CalendarProviderTimePolicyTest {
    @Test
    fun parsesLongRecurringDurationWithoutCollapsingToOneHour() {
        assertEquals(
            12_600_000L,
            CalendarProviderTimePolicy.durationMillis("PT3H30M")
        )
        assertEquals(
            12_600_000L,
            CalendarProviderTimePolicy.durationMillis("PT12600S")
        )
    }

    @Test
    fun parsesWeekDurationFromCalendarProvider() {
        assertEquals(
            14L * 24L * 60L * 60L * 1000L,
            CalendarProviderTimePolicy.durationMillis("P2W")
        )
    }

    @Test
    fun missingProviderEndKeepsExistingLocalDuration() {
        val result = CalendarProviderTimePolicy.reconciledEndTime(
            localStart = LocalTime.of(6, 0),
            localEnd = LocalTime.of(9, 30),
            providerStart = LocalTime.of(6, 0),
            providerEnd = null,
            allDay = false
        )

        assertEquals(LocalTime.of(9, 30), result)
    }

    @Test
    fun missingProviderEndPreservesDurationWhenProviderMovesStart() {
        val result = CalendarProviderTimePolicy.reconciledEndTime(
            localStart = LocalTime.of(6, 0),
            localEnd = LocalTime.of(9, 30),
            providerStart = LocalTime.of(7, 0),
            providerEnd = null,
            allDay = false
        )

        assertEquals(LocalTime.of(10, 30), result)
    }

    @Test
    fun trustworthyProviderEndWins() {
        val result = CalendarProviderTimePolicy.reconciledEndTime(
            localStart = LocalTime.of(6, 0),
            localEnd = LocalTime.of(9, 30),
            providerStart = LocalTime.of(6, 0),
            providerEnd = LocalTime.of(8, 45),
            allDay = false
        )

        assertEquals(LocalTime.of(8, 45), result)
    }

    @Test
    fun allDayEventsDoNotInventAnEndTime() {
        assertNull(
            CalendarProviderTimePolicy.reconciledEndTime(
                localStart = null,
                localEnd = null,
                providerStart = null,
                providerEnd = null,
                allDay = true
            )
        )
    }
}
