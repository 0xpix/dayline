package com.pix.dayline.widgets

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.LocalContext
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.appWidgetBackground
import androidx.glance.appwidget.background
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.updateAll
import androidx.glance.color.ColorProvider
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
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

private val widgetBackgroundDay = Color(0xFFFBFAF8)
private val widgetBackgroundNight = Color(0xFF050505)
private val widgetTextDay = Color(0xFF1A1A1A)
private val widgetTextNight = Color(0xFFF5F4F1)
private val widgetMutedDay = Color(0xFF77736F)
private val widgetMutedNight = Color(0xFFA5A5A1)

private val textColor = ColorProvider(widgetTextDay, widgetTextNight)
private val mutedColor = ColorProvider(widgetMutedDay, widgetMutedNight)

private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")

private data class Occurrence(
    val item: DaylineItem,
    val date: LocalDate,
    val time: LocalTime?
)

private fun todayItems(items: List<DaylineItem>, date: LocalDate): List<DaylineItem> =
    items
        .filter { it.occursOn(date) }
        .sortedWith(compareBy<DaylineItem> { it.startTime == null }.thenBy { it.startTime })

private fun nextOccurrence(items: List<DaylineItem>, now: LocalDateTime): Occurrence? {
    for (offset in 0L..30L) {
        val date = now.toLocalDate().plusDays(offset)
        val candidates = todayItems(items, date)

        val activeOrFuture = candidates.firstOrNull { item ->
            val start = item.startTime
            val end = item.endTime
            when {
                date.isAfter(now.toLocalDate()) -> true
                start == null -> true
                end != null && end.isAfter(now.toLocalTime()) -> true
                else -> !start.isBefore(now.toLocalTime())
            }
        }

        if (activeOrFuture != null) {
            return Occurrence(activeOrFuture, date, activeOrFuture.startTime)
        }
    }
    return null
}

private fun itemLead(item: DaylineItem): String =
    when {
        item.startTime != null -> item.startTime.format(timeFormatter)
        item.kind == AgendaKind.TASK -> "TODO"
        else -> "ALL"
    }

@Composable
private fun WidgetSurface(content: @Composable () -> Unit) {
    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .appWidgetBackground()
            .background(widgetBackgroundDay, widgetBackgroundNight)
            .cornerRadius(android.R.dimen.system_app_widget_background_radius)
            .clickable(actionStartActivity<MainActivity>())
            .padding(14.dp)
    ) {
        content()
    }
}

class DaylineCompactWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val items = DaylineStore(context).loadItems()
        val now = LocalDateTime.now()
        val next = nextOccurrence(items, now)

        provideContent {
            WidgetSurface {
                Column(
                    modifier = GlanceModifier.fillMaxSize(),
                    verticalAlignment = Alignment.Vertical.CenterVertically
                ) {
                    Text(
                        text = now.dayOfMonth.toString(),
                        style = TextStyle(
                            color = textColor,
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    Text(
                        text = now.dayOfWeek.getDisplayName(JavaTextStyle.SHORT, Locale.getDefault()).uppercase(),
                        style = TextStyle(color = mutedColor, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    )
                    Spacer(GlanceModifier.height(12.dp))

                    if (next == null) {
                        Text("CLEAR", style = TextStyle(color = mutedColor, fontSize = 10.sp, fontWeight = FontWeight.Bold))
                        Text("No plans", style = TextStyle(color = textColor, fontSize = 12.sp))
                    } else {
                        Text(
                            if (next.date == now.toLocalDate()) itemLead(next.item) else next.date.dayOfWeek.getDisplayName(JavaTextStyle.SHORT, Locale.getDefault()).uppercase(),
                            style = TextStyle(color = mutedColor, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        )
                        Text(next.item.title, style = TextStyle(color = textColor, fontSize = 12.sp, fontWeight = FontWeight.Bold))
                    }
                }
            }
        }
    }
}

class DaylineCompactWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = DaylineCompactWidget()
}

class DaylineSquareWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val items = DaylineStore(context).loadItems()
        val today = LocalDate.now()
        val todayItems = todayItems(items, today).take(3)

        provideContent {
            WidgetSurface {
                Column(GlanceModifier.fillMaxSize()) {
                    Row(
                        modifier = GlanceModifier.fillMaxWidth(),
                        verticalAlignment = Alignment.Vertical.CenterVertically
                    ) {
                        Text(
                            today.dayOfMonth.toString(),
                            style = TextStyle(color = textColor, fontSize = 32.sp, fontWeight = FontWeight.Bold)
                        )
                        Spacer(GlanceModifier.width(10.dp))
                        Column {
                            Text(
                                today.dayOfWeek.getDisplayName(JavaTextStyle.FULL, Locale.getDefault()),
                                style = TextStyle(color = textColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            )
                            Text(
                                today.month.getDisplayName(JavaTextStyle.SHORT, Locale.getDefault()),
                                style = TextStyle(color = mutedColor, fontSize = 11.sp)
                            )
                        }
                    }

                    Spacer(GlanceModifier.height(12.dp))

                    if (todayItems.isEmpty()) {
                        Text("Your day is clear.", style = TextStyle(color = mutedColor, fontSize = 12.sp))
                    } else {
                        todayItems.forEach { item ->
                            AgendaRow(item)
                            Spacer(GlanceModifier.height(6.dp))
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

class DaylineAgendaWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val items = DaylineStore(context).loadItems()
        val today = LocalDate.now()
        val todayItems = todayItems(items, today).take(6)
        val next = nextOccurrence(items, LocalDateTime.now())

        provideContent {
            WidgetSurface {
                Column(GlanceModifier.fillMaxSize()) {
                    Text(
                        "TODAY",
                        style = TextStyle(color = mutedColor, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    )
                    Text(
                        "${today.dayOfWeek.getDisplayName(JavaTextStyle.FULL, Locale.getDefault())} ${today.dayOfMonth}",
                        style = TextStyle(color = textColor, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    )

                    Spacer(GlanceModifier.height(14.dp))

                    if (todayItems.isNotEmpty()) {
                        todayItems.forEach { item ->
                            Column {
                                AgendaRow(item)
                                Spacer(GlanceModifier.height(8.dp))
                            }
                        }
                    } else if (next != null) {
                        Text("Nothing else today", style = TextStyle(color = mutedColor, fontSize = 12.sp))
                        Spacer(GlanceModifier.height(12.dp))
                        Text(
                            "NEXT · ${next.date.dayOfWeek.getDisplayName(JavaTextStyle.SHORT, Locale.getDefault()).uppercase()}",
                            style = TextStyle(color = mutedColor, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        )
                        Text(next.item.title, style = TextStyle(color = textColor, fontSize = 14.sp, fontWeight = FontWeight.Bold))
                    } else {
                        Text("Your schedule is clear.", style = TextStyle(color = mutedColor, fontSize = 12.sp))
                    }
                }
            }
        }
    }
}

class DaylineAgendaWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = DaylineAgendaWidget()
}

@Composable
private fun AgendaRow(item: DaylineItem) {
    Row(
        modifier = GlanceModifier.fillMaxWidth(),
        verticalAlignment = Alignment.Vertical.CenterVertically
    ) {
        Text(
            itemLead(item),
            modifier = GlanceModifier.width(48.dp),
            style = TextStyle(color = mutedColor, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        )
        Text(
            item.title,
            style = TextStyle(color = textColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        )
    }
}

object DaylineWidgetUpdater {
    suspend fun updateAll(context: Context) {
        DaylineCompactWidget().updateAll(context)
        DaylineSquareWidget().updateAll(context)
        DaylineAgendaWidget().updateAll(context)
    }
}
