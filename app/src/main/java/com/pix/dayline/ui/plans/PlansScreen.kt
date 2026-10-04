package com.pix.dayline.ui.plans

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.pix.dayline.model.DaylinePlan
import com.pix.dayline.model.DaylinePlanItem
import com.pix.dayline.ui.components.FloatingControls
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale
import java.util.UUID

@Composable
fun PlansScreen(
    plans: List<DaylinePlan>,
    date: LocalDate,
    onDateChange: (LocalDate) -> Unit,
    onPlansChange: (List<DaylinePlan>) -> Unit,
    onMenu: () -> Unit,
    onToday: () -> Unit,
    onSwipeToday: () -> Unit
) {
    var selectedPlanId by remember { mutableStateOf(plans.firstOrNull()?.id) }
    var planEditor by remember { mutableStateOf<DaylinePlan?>(null) }
    var planEditorOpen by remember { mutableStateOf(false) }
    var itemEditor by remember { mutableStateOf<DaylinePlanItem?>(null) }
    var itemEditorSection by remember { mutableStateOf<String?>(null) }
    var itemEditorOpen by remember { mutableStateOf(false) }
    var dragTotal by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(plans) {
        if (plans.none { it.id == selectedPlanId }) selectedPlanId = plans.firstOrNull()?.id
    }

    val selected = plans.firstOrNull { it.id == selectedPlanId } ?: plans.firstOrNull()
    val selectedWeekday = date.dayOfWeek.value

    fun selectWeekday(day: Int) {
        if (day !in 1..7) return
        val delta = day - date.dayOfWeek.value
        onDateChange(date.plusDays(delta.toLong()))
    }

    fun savePlan(updated: DaylinePlan) {
        val next = if (plans.any { it.id == updated.id }) {
            plans.map { if (it.id == updated.id) updated else it }
        } else {
            plans + updated
        }
        selectedPlanId = updated.id
        onPlansChange(next)
    }

    fun saveItem(updated: DaylinePlanItem) {
        val plan = selected ?: return
        savePlan(plan.upsertItem(updated))
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .pointerInput(onSwipeToday) {
                detectHorizontalDragGestures(
                    onDragStart = { dragTotal = 0f },
                    onHorizontalDrag = { _, amount -> dragTotal += amount },
                    onDragCancel = { dragTotal = 0f },
                    onDragEnd = {
                        if (dragTotal > 120f) onSwipeToday()
                        dragTotal = 0f
                    }
                )
            }
            .padding(
                top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding(),
                bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
            )
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(start = 28.dp, end = 28.dp, top = 48.dp, bottom = 138.dp)
        ) {
            Text("Plans", style = MaterialTheme.typography.displayMedium)
            Spacer(Modifier.height(6.dp))
            Text(
                "Weekly routines that stay outside your calendar.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(24.dp))

            if (plans.isEmpty()) {
                EmptyPlans {
                    planEditor = null
                    planEditorOpen = true
                }
            } else {
                PlanTabs(
                    plans = plans,
                    selectedId = selected?.id,
                    onSelect = { selectedPlanId = it },
                    onNew = {
                        planEditor = null
                        planEditorOpen = true
                    }
                )

                Spacer(Modifier.height(28.dp))

                WeekdayStrip(
                    selectedDay = selectedWeekday,
                    onSelect = ::selectWeekday
                )

                Spacer(Modifier.height(12.dp))
                Text(
                    "EVERY " + DayOfWeek.of(selectedWeekday)
                        .getDisplayName(TextStyle.FULL, Locale.getDefault())
                        .uppercase(Locale.getDefault()),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(Modifier.height(28.dp))

                selected?.let { plan ->
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(plan.name, style = MaterialTheme.typography.headlineSmall)
                        Text(
                            "EDIT PLAN",
                            modifier = Modifier
                                .clickable {
                                    planEditor = plan
                                    planEditorOpen = true
                                }
                                .padding(vertical = 10.dp),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(Modifier.height(18.dp))

                    val mealMode = plan.sections == DaylinePlan.MEAL_SECTIONS
                    plan.sections.forEach { section ->
                        val sectionItems = plan.itemsFor(date, section)
                        PlanSection(
                            title = section,
                            items = sectionItems,
                            mealMode = mealMode,
                            onAdd = {
                                itemEditor = null
                                itemEditorSection = section
                                itemEditorOpen = true
                            },
                            onEdit = { item ->
                                itemEditor = item
                                itemEditorSection = item.section
                                itemEditorOpen = true
                            }
                        )
                        Spacer(Modifier.height(22.dp))
                    }
                }
            }
        }

        FloatingControls(
            modifier = Modifier.align(Alignment.BottomEnd).padding(end = 20.dp, bottom = 28.dp),
            onMenu = onMenu,
            onToday = onToday,
            onAdd = {
                if (selected == null) {
                    planEditor = null
                    planEditorOpen = true
                } else {
                    itemEditor = null
                    itemEditorSection = selected.sections.firstOrNull()
                    itemEditorOpen = true
                }
            }
        )
    }

    if (planEditorOpen) {
        PlanEditorSheet(
            plan = planEditor,
            onDismiss = { planEditorOpen = false },
            onSave = {
                savePlan(it)
                planEditorOpen = false
            },
            onDelete = planEditor?.let { existing ->
                {
                    onPlansChange(plans.filterNot { it.id == existing.id })
                    planEditorOpen = false
                }
            }
        )
    }

    if (itemEditorOpen && selected != null) {
        val mealMode = selected.sections == DaylinePlan.MEAL_SECTIONS
        PlanItemEditorSheet(
            plan = selected,
            item = itemEditor,
            initialSection = itemEditorSection ?: selected.sections.firstOrNull().orEmpty(),
            initialWeekday = selectedWeekday,
            mealMode = mealMode,
            onDismiss = { itemEditorOpen = false },
            onSave = {
                saveItem(it)
                itemEditorOpen = false
            },
            onDelete = itemEditor?.let { existing ->
                {
                    savePlan(selected.removeItem(existing.id))
                    itemEditorOpen = false
                }
            }
        )
    }
}

@Composable
private fun EmptyPlans(onCreate: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(Modifier.padding(20.dp)) {
            Text("No plans yet", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(7.dp))
            Text(
                "Create a weekly plan once, then it repeats automatically.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(18.dp))
            Text(
                "CREATE PLAN  +",
                modifier = Modifier.clickable(onClick = onCreate).padding(vertical = 10.dp),
                style = MaterialTheme.typography.labelLarge
            )
        }
    }
}

@Composable
private fun PlanTabs(
    plans: List<DaylinePlan>,
    selectedId: String?,
    onSelect: (String) -> Unit,
    onNew: () -> Unit
) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        plans.take(3).forEach { plan ->
            PlanChip(
                label = plan.name,
                selected = plan.id == selectedId,
                modifier = Modifier.weight(1f),
                onClick = { onSelect(plan.id) }
            )
        }
        PlanChip(
            label = "+",
            selected = false,
            modifier = Modifier.width(48.dp),
            onClick = onNew
        )
    }
}

@Composable
private fun PlanChip(
    label: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier.clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null,
            onClick = onClick
        ),
        shape = RoundedCornerShape(999.dp),
        color = if (selected) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.surface,
        contentColor = if (selected) MaterialTheme.colorScheme.background else MaterialTheme.colorScheme.onBackground
    ) {
        Box(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 11.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(label, style = MaterialTheme.typography.labelLarge, maxLines = 1)
        }
    }
}

@Composable
private fun WeekdayStrip(selectedDay: Int, onSelect: (Int) -> Unit) {
    val labels = listOf("MO", "TU", "WE", "TH", "FR", "SA", "SU")
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        labels.forEachIndexed { index, label ->
            val day = index + 1
            Surface(
                modifier = Modifier
                    .size(39.dp)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { onSelect(day) },
                shape = RoundedCornerShape(13.dp),
                color = if (day == selectedDay) {
                    MaterialTheme.colorScheme.onBackground
                } else {
                    MaterialTheme.colorScheme.surface
                },
                contentColor = if (day == selectedDay) {
                    MaterialTheme.colorScheme.background
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(label, style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }
}

@Composable
private fun PlanSection(
    title: String,
    items: List<DaylinePlanItem>,
    mealMode: Boolean,
    onAdd: () -> Unit,
    onEdit: (DaylinePlanItem) -> Unit
) {
    Column(Modifier.fillMaxWidth()) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                title.uppercase(Locale.getDefault()),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                "+ ADD",
                modifier = Modifier.clickable(onClick = onAdd).padding(vertical = 9.dp),
                style = MaterialTheme.typography.labelLarge
            )
        }

        if (items.isEmpty()) {
            Text(
                if (mealMode) "No food added." else "Nothing added.",
                modifier = Modifier.padding(vertical = 8.dp),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = .65f)
            )
        } else {
            items.forEachIndexed { index, item ->
                if (index > 0) Spacer(Modifier.height(9.dp))
                PlanItemCard(item = item, onClick = { onEdit(item) })
            }
        }
    }
}

@Composable
private fun PlanItemCard(item: DaylinePlanItem, onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            ),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface
    ) {
        Row(
            Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.Top
        ) {
            Column(Modifier.weight(1f)) {
                Text(item.title, style = MaterialTheme.typography.titleMedium)
                if (item.ingredients.isNotEmpty()) {
                    Spacer(Modifier.height(7.dp))
                    item.ingredients.forEach { ingredient ->
                        Text(
                            "•  $ingredient",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            Text(
                "›",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = .55f)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PlanItemEditorSheet(
    plan: DaylinePlan,
    item: DaylinePlanItem?,
    initialSection: String,
    initialWeekday: Int,
    mealMode: Boolean,
    onDismiss: () -> Unit,
    onSave: (DaylinePlanItem) -> Unit,
    onDelete: (() -> Unit)?
) {
    var title by remember(item?.id) { mutableStateOf(item?.title.orEmpty()) }
    var section by remember(item?.id) {
        mutableStateOf(item?.section?.takeIf { it in plan.sections } ?: initialSection)
    }
    var weekdays by remember(item?.id) {
        mutableStateOf(item?.weekdays?.takeIf { it.isNotEmpty() } ?: setOf(initialWeekday))
    }
    val ingredients = remember(item?.id) {
        mutableStateListOf<String>().apply {
            addAll(item?.ingredients.orEmpty())
            if (isEmpty()) add("")
        }
    }

    val noun = if (mealMode) "food" else "item"
    val detailNoun = if (mealMode) "ingredient" else "detail"
    val canSave = title.isNotBlank() && section.isNotBlank() && weekdays.isNotEmpty()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.background
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 26.dp)
                .padding(bottom = 40.dp)
        ) {
            Text(
                if (item == null) "Add $noun" else "Edit $noun",
                style = MaterialTheme.typography.displaySmall
            )
            Spacer(Modifier.height(22.dp))

            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                label = { Text(if (mealMode) "Food name" else "Item name") },
                placeholder = { Text(if (mealMode) "Greek yogurt bowl" else "Name") }
            )

            Spacer(Modifier.height(20.dp))
            Text(
                if (mealMode) "MEAL" else "SECTION",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(8.dp))
            FlowingChips(
                values = plan.sections,
                selected = setOf(section),
                onToggle = { section = it }
            )

            Spacer(Modifier.height(22.dp))
            Text(
                "REPEATS EVERY",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(8.dp))
            WeekdayPicker(
                selected = weekdays,
                onToggle = { day ->
                    weekdays = if (day in weekdays) weekdays - day else weekdays + day
                }
            )

            Spacer(Modifier.height(24.dp))
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    if (mealMode) "INGREDIENTS" else "DETAILS",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    "+ ADD",
                    modifier = Modifier.clickable { ingredients.add("") }.padding(vertical = 8.dp),
                    style = MaterialTheme.typography.labelLarge
                )
            }

            Spacer(Modifier.height(4.dp))
            ingredients.forEachIndexed { index, value ->
                IngredientRow(
                    index = index,
                    value = value,
                    noun = detailNoun,
                    canRemove = ingredients.size > 1,
                    onValueChange = { ingredients[index] = it },
                    onRemove = { ingredients.removeAt(index) }
                )
                if (index < ingredients.lastIndex) Spacer(Modifier.height(8.dp))
            }

            Spacer(Modifier.height(26.dp))
            PrimaryAction(
                label = if (item == null) "ADD " + noun.uppercase(Locale.getDefault()) else "SAVE",
                enabled = canSave,
                onClick = {
                    val base = item ?: DaylinePlanItem(
                        id = UUID.randomUUID().toString(),
                        title = "",
                        section = section
                    )
                    onSave(
                        base.copy(
                            title = title.trim(),
                            section = section,
                            ingredients = ingredients.map(String::trim).filter(String::isNotBlank),
                            weekdays = weekdays
                        )
                    )
                }
            )

            if (onDelete != null) {
                Spacer(Modifier.height(10.dp))
                Text(
                    "DELETE " + noun.uppercase(Locale.getDefault()),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onDelete)
                        .padding(vertical = 14.dp),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
private fun IngredientRow(
    index: Int,
    value: String,
    noun: String,
    canRemove: Boolean,
    onValueChange: (String) -> Unit,
    onRemove: () -> Unit
) {
    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            "•",
            modifier = Modifier.width(22.dp),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.weight(1f),
            singleLine = true,
            placeholder = {
                Text(noun.replaceFirstChar { it.uppercase() } + " " + (index + 1))
            }
        )
        if (canRemove) {
            Text(
                "×",
                modifier = Modifier.clickable(onClick = onRemove).padding(start = 12.dp, top = 12.dp, bottom = 12.dp),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun WeekdayPicker(selected: Set<Int>, onToggle: (Int) -> Unit) {
    val labels = listOf("M", "T", "W", "T", "F", "S", "S")
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        labels.forEachIndexed { index, label ->
            val day = index + 1
            Surface(
                modifier = Modifier
                    .size(40.dp)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { onToggle(day) },
                shape = RoundedCornerShape(14.dp),
                color = if (day in selected) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.surface,
                contentColor = if (day in selected) MaterialTheme.colorScheme.background else MaterialTheme.colorScheme.onSurfaceVariant
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(label, style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }
}

@Composable
private fun FlowingChips(
    values: List<String>,
    selected: Set<String>,
    onToggle: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        values.chunked(2).forEach { rowValues ->
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                rowValues.forEach { value ->
                    PlanChip(
                        label = value,
                        selected = value in selected,
                        modifier = Modifier.weight(1f),
                        onClick = { onToggle(value) }
                    )
                }
                if (rowValues.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PlanEditorSheet(
    plan: DaylinePlan?,
    onDismiss: () -> Unit,
    onSave: (DaylinePlan) -> Unit,
    onDelete: (() -> Unit)?
) {
    val isNew = plan == null
    var name by remember(plan?.id) { mutableStateOf(plan?.name.orEmpty()) }
    var useMeals by remember(plan?.id) {
        mutableStateOf(plan?.sections == DaylinePlan.MEAL_SECTIONS || isNew)
    }
    var sectionsText by remember(plan?.id) {
        mutableStateOf(
            when {
                plan != null -> plan.sections.joinToString("\n")
                else -> DaylinePlan.MEAL_SECTIONS.joinToString("\n")
            }
        )
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.background
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 26.dp)
                .padding(bottom = 40.dp)
        ) {
            Text(if (isNew) "New plan" else "Edit plan", style = MaterialTheme.typography.displaySmall)
            Spacer(Modifier.height(8.dp))
            Text(
                "Weekly only. It never creates calendar events.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(22.dp))
            if (isNew) {
                Text(
                    "TYPE",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PlanChip(
                        label = "Meals",
                        selected = useMeals,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            useMeals = true
                            sectionsText = DaylinePlan.MEAL_SECTIONS.joinToString("\n")
                            if (name.isBlank()) name = "Diet"
                        }
                    )
                    PlanChip(
                        label = "Custom",
                        selected = !useMeals,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            useMeals = false
                            sectionsText = ""
                        }
                    )
                }
                Spacer(Modifier.height(20.dp))
            }

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                label = { Text("Plan name") },
                placeholder = { Text(if (useMeals) "Diet" else "Study, routine…") }
            )

            if (!useMeals) {
                Spacer(Modifier.height(14.dp))
                OutlinedTextField(
                    value = sectionsText,
                    onValueChange = { sectionsText = it },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                    maxLines = 8,
                    label = { Text("Sections") },
                    supportingText = { Text("One section per line") },
                    placeholder = { Text("Morning\nAfternoon\nEvening") }
                )
            }

            Spacer(Modifier.height(24.dp))
            val sections = if (useMeals) {
                DaylinePlan.MEAL_SECTIONS
            } else {
                sectionsText.lines().map(String::trim).filter(String::isNotBlank).distinct()
            }
            PrimaryAction(
                label = if (isNew) "CREATE PLAN" else "SAVE PLAN",
                enabled = name.isNotBlank() && sections.isNotEmpty(),
                onClick = {
                    val base = plan ?: DaylinePlan(
                        id = UUID.randomUUID().toString(),
                        name = name.trim(),
                        sections = sections
                    )
                    onSave(
                        base.copy(name = name.trim())
                            .normalizedSections(sections)
                    )
                }
            )

            if (onDelete != null) {
                Spacer(Modifier.height(10.dp))
                Text(
                    "DELETE PLAN",
                    modifier = Modifier.fillMaxWidth().clickable(onClick = onDelete).padding(vertical = 14.dp),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
private fun PrimaryAction(label: String, enabled: Boolean, onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, onClick = onClick),
        shape = RoundedCornerShape(999.dp),
        color = if (enabled) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.surface,
        contentColor = if (enabled) MaterialTheme.colorScheme.background else MaterialTheme.colorScheme.onSurfaceVariant
    ) {
        Box(
            Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(label, style = MaterialTheme.typography.labelLarge)
        }
    }
}
