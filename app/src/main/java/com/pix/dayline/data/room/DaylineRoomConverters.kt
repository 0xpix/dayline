package com.pix.dayline.data.room

import androidx.room.TypeConverter
import com.pix.dayline.model.AgendaKind
import com.pix.dayline.model.FocusCycle
import com.pix.dayline.model.ItemColor
import com.pix.dayline.model.Recurrence
import com.pix.dayline.model.TaskDetail
import com.pix.dayline.model.TaskPriority
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate
import java.time.LocalTime

class DaylineRoomConverters {
    @TypeConverter
    fun localDateToString(value: LocalDate?): String? = value?.toString()

    @TypeConverter
    fun stringToLocalDate(value: String?): LocalDate? =
        value?.takeIf { it.isNotBlank() }?.let(LocalDate::parse)

    @TypeConverter
    fun localTimeToString(value: LocalTime?): String? = value?.toString()

    @TypeConverter
    fun stringToLocalTime(value: String?): LocalTime? =
        value?.takeIf { it.isNotBlank() }?.let(LocalTime::parse)

    @TypeConverter
    fun agendaKindToString(value: AgendaKind): String = value.name

    @TypeConverter
    fun stringToAgendaKind(value: String): AgendaKind =
        enumValueOrDefault(value, AgendaKind.EVENT)

    @TypeConverter
    fun recurrenceToString(value: Recurrence): String = value.name

    @TypeConverter
    fun stringToRecurrence(value: String): Recurrence =
        enumValueOrDefault(value, Recurrence.ONCE)

    @TypeConverter
    fun focusCycleToString(value: FocusCycle): String = value.name

    @TypeConverter
    fun stringToFocusCycle(value: String): FocusCycle =
        enumValueOrDefault(value, FocusCycle.OFF)

    @TypeConverter
    fun priorityToString(value: TaskPriority): String = value.name

    @TypeConverter
    fun stringToPriority(value: String): TaskPriority =
        enumValueOrDefault(value, TaskPriority.NORMAL)

    @TypeConverter
    fun itemColorToString(value: ItemColor): String = value.name

    @TypeConverter
    fun stringToItemColor(value: String): ItemColor =
        enumValueOrDefault(value, ItemColor.MONO)

    @TypeConverter
    fun intSetToJson(values: Set<Int>): String =
        JSONArray().apply { values.sorted().forEach(::put) }.toString()

    @TypeConverter
    fun jsonToIntSet(raw: String): Set<Int> = runCatching {
        val array = JSONArray(raw)
        buildSet {
            for (index in 0 until array.length()) {
                array.optInt(index, -1).takeIf { it >= 0 }?.let(::add)
            }
        }
    }.getOrDefault(emptySet())

    @TypeConverter
    fun dateSetToJson(values: Set<LocalDate>): String =
        JSONArray().apply { values.sorted().forEach { put(it.toString()) } }.toString()

    @TypeConverter
    fun jsonToDateSet(raw: String): Set<LocalDate> = runCatching {
        val array = JSONArray(raw)
        buildSet {
            for (index in 0 until array.length()) {
                runCatching { LocalDate.parse(array.getString(index)) }
                    .getOrNull()
                    ?.let(::add)
            }
        }
    }.getOrDefault(emptySet())

    @TypeConverter
    fun taskDetailsToJson(values: List<TaskDetail>): String =
        JSONArray().apply {
            values.forEach { detail ->
                put(
                    JSONObject()
                        .put("id", detail.id)
                        .put("text", detail.text)
                        .put("done", detail.done)
                )
            }
        }.toString()

    @TypeConverter
    fun jsonToTaskDetails(raw: String): List<TaskDetail> = runCatching {
        val array = JSONArray(raw)
        buildList {
            for (index in 0 until array.length()) {
                val detail = array.getJSONObject(index)
                add(
                    TaskDetail(
                        id = detail.getString("id"),
                        text = detail.optString("text"),
                        done = detail.optBoolean("done", false)
                    )
                )
            }
        }
    }.getOrDefault(emptyList())

    private inline fun <reified T : Enum<T>> enumValueOrDefault(value: String, fallback: T): T =
        runCatching { enumValueOf<T>(value) }.getOrDefault(fallback)
}
