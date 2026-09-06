package com.pix.dayline.model

import java.time.LocalTime

data class DeviceCalendar(
    val id: Long,
    val name: String,
    val accountName: String,
    val writable: Boolean,
    val primary: Boolean
)

data class CalendarRule(
    val calendarId: Long,
    val visible: Boolean = true,
    val editable: Boolean = false,
    val spaceId: String? = null,
    val color: ItemColor = ItemColor.MONO
)

data class CalendarPreferences(
    val defaultCalendarId: Long? = null,
    val rules: List<CalendarRule> = emptyList()
) {
    fun ruleFor(id: Long): CalendarRule? = rules.firstOrNull { it.calendarId == id }
    fun isVisible(id: Long): Boolean = ruleFor(id)?.visible ?: true
    fun isEditable(id: Long): Boolean = ruleFor(id)?.editable ?: false
}

data class EventTemplate(
    val id: String,
    val title: String,
    val kind: AgendaKind = AgendaKind.EVENT,
    val durationMinutes: Int = 60,
    val startTime: LocalTime? = null,
    val spaceId: String? = null,
    val color: ItemColor = ItemColor.MONO,
    val focusCycle: FocusCycle = FocusCycle.OFF,
    val customFocusMinutes: Int = 25,
    val customBreakMinutes: Int = 5,
    val bufferBeforeMinutes: Int = 0,
    val bufferAfterMinutes: Int = 0,
    val priority: TaskPriority = TaskPriority.NORMAL
)

enum class WidgetBackgroundMode { SYSTEM, TRANSPARENT }
enum class WidgetContentMode { SMART, CURRENT, NEXT }

data class WidgetInstancePrefs(
    val appWidgetId: Int,
    val spaceId: String? = null,
    val calendarId: Long? = null,
    val emoji: String? = null,
    val font: String? = null,
    val showTasks: Boolean = true,
    val showEvents: Boolean = true,
    val backgroundMode: WidgetBackgroundMode = WidgetBackgroundMode.SYSTEM,
    val contentMode: WidgetContentMode = WidgetContentMode.SMART,
    val showFocusState: Boolean = true
)
