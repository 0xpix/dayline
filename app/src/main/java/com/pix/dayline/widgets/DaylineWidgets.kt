package com.pix.dayline.widgets

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.appWidgetBackground
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.updateAll
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.pix.dayline.MainActivity
import com.pix.dayline.data.DaylineStore
import com.pix.dayline.model.AgendaKind
import com.pix.dayline.model.DaylineItem
import com.pix.dayline.model.occursOn
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle as JavaTextStyle
import java.util.Locale

private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")

private data class Occurrence(
    val item: DaylineItem,
    val date: LocalDate
)

private fun todayItems(items: List<DaylineItem>, date: LocalDate): List<DaylineItem> =
    items
        .filter { it.occursOn(date) }
        .sortedWith(compareBy<DaylineItem> { it.startTime == null }.thenBy { it.startTime })

private fun nextOccurrence(items: List<DaylineItem>, now: LocalDateTime): Occurrence? {
    for (offset in 0L..30L) {
        val date = now.toLocalDate().plusDays(offset)
        val candidate = todayItems(items, date).firstOrNull { item ->
            if (date.isAfter(now.toLocalDate())) {
                true
            } else {
                val start = item.startTime
                val end = item.endTime
                when {
                    start == null -> true
                    end != null && end.isAfter(now.toLocalTime()) -> true
                    else -> !start.isBefore(now.toLocalTime())
                }
            }
        }
        if (candidate != null) return Occurrence(candidate, date)
    }
    return null
}

private fun itemLead(item: DaylineItem): String =
    when {
        item.startTime != null -> item.startTime.format(timeFormatter)
        item.kind == AgendaKind.TASK -> "TODO"
        else -> "ALL"
    }

private fun compactTitle(title: String, max: Int): String =
    if (title.length <= max) title else title.take(max - 1) + "…"

private fun isHourBusy(items: List<DaylineItem>, date: LocalDate, hour: Int): Boolean {
    val hourStart = hour * 60
    val hourEnd = hourStart + 60

    return items.any { item ->
        if (!item.occursOn(date) || item.startTime == null) return@any false

        val start = item.startTime.hour * 60 + item.startTime.minute
        val endTime = item.endTime
        val end = if (endTime != null && endTime.isAfter(item.startTime)) {
            endTime.hour * 60 + endTime.minute
        } else {
            start + 60
        }

        start < hourEnd && end > hourStart
    }
}

private fun moodLine(items: List<DaylineItem>, date: LocalDate): String {
    val busy = (0..23).count { isHourBusy(items, date, it) }
    return when {
        busy == 0 -> "WIDE OPEN"
        busy <= 4 -> "LIGHT DAY"
        busy <= 8 -> "IN MOTION"
        busy <= 12 -> "FULL RHYTHM"
        else -> "PACKED DAY"
    }
}

@Composable
private fun WidgetSurface(content: @Composable () -> Unit) {
    GlanceTheme {
        Box(
            modifier = GlanceModifier
                .fillMaxSize()
                .appWidgetBackground()
                .background(GlanceTheme.colors.widgetBackground)
                .cornerRadius(android.R.dimen.system_app_widget_background_radius)
                .clickable(actionStartActivity<MainActivity>())
                .padding(12.dp)
        ) {
            content()
        }
    }
}

@Composable
private fun Pill(
    text: String,
    emphasized: Boolean = false,
    small: Boolean = false
) {
    val background = if (emphasized) {
        GlanceTheme.colors.primaryContainer
    } else {
        GlanceTheme.colors.secondaryContainer
    }
    val foreground = if (emphasized) {
        GlanceTheme.colors.onPrimaryContainer
    } else {
        GlanceTheme.colors.onSecondaryContainer
    }

    Box(
        modifier = GlanceModifier
            .background(background)
            .cornerRadius(30.dp)
            .padding(
                horizontal = if (small) 7.dp else 9.dp,
                vertical = if (small) 3.dp else 5.dp
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text,
            style = TextStyle(
                color = foreground,
                fontSize = if (small) 9.sp else 11.sp,
                fontWeight = FontWeight.Bold
            )
        )
    }
}

@Composable
private fun BrandMark(compact: Boolean = false) {
    val size = if (compact) 34 else 42
    Box(
        modifier = GlanceModifier
            .size(size.dp)
            .background(GlanceTheme.colors.primaryContainer)
            .cornerRadius((size / 2).dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            "D.",
            style = TextStyle(
                color = GlanceTheme.colors.onPrimaryContainer,
                fontSize = if (compact) 15.sp else 18.sp,
                fontWeight = FontWeight.Bold
            )
        )
    }
}

@Composable
private fun TrackDot(
    color: ColorProvider,
    active: Boolean
) {
    val outer = if (active) 12 else 10
    val inner = if (active) 8 else 6

    Box(
        modifier = GlanceModifier
            .size(outer.dp)
            .background(
                if (active) GlanceTheme.colors.secondaryContainer
                else GlanceTheme.colors.widgetBackground
            )
            .cornerRadius(20.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = GlanceModifier
                .size(inner.dp)
                .background(color)
                .cornerRadius(20.dp)
        ) { }
    }
}

@Composable
private fun MiniDayTrack(
    items: List<DaylineItem>,
    date: LocalDate,
    dots: Int
) {
    val now = LocalDateTime.now()

    Row(verticalAlignment = Alignment.Vertical.CenterVertically) {
        repeat(dots.coerceAtMost(8)) { index ->
            val startHour = index * 24 / dots
            val endHour = (index + 1) * 24 / dots
            val current = date == now.toLocalDate() &&
                now.hour in startHour until endHour.coerceAtMost(24)

            var busy = false
            for (hour in startHour until endHour.coerceAtMost(24)) {
                if (isHourBusy(items, date, hour)) {
                    busy = true
                    break
                }
            }

            val color = when {
                current -> GlanceTheme.colors.tertiary
                busy -> GlanceTheme.colors.primary
                else -> GlanceTheme.colors.surfaceVariant
            }

            TrackDot(color = color, active = current)
        }
    }
}

@Composable
private fun EventCapsule(
    item: DaylineItem,
    highlight: Boolean,
    width: Int? = null
) {
    val modifier = if (width != null) {
        GlanceModifier
            .width(width.dp)
            .background(
                if (highlight) GlanceTheme.colors.primaryContainer
                else GlanceTheme.colors.secondaryContainer
            )
            .cornerRadius(30.dp)
            .padding(horizontal = 9.dp, vertical = 5.dp)
    } else {
        GlanceModifier
            .fillMaxWidth()
            .background(
                if (highlight) GlanceTheme.colors.primaryContainer
                else GlanceTheme.colors.secondaryContainer
            )
            .cornerRadius(30.dp)
            .padding(horizontal = 9.dp, vertical = 5.dp)
    }

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.Vertical.CenterVertically
    ) {
        Text(
            if (item.kind == AgendaKind.TASK) "□" else "↗",
            style = TextStyle(
                color = if (highlight) GlanceTheme.colors.onPrimaryContainer
                else GlanceTheme.colors.onSecondaryContainer,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        )
        Spacer(GlanceModifier.width(6.dp))
        Text(
            "${compactTitle(item.title, 13)} ${itemLead(item)}",
            style = TextStyle(
                color = if (highlight) GlanceTheme.colors.onPrimaryContainer
                else GlanceTheme.colors.onSecondaryContainer,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        )
    }
}

/**
 * Dayline Pulse — 3 columns x 1 row
 *
 * Wide Nothing-inspired strip:
 * orbit brand mark + weekday/date pills + highlighted next event
 * + tiny NEXT UP label + AM→PM day track.
 */
class DaylineCompactWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val items = DaylineStore(context).loadItems()
        val now = LocalDateTime.now()
        val today = now.toLocalDate()
        val next = nextOccurrence(items, now)
        val todayCount = todayItems(items, today).size

        provideContent {
            WidgetSurface {
                Row(
                    modifier = GlanceModifier.fillMaxSize(),
                    verticalAlignment = Alignment.Vertical.CenterVertically
                ) {
                    // Left: playful Dayline orbit puck.
                    Column(
                        modifier = GlanceModifier.width(54.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = GlanceModifier
                                .size(42.dp)
                                .background(GlanceTheme.colors.primaryContainer)
                                .cornerRadius(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "◔",
                                style = TextStyle(
                                    color = GlanceTheme.colors.onPrimaryContainer,
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }

                        Spacer(GlanceModifier.height(3.dp))

                        Text(
                            "DAYLINE",
                            style = TextStyle(
                                color = GlanceTheme.colors.onSurfaceVariant,
                                fontSize = 7.sp,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }

                    Spacer(GlanceModifier.width(8.dp))

                    // Main strip.
                    Column {
                        Row(verticalAlignment = Alignment.Vertical.CenterVertically) {
                            Text(
                                today.dayOfWeek
                                    .getDisplayName(JavaTextStyle.SHORT, Locale.getDefault())
                                    .uppercase(),
                                style = TextStyle(
                                    color = GlanceTheme.colors.onSurface,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            )

                            Spacer(GlanceModifier.width(5.dp))
                            Pill(today.dayOfMonth.toString(), emphasized = true, small = true)
                            Spacer(GlanceModifier.width(4.dp))
                            Pill(
                                today.month
                                    .getDisplayName(JavaTextStyle.SHORT, Locale.getDefault())
                                    .uppercase(),
                                small = true
                            )
                            Spacer(GlanceModifier.width(5.dp))
                            Pill(
                                if (todayCount == 1) "1 PLAN" else "$todayCount PLANS",
                                small = true
                            )
                        }

                        Spacer(GlanceModifier.height(5.dp))

                        Row(verticalAlignment = Alignment.Vertical.CenterVertically) {
                            if (next == null) {
                                Pill("↗  YOUR DAY IS CLEAR", emphasized = true)
                            } else {
                                EventCapsule(
                                    item = next.item,
                                    highlight = true,
                                    width = 142
                                )
                                Spacer(GlanceModifier.width(7.dp))
                                Column {
                                    Text(
                                        "NEXT",
                                        style = TextStyle(
                                            color = GlanceTheme.colors.onSurfaceVariant,
                                            fontSize = 7.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    )
                                    Text(
                                        "UP",
                                        style = TextStyle(
                                            color = GlanceTheme.colors.onSurfaceVariant,
                                            fontSize = 7.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    )
                                }
                            }
                        }

                        Spacer(GlanceModifier.height(5.dp))

                        Row(verticalAlignment = Alignment.Vertical.CenterVertically) {
                            Text(
                                "AM",
                                style = TextStyle(
                                    color = GlanceTheme.colors.onSurfaceVariant,
                                    fontSize = 7.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                            Spacer(GlanceModifier.width(5.dp))
                            MiniDayTrack(items = items, date = today, dots = 8)
                            Spacer(GlanceModifier.width(5.dp))
                            Text(
                                "PM",
                                style = TextStyle(
                                    color = GlanceTheme.colors.onSurfaceVariant,
                                    fontSize = 7.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}

class DaylineCompactWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = DaylineCompactWidget()
}

/**
 * Dayline Orbit — 2 columns x 2 rows
 *
 * Date pills + playful day-status phrase + 8-step orbit track + two agenda capsules.
 */
class DaylineSquareWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val items = DaylineStore(context).loadItems()
        val today = LocalDate.now()
        val agenda = todayItems(items, today).take(2)

        provideContent {
            WidgetSurface {
                Column(GlanceModifier.fillMaxSize()) {
                    Row(
                        modifier = GlanceModifier.fillMaxWidth(),
                        verticalAlignment = Alignment.Vertical.CenterVertically
                    ) {
                        BrandMark()

                        Spacer(GlanceModifier.width(9.dp))

                        Column {
                            Row(verticalAlignment = Alignment.Vertical.CenterVertically) {
                                Text(
                                    today.dayOfWeek
                                        .getDisplayName(JavaTextStyle.SHORT, Locale.getDefault())
                                        .uppercase(),
                                    style = TextStyle(
                                        color = GlanceTheme.colors.onSurface,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                                Spacer(GlanceModifier.width(5.dp))
                                Pill(today.dayOfMonth.toString(), emphasized = true, small = true)
                                Spacer(GlanceModifier.width(4.dp))
                                Pill(
                                    today.month
                                        .getDisplayName(JavaTextStyle.SHORT, Locale.getDefault())
                                        .uppercase(),
                                    small = true
                                )
                            }

                            Spacer(GlanceModifier.height(4.dp))

                            Text(
                                moodLine(items, today),
                                style = TextStyle(
                                    color = GlanceTheme.colors.onSurfaceVariant,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }
                    }

                    Spacer(GlanceModifier.height(9.dp))

                    Row(
                        verticalAlignment = Alignment.Vertical.CenterVertically
                    ) {
                        Text(
                            "AM",
                            style = TextStyle(
                                color = GlanceTheme.colors.onSurfaceVariant,
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold
                            )
                        )
                        Spacer(GlanceModifier.width(5.dp))
                        MiniDayTrack(items = items, date = today, dots = 8)
                        Spacer(GlanceModifier.width(5.dp))
                        Text(
                            "PM",
                            style = TextStyle(
                                color = GlanceTheme.colors.onSurfaceVariant,
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }

                    Spacer(GlanceModifier.height(9.dp))

                    if (agenda.isEmpty()) {
                        Pill("A BRIGHTER DAY AHEAD", emphasized = true)
                    } else {
                        agenda.forEachIndexed { index, item ->
                            Column {
                                EventCapsule(item = item, highlight = index == 0)
                                if (index != agenda.lastIndex) {
                                    Spacer(GlanceModifier.height(6.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

class DaylineSquareWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = DaylineSquareWidget()
}

/**
 * Dayline Board — 3 columns x 2 rows
 *
 * Wide modular board:
 * left brand/status rail, date pills, 12-step day track, and up to three event capsules.
 */
class DaylineAgendaWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val items = DaylineStore(context).loadItems()
        val today = LocalDate.now()
        val agenda = todayItems(items, today).take(3)

        provideContent {
            WidgetSurface {
                Row(
                    modifier = GlanceModifier.fillMaxSize(),
                    verticalAlignment = Alignment.Vertical.Top
                ) {
                    Column(
                        modifier = GlanceModifier.width(62.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        BrandMark()
                        Spacer(GlanceModifier.height(5.dp))
                        Text(
                            "DAYLINE",
                            style = TextStyle(
                                color = GlanceTheme.colors.onSurface,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                        )
                        Spacer(GlanceModifier.height(6.dp))
                        Text(
                            moodLine(items, today),
                            style = TextStyle(
                                color = GlanceTheme.colors.onSurfaceVariant,
                                fontSize = 7.sp,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }

                    Spacer(GlanceModifier.width(10.dp))

                    Column {
                        Row(verticalAlignment = Alignment.Vertical.CenterVertically) {
                            Text(
                                today.dayOfWeek
                                    .getDisplayName(JavaTextStyle.SHORT, Locale.getDefault())
                                    .uppercase(),
                                style = TextStyle(
                                    color = GlanceTheme.colors.onSurface,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                            Spacer(GlanceModifier.width(6.dp))
                            Pill(today.dayOfMonth.toString(), emphasized = true, small = true)
                            Spacer(GlanceModifier.width(5.dp))
                            Pill(
                                today.month
                                    .getDisplayName(JavaTextStyle.SHORT, Locale.getDefault())
                                    .uppercase(),
                                small = true
                            )
                        }

                        Spacer(GlanceModifier.height(8.dp))

                        Row(verticalAlignment = Alignment.Vertical.CenterVertically) {
                            Text(
                                "AM",
                                style = TextStyle(
                                    color = GlanceTheme.colors.onSurfaceVariant,
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                            Spacer(GlanceModifier.width(5.dp))
                            MiniDayTrack(items = items, date = today, dots = 8)
                            Spacer(GlanceModifier.width(5.dp))
                            Text(
                                "PM",
                                style = TextStyle(
                                    color = GlanceTheme.colors.onSurfaceVariant,
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }

                        Spacer(GlanceModifier.height(8.dp))

                        if (agenda.isEmpty()) {
                            Pill("YOUR DAY IS WIDE OPEN", emphasized = true)
                        } else {
                            agenda.forEachIndexed { index, item ->
                                Column {
                                    EventCapsule(
                                        item = item,
                                        highlight = index == 0,
                                        width = 182
                                    )
                                    if (index != agenda.lastIndex) {
                                        Spacer(GlanceModifier.height(5.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

class DaylineAgendaWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = DaylineAgendaWidget()
}

object DaylineWidgetUpdater {
    suspend fun updateAll(context: Context) {
        DaylineCompactWidget().updateAll(context)
        DaylineSquareWidget().updateAll(context)
        DaylineAgendaWidget().updateAll(context)
    }
}
