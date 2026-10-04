package com.pix.dayline.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime

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
    @Test
    fun mealSummaryUsesExpectedTodayWindows() {
        val plan = DaylinePlan(
            id = "diet",
            name = "Diet",
            sections = DaylinePlan.MEAL_SECTIONS,
            items = listOf(
                DaylinePlanItem(
                    id = "breakfast",
                    title = "Oat bowl",
                    section = "Breakfast",
                    weekdays = setOf(1)
                ),
                DaylinePlanItem(
                    id = "lunch",
                    title = "Chicken rice",
                    section = "Lunch",
                    weekdays = setOf(1)
                ),
                DaylinePlanItem(
                    id = "snack",
                    title = "Greek yogurt",
                    section = "Snacks",
                    weekdays = setOf(1)
                ),
                DaylinePlanItem(
                    id = "dinner",
                    title = "Salmon potatoes",
                    section = "Dinner",
                    weekdays = setOf(1)
                )
            )
        )

        val summary = mealSummaryFor(listOf(plan), monday)

        assertEquals(
            listOf("Breakfast", "Lunch", "Snacks", "Dinner"),
            summary.map { it.section }
        )
        assertEquals("06:00", summary[0].start.toString())
        assertEquals("09:00", summary[0].end.toString())
        assertEquals("09:00", summary[1].start.toString())
        assertEquals("13:00", summary[1].end.toString())
        assertEquals("15:00", summary[2].start.toString())
        assertEquals("17:00", summary[2].end.toString())
        assertEquals("18:00", summary[3].start.toString())
        assertEquals("21:00", summary[3].end.toString())
    }

    @Test
    fun mealSummaryOnlyIncludesFoodsScheduledForThatWeekday() {
        val plan = DaylinePlan(
            id = "diet",
            name = "Diet",
            sections = DaylinePlan.MEAL_SECTIONS,
            items = listOf(
                DaylinePlanItem(
                    id = "monday",
                    title = "Eggs and toast",
                    section = "Breakfast",
                    weekdays = setOf(1)
                ),
                DaylinePlanItem(
                    id = "tuesday",
                    title = "Porridge",
                    section = "Breakfast",
                    weekdays = setOf(2)
                )
            )
        )

        val summary = mealSummaryFor(listOf(plan), monday)

        assertEquals(listOf("Eggs and toast"), summary.single().titles)
    }

    @Test
    fun mealGlanceShowsOnlyCurrentMealInsideItsWindow() {
        val plan = DaylinePlan(
            id = "diet",
            name = "Diet",
            sections = DaylinePlan.MEAL_SECTIONS,
            items = listOf(
                DaylinePlanItem("breakfast", "Oat bowl", "Breakfast", weekdays = setOf(1)),
                DaylinePlanItem("lunch", "Chicken rice", "Lunch", weekdays = setOf(1)),
                DaylinePlanItem("snack", "Greek yogurt", "Snacks", weekdays = setOf(1)),
                DaylinePlanItem("dinner", "Salmon potatoes", "Dinner", weekdays = setOf(1))
            )
        )

        val glance = mealGlanceFor(listOf(plan), monday, LocalTime.of(15, 47))

        assertEquals(DaylineMealGlanceState.NOW, glance?.state)
        assertEquals("Snacks", glance?.meal?.section)
        assertEquals(listOf("Greek yogurt"), glance?.meal?.titles)
    }

    @Test
    fun mealGlanceShowsNextMealBetweenWindows() {
        val plan = DaylinePlan(
            id = "diet",
            name = "Diet",
            sections = DaylinePlan.MEAL_SECTIONS,
            items = listOf(
                DaylinePlanItem("snack", "Greek yogurt", "Snacks", weekdays = setOf(1)),
                DaylinePlanItem("dinner", "Salmon potatoes", "Dinner", weekdays = setOf(1))
            )
        )

        val glance = mealGlanceFor(listOf(plan), monday, LocalTime.of(17, 30))

        assertEquals(DaylineMealGlanceState.NEXT, glance?.state)
        assertEquals("Dinner", glance?.meal?.section)
    }

    @Test
    fun mealGlanceDisappearsAfterLastMeal() {
        val plan = DaylinePlan(
            id = "diet",
            name = "Diet",
            sections = DaylinePlan.MEAL_SECTIONS,
            items = listOf(
                DaylinePlanItem("dinner", "Salmon potatoes", "Dinner", weekdays = setOf(1))
            )
        )

        assertEquals(null, mealGlanceFor(listOf(plan), monday, LocalTime.of(21, 30)))
    }

}
