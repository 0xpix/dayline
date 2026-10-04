package com.pix.dayline.ui.plans

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.horizontalScroll
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
import com.pix.dayline.ui.components.FloatingControls
import java.time.LocalDate
import java.time.format.DateTimeFormatter
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
    var editorPlan by remember { mutableStateOf<DaylinePlan?>(null) }
    var editorOpen by remember { mutableStateOf(false) }
    var dragTotal by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(plans) {
        if (plans.none { it.id == selectedPlanId }) selectedPlanId = plans.firstOrNull()?.id
    }

    val selected = plans.firstOrNull { it.id == selectedPlanId } ?: plans.firstOrNull()

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
                .padding(start = 30.dp, end = 30.dp, top = 52.dp, bottom = 138.dp)
        ) {
            Text("Plans", style = MaterialTheme.typography.displayMedium)
            Spacer(Modifier.height(8.dp))
            Text(
                "Date-aware things you follow, without putting them on your calendar.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "Swipe right for Today",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = .62f)
            )
            Spacer(Modifier.height(26.dp))

            if (plans.isEmpty()) {
                EmptyPlans {
                    editorPlan = null
                    editorOpen = true
                }
            } else {
                Row(
                    Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    plans.forEach { plan ->
                        PlanChip(
                            label = plan.name,
                            selected = plan.id == selected?.id,
                            onClick = { selectedPlanId = plan.id }
                        )
                    }
                    PlanChip(
                        label = "+ New",
                        selected = false,
                        onClick = {
                            editorPlan = null
                            editorOpen = true
                        }
                    )
                }

                Spacer(Modifier.height(24.dp))
                DateSelector(
                    date = date,
                    onPrevious = { onDateChange(date.minusDays(1)) },
                    onNext = { onDateChange(date.plusDays(1)) }
                )
                Spacer(Modifier.height(24.dp))

                selected?.let { plan ->
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(plan.name, style = MaterialTheme.typography.headlineSmall)
                        Text(
                            "EDIT",
                            modifier = Modifier
                                .clickable {
                                    editorPlan = plan
                                    editorOpen = true
                                }
                                .padding(vertical = 10.dp, horizontal = 4.dp),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(Modifier.height(16.dp))

                    if (plan.sections.isEmpty()) {
                        Text(
                            "This plan has no sections yet. Edit it to add some.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        plan.sections.forEach { section ->
                            PlanSection(
                                title = section,
                                value = plan.entry(date, section),
                                onValueChange = { value ->
                                    val updated = plan.withEntry(date, section, value)
                                    onPlansChange(plans.map { if (it.id == plan.id) updated else it })
                                }
                            )
                            Spacer(Modifier.height(16.dp))
                        }
                    }
                }
            }
        }

        FloatingControls(
            modifier = Modifier.align(Alignment.BottomEnd).padding(end = 20.dp, bottom = 28.dp),
            onMenu = onMenu,
            onToday = onToday,
            onAdd = {
                editorPlan = null
                editorOpen = true
            }
        )
    }

    if (editorOpen) {
        PlanEditorSheet(
            plan = editorPlan,
            onDismiss = { editorOpen = false },
            onSave = { updated ->
                val next = if (plans.any { it.id == updated.id }) {
                    plans.map { if (it.id == updated.id) updated else it }
                } else {
                    plans + updated
                }
                selectedPlanId = updated.id
                onPlansChange(next)
                editorOpen = false
            },
            onDelete = editorPlan?.let { existing ->
                {
                    onPlansChange(plans.filterNot { it.id == existing.id })
                    editorOpen = false
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
            Spacer(Modifier.height(8.dp))
            Text(
                "Create a blank plan or start from a simple template. Plans stay outside your timeline and Upcoming calendar.",
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
private fun PlanChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null,
            onClick = onClick
        ),
        shape = RoundedCornerShape(999.dp),
        color = if (selected) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.surface,
        contentColor = if (selected) MaterialTheme.colorScheme.background else MaterialTheme.colorScheme.onBackground
    ) {
        Text(label, modifier = Modifier.padding(horizontal = 15.dp, vertical = 10.dp), style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
private fun DateSelector(date: LocalDate, onPrevious: () -> Unit, onNext: () -> Unit) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("‹", modifier = Modifier.clickable(onClick = onPrevious).padding(12.dp), style = MaterialTheme.typography.headlineMedium)
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                date.format(DateTimeFormatter.ofPattern("EEEE", Locale.getDefault())),
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                date.format(DateTimeFormatter.ofPattern("dd MMMM", Locale.getDefault())),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Text("›", modifier = Modifier.clickable(onClick = onNext).padding(12.dp), style = MaterialTheme.typography.headlineMedium)
    }
}

@Composable
private fun PlanSection(title: String, value: String, onValueChange: (String) -> Unit) {
    Column {
        Text(title.uppercase(Locale.getDefault()), style = MaterialTheme.typography.labelLarge)
        Spacer(Modifier.height(7.dp))
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            minLines = 2,
            maxLines = 6,
            placeholder = { Text("Add " + title.lowercase(Locale.getDefault())) }
        )
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
    var sectionsText by remember(plan?.id) {
        mutableStateOf(plan?.sections?.joinToString("\n").orEmpty())
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.background
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 28.dp)
                .padding(bottom = 38.dp)
        ) {
            Text(if (isNew) "New plan" else "Edit plan", style = MaterialTheme.typography.displaySmall)
            Spacer(Modifier.height(8.dp))
            Text(
                "This is separate from Quick Add. Nothing here creates calendar time.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(22.dp))

            if (isNew) {
                Text("START FROM", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(9.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PlanChip(
                        label = "Blank",
                        selected = sectionsText.isBlank(),
                        onClick = { sectionsText = "" }
                    )
                    PlanChip(
                        label = "Meals",
                        selected = sectionsText.lines().filter(String::isNotBlank) == DaylinePlan.MEAL_SECTIONS,
                        onClick = {
                            if (name.isBlank()) name = "Diet"
                            sectionsText = DaylinePlan.MEAL_SECTIONS.joinToString("\n")
                        }
                    )
                }
                Spacer(Modifier.height(22.dp))
            }

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                label = { Text("Plan name") },
                placeholder = { Text("Diet, Study, Travel…") }
            )
            Spacer(Modifier.height(14.dp))
            OutlinedTextField(
                value = sectionsText,
                onValueChange = { sectionsText = it },
                modifier = Modifier.fillMaxWidth(),
                minLines = 4,
                maxLines = 10,
                label = { Text("Sections") },
                supportingText = { Text("One section per line") },
                placeholder = { Text("Morning\nAfternoon\nEvening") }
            )
            Spacer(Modifier.height(22.dp))

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(enabled = name.isNotBlank()) {
                        val sections = sectionsText.lines().map(String::trim).filter(String::isNotBlank).distinct()
                        val base = plan ?: DaylinePlan(UUID.randomUUID().toString(), name.trim(), sections)
                        onSave(base.copy(name = name.trim()).normalizedSections(sections))
                    },
                shape = RoundedCornerShape(999.dp),
                color = if (name.isNotBlank()) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.surface,
                contentColor = if (name.isNotBlank()) MaterialTheme.colorScheme.background else MaterialTheme.colorScheme.onSurfaceVariant
            ) {
                Text(
                    if (isNew) "CREATE PLAN" else "SAVE PLAN",
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
                    style = MaterialTheme.typography.labelLarge
                )
            }

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
