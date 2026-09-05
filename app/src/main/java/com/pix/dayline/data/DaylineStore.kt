package com.pix.dayline.data

import android.content.Context
import com.pix.dayline.model.*
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID

class DaylineStore(context: Context) {
    private val prefs = context.getSharedPreferences("dayline", Context.MODE_PRIVATE)

    fun loadItems(): List<DaylineItem> {
        val raw = prefs.getString(KEY_ITEMS, null) ?: return emptyList()
        return runCatching {
            val array = JSONArray(raw)
            buildList {
                for (index in 0 until array.length()) {
                    val json = array.getJSONObject(index)
                    val completed = buildSet {
                        val a = json.optJSONArray("completedDates") ?: JSONArray()
                        for (i in 0 until a.length()) add(LocalDate.parse(a.getString(i)))
                    }
                    val details = buildList {
                        val a = json.optJSONArray("details") ?: JSONArray()
                        for (i in 0 until a.length()) {
                            val d = a.getJSONObject(i)
                            add(
                                TaskDetail(
                                    d.optString("id").ifBlank { UUID.randomUUID().toString() },
                                    d.optString("text"),
                                    d.optBoolean("done", false)
                                )
                            )
                        }
                    }
                    val startTimeRaw = json.optString("startTime").ifBlank { json.optString("time") }
                    add(
                        DaylineItem(
                            id = json.getString("id"),
                            title = json.getString("title"),
                            kind = AgendaKind.valueOf(json.getString("kind")),
                            startDate = LocalDate.parse(json.getString("startDate")),
                            startTime = startTimeRaw.takeIf { it.isNotBlank() }?.let(LocalTime::parse),
                            endTime = json.optString("endTime").takeIf { it.isNotBlank() }?.let(LocalTime::parse),
                            recurrence = Recurrence.valueOf(json.getString("recurrence")),
                            reminderMinutes = json.optInt("reminderMinutes", -1).takeIf { it >= 0 },
                            color = runCatching {
                                ItemColor.valueOf(json.optString("color", ItemColor.MONO.name))
                            }.getOrDefault(ItemColor.MONO),
                            spaceId = json.optString("spaceId").takeIf { it.isNotBlank() },
                            details = details,
                            completedDates = completed
                        )
                    )
                }
            }
        }.getOrDefault(emptyList())
    }

    fun saveItems(items: List<DaylineItem>) {
        val array = JSONArray()
        items.forEach { item ->
            val completed = JSONArray().apply {
                item.completedDates.sorted().forEach { put(it.toString()) }
            }
            val details = JSONArray().apply {
                item.details.forEach { d ->
                    put(
                        JSONObject()
                            .put("id", d.id)
                            .put("text", d.text)
                            .put("done", d.done)
                    )
                }
            }
            array.put(
                JSONObject()
                    .put("id", item.id)
                    .put("title", item.title)
                    .put("kind", item.kind.name)
                    .put("startDate", item.startDate.toString())
                    .put("startTime", item.startTime?.toString().orEmpty())
                    .put("endTime", item.endTime?.toString().orEmpty())
                    .put("recurrence", item.recurrence.name)
                    .put("reminderMinutes", item.reminderMinutes ?: -1)
                    .put("color", item.color.name)
                    .put("spaceId", item.spaceId.orEmpty())
                    .put("details", details)
                    .put("completedDates", completed)
            )
        }
        prefs.edit().putString(KEY_ITEMS, array.toString()).apply()
    }

    fun loadSpaces(): List<DaylineSpace> {
        val raw = prefs.getString(KEY_SPACES, null)
        if (raw == null) {
            val defaults = listOf(
                DaylineSpace("personal", "Personal", ItemColor.BLUE),
                DaylineSpace("work", "Work", ItemColor.VIOLET)
            )
            saveSpaces(defaults)
            return defaults
        }
        return runCatching {
            val a = JSONArray(raw)
            buildList {
                for (i in 0 until a.length()) {
                    val j = a.getJSONObject(i)
                    add(
                        DaylineSpace(
                            j.getString("id"),
                            j.getString("name"),
                            runCatching {
                                ItemColor.valueOf(j.optString("color", ItemColor.MONO.name))
                            }.getOrDefault(ItemColor.MONO)
                        )
                    )
                }
            }
        }.getOrDefault(emptyList())
    }

    fun saveSpaces(spaces: List<DaylineSpace>) {
        val a = JSONArray()
        spaces.forEach {
            a.put(
                JSONObject()
                    .put("id", it.id)
                    .put("name", it.name)
                    .put("color", it.color.name)
            )
        }
        prefs.edit().putString(KEY_SPACES, a.toString()).apply()
    }

    fun loadAppearance(): Appearance = runCatching {
        Appearance.valueOf(
            prefs.getString(KEY_APPEARANCE, Appearance.SYSTEM.name) ?: Appearance.SYSTEM.name
        )
    }.getOrDefault(Appearance.SYSTEM)

    fun saveAppearance(appearance: Appearance) {
        prefs.edit().putString(KEY_APPEARANCE, appearance.name).apply()
    }

    fun loadFontChoice(): FontChoice = runCatching {
        FontChoice.valueOf(
            prefs.getString(KEY_FONT, FontChoice.PIXELIFY.name) ?: FontChoice.PIXELIFY.name
        )
    }.getOrDefault(FontChoice.PIXELIFY)

    fun saveFontChoice(font: FontChoice) {
        prefs.edit().putString(KEY_FONT, font.name).apply()
    }

    fun loadShowOrb(): Boolean = prefs.getBoolean(KEY_SHOW_ORB, true)
    fun saveShowOrb(show: Boolean) { prefs.edit().putBoolean(KEY_SHOW_ORB, show).apply() }

    fun loadWeekStartsMonday(): Boolean = prefs.getBoolean(KEY_WEEK_STARTS_MONDAY, true)
    fun saveWeekStartsMonday(monday: Boolean) {
        prefs.edit().putBoolean(KEY_WEEK_STARTS_MONDAY, monday).apply()
    }

    companion object {
        private const val KEY_ITEMS = "items"
        private const val KEY_SPACES = "spaces"
        private const val KEY_APPEARANCE = "appearance"
        private const val KEY_FONT = "font"
        private const val KEY_SHOW_ORB = "show_orb"
        private const val KEY_WEEK_STARTS_MONDAY = "week_starts_monday"
    }
}

enum class Appearance { SYSTEM, LIGHT, DARK }
enum class FontChoice { PIXELIFY, GEIST, GEIST_PIXEL, SYSTEM }
