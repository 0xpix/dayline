package com.pix.dayline.widgets

import android.content.Context
import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.glance.ColorFilter
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
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
import androidx.glance.layout.ContentScale
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import com.pix.dayline.MainActivity
import com.pix.dayline.R
import com.pix.dayline.data.DaylineStore
import com.pix.dayline.data.WidgetCoverChoice
import com.pix.dayline.data.WidgetFontChoice
import com.pix.dayline.model.AgendaKind
import com.pix.dayline.model.DaylineItem
import com.pix.dayline.model.occursOn
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
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

private fun nextStatus(next: Occurrence?, now: LocalDateTime): String {
    if (next == null) return "OPEN"

    val start = next.item.startTime
        ?: return if (next.date == now.toLocalDate()) "TODAY"
        else next.date.dayOfWeek
            .getDisplayName(JavaTextStyle.SHORT, Locale.getDefault())
            .uppercase()

    val startDateTime = LocalDateTime.of(next.date, start)
    val minutes = Duration.between(now, startDateTime).toMinutes()

    return when {
        minutes <= 0 -> "NOW"
        minutes < 60 -> "${minutes}M"
        minutes < 24 * 60 -> "${minutes / 60}H"
        else -> next.date.dayOfWeek
            .getDisplayName(JavaTextStyle.SHORT, Locale.getDefault())
            .uppercase()
    }
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
private fun WidgetText(
    text: String,
    fontChoice: WidgetFontChoice,
    scale: Float = 1f,
    color: androidx.glance.unit.ColorProvider,
    maxChars: Int = 24
) {
    if (fontChoice == WidgetFontChoice.MONO) {
        Text(
            text = text,
            style = TextStyle(
                color = color,
                fontSize = (10f * scale).sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
        )
        return
    }

    val context = LocalContext.current
    val rendered = DotMatrixRenderer.render(
        context = context,
        rawText = text,
        style = fontChoice,
        scale = scale,
        maxChars = maxChars
    )

    Image(
        provider = ImageProvider(rendered.bitmap),
        contentDescription = text,
        modifier = GlanceModifier
            .width(rendered.widthDp.dp)
            .height(rendered.heightDp.dp),
        contentScale = ContentScale.Fit,
        colorFilter = ColorFilter.tint(color)
    )
}


@Composable
private fun TransparentPulseSurface(content: @Composable () -> Unit) {
    GlanceTheme {
        Box(
            modifier = GlanceModifier
                .fillMaxSize()
                .appWidgetBackground()
                .clickable(actionStartActivity<MainActivity>())
                .padding(horizontal = 5.dp, vertical = 4.dp)
        ) {
            content()
        }
    }
}

@Composable
private fun SystemSquareSurface(content: @Composable () -> Unit) {
    GlanceTheme {
        Box(
            modifier = GlanceModifier
                .fillMaxSize()
                .appWidgetBackground()
                .background(GlanceTheme.colors.widgetBackground)
                .cornerRadius(android.R.dimen.system_app_widget_background_radius)
                .clickable(actionStartActivity<MainActivity>())
                .padding(11.dp)
        ) {
            content()
        }
    }
}

private fun widgetCoverResource(cover: WidgetCoverChoice): Int = when (cover) {
    WidgetCoverChoice.DAYLINE -> R.drawable.widget_cover_dayline
    WidgetCoverChoice.CALENDAR -> R.drawable.widget_cover_calendar
    WidgetCoverChoice.WORK -> R.drawable.widget_cover_work
    WidgetCoverChoice.GAME -> R.drawable.widget_cover_game
    WidgetCoverChoice.CHAT -> R.drawable.widget_cover_chat
    WidgetCoverChoice.HOME -> R.drawable.widget_cover_home
}

@Composable
private fun WidgetCoverMark(
    cover: WidgetCoverChoice,
    size: Int
) {
    Image(
        provider = ImageProvider(widgetCoverResource(cover)),
        contentDescription = null,
        modifier = GlanceModifier.size(size.dp),
        contentScale = ContentScale.Fit,
        colorFilter = ColorFilter.tint(GlanceTheme.colors.onSurface)
    )
}

@Composable
private fun SystemPill(
    strong: Boolean = false,
    horizontalPadding: Int = 7,
    verticalPadding: Int = 3,
    content: @Composable () -> Unit
) {
    Box(
        modifier = GlanceModifier
            .background(
                if (strong) GlanceTheme.colors.primaryContainer
                else GlanceTheme.colors.secondaryContainer
            )
            .cornerRadius(30.dp)
            .padding(
                horizontal = horizontalPadding.dp,
                vertical = verticalPadding.dp
            ),
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}

@Composable
private fun DayTrack(
    items: List<DaylineItem>,
    date: LocalDate
) {
    val now = LocalDateTime.now()

    Row(verticalAlignment = Alignment.Vertical.CenterVertically) {
        repeat(8) { index ->
            val startHour = index * 3
            val endHour = startHour + 3
            val current = date == now.toLocalDate() && now.hour in startHour until endHour

            var busy = false
            for (hour in startHour until endHour) {
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

            Box(
                modifier = GlanceModifier.size(11.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = GlanceModifier
                        .size(if (current) 9.dp else if (busy) 7.dp else 5.dp)
                        .background(color)
                        .cornerRadius(20.dp)
                ) { }
            }
        }
    }
}

@Composable
private fun SystemEventPill(
    item: DaylineItem,
    strong: Boolean,
    fontChoice: WidgetFontChoice,
    width: Int? = null
) {
    val base = GlanceModifier
        .background(
            if (strong) GlanceTheme.colors.primaryContainer
            else GlanceTheme.colors.secondaryContainer
        )
        .cornerRadius(30.dp)
        .padding(horizontal = 8.dp, vertical = 4.dp)

    val modifier = if (width != null) base.width(width.dp) else base.fillMaxWidth()

    Box(
        modifier = modifier,
        contentAlignment = Alignment.CenterStart
    ) {
        WidgetText(
            text = "${if (item.kind == AgendaKind.TASK) "+" else ">"} ${compactTitle(item.title, 11)} ${itemLead(item)}",
            fontChoice = fontChoice,
            scale = 0.82f,
            color = if (strong) GlanceTheme.colors.onPrimaryContainer
            else GlanceTheme.colors.onSecondaryContainer,
            maxChars = 20
        )
    }
}


/**
 * Pulse 3×1
 * Transparent body + system-colored pills + Nothing-style dot-matrix typography.
 */
class DaylineCompactWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val items = DaylineStore(context).loadItems()
        val now = LocalDateTime.now()
        val today = now.toLocalDate()
        val next = nextOccurrence(items, now)
        val widgetFont = DaylineStore(context).loadWidgetFontChoice()
        val widgetCover = DaylineStore(context).loadWidgetCoverChoice()

        provideContent {
            TransparentPulseSurface {
                Row(
                    modifier = GlanceModifier.fillMaxSize(),
                    verticalAlignment = Alignment.Vertical.CenterVertically
                ) {
                    Column(
                        modifier = GlanceModifier.width(44.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        WidgetCoverMark(widgetCover, 36)
                        Spacer(GlanceModifier.height(2.dp))
                        WidgetText(
                            "DAYLINE",
                            widgetFont,
                            scale = 0.48f,
                            color = GlanceTheme.colors.onSurface
                        )
                    }

                    Spacer(GlanceModifier.width(6.dp))

                    Box(
                        modifier = GlanceModifier
                            .width(1.dp)
                            .height(48.dp)
                            .background(GlanceTheme.colors.onSurfaceVariant)
                    ) { }

                    Spacer(GlanceModifier.width(7.dp))

                    Column {
                        Row(verticalAlignment = Alignment.Vertical.CenterVertically) {
                            WidgetText(
                                today.dayOfWeek
                                    .getDisplayName(JavaTextStyle.SHORT, Locale.getDefault())
                                    .uppercase(),
                                widgetFont,
                                scale = 0.72f,
                                color = GlanceTheme.colors.onSurface
                            )

                            Spacer(GlanceModifier.width(5.dp))

                            SystemPill(strong = true, horizontalPadding = 6, verticalPadding = 2) {
                                WidgetText(
                                    today.dayOfMonth.toString(),
                                    widgetFont,
                                    scale = 0.64f,
                                    color = GlanceTheme.colors.onPrimaryContainer
                                )
                            }

                            Spacer(GlanceModifier.width(4.dp))

                            SystemPill(horizontalPadding = 6, verticalPadding = 2) {
                                WidgetText(
                                    today.month
                                        .getDisplayName(JavaTextStyle.SHORT, Locale.getDefault())
                                        .uppercase(),
                                    widgetFont,
                                    scale = 0.58f,
                                    color = GlanceTheme.colors.onSecondaryContainer
                                )
                            }

                            Spacer(GlanceModifier.width(4.dp))

                            SystemPill(strong = true, horizontalPadding = 6, verticalPadding = 2) {
                                WidgetText(
                                    nextStatus(next, now),
                                    widgetFont,
                                    scale = 0.56f,
                                    color = GlanceTheme.colors.onPrimaryContainer
                                )
                            }
                        }

                        Spacer(GlanceModifier.height(4.dp))

                        Row(verticalAlignment = Alignment.Vertical.CenterVertically) {
                            if (next == null) {
                                SystemPill(strong = true, horizontalPadding = 8, verticalPadding = 4) {
                                    WidgetText(
                                        "YOUR DAY IS CLEAR",
                                        widgetFont,
                                        scale = 0.69f,
                                        color = GlanceTheme.colors.onPrimaryContainer,
                                        maxChars = 20
                                    )
                                }
                            } else {
                                SystemEventPill(
                                    item = next.item,
                                    strong = true,
                                    fontChoice = widgetFont,
                                    width = 137
                                )
                                Spacer(GlanceModifier.width(5.dp))
                                Column {
                                    WidgetText(
                                        "NEXT",
                                        widgetFont,
                                        scale = 0.44f,
                                        color = GlanceTheme.colors.onSurface
                                    )
                                    Spacer(GlanceModifier.height(1.dp))
                                    WidgetText(
                                        "UP",
                                        widgetFont,
                                        scale = 0.44f,
                                        color = GlanceTheme.colors.onSurface
                                    )
                                }
                            }
                        }

                        Spacer(GlanceModifier.height(4.dp))

                        Row(verticalAlignment = Alignment.Vertical.CenterVertically) {
                            WidgetText(
                                "AM",
                                widgetFont,
                                scale = 0.48f,
                                color = GlanceTheme.colors.onSurface
                            )
                            Spacer(GlanceModifier.width(4.dp))
                            DayTrack(items = items, date = today)
                            Spacer(GlanceModifier.width(4.dp))
                            WidgetText(
                                "PM",
                                widgetFont,
                                scale = 0.48f,
                                color = GlanceTheme.colors.onSurface
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
 * Orbit 2×2
 * System background and system pills, but the display typography uses the same dot matrix.
 */
class DaylineSquareWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val items = DaylineStore(context).loadItems()
        val today = LocalDate.now()
        val agenda = todayItems(items, today).take(2)
        val busyHours = (0..23).count { isHourBusy(items, today, it) }
        val freeHours = 24 - busyHours
        val widgetFont = DaylineStore(context).loadWidgetFontChoice()

        provideContent {
            SystemSquareSurface {
                Column(GlanceModifier.fillMaxSize()) {
                    Row(
                        modifier = GlanceModifier.fillMaxWidth(),
                        verticalAlignment = Alignment.Vertical.CenterVertically
                    ) {
                        WidgetText(
                            today.dayOfMonth.toString(),
                            widgetFont,
                            scale = 1.15f,
                            color = GlanceTheme.colors.onSurface
                        )

                        Spacer(GlanceModifier.width(8.dp))

                        Column {
                            WidgetText(
                                today.dayOfWeek
                                    .getDisplayName(JavaTextStyle.SHORT, Locale.getDefault())
                                    .uppercase(),
                                widgetFont,
                                scale = 0.60f,
                                color = GlanceTheme.colors.onSurface
                            )
                            Spacer(GlanceModifier.height(2.dp))
                            WidgetText(
                                today.month
                                    .getDisplayName(JavaTextStyle.SHORT, Locale.getDefault())
                                    .uppercase(),
                                widgetFont,
                                scale = 0.52f,
                                color = GlanceTheme.colors.onSurfaceVariant
                            )
                        }

                        Spacer(GlanceModifier.width(7.dp))

                        SystemPill(strong = busyHours > 0) {
                            WidgetText(
                                "$freeHours H FREE",
                                widgetFont,
                                scale = 0.50f,
                                color = if (busyHours > 0) GlanceTheme.colors.onPrimaryContainer
                                else GlanceTheme.colors.onSecondaryContainer
                            )
                        }
                    }

                    Spacer(GlanceModifier.height(9.dp))

                    Row(verticalAlignment = Alignment.Vertical.CenterVertically) {
                        WidgetText(
                            "AM",
                            widgetFont,
                            scale = 0.45f,
                            color = GlanceTheme.colors.onSurfaceVariant
                        )
                        Spacer(GlanceModifier.width(4.dp))
                        DayTrack(items, today)
                        Spacer(GlanceModifier.width(4.dp))
                        WidgetText(
                            "PM",
                            widgetFont,
                            scale = 0.45f,
                            color = GlanceTheme.colors.onSurfaceVariant
                        )
                    }

                    Spacer(GlanceModifier.height(9.dp))

                    if (agenda.isEmpty()) {
                        SystemPill(strong = true) {
                            WidgetText(
                                "YOUR DAY IS WIDE OPEN",
                                widgetFont,
                                scale = 0.58f,
                                color = GlanceTheme.colors.onPrimaryContainer,
                                maxChars = 22
                            )
                        }
                        Spacer(GlanceModifier.height(7.dp))
                        SystemPill {
                            WidgetText(
                                "TAP TO PLAN",
                                widgetFont,
                                scale = 0.58f,
                                color = GlanceTheme.colors.onSecondaryContainer
                            )
                        }
                    } else {
                        agenda.forEachIndexed { index, item ->
                            Column {
                                SystemEventPill(
                                    item = item,
                                    strong = index == 0,
                                    fontChoice = widgetFont
                                )
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

object DaylineWidgetUpdater {
    suspend fun updateAll(context: Context) {
        DaylineCompactWidget().updateAll(context)
        DaylineSquareWidget().updateAll(context)
    }
}
