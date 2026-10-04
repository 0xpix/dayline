package com.pix.dayline.model

import java.time.LocalDate

/**
 * A lightweight, date-aware plan that never becomes a calendar event.
 *
 * Plans intentionally have no time, reminders, busy-state, calories, macros,
 * calendar provider identity or conflict semantics.
 */
data class DaylinePlan(
    val id: String,
    val name: String,
    val sections: List<String>,
    val entries: Map<String, Map<String, String>> = emptyMap()
) {
    fun entry(date: LocalDate, section: String): String =
        entries[date.toString()]?.get(section).orEmpty()

    fun withEntry(date: LocalDate, section: String, value: String): DaylinePlan {
        val dateKey = date.toString()
        val currentDay = entries[dateKey].orEmpty().toMutableMap()
        if (value.isBlank()) currentDay.remove(section) else currentDay[section] = value

        val nextEntries = entries.toMutableMap()
        if (currentDay.isEmpty()) nextEntries.remove(dateKey) else nextEntries[dateKey] = currentDay
        return copy(entries = nextEntries)
    }

    fun normalizedSections(nextSections: List<String>): DaylinePlan {
        val cleaned = nextSections.map(String::trim).filter(String::isNotBlank).distinct()
        val allowed = cleaned.toSet()
        val cleanedEntries = entries.mapValues { (_, values) ->
            values.filterKeys { it in allowed }
        }.filterValues { it.isNotEmpty() }
        return copy(sections = cleaned, entries = cleanedEntries)
    }

    companion object {
        val MEAL_SECTIONS = listOf("Breakfast", "Lunch", "Dinner", "Snacks")
    }
}
