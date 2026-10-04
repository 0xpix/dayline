package com.pix.dayline.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import java.time.LocalDate

class DaylinePlanTest {
    private val sunday = LocalDate.of(2026, 10, 4)
    private val monday = sunday.plusDays(1)

    @Test
    fun mealsTemplateIsOnlyASectionPreset() {
        assertEquals(
            listOf("Breakfast", "Lunch", "Dinner", "Snacks"),
            DaylinePlan.MEAL_SECTIONS
        )
    }

    @Test
    fun entriesStayIsolatedByDateAndSection() {
        val plan = DaylinePlan(
            id = "diet",
            name = "Diet",
            sections = DaylinePlan.MEAL_SECTIONS
        )
            .withEntry(sunday, "Breakfast", "Oats and yogurt")
            .withEntry(sunday, "Dinner", "Salmon and potatoes")
            .withEntry(monday, "Breakfast", "Eggs and toast")

        assertEquals("Oats and yogurt", plan.entry(sunday, "Breakfast"))
        assertEquals("Salmon and potatoes", plan.entry(sunday, "Dinner"))
        assertEquals("Eggs and toast", plan.entry(monday, "Breakfast"))
        assertEquals("", plan.entry(monday, "Dinner"))
    }

    @Test
    fun blankEntryRemovesStoredValue() {
        val plan = DaylinePlan(
            id = "study",
            name = "Study",
            sections = listOf("Morning")
        )
            .withEntry(sunday, "Morning", "IELTS reading")
            .withEntry(sunday, "Morning", "")

        assertEquals("", plan.entry(sunday, "Morning"))
        assertFalse(plan.entries.containsKey(sunday.toString()))
    }

    @Test
    fun removingASectionAlsoRemovesItsStoredEntries() {
        val plan = DaylinePlan(
            id = "diet",
            name = "Diet",
            sections = listOf("Breakfast", "Dinner")
        )
            .withEntry(sunday, "Breakfast", "Oats")
            .withEntry(sunday, "Dinner", "Rice")
            .normalizedSections(listOf("Breakfast"))

        assertEquals(listOf("Breakfast"), plan.sections)
        assertEquals("Oats", plan.entry(sunday, "Breakfast"))
        assertEquals("", plan.entry(sunday, "Dinner"))
    }
}
