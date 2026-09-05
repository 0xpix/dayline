package com.pix.dayline.widgets

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.background
import androidx.glance.GlanceTheme
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.appWidgetBackground
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.updateAll
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
        val candidates = todayItems(items, date)

        val candidate = candidates.firstOrNull { item ->
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
                .padding(14.dp)
        ) {
            content()
        }
    }
}

@Composable
private fun Dot(
    color: ColorProvider,
    size: Int = 7
) {
    Box(
        modifier = GlanceModifier
            .size(15.dp)
            .padding(4.dp)
    ) {
        Box(
            modifier = GlanceModifier
                .size(size.dp)
                .background(color)
                .cornerRadius(20.dp)
        ) { }
    }
}

@Composable
private fun HourMap(
    items: List<DaylineItem>,
    date: LocalDate,
    compact: Boolean
) {
    val now = LocalDateTime.now()
    val hours = if (compact) {
        listOf(0, 2, 4, 6, 8, 10, 12, 14, 16, 18, 20, 22)
    } else {
        (0..23).toList()
    }

    val rows = hours.chunked(6)

    Column {
        rows.forEach { rowHours ->
            Row {
                rowHours.forEach { hour ->
                    val current = date == now.toLocalDate() &&
                        if (compact) now.hour / 2 * 2 == hour else now.hour == hour
                    val busy = if (compact) {
                        isHourBusy(items, date, hour) || isHourBusy(items, date, (hour + 1).coerceAtMost(23))
                    } else {
                        isHourBusy(items, date, hour)
                    }

                    val color = when {
                        current -> GlanceTheme.colors.tertiary
                        busy -> GlanceTheme.colors.primary
                        else -> GlanceTheme.colors.surfaceVariant
                    }

                    Dot(color = color, size = if (current) 8 else 7)
                }
            }
        }
    }
}

@Composable
private fun DateBubble(date: LocalDate) {
    Box(
        modifier = GlanceModifier
            .size(48.dp)
            .background(GlanceTheme.colors.primaryContainer)
            .cornerRadius(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            date.dayOfMonth.toString(),
            style = TextStyle(
                color = GlanceTheme.colors.onPrimaryContainer,
                fontSize = 25.sp,
                fontWeight = FontWeight.Bold
            )
        )
    }
}

@Composable
private fun TinyAgendaRow(item: DaylineItem) {
    Row(
        modifier = GlanceModifier.fillMaxWidth(),
        verticalAlignment = Alignment.Vertical.CenterVertically
    ) {
        Text(
            itemLead(item),
            modifier = GlanceModifier.width(42.dp),
            style = TextStyle(
                color = GlanceTheme.colors.primary,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold
            )
        )
        Text(
            item.title,
                        style = TextStyle(
                color = GlanceTheme.colors.onSurface,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        )
    }
}

/**
 * 2 columns x 1 row
 * A playful "day pulse": date bubble + 12-dot two-hour map + next item.
 */
class DaylineCompactWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val items = DaylineStore(context).loadItems()
        val now = LocalDateTime.now()
        val today = now.toLocalDate()
        val next = nextOccurrence(items, now)

        provideContent {
            WidgetSurface {
                Row(
                    modifier = GlanceModifier.fillMaxSize(),
                    verticalAlignment = Alignment.Vertical.CenterVertically
                ) {
                    DateBubble(today)
                    Spacer(GlanceModifier.width(10.dp))

                    Column(
                                                verticalAlignment = Alignment.Vertical.CenterVertically
                    ) {
                        Row(
                            modifier = GlanceModifier.fillMaxWidth(),
                            verticalAlignment = Alignment.Vertical.CenterVertically
                        ) {
                            Text(
                                "DAY MAP",
                                                                style = TextStyle(
                                    color = GlanceTheme.colors.onSurfaceVariant,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            )

                            Text(
                                today.dayOfWeek
                                    .getDisplayName(JavaTextStyle.SHORT, Locale.getDefault())
                                    .uppercase(),
                                style = TextStyle(
                                    color = GlanceTheme.colors.onSurfaceVariant,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }

                        Spacer(GlanceModifier.height(3.dp))
                        HourMap(items = items, date = today, compact = true)
                        Spacer(GlanceModifier.height(3.dp))

                        if (next == null) {
                            Text(
                                "Clear day",
                                style = TextStyle(
                                    color = GlanceTheme.colors.onSurface,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        } else {
                            Text(
                                "${if (next.date == today) itemLead(next.item) else next.date.dayOfWeek.getDisplayName(JavaTextStyle.SHORT, Locale.getDefault()).uppercase()}  ${next.item.title}",
                                style = TextStyle(
                                    color = GlanceTheme.colors.onSurface,
                                    fontSize = 11.sp,
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
 * 2 columns x 3 rows
 * A compact visual schedule: full 24-dot map with today's first items.
 */
class DaylineSquareWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val items = DaylineStore(context).loadItems()
        val today = LocalDate.now()
        val agenda = todayItems(items, today).take(3)

        provideContent {
            WidgetSurface {
                Column(GlanceModifier.fillMaxSize()) {
                    Row(
                        modifier = GlanceModifier.fillMaxWidth(),
                        verticalAlignment = Alignment.Vertical.CenterVertically
                    ) {
                        DateBubble(today)
                        Spacer(GlanceModifier.width(10.dp))
                        Column {
                            Text(
                                today.dayOfWeek.getDisplayName(JavaTextStyle.FULL, Locale.getDefault()),
                                style = TextStyle(
                                    color = GlanceTheme.colors.onSurface,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                            Text(
                                today.month.getDisplayName(JavaTextStyle.SHORT, Locale.getDefault()),
                                style = TextStyle(
                                    color = GlanceTheme.colors.onSurfaceVariant,
                                    fontSize = 11.sp
                                )
                            )
                        }
                    }

                    Spacer(GlanceModifier.height(12.dp))

                    Text(
                        "24 HOURS",
                        style = TextStyle(
                            color = GlanceTheme.colors.onSurfaceVariant,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    Spacer(GlanceModifier.height(4.dp))
                    HourMap(items = items, date = today, compact = false)

                    Spacer(GlanceModifier.height(10.dp))

                    if (agenda.isEmpty()) {
                        Text(
                            "Nothing planned — enjoy the gaps.",
                            style = TextStyle(
                                color = GlanceTheme.colors.onSurface,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    } else {
                        agenda.forEach { item ->
                            Column {
                                TinyAgendaRow(item)
                                Spacer(GlanceModifier.height(6.dp))
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
 * 3 columns x 3 rows
 * Day board: date, 24-hour dot matrix, free/busy summary and up to four items.
 */
class DaylineAgendaWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val items = DaylineStore(context).loadItems()
        val today = LocalDate.now()
        val agenda = todayItems(items, today).take(4)
        val busyHours = (0..23).count { isHourBusy(items, today, it) }
        val freeHours = 24 - busyHours

        provideContent {
            WidgetSurface {
                Column(GlanceModifier.fillMaxSize()) {
                    Row(
                        modifier = GlanceModifier.fillMaxWidth(),
                        verticalAlignment = Alignment.Vertical.CenterVertically
                    ) {
                        Text(
                            today.dayOfMonth.toString(),
                            style = TextStyle(
                                color = GlanceTheme.colors.onSurface,
                                fontSize = 38.sp,
                                fontWeight = FontWeight.Bold
                            )
                        )

                        Spacer(GlanceModifier.width(10.dp))

                        Column {
                            Text(
                                today.dayOfWeek.getDisplayName(JavaTextStyle.FULL, Locale.getDefault()),
                                style = TextStyle(
                                    color = GlanceTheme.colors.onSurface,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                            Text(
                                today.month.getDisplayName(JavaTextStyle.FULL, Locale.getDefault()),
                                style = TextStyle(
                                    color = GlanceTheme.colors.onSurfaceVariant,
                                    fontSize = 11.sp
                                )
                            )
                        }

                        Box(
                            modifier = GlanceModifier
                                .background(GlanceTheme.colors.secondaryContainer)
                                .cornerRadius(18.dp)
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                "$freeHours free",
                                style = TextStyle(
                                    color = GlanceTheme.colors.onSecondaryContainer,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }
                    }

                    Spacer(GlanceModifier.height(13.dp))

                    Row(
                        modifier = GlanceModifier.fillMaxWidth(),
                        verticalAlignment = Alignment.Vertical.CenterVertically
                    ) {
                        Column {
                            Text(
                                "YOUR DAY",
                                style = TextStyle(
                                    color = GlanceTheme.colors.onSurfaceVariant,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                            Spacer(GlanceModifier.height(4.dp))
                            HourMap(items = items, date = today, compact = false)
                        }

                        Spacer(GlanceModifier.width(12.dp))

                        Column {
                            if (agenda.isEmpty()) {
                                Text(
                                    "No blocks today.",
                                    style = TextStyle(
                                        color = GlanceTheme.colors.onSurface,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                                Spacer(GlanceModifier.height(4.dp))
                                Text(
                                    "A little room to improvise.",
                                    style = TextStyle(
                                        color = GlanceTheme.colors.onSurfaceVariant,
                                        fontSize = 11.sp
                                    )
                                )
                            } else {
                                agenda.forEach { item ->
                                    Column {
                                        TinyAgendaRow(item)
                                        Spacer(GlanceModifier.height(8.dp))
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
