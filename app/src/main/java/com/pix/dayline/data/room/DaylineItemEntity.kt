package com.pix.dayline.data.room

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.pix.dayline.model.AgendaKind
import com.pix.dayline.model.DaylineItem
import com.pix.dayline.model.FocusCycle
import com.pix.dayline.model.ItemColor
import com.pix.dayline.model.Recurrence
import com.pix.dayline.model.TaskDetail
import com.pix.dayline.model.TaskPriority
import java.time.LocalDate
import java.time.LocalTime

@Entity(
    tableName = "dayline_items",
    indices = [
        Index(value = ["startDate"]),
        Index(value = ["kind"]),
        Index(value = ["spaceId"]),
        Index(value = ["calendarEventId"])
    ]
)
@TypeConverters(DaylineRoomConverters::class)
data class DaylineItemEntity(
    @PrimaryKey val id: String,
    val position: Int,
    val title: String,
    val kind: AgendaKind,
    val startDate: LocalDate,
    val startTime: LocalTime?,
    val endTime: LocalTime?,
    val recurrence: Recurrence,
    val repeatDays: Set<Int>,
    val recurrenceEndDate: LocalDate?,
    val excludedDates: Set<LocalDate>,
    val seriesParentId: String?,
    val reminderMinutes: Int?,
    val focusCycle: FocusCycle,
    val customFocusMinutes: Int,
    val customBreakMinutes: Int,
    val focusSessionsCompleted: Int,
    val focusedMinutesCompleted: Int,
    val bufferBeforeMinutes: Int,
    val bufferAfterMinutes: Int,
    val priority: TaskPriority,
    val estimatedDurationMinutes: Int,
    val earliestDate: LocalDate?,
    val deadlineDate: LocalDate?,
    val allDay: Boolean,
    val timeZoneId: String?,
    val color: ItemColor,
    val spaceId: String?,
    val details: List<TaskDetail>,
    val completedDates: Set<LocalDate>,
    val calendarEventId: Long?,
    val calendarId: Long?,
    val calendarName: String?,
    val calendarReadOnly: Boolean
) {
    fun toModel(): DaylineItem = DaylineItem(
        id = id,
        title = title,
        kind = kind,
        startDate = startDate,
        startTime = startTime,
        endTime = endTime,
        recurrence = recurrence,
        repeatDays = repeatDays,
        recurrenceEndDate = recurrenceEndDate,
        excludedDates = excludedDates,
        seriesParentId = seriesParentId,
        reminderMinutes = reminderMinutes,
        focusCycle = focusCycle,
        customFocusMinutes = customFocusMinutes,
        customBreakMinutes = customBreakMinutes,
        focusSessionsCompleted = focusSessionsCompleted,
        focusedMinutesCompleted = focusedMinutesCompleted,
        bufferBeforeMinutes = bufferBeforeMinutes,
        bufferAfterMinutes = bufferAfterMinutes,
        priority = priority,
        estimatedDurationMinutes = estimatedDurationMinutes,
        earliestDate = earliestDate,
        deadlineDate = deadlineDate,
        allDay = allDay,
        timeZoneId = timeZoneId,
        color = color,
        spaceId = spaceId,
        details = details,
        completedDates = completedDates,
        calendarEventId = calendarEventId,
        calendarId = calendarId,
        calendarName = calendarName,
        calendarReadOnly = calendarReadOnly
    )

    companion object {
        fun fromModel(item: DaylineItem, position: Int = 0): DaylineItemEntity = DaylineItemEntity(
            id = item.id,
            position = position,
            title = item.title,
            kind = item.kind,
            startDate = item.startDate,
            startTime = item.startTime,
            endTime = item.endTime,
            recurrence = item.recurrence,
            repeatDays = item.repeatDays,
            recurrenceEndDate = item.recurrenceEndDate,
            excludedDates = item.excludedDates,
            seriesParentId = item.seriesParentId,
            reminderMinutes = item.reminderMinutes,
            focusCycle = item.focusCycle,
            customFocusMinutes = item.customFocusMinutes,
            customBreakMinutes = item.customBreakMinutes,
            focusSessionsCompleted = item.focusSessionsCompleted,
            focusedMinutesCompleted = item.focusedMinutesCompleted,
            bufferBeforeMinutes = item.bufferBeforeMinutes,
            bufferAfterMinutes = item.bufferAfterMinutes,
            priority = item.priority,
            estimatedDurationMinutes = item.estimatedDurationMinutes,
            earliestDate = item.earliestDate,
            deadlineDate = item.deadlineDate,
            allDay = item.allDay,
            timeZoneId = item.timeZoneId,
            color = item.color,
            spaceId = item.spaceId,
            details = item.details,
            completedDates = item.completedDates,
            calendarEventId = item.calendarEventId,
            calendarId = item.calendarId,
            calendarName = item.calendarName,
            calendarReadOnly = item.calendarReadOnly
        )
    }
}
