package com.pix.dayline.model

import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID

/**
 * A lightweight weekly plan that never becomes a calendar event.
 *
 * A Plan item repeats by weekday, so defining Monday once means every Monday.
 * Plans intentionally have no time, reminders, busy-state, calories, macros,
 * calendar provider identity or conflict semantics.
 */
data class DaylinePlanItem(
    val id: String,
    val title: String,
    val section: String,
    val ingredients: List<String> = emptyList(),
    val weekdays: Set<Int> = emptySet()
) {
    fun occursOn(date: LocalDate): Boolean = date.dayOfWeek.value in weekdays
}

data class DaylineMealSummary(
    val section: String,
    val start: LocalTime,
    val end: LocalTime,
    val titles: List<String>
)

enum class DaylineMealGlanceState { NOW, NEXT }

data class DaylineMealGlance(
    val meal: DaylineMealSummary,
    val state: DaylineMealGlanceState
)

fun mealGlanceFor(
    plans: List<DaylinePlan>,
    date: LocalDate,
    now: LocalTime
): DaylineMealGlance? {
    val meals = mealSummaryFor(plans, date)
    val current = meals.firstOrNull { !now.isBefore(it.start) && now.isBefore(it.end) }
    if (current != null) {
        return DaylineMealGlance(current, DaylineMealGlanceState.NOW)
    }

    val next = meals.firstOrNull { now.isBefore(it.start) } ?: return null
    return DaylineMealGlance(next, DaylineMealGlanceState.NEXT)
}

fun mealSummaryFor(plans: List<DaylinePlan>, date: LocalDate): List<DaylineMealSummary> {
    val windows = listOf(
        Triple("Breakfast", LocalTime.of(6, 0), LocalTime.of(9, 0)),
        Triple("Lunch", LocalTime.of(9, 0), LocalTime.of(13, 0)),
        Triple("Snacks", LocalTime.of(15, 0), LocalTime.of(17, 0)),
        Triple("Dinner", LocalTime.of(18, 0), LocalTime.of(21, 0))
    )

    return windows.mapNotNull { (section, start, end) ->
        val titles = plans
            .filter { section in it.sections }
            .flatMap { it.itemsFor(date, section) }
            .map { it.title.trim() }
            .filter(String::isNotBlank)
            .distinct()

        titles.takeIf { it.isNotEmpty() }?.let {
            DaylineMealSummary(
                section = section,
                start = start,
                end = end,
                titles = it
            )
        }
    }
}

data class DaylinePlan(
    val id: String,
    val name: String,
    val sections: List<String>,
    val items: List<DaylinePlanItem> = emptyList(),
    /**
     * v0.21.0 compatibility only. Older per-date text entries are migrated to
     * weekly items on load and are not used by the current UI.
     */
    val entries: Map<String, Map<String, String>> = emptyMap()
) {
    fun itemsFor(date: LocalDate, section: String): List<DaylinePlanItem> =
        items.filter { it.section == section && it.occursOn(date) }

    fun upsertItem(item: DaylinePlanItem): DaylinePlan {
        val normalized = item.copy(
            title = item.title.trim(),
            ingredients = item.ingredients.map(String::trim).filter(String::isNotBlank),
            weekdays = item.weekdays.filter { it in 1..7 }.toSet()
        )
        return copy(
            items = if (items.any { it.id == normalized.id }) {
                items.map { if (it.id == normalized.id) normalized else it }
            } else {
                items + normalized
            }
        )
    }

    fun removeItem(itemId: String): DaylinePlan =
        copy(items = items.filterNot { it.id == itemId })

    fun normalizedSections(nextSections: List<String>): DaylinePlan {
        val cleaned = nextSections.map(String::trim).filter(String::isNotBlank).distinct()
        val allowed = cleaned.toSet()
        return copy(
            sections = cleaned,
            items = items.filter { it.section in allowed },
            entries = entries.mapValues { (_, values) ->
                values.filterKeys { it in allowed }
            }.filterValues { it.isNotEmpty() }
        )
    }

    /**
     * Converts v0.21.0 date-specific text into weekly repeating items.
     * Identical section/title/day combinations are collapsed so migration
     * never creates duplicate Monday meals.
     */
    fun migrateLegacyEntries(): DaylinePlan {
        if (entries.isEmpty()) return this
        val migrated = buildList {
            addAll(items)
            entries.forEach { (dateKey, values) ->
                val date = runCatching { LocalDate.parse(dateKey) }.getOrNull() ?: return@forEach
                values.forEach { (section, value) ->
                    val title = value.trim()
                    if (title.isBlank() || section !in sections) return@forEach
                    val weekday = date.dayOfWeek.value
                    val duplicate = any {
                        it.section == section &&
                            it.title.equals(title, ignoreCase = true) &&
                            weekday in it.weekdays
                    }
                    if (!duplicate) {
                        add(
                            DaylinePlanItem(
                                id = UUID.randomUUID().toString(),
                                title = title,
                                section = section,
                                weekdays = setOf(weekday)
                            )
                        )
                    }
                }
            }
        }
        return copy(items = migrated, entries = emptyMap())
    }

    companion object {
        val MEAL_SECTIONS = listOf("Breakfast", "Lunch", "Dinner", "Snacks")
    }
}
