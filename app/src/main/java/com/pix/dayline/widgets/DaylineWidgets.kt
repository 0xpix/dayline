package com.pix.dayline.widgets

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
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
import androidx.glance.text.FontFamily
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
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

// Deliberately independent from Material You.
// The approved concept is a cool frost / pale-blue widget family.
private val FrostWhite = ColorProvider(Color(0xFFF9FBFF))
private val FrostInk = ColorProvider(Color(0xFF92A1BB))
private val FrostMuted = ColorProvider(Color(0xFFDCE4EF))
private val FrostFaint = ColorProvider(Color(0xFFC8D2E1))
private val FrostGlass = ColorProvider(Color(0xDDF5F8FC))
private val FrostSoftGlass = ColorProvider(Color(0x55F6F9FD))
private val FrostTrack = ColorProvider(Color(0x99EDF2F8))
private val FrostApproxBackground = ColorProvider(Color(0xFFA8B6CC))

private val Mono = FontFamily.Monospace

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

    val start = next.item.startTime ?: return if (next.date == now.toLocalDate()) "TODAY" else
        next.date.dayOfWeek.getDisplayName(JavaTextStyle.SHORT, Locale.getDefault()).uppercase()

    val startDateTime = LocalDateTime.of(next.date, start)
    val minutes = Duration.between(now, startDateTime).toMinutes()

    return when {
        minutes <= 0 -> "NOW"
        minutes < 60 -> "${minutes}M"
        minutes < 24 * 60 -> "${minutes / 60}H"
        else -> next.date.dayOfWeek.getDisplayName(JavaTextStyle.SHORT, Locale.getDefault()).uppercase()
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
private fun FrostSurface(content: @Composable () -> Unit) {
    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .appWidgetBackground()
            .background(
                imageProvider = ImageProvider(R.drawable.widget_frost_gradient),
                contentScale = ContentScale.FillBounds
            )
            .cornerRadius(android.R.dimen.system_app_widget_background_radius)
            .clickable(actionStartActivity<MainActivity>())
            .padding(8.dp)
    ) {
        content()
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
private fun MonoText(
    text: String,
    size: Int,
    color: ColorProvider = FrostWhite,
    bold: Boolean = false
) {
    Text(
        text,
        style = TextStyle(
            color = color,
            fontSize = size.sp,
            fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal,
            fontFamily = Mono
        )
    )
}

@Composable
private fun FrostPill(
    text: String,
    strong: Boolean = false,
    compact: Boolean = false
) {
    Box(
        modifier = GlanceModifier
            .background(if (strong) FrostGlass else FrostSoftGlass)
            .cornerRadius(30.dp)
            .padding(
                horizontal = if (compact) 7.dp else 9.dp,
                vertical = if (compact) 2.dp else 4.dp
            ),
        contentAlignment = Alignment.Center
    ) {
        MonoText(
            text = text,
            size = if (compact) 9 else 11,
            color = if (strong) FrostInk else FrostWhite,
            bold = strong
        )
    }
}

@Composable
private fun TrackDot(
    current: Boolean,
    busy: Boolean
) {
    if (current) {
        Box(
            modifier = GlanceModifier
                .size(11.dp)
                .background(FrostWhite)
                .cornerRadius(20.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = GlanceModifier
                    .size(6.dp)
                    .background(FrostApproxBackground)
                    .cornerRadius(20.dp)
            ) { }
        }
    } else {
        Box(
            modifier = GlanceModifier
                .size(11.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = GlanceModifier
                    .size(if (busy) 7.dp else 5.dp)
                    .background(if (busy) FrostWhite else FrostTrack)
                    .cornerRadius(20.dp)
            ) { }
        }
    }
}

@Composable
private fun DayTrack(
    items: List<DaylineItem>,
    date: LocalDate,
    segments: Int
) {
    val now = LocalDateTime.now()

    Row(verticalAlignment = Alignment.Vertical.CenterVertically) {
        repeat(segments.coerceAtMost(8)) { index ->
            val startHour = index * 24 / segments
            val endHour = (index + 1) * 24 / segments

            val current =
                date == now.toLocalDate() &&
                now.hour in startHour until endHour.coerceAtMost(24)

            var busy = false
            for (hour in startHour until endHour.coerceAtMost(24)) {
                if (isHourBusy(items, date, hour)) {
                    busy = true
                    break
                }
            }

            TrackDot(current = current, busy = busy)
        }
    }
}

@Composable
private fun EventPill(
    item: DaylineItem,
    width: Int? = null,
    strong: Boolean = true
) {
    val base = GlanceModifier
        .background(if (strong) FrostGlass else FrostSoftGlass)
        .cornerRadius(30.dp)
        .padding(horizontal = 8.dp, vertical = 4.dp)

    val modifier = if (width != null) {
        base.width(width.dp)
    } else {
        base.fillMaxWidth()
    }

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.Vertical.CenterVertically
    ) {
        MonoText(
            if (item.kind == AgendaKind.TASK) "□" else "↗",
            size = 11,
            color = if (strong) FrostInk else FrostWhite,
            bold = true
        )
        Spacer(GlanceModifier.width(5.dp))
        MonoText(
            "${compactTitle(item.title, 12)} ${itemLead(item)}",
            size = 10,
            color = if (strong) FrostInk else FrostWhite,
            bold = strong
        )
    }
}

/**
 * Pulse 3×1
 *
 * This is intentionally composed like the approved concept:
 * left orbit mark / divider / TUE + date pills / next-up pill / AM→PM dot track.
 */
class DaylineCompactWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val items = DaylineStore(context).loadItems()
        val now = LocalDateTime.now()
        val today = now.toLocalDate()
        val next = nextOccurrence(items, now)

        provideContent {
            FrostSurface {
                Row(
                    modifier = GlanceModifier.fillMaxSize(),
                    verticalAlignment = Alignment.Vertical.CenterVertically
                ) {
                    Column(
                        modifier = GlanceModifier.width(48.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        OrbitMark(38)
                        MonoText("DAYLINE", 6, FrostWhite, true)
                    }

                    Spacer(GlanceModifier.width(6.dp))

                    Box(
                        modifier = GlanceModifier
                            .width(1.dp)
                            .height(50.dp)
                            .background(FrostMuted)
                    ) { }

                    Spacer(GlanceModifier.width(8.dp))

                    Column {
                        // TUE  5  SEPT  [time to next]
                        Row(verticalAlignment = Alignment.Vertical.CenterVertically) {
                            MonoText(
                                today.dayOfWeek
                                    .getDisplayName(JavaTextStyle.SHORT, Locale.getDefault())
                                    .uppercase(),
                                10,
                                FrostWhite,
                                true
                            )

                            Spacer(GlanceModifier.width(5.dp))
                            FrostPill(today.dayOfMonth.toString(), strong = true, compact = true)
                            Spacer(GlanceModifier.width(4.dp))
                            FrostPill(
                                today.month
                                    .getDisplayName(JavaTextStyle.SHORT, Locale.getDefault())
                                    .uppercase(),
                                compact = true
                            )
                            Spacer(GlanceModifier.width(4.dp))
                            FrostPill(nextStatus(next, now), strong = true, compact = true)
                        }

                        Spacer(GlanceModifier.height(4.dp))

                        Row(verticalAlignment = Alignment.Vertical.CenterVertically) {
                            if (next == null) {
                                FrostPill("↗  YOUR DAY IS CLEAR", strong = true)
                            } else {
                                EventPill(next.item, width = 132, strong = true)
                                Spacer(GlanceModifier.width(6.dp))
                                Column {
                                    MonoText("NEXT", 6, FrostMuted, true)
                                    MonoText("UP", 6, FrostMuted, true)
                                }
                            }
                        }

                        Spacer(GlanceModifier.height(4.dp))

                        Row(verticalAlignment = Alignment.Vertical.CenterVertically) {
                            MonoText("AM", 6, FrostWhite, true)
                            Spacer(GlanceModifier.width(4.dp))
                            DayTrack(items = items, date = today, segments = 8)
                            Spacer(GlanceModifier.width(4.dp))
                            MonoText("PM", 6, FrostWhite, true)
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
 * Orbit 2×2 — same Frost visual system, slightly richer.
 */
class DaylineSquareWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val items = DaylineStore(context).loadItems()
        val today = LocalDate.now()
        val agenda = todayItems(items, today).take(2)

        provideContent {
            FrostSurface {
                Column(GlanceModifier.fillMaxSize()) {
                    Row(verticalAlignment = Alignment.Vertical.CenterVertically) {
                        OrbitMark(34)
                        Spacer(GlanceModifier.width(7.dp))

                        Column {
                            Row(verticalAlignment = Alignment.Vertical.CenterVertically) {
                                MonoText(
                                    today.dayOfWeek
                                        .getDisplayName(JavaTextStyle.SHORT, Locale.getDefault())
                                        .uppercase(),
                                    10,
                                    FrostWhite,
                                    true
                                )
                                Spacer(GlanceModifier.width(5.dp))
                                FrostPill(today.dayOfMonth.toString(), true, true)
                                Spacer(GlanceModifier.width(4.dp))
                                FrostPill(
                                    today.month
                                        .getDisplayName(JavaTextStyle.SHORT, Locale.getDefault())
                                        .uppercase(),
                                    compact = true
                                )
                            }
                            MonoText("DAYLINE", 6, FrostMuted, true)
                        }
                    }

                    Spacer(GlanceModifier.height(8.dp))

                    Row(verticalAlignment = Alignment.Vertical.CenterVertically) {
                        MonoText("AM", 6, FrostWhite, true)
                        Spacer(GlanceModifier.width(4.dp))
                        DayTrack(items, today, 8)
                        Spacer(GlanceModifier.width(4.dp))
                        MonoText("PM", 6, FrostWhite, true)
                    }

                    Spacer(GlanceModifier.height(8.dp))

                    if (agenda.isEmpty()) {
                        FrostPill("A BRIGHTER DAY AHEAD", strong = true)
                    } else {
                        agenda.forEachIndexed { index, item ->
                            Column {
                                EventPill(item, strong = index == 0)
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

class DaylineSquareWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = DaylineSquareWidget()
}

/**
 * Board 3×2 — wider extension of the same approved strip.
 */
class DaylineAgendaWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val items = DaylineStore(context).loadItems()
        val today = LocalDate.now()
        val agenda = todayItems(items, today).take(3)

        provideContent {
            FrostSurface {
                Row(
                    modifier = GlanceModifier.fillMaxSize(),
                    verticalAlignment = Alignment.Vertical.Top
                ) {
                    Column(
                        modifier = GlanceModifier.width(50.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        OrbitMark(40)
                        MonoText("DAYLINE", 6, FrostWhite, true)
                    }

                    Spacer(GlanceModifier.width(8.dp))

                    Box(
                        modifier = GlanceModifier
                            .width(1.dp)
                            .height(94.dp)
                            .background(FrostMuted)
                    ) { }

                    Spacer(GlanceModifier.width(9.dp))

                    Column {
                        Row(verticalAlignment = Alignment.Vertical.CenterVertically) {
                            MonoText(
                                today.dayOfWeek
                                    .getDisplayName(JavaTextStyle.SHORT, Locale.getDefault())
                                    .uppercase(),
                                11,
                                FrostWhite,
                                true
                            )
                            Spacer(GlanceModifier.width(5.dp))
                            FrostPill(today.dayOfMonth.toString(), true, true)
                            Spacer(GlanceModifier.width(4.dp))
                            FrostPill(
                                today.month
                                    .getDisplayName(JavaTextStyle.SHORT, Locale.getDefault())
                                    .uppercase(),
                                compact = true
                            )
                        }

                        Spacer(GlanceModifier.height(6.dp))

                        Row(verticalAlignment = Alignment.Vertical.CenterVertically) {
                            MonoText("AM", 6, FrostWhite, true)
                            Spacer(GlanceModifier.width(4.dp))
                            DayTrack(items, today, 8)
                            Spacer(GlanceModifier.width(4.dp))
                            MonoText("PM", 6, FrostWhite, true)
                        }

                        Spacer(GlanceModifier.height(7.dp))

                        if (agenda.isEmpty()) {
                            FrostPill("YOUR DAY IS WIDE OPEN", strong = true)
                        } else {
                            agenda.forEachIndexed { index, item ->
                                Column {
                                    EventPill(item, width = 164, strong = index == 0)
                                    if (index != agenda.lastIndex) {
                                        Spacer(GlanceModifier.height(4.dp))
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
