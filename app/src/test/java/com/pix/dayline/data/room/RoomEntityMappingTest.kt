package com.pix.dayline.data.room

import com.pix.dayline.model.AgendaKind
import com.pix.dayline.model.DaylineItem
import com.pix.dayline.model.DaylineSpace
import com.pix.dayline.model.FocusCycle
import com.pix.dayline.model.ItemColor
import com.pix.dayline.model.Recurrence
import com.pix.dayline.model.TaskDetail
import com.pix.dayline.model.TaskPriority
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime

class RoomEntityMappingTest {
    @Test
    fun itemRoundTripPreservesAllDomainFields() {
        val item = DaylineItem(
            id = "item-1",
            title = "Deep work",
            kind = AgendaKind.TASK,
            startDate = LocalDate.of(2026, 9, 24),
            startTime = LocalTime.of(9, 15),
            endTime = LocalTime.of(10, 45),
            recurrence = Recurrence.CUSTOM,
            repeatDays = setOf(1, 3, 5),
            recurrenceEndDate = LocalDate.of(2026, 12, 31),
            excludedDates = setOf(LocalDate.of(2026, 10, 2)),
            seriesParentId = "series-1",
            reminderMinutes = 15,
            focusCycle = FocusCycle.CUSTOM,
            customFocusMinutes = 50,
            customBreakMinutes = 10,
            focusSessionsCompleted = 3,
            focusedMinutesCompleted = 140,
            bufferBeforeMinutes = 10,
            bufferAfterMinutes = 15,
            priority = TaskPriority.HIGH,
            estimatedDurationMinutes = 90,
            earliestDate = LocalDate.of(2026, 9, 24),
            deadlineDate = LocalDate.of(2026, 9, 28),
            allDay = false,
            timeZoneId = "Europe/Berlin",
            color = ItemColor.VIOLET,
            spaceId = "work",
            details = listOf(
                TaskDetail("detail-1", "Outline", true),
                TaskDetail("detail-2", "Write", false)
            ),
            completedDates = setOf(LocalDate.of(2026, 9, 24)),
            calendarEventId = 42L,
            calendarId = 7L,
            calendarName = "Work",
            calendarReadOnly = true
        )

        val entity = DaylineItemEntity.fromModel(item, position = 6)

        assertEquals(6, entity.position)
        assertEquals(item, entity.toModel())
    }

    @Test
    fun spaceRoundTripPreservesDomainFieldsAndPosition() {
        val space = DaylineSpace(
            id = "research",
            name = "Research",
            color = ItemColor.SAGE,
            calendarId = 99L
        )

        val entity = DaylineSpaceEntity.fromModel(space, position = 4)

        assertEquals(4, entity.position)
        assertEquals(space, entity.toModel())
    }
}
