package com.pix.dayline.data

import android.content.Context
import com.pix.dayline.model.AgendaKind
import com.pix.dayline.model.DaylineItem
import com.pix.dayline.model.Recurrence
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate
import java.time.LocalTime

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
                        val completedArray = json.optJSONArray("completedDates") ?: JSONArray()
                        for (completedIndex in 0 until completedArray.length()) {
                            add(LocalDate.parse(completedArray.getString(completedIndex)))
                        }
                    }

                    add(
                        DaylineItem(
                            id = json.getString("id"),
                            title = json.getString("title"),
                            kind = AgendaKind.valueOf(json.getString("kind")),
                            startDate = LocalDate.parse(json.getString("startDate")),
                            time = json.optString("time").takeIf { it.isNotBlank() }?.let(LocalTime::parse),
                            recurrence = Recurrence.valueOf(json.getString("recurrence")),
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
            val completed = JSONArray()
            item.completedDates.sorted().forEach { completed.put(it.toString()) }

            array.put(
                JSONObject()
                    .put("id", item.id)
                    .put("title", item.title)
                    .put("kind", item.kind.name)
                    .put("startDate", item.startDate.toString())
                    .put("time", item.time?.toString().orEmpty())
                    .put("recurrence", item.recurrence.name)
                    .put("completedDates", completed)
            )
        }

        prefs.edit().putString(KEY_ITEMS, array.toString()).apply()
    }

    fun loadAppearance(): Appearance = runCatching {
        Appearance.valueOf(prefs.getString(KEY_APPEARANCE, Appearance.SYSTEM.name) ?: Appearance.SYSTEM.name)
    }.getOrDefault(Appearance.SYSTEM)

    fun saveAppearance(appearance: Appearance) {
        prefs.edit().putString(KEY_APPEARANCE, appearance.name).apply()
    }

    fun loadShowOrb(): Boolean = prefs.getBoolean(KEY_SHOW_ORB, true)

    fun saveShowOrb(show: Boolean) {
        prefs.edit().putBoolean(KEY_SHOW_ORB, show).apply()
    }

    fun loadWeekStartsMonday(): Boolean = prefs.getBoolean(KEY_WEEK_STARTS_MONDAY, true)

    fun saveWeekStartsMonday(monday: Boolean) {
        prefs.edit().putBoolean(KEY_WEEK_STARTS_MONDAY, monday).apply()
    }

    companion object {
        private const val KEY_ITEMS = "items"
        private const val KEY_APPEARANCE = "appearance"
        private const val KEY_SHOW_ORB = "show_orb"
        private const val KEY_WEEK_STARTS_MONDAY = "week_starts_monday"
    }
}

enum class Appearance {
    SYSTEM,
    LIGHT,
    DARK
}
