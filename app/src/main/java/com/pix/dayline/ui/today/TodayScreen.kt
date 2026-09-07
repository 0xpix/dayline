package com.pix.dayline.ui.today

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.pix.dayline.model.DaylineItem
import com.pix.dayline.model.occursOn
import com.pix.dayline.ui.components.DayGlyph
import com.pix.dayline.ui.components.DayTimeline
import com.pix.dayline.ui.components.FloatingControls
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun TodayScreen(
    items: List<DaylineItem>,
    showOrb: Boolean,
    date: LocalDate = LocalDate.now(),
    onMenu: () -> Unit,
    onAdd: (LocalDate) -> Unit,
    onAddAt: (LocalDate, LocalTime) -> Unit,
    onAddTaskAt: (LocalDate, LocalTime) -> Unit = { _, _ -> },
    onStartFocusAt: (LocalDate, LocalTime, LocalTime) -> Unit = { _, _, _ -> },
    onEdit: (DaylineItem) -> Unit,
    onToggleTask: (DaylineItem, LocalDate) -> Unit,
    onReschedule: (DaylineItem) -> Unit,
    onResize: (DaylineItem) -> Unit = onReschedule,
    onScheduleTask: (DaylineItem) -> Unit = onReschedule,
    onReturnToday: () -> Unit = {},
    onSwipeUpcoming: () -> Unit = {}
) {
    var now by remember { mutableStateOf(LocalTime.now()) }
    var dragTotal by remember { mutableFloatStateOf(0f) }
    val scrollState = rememberScrollState()
    val scope = rememberCoroutineScope()
    val actualToday = LocalDate.now()

    fun scrollNear(time: LocalTime, animate: Boolean = true) {
        val minute = time.hour * 60 + time.minute
        val fraction = ((minute - 330).coerceIn(0, 1020) / 1020f)
        val target = (fraction * scrollState.maxValue.coerceAtLeast(1)).roundToInt()
        scope.launch {
            if (animate) scrollState.animateScrollTo(target) else scrollState.scrollTo(target)
        }
    }

    fun scrollToNow() {
        scrollNear(now)
    }

    fun handleTodayButton() {
        if (date != LocalDate.now()) onReturnToday() else scrollToNow()
    }

    LaunchedEffect(Unit) {
        while (true) {
            now = LocalTime.now()
            delay(30_000L)
        }
    }

    val dayItems = items.filter { it.occursOn(date) }
        .sortedWith(compareBy<DaylineItem> { it.startTime == null }.thenBy { it.startTime })

    // On first display, land slightly before the useful part of the day instead
    // of always starting at the top. This waits for the scroll range to exist.
    LaunchedEffect(date, dayItems.map { it.id to it.startTime }) {
        delay(160L)
        val target = when {
            date == LocalDate.now() -> now.minusMinutes(45)
            else -> dayItems.firstOrNull { it.startTime != null }?.startTime ?: LocalTime.of(8, 0)
        }
        scrollNear(target, animate = false)
    }

    Box(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)
            .pointerInput(onSwipeUpcoming) {
                detectHorizontalDragGestures(
                    onDragStart = { dragTotal = 0f },
                    onHorizontalDrag = { _, amount -> dragTotal += amount },
                    onDragCancel = { dragTotal = 0f },
                    onDragEnd = {
                        if (dragTotal < -120f) onSwipeUpcoming()
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
            modifier = Modifier.fillMaxSize().verticalScroll(scrollState)
                .padding(start = 32.dp, end = 32.dp, top = 48.dp, bottom = 138.dp)
        ) {
            if (showOrb) {
                DayGlyph(items = dayItems, date = date)
                Spacer(Modifier.height(20.dp))
            }

            Text(
                date.format(DateTimeFormatter.ofPattern("EEE · dd MMM", Locale.getDefault())).uppercase(Locale.getDefault()),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(9.dp))
            Text(
                if (date == actualToday) greetingText(now, date) else dayHeading(date),
                style = MaterialTheme.typography.displayMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(Modifier.height(14.dp))
            Text(
                todaySummary(dayItems),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(6.dp))
            Text(
                "Swipe left for Upcoming",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = .58f)
            )

            Spacer(Modifier.height(30.dp))
            DayTimeline(
                items = dayItems,
                date = date,
                emptyText = "Your day is clear.",
                onEdit = onEdit,
                onToggleTask = onToggleTask,
                onReschedule = onReschedule,
                onResize = onResize,
                onCreateAt = onAddAt,
                onCreateTaskAt = onAddTaskAt,
                onStartFocusAt = onStartFocusAt,
                onScheduleTask = onScheduleTask,
                onCurrentTimeTap = { if (date == LocalDate.now()) scrollToNow() },
                onAutoScroll = { delta ->
                    scope.launch { scrollState.scrollBy(delta.coerceIn(-64f, 64f)) }
                }
            )
        }

        FloatingControls(
            modifier = Modifier.align(Alignment.BottomEnd).padding(end = 20.dp, bottom = 28.dp),
            onMenu = onMenu,
            onToday = ::handleTodayButton,
            onAdd = { onAdd(date) }
        )
    }
}

private fun todaySummary(items: List<DaylineItem>): String {
    val intervals = items.mapNotNull { item ->
        if (item.allDay) return@mapNotNull null
        val start = item.startTime ?: return@mapNotNull null
        val end = item.endTime?.takeIf { it.isAfter(start) }
            ?: start.plusMinutes(if (item.kind.name == "TASK") item.estimatedDurationMinutes.toLong() else 60L)
        val startMinute = (start.hour * 60 + start.minute - item.bufferBeforeMinutes).coerceAtLeast(0)
        val endMinute = (end.hour * 60 + end.minute + item.bufferAfterMinutes).coerceAtMost(1440)
        startMinute to endMinute
    }.sortedBy { it.first }

    var busy = 0
    var currentStart: Int? = null
    var currentEnd: Int? = null
    intervals.forEach { (start, end) ->
        if (currentStart == null) {
            currentStart = start; currentEnd = end
        } else if (start <= currentEnd!!) {
            currentEnd = maxOf(currentEnd!!, end)
        } else {
            busy += currentEnd!! - currentStart!!; currentStart = start; currentEnd = end
        }
    }
    if (currentStart != null) busy += currentEnd!! - currentStart!!

    val open = (1440 - busy).coerceAtLeast(0)
    val allDay = items.count { it.allDay || (it.kind.name == "EVENT" && it.startTime == null) }
    val timed = intervals.size
    return buildString {
        append("BUSY ${durationShort(busy)}   ·   OPEN ${durationShort(open)}   ·   $timed ${if (timed == 1) "BLOCK" else "BLOCKS"}")
        if (allDay > 0) append("   ·   $allDay ALL DAY")
    }
}

private fun durationShort(minutes: Int): String {
    val hours = minutes / 60
    val rest = minutes % 60
    return when {
        hours > 0 && rest > 0 -> "${hours}H${rest.toString().padStart(2, '0')}"
        hours > 0 -> "${hours}H"
        else -> "${rest}M"
    }
}

private fun greetingText(now: LocalTime, date: LocalDate): String {
    val greeting = when (now.hour) {
        in 5..11 -> "Good morning"
        in 12..16 -> "Good afternoon"
        in 17..21 -> "Good evening"
        else -> "Good night"
    }
    val dayName = date.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.getDefault())
    val monthName = date.month.getDisplayName(TextStyle.FULL, Locale.getDefault())
    return "$greeting.\n$dayName, $monthName ${ordinal(date.dayOfMonth)}."
}

private fun dayHeading(date: LocalDate): String {
    val dayName = date.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.getDefault())
    val monthName = date.month.getDisplayName(TextStyle.FULL, Locale.getDefault())
    return "$dayName.\n$monthName ${ordinal(date.dayOfMonth)}."
}

private fun ordinal(day: Int): String {
    val suffix = if (day in 11..13) "th" else when (day % 10) {
        1 -> "st"; 2 -> "nd"; 3 -> "rd"; else -> "th"
    }
    return "$day$suffix"
}
