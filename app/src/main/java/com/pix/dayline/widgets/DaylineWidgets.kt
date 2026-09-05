package com.pix.dayline.widgets

import android.content.Context
import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
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

private fun isNight(context: Context): Boolean =
    context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK ==
        Configuration.UI_MODE_NIGHT_YES

private fun dotForeground(context: Context): Int =
    if (isNight(context)) 0xFFF5F0EC.toInt() else 0xFF302E2D.toInt()

private fun dotOnSystemPill(context: Context): Int =
    if (isNight(context)) 0xFFF9F4EE.toInt() else 0xFF2E2926.toInt()

@Composable
private fun DotText(
    text: String,
    scale: Float = 1f,
    onSystemPill: Boolean = false,
    maxChars: Int = 24
) {
    val context = LocalContext.current
    val rendered = DotMatrixRenderer.render(
        context = context,
        rawText = text,
        color = if (onSystemPill) dotOnSystemPill(context) else dotForeground(context),
        scale = scale,
        maxChars = maxChars
    )

    Image(
        provider = ImageProvider(rendered.bitmap),
        contentDescription = text,
        modifier = GlanceModifier
            .width(rendered.widthDp.dp)
            .height(rendered.heightDp.dp),
        contentScale = ContentScale.Fit
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

@Composable
private fun OrbitMark(size: Int) {
    Image(
        provider = ImageProvider(R.drawable.ic_widget_dayline_orbit),
        contentDescription = null,
        modifier = GlanceModifier.size(size.dp),
        contentScale = ContentScale.Fit
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
        DotText(
            text = "${if (item.kind == AgendaKind.TASK) "+" else ">"} ${compactTitle(item.title, 11)} ${itemLead(item)}",
            scale = 0.82f,
            onSystemPill = true,
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
                        OrbitMark(34)
                        Spacer(GlanceModifier.height(2.dp))
                        DotText("DAYLINE", scale = 0.48f)
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
                            DotText(
                                today.dayOfWeek
                                    .getDisplayName(JavaTextStyle.SHORT, Locale.getDefault())
                                    .uppercase(),
                                scale = 0.72f
                            )

                            Spacer(GlanceModifier.width(5.dp))

                            SystemPill(strong = true, horizontalPadding = 6, verticalPadding = 2) {
                                DotText(
                                    today.dayOfMonth.toString(),
                                    scale = 0.64f,
                                    onSystemPill = true
                                )
                            }

                            Spacer(GlanceModifier.width(4.dp))

                            SystemPill(horizontalPadding = 6, verticalPadding = 2) {
                                DotText(
                                    today.month
                                        .getDisplayName(JavaTextStyle.SHORT, Locale.getDefault())
                                        .uppercase(),
                                    scale = 0.58f,
                                    onSystemPill = true
                                )
                            }

                            Spacer(GlanceModifier.width(4.dp))

                            SystemPill(strong = true, horizontalPadding = 6, verticalPadding = 2) {
                                DotText(
                                    nextStatus(next, now),
                                    scale = 0.56f,
                                    onSystemPill = true
                                )
                            }
                        }

                        Spacer(GlanceModifier.height(4.dp))

                        Row(verticalAlignment = Alignment.Vertical.CenterVertically) {
                            if (next == null) {
                                SystemPill(strong = true, horizontalPadding = 8, verticalPadding = 4) {
                                    DotText(
                                        "YOUR DAY IS CLEAR",
                                        scale = 0.69f,
                                        onSystemPill = true,
                                        maxChars = 20
                                    )
                                }
                            } else {
                                SystemEventPill(
                                    item = next.item,
                                    strong = true,
                                    width = 137
                                )
                                Spacer(GlanceModifier.width(5.dp))
                                Column {
                                    DotText("NEXT", scale = 0.44f)
                                    Spacer(GlanceModifier.height(1.dp))
                                    DotText("UP", scale = 0.44f)
                                }
                            }
                        }

                        Spacer(GlanceModifier.height(4.dp))

                        Row(verticalAlignment = Alignment.Vertical.CenterVertically) {
                            DotText("AM", scale = 0.48f)
                            Spacer(GlanceModifier.width(4.dp))
                            DayTrack(items = items, date = today)
                            Spacer(GlanceModifier.width(4.dp))
                            DotText("PM", scale = 0.48f)
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

        provideContent {
            SystemSquareSurface {
                Column(GlanceModifier.fillMaxSize()) {
                    Row(
                        modifier = GlanceModifier.fillMaxWidth(),
                        verticalAlignment = Alignment.Vertical.CenterVertically
                    ) {
                        DotText(today.dayOfMonth.toString(), scale = 1.15f)

                        Spacer(GlanceModifier.width(8.dp))

                        Column {
                            DotText(
                                today.dayOfWeek
                                    .getDisplayName(JavaTextStyle.SHORT, Locale.getDefault())
                                    .uppercase(),
                                scale = 0.60f
                            )
                            Spacer(GlanceModifier.height(2.dp))
                            DotText(
                                today.month
                                    .getDisplayName(JavaTextStyle.SHORT, Locale.getDefault())
                                    .uppercase(),
                                scale = 0.52f
                            )
                        }

                        Spacer(GlanceModifier.width(7.dp))

                        SystemPill(strong = busyHours > 0) {
                            DotText(
                                "$freeHours H FREE",
                                scale = 0.50f,
                                onSystemPill = true
                            )
                        }
                    }

                    Spacer(GlanceModifier.height(9.dp))

                    Row(verticalAlignment = Alignment.Vertical.CenterVertically) {
                        DotText("AM", scale = 0.45f)
                        Spacer(GlanceModifier.width(4.dp))
                        DayTrack(items, today)
                        Spacer(GlanceModifier.width(4.dp))
                        DotText("PM", scale = 0.45f)
                    }

                    Spacer(GlanceModifier.height(9.dp))

                    if (agenda.isEmpty()) {
                        SystemPill(strong = true) {
                            DotText(
                                "YOUR DAY IS WIDE OPEN",
                                scale = 0.58f,
                                onSystemPill = true,
                                maxChars = 22
                            )
                        }
                        Spacer(GlanceModifier.height(7.dp))
                        SystemPill {
                            DotText(
                                "TAP TO PLAN",
                                scale = 0.58f,
                                onSystemPill = true
                            )
                        }
                    } else {
                        agenda.forEachIndexed { index, item ->
                            Column {
                                SystemEventPill(item = item, strong = index == 0)
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
