package com.pix.dayline.ui.today

import androidx.compose.foundation.background
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.pix.dayline.model.DaylineItem
import com.pix.dayline.model.occursOn
import com.pix.dayline.ui.components.DayGlyph
import com.pix.dayline.ui.components.DayTimeline
import com.pix.dayline.ui.components.FloatingControls
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.Duration
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
    onMenu: () -> Unit,
    onAdd: (LocalDate) -> Unit,
    onAddAt: (LocalDate, LocalTime) -> Unit,
    onEdit: (DaylineItem) -> Unit,
    onToggleTask: (DaylineItem, LocalDate) -> Unit,
    onReschedule: (DaylineItem) -> Unit,
    onResize: (DaylineItem) -> Unit = onReschedule,
    onScheduleTask: (DaylineItem) -> Unit = onReschedule
) {
    var now by remember { mutableStateOf(LocalTime.now()) }
    var today by remember { mutableStateOf(LocalDate.now()) }
    val scrollState = rememberScrollState()
    val scope = rememberCoroutineScope()

    fun scrollToNow() {
        val minute = now.hour * 60 + now.minute
        val fraction = ((minute - 360).coerceIn(0, 960) / 960f)
        val target = (fraction * scrollState.maxValue.coerceAtLeast(1)).roundToInt()
        scope.launch { scrollState.animateScrollTo(target) }
    }

    LaunchedEffect(Unit) {
        while (true) {
            now = LocalTime.now()
            today = LocalDate.now()
            delay(30_000L)
        }
    }

    val todaysItems = items
        .filter { it.occursOn(today) }
        .sortedWith(compareBy<DaylineItem> { it.startTime == null }.thenBy { it.startTime })

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(
                top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding(),
                bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(start = 32.dp, end = 32.dp, top = 60.dp, bottom = 138.dp)
        ) {
            if (showOrb) {
                DayGlyph(items = todaysItems, date = today)
                Spacer(Modifier.height(22.dp))
            }

            Text(
                text = today.format(DateTimeFormatter.ofPattern("EEE · dd MMM", Locale.getDefault())).uppercase(Locale.getDefault()),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(10.dp))

            Text(
                text = greetingText(now, today),
                style = MaterialTheme.typography.displayMedium,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(Modifier.height(12.dp))
            Text(
                text = todaySummary(todaysItems),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(28.dp))
            DayTimeline(
                items = todaysItems,
                date = today,
                emptyText = "Your day is clear.",
                onEdit = onEdit,
                onToggleTask = onToggleTask,
                onReschedule = onReschedule,
                onResize = onResize,
                onCreateAt = onAddAt,
                onScheduleTask = onScheduleTask,
                onCurrentTimeTap = ::scrollToNow
            )
        }

        FloatingControls(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 20.dp, bottom = 28.dp),
            onMenu = onMenu,
            onToday = ::scrollToNow,
            onAdd = { onAdd(today) }
        )
    }
}

private fun todaySummary(items: List<DaylineItem>): String {
    val intervals = items.mapNotNull { item ->
        val start = item.startTime ?: return@mapNotNull null
        val end = item.endTime?.takeIf { it.isAfter(start) } ?: start.plusHours(1)
        val startMinute = (start.hour * 60 + start.minute - item.bufferBeforeMinutes).coerceAtLeast(0)
        val endMinute = (end.hour * 60 + end.minute + item.bufferAfterMinutes).coerceAtMost(1440)
        startMinute to endMinute
    }.sortedBy { it.first }

    var busy = 0
    var currentStart: Int? = null
    var currentEnd: Int? = null
    intervals.forEach { (start, end) ->
        if (currentStart == null) {
            currentStart = start
            currentEnd = end
        } else if (start <= currentEnd!!) {
            currentEnd = maxOf(currentEnd!!, end)
        } else {
            busy += currentEnd!! - currentStart!!
            currentStart = start
            currentEnd = end
        }
    }
    if (currentStart != null) busy += currentEnd!! - currentStart!!

    val open = (1440 - busy).coerceAtLeast(0)
    val blocks = intervals.size
    return "BUSY ${durationShort(busy)}   ·   OPEN ${durationShort(open)}   ·   $blocks ${if (blocks == 1) "BLOCK" else "BLOCKS"}"
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
        in 5..11 -> "Good morning!"
        in 12..16 -> "Good afternoon!"
        in 17..21 -> "Good evening!"
        else -> "Good night!"
    }

    val dayName = date.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.getDefault())
    val monthName = date.month.getDisplayName(TextStyle.FULL, Locale.getDefault())
    return "$greeting\nIt's $dayName\n$monthName ${ordinal(date.dayOfMonth)}."
}

private fun ordinal(day: Int): String {
    val suffix = if (day in 11..13) "th" else when (day % 10) {
        1 -> "st"
        2 -> "nd"
        3 -> "rd"
        else -> "th"
    }
    return "$day$suffix"
}
