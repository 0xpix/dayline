package com.pix.dayline.widgets

import android.content.Context
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.widget.RemoteViews
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.ColorFilter
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.AndroidRemoteViews
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.appWidgetBackground
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
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
import com.pix.dayline.data.WidgetEmojiChoice
import com.pix.dayline.data.WidgetFontChoice
import com.pix.dayline.data.iconRes
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

private val PulseFreeText = ColorProvider(Color(0xFFF8F4F0))
private val PulseFreeMuted = ColorProvider(Color(0xFFD9D1CB))


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

private fun splitForSlide(text: String, maxChars: Int = 14): List<String> {
    val words = text.trim().split(Regex("\\s+")).filter { it.isNotBlank() }
    if (words.isEmpty()) return listOf("")

    val chunks = mutableListOf<String>()
    var current = ""

    fun flush() {
        if (current.isNotBlank()) {
            chunks += current
            current = ""
        }
    }

    for (word in words) {
        if (word.length > maxChars) {
            flush()
            word.chunked(maxChars).forEach { chunks += it }
            continue
        }

        val candidate = if (current.isBlank()) word else "$current $word"
        if (candidate.length <= maxChars) {
            current = candidate
        } else {
            flush()
            current = word
        }
    }
    flush()

    return chunks.take(4)
}

private fun tintMask(bitmap: Bitmap, color: Int): Bitmap {
    val out = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(out)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        colorFilter = PorterDuffColorFilter(color, PorterDuff.Mode.SRC_IN)
    }
    canvas.drawBitmap(bitmap, 0f, 0f, paint)
    return out
}

private fun animatedPillTextColor(context: Context): Int =
    if (
        context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK ==
        Configuration.UI_MODE_NIGHT_YES
    ) {
        0xFFF9F5F1.toInt()
    } else {
        0xFF2D2926.toInt()
    }

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
        fontChoice = fontChoice,
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

@Composable
private fun WidgetEmojiMark(
    emoji: WidgetEmojiChoice,
    size: Int
) {
    Box(
        modifier = GlanceModifier
            .size(size.dp)
            .background(GlanceTheme.colors.secondaryContainer)
            .cornerRadius((size / 2).dp),
        contentAlignment = Alignment.Center
    ) {
        Image(
            provider = ImageProvider(emoji.iconRes),
            contentDescription = emoji.name,
            modifier = GlanceModifier.size((size * 0.56f).dp),
            contentScale = ContentScale.Fit,
            colorFilter = ColorFilter.tint(GlanceTheme.colors.onSecondaryContainer)
        )
    }
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
private fun AnimatedEventText(
    fullText: String,
    fontChoice: WidgetFontChoice,
    width: Int
) {
    val context = LocalContext.current
    val chunks = splitForSlide(fullText, maxChars = 14)

    if (chunks.size <= 1) {
        WidgetText(
            text = fullText,
            fontChoice = fontChoice,
            scale = 0.72f,
            color = GlanceTheme.colors.onPrimaryContainer,
            maxChars = 18
        )
        return
    }

    val remoteViews = RemoteViews(context.packageName, R.layout.widget_event_flipper)
    val frameIds = intArrayOf(
        R.id.event_frame_1,
        R.id.event_frame_2,
        R.id.event_frame_3,
        R.id.event_frame_4
    )

    val color = animatedPillTextColor(context)

    frameIds.forEachIndexed { index, viewId ->
        val chunk = chunks.getOrElse(index) { chunks[index % chunks.size] }
        val rendered = DotMatrixRenderer.render(
            context = context,
            rawText = chunk,
            fontChoice = fontChoice,
            scale = 0.72f,
            maxChars = 14
        )
        remoteViews.setImageViewBitmap(viewId, tintMask(rendered.bitmap, color))
    }

    AndroidRemoteViews(
        remoteViews = remoteViews,
        modifier = GlanceModifier
            .width(width.dp)
            .height(18.dp)
    )
}

@Composable
private fun SystemEventPill(
    item: DaylineItem,
    strong: Boolean,
    fontChoice: WidgetFontChoice,
    autoSlide: Boolean,
    width: Int? = null
) {
    val isTask = item.kind == AgendaKind.TASK

    val base = GlanceModifier
        .background(
            when {
                isTask -> GlanceTheme.colors.secondaryContainer
                strong -> GlanceTheme.colors.primaryContainer
                else -> GlanceTheme.colors.secondaryContainer
            }
        )
        .cornerRadius(30.dp)
        .padding(horizontal = 8.dp, vertical = 4.dp)

    val modifier = if (width != null) base.width(width.dp) else base.fillMaxWidth()

    Box(
        modifier = modifier,
        contentAlignment = Alignment.CenterStart
    ) {
        val complete = "${if (item.kind == AgendaKind.TASK) "+" else ">"} ${item.title} ${itemLead(item)}"

        if (autoSlide && complete.length > 18 && strong && !isTask) {
            AnimatedEventText(
                fullText = complete,
                fontChoice = fontChoice,
                width = (width ?: 148) - 16
            )
        } else {
            WidgetText(
                text = "${if (item.kind == AgendaKind.TASK) "+" else ">"} ${compactTitle(item.title, 11)} ${itemLead(item)}",
                fontChoice = fontChoice,
                scale = 0.82f,
                color = when {
                    isTask -> GlanceTheme.colors.onSecondaryContainer
                    strong -> GlanceTheme.colors.onPrimaryContainer
                    else -> GlanceTheme.colors.onSecondaryContainer
                },
                maxChars = 20
            )
        }
    }
}


/**
 * Pulse 3×1
 * Transparent body + system-colored pills + Nothing-style dot-matrix typography.
 */
class DaylineCompactWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            val store = DaylineStore(context)
            val items = store.loadItems()
            val now = LocalDateTime.now()
            val today = now.toLocalDate()
            val next = nextOccurrence(items, now)
            val widgetFont = store.loadWidgetFontChoice()
            val widgetEmoji = store.loadWidgetEmojiChoice()
            val widgetAutoSlide = store.loadWidgetAutoSlide()

            TransparentPulseSurface {
                Row(
                    modifier = GlanceModifier.fillMaxSize(),
                    verticalAlignment = Alignment.Vertical.CenterVertically
                ) {
                    Column(
                        modifier = GlanceModifier.width(44.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        WidgetEmojiMark(widgetEmoji, 36)
                        Spacer(GlanceModifier.height(2.dp))
                        WidgetText(
                            "DAYLINE",
                            widgetFont,
                            scale = 0.48f,
                            color = PulseFreeText
                        )
                    }

                    Spacer(GlanceModifier.width(6.dp))

                    Box(
                        modifier = GlanceModifier
                            .width(1.dp)
                            .height(48.dp)
                            .background(PulseFreeMuted)
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
                                color = PulseFreeText
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
                                    autoSlide = widgetAutoSlide,
                                    width = 137
                                )
                                Spacer(GlanceModifier.width(5.dp))
                                Column {
                                    WidgetText(
                                        "NEXT",
                                        widgetFont,
                                        scale = 0.44f,
                                        color = PulseFreeMuted
                                    )
                                    Spacer(GlanceModifier.height(1.dp))
                                    WidgetText(
                                        "UP",
                                        widgetFont,
                                        scale = 0.44f,
                                        color = PulseFreeMuted
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
                                color = PulseFreeText
                            )
                            Spacer(GlanceModifier.width(4.dp))
                            DayTrack(items = items, date = today)
                            Spacer(GlanceModifier.width(4.dp))
                            WidgetText(
                                "PM",
                                widgetFont,
                                scale = 0.48f,
                                color = PulseFreeText
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
        provideContent {
            val store = DaylineStore(context)
            val items = store.loadItems()
            val today = LocalDate.now()
            val agenda = todayItems(items, today).take(2)
            val busyHours = (0..23).count { isHourBusy(items, today, it) }
            val freeHours = 24 - busyHours
            val widgetFont = store.loadWidgetFontChoice()
            val widgetAutoSlide = store.loadWidgetAutoSlide()

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
                                    fontChoice = widgetFont,
                                    autoSlide = widgetAutoSlide
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

class DaylineLockWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            val store = DaylineStore(context)
            val items = store.loadItems()
            val now = LocalDateTime.now()
            val next = nextOccurrence(items, now)
            val widgetFont = store.loadWidgetFontChoice()
            val widgetEmoji = store.loadWidgetEmojiChoice()

            GlanceTheme {
                Row(
                    modifier = GlanceModifier
                        .fillMaxSize()
                        .appWidgetBackground()
                        .clickable(actionStartActivity<MainActivity>())
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.Vertical.CenterVertically
                ) {
                    WidgetEmojiMark(widgetEmoji, 32)

                    Spacer(GlanceModifier.width(9.dp))

                    Column {
                        WidgetText(
                            text = now.dayOfWeek
                                .getDisplayName(JavaTextStyle.SHORT, Locale.getDefault())
                                .uppercase() +
                                " ${now.dayOfMonth} " +
                                now.month
                                    .getDisplayName(JavaTextStyle.SHORT, Locale.getDefault())
                                    .uppercase(),
                            fontChoice = widgetFont,
                            scale = 0.58f,
                            color = GlanceTheme.colors.onSurface,
                            maxChars = 16
                        )

                        Spacer(GlanceModifier.height(4.dp))

                        val text = if (next == null) {
                            "DAY CLEAR"
                        } else {
                            val start = next.item.startTime
                            val end = next.item.endTime
                            val active =
                                next.date == now.toLocalDate() &&
                                start != null &&
                                end != null &&
                                !now.toLocalTime().isBefore(start) &&
                                now.toLocalTime().isBefore(end)

                            if (active) {
                                "NOW  ${compactTitle(next.item.title, 12)}"
                            } else {
                                "NEXT ${compactTitle(next.item.title, 11)} ${itemLead(next.item)}"
                            }
                        }

                        WidgetText(
                            text = text,
                            fontChoice = widgetFont,
                            scale = 0.68f,
                            color = GlanceTheme.colors.onSurface,
                            maxChars = 22
                        )
                    }
                }
            }
        }
    }
}

class DaylineLockWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = DaylineLockWidget()
}

object DaylineWidgetUpdater {
    suspend fun updateAll(context: Context) {
        val manager = GlanceAppWidgetManager(context)

        val compact = DaylineCompactWidget()
        manager.getGlanceIds(DaylineCompactWidget::class.java).forEach { id ->
            compact.update(context, id)
        }

        val square = DaylineSquareWidget()
        manager.getGlanceIds(DaylineSquareWidget::class.java).forEach { id ->
            square.update(context, id)
        }

        val lock = DaylineLockWidget()
        manager.getGlanceIds(DaylineLockWidget::class.java).forEach { id ->
            lock.update(context, id)
        }
    }
}
