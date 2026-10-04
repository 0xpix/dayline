package com.pix.dayline.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class DaylinePlanTest {
    private val sunday = LocalDate.of(2026, 10, 4)
    private val monday = sunday.plusDays(1)
    private val nextMonday = monday.plusWeeks(1)

    @Test
    fun mealsTemplateUsesExpectedSections() {
        assertEquals(
            listOf("Breakfast", "Lunch", "Dinner", "Snacks"),
            DaylinePlan.MEAL_SECTIONS
        )
    }

    @Test
    fun mondayFoodRepeatsEveryMonday() {
        val item = DaylinePlanItem(
            id = "oats",
            title = "Oat bowl",
            section = "Breakfast",
            ingredients = listOf("Oats", "Yogurt", "Berries"),
            weekdays = setOf(1)
        )
        val plan = DaylinePlan(
            id = "diet",
            name = "Diet",
            sections = DaylinePlan.MEAL_SECTIONS,
            items = listOf(item)
        )

        assertEquals(listOf(item), plan.itemsFor(monday, "Breakfast"))
        assertEquals(listOf(item), plan.itemsFor(nextMonday, "Breakfast"))
        assertTrue(plan.itemsFor(sunday, "Breakfast").isEmpty())
    }

    @Test
    fun foodCanRepeatOnSeveralWeekdays() {
        val item = DaylinePlanItem(
            id = "salmon",
            title = "Salmon rice",
            section = "Dinner",
            weekdays = setOf(1, 3, 5)
        )

        assertTrue(item.occursOn(LocalDate.of(2026, 10, 5)))
        assertTrue(item.occursOn(LocalDate.of(2026, 10, 7)))
        assertTrue(item.occursOn(LocalDate.of(2026, 10, 9)))
        assertFalse(item.occursOn(LocalDate.of(2026, 10, 6)))
    }

    @Test
    fun upsertCleansIngredientsAndWeekdays() {
        val plan = DaylinePlan(
            id = "diet",
            name = "Diet",
            sections = DaylinePlan.MEAL_SECTIONS
        ).upsertItem(
            DaylinePlanItem(
                id = "food",
                title = "  Yogurt bowl  ",
                section = "Breakfast",
                ingredients = listOf(" Yogurt ", "", " Berries "),
                weekdays = setOf(1, 8)
            )
        )

        val saved = plan.items.single()
        assertEquals("Yogurt bowl", saved.title)
        assertEquals(listOf("Yogurt", "Berries"), saved.ingredients)
        assertEquals(setOf(1), saved.weekdays)
    }

    @Test
    fun removingSectionAlsoRemovesItsItems() {
        val plan = DaylinePlan(
            id = "custom",
            name = "Custom",
            sections = listOf("Morning", "Evening"),
            items = listOf(
                DaylinePlanItem("a", "Read", "Morning", weekdays = setOf(1)),
                DaylinePlanItem("b", "Walk", "Evening", weekdays = setOf(1))
            )
        ).normalizedSections(listOf("Morning"))

        assertEquals(listOf("Morning"), plan.sections)
        assertEquals(listOf("Read"), plan.items.map { it.title })
    }

    @Test
    fun v021DateEntriesMigrateToWeeklyItems() {
        val plan = DaylinePlan(
            id = "diet",
            name = "Diet",
            sections = DaylinePlan.MEAL_SECTIONS,
            entries = mapOf(
                "2026-10-05" to mapOf("Breakfast" to "Eggs and toast"),
                "2026-10-12" to mapOf("Breakfast" to "Eggs and toast")
            )
        ).migrateLegacyEntries()

        assertTrue(plan.entries.isEmpty())
        assertEquals(1, plan.items.size)
        assertEquals("Eggs and toast", plan.items.single().title)
        assertEquals("Breakfast", plan.items.single().section)
        assertEquals(setOf(1), plan.items.single().weekdays)
    }
}
