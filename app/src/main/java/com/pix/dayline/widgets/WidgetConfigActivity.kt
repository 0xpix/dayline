package com.pix.dayline.widgets

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import com.pix.dayline.data.AndroidCalendarSync
import com.pix.dayline.data.DaylineStore
import com.pix.dayline.data.WidgetEmojiChoice
import com.pix.dayline.data.WidgetFontChoice
import com.pix.dayline.data.label
import com.pix.dayline.data.symbol
import com.pix.dayline.model.AgendaKind
import com.pix.dayline.model.DaylineItem
import com.pix.dayline.model.DaylineSpace
import com.pix.dayline.model.DeviceCalendar
import com.pix.dayline.model.FocusCycle
import com.pix.dayline.model.WidgetBackgroundMode
import com.pix.dayline.model.WidgetContentMode
import com.pix.dayline.model.WidgetInstancePrefs
import com.pix.dayline.model.occursOn
import com.pix.dayline.ui.theme.DaylineTheme
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

class WidgetConfigActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val appWidgetId = intent?.extras?.getInt(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID
        ) ?: AppWidgetManager.INVALID_APPWIDGET_ID

        if (
            appWidgetId ==
            AppWidgetManager.INVALID_APPWIDGET_ID
        ) {
            finish()
            return
        }

        setResult(Activity.RESULT_CANCELED)

        val store = DaylineStore(applicationContext)

        val previewItems = AndroidCalendarSync.mergedItems(
            context = applicationContext,
            local = store.loadItems(),
            enabled = store.loadCalendarSyncEnabled(),
            preferences = store.loadCalendarPreferences(),
            from = LocalDate.now().minusDays(1),
            to = LocalDate.now().plusDays(31)
        )

        setContent {
            DaylineTheme(
                darkTheme =
                    androidx.compose.foundation.isSystemInDarkTheme(),
                fontChoice = store.loadFontChoice(),
                dynamicColor = true
            ) {
                WidgetConfigScreen(
                    initial =
                        store.loadWidgetInstancePrefs(appWidgetId),
                    spaces = store.loadSpaces(),
                    calendars =
                        AndroidCalendarSync.listCalendars(
                            applicationContext
                        ),
                    items = previewItems,
                    globalEmoji = store.loadWidgetEmojiChoice(),
                    globalFont = store.loadWidgetFontChoice(),
                    onSave = { value ->
                        store.saveWidgetInstancePrefs(
                            value.copy(
                                backgroundMode =
                                    WidgetBackgroundMode.SYSTEM
                            )
                        )

                        lifecycleScope.launch {
                            DaylineWidgetUpdater.updateAll(
                                applicationContext
                            )
                        }

                        setResult(
                            Activity.RESULT_OK,
                            Intent().putExtra(
                                AppWidgetManager.EXTRA_APPWIDGET_ID,
                                appWidgetId
                            )
                        )
                        finish()
                    },
                    onCancel = { finish() }
                )
            }
        }
    }
}

@Composable
private fun WidgetConfigScreen(
    initial: WidgetInstancePrefs,
    spaces: List<DaylineSpace>,
    calendars: List<DeviceCalendar>,
    items: List<DaylineItem>,
    globalEmoji: WidgetEmojiChoice,
    globalFont: WidgetFontChoice,
    onSave: (WidgetInstancePrefs) -> Unit,
    onCancel: () -> Unit
) {
    var value by remember { mutableStateOf(initial) }

    val emoji = resolvePreviewEmoji(
        value = value,
        fallback = globalEmoji
    )
    val font = resolvePreviewFont(
        value = value,
        fallback = globalFont
    )

    Box(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(
                top =
                    WindowInsets.statusBars
                        .asPaddingValues()
                        .calculateTopPadding(),
                bottom =
                    WindowInsets.navigationBars
                        .asPaddingValues()
                        .calculateBottomPadding()
            )
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(
                    horizontal = 28.dp,
                    vertical = 42.dp
                )
        ) {
            Text(
                "Widget",
                style = MaterialTheme.typography.displayMedium
            )

            Spacer(Modifier.height(10.dp))

            Text(
                "Preview",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(10.dp))

            WidgetPreview(
                value = value,
                emoji = emoji,
                font = font,
                items = items
            )

            Spacer(Modifier.height(28.dp))

            ConfigCycleRow(
                "Space",
                spaces.firstOrNull {
                    it.id == value.spaceId
                }?.name ?: "All"
            ) {
                value = value.copy(
                    spaceId = cycleSpace(
                        value.spaceId,
                        spaces
                    )
                )
            }

            ConfigCycleRow(
                "Calendar",
                calendars.firstOrNull {
                    it.id == value.calendarId
                }?.name ?: "All"
            ) {
                value = value.copy(
                    calendarId = cycleCalendar(
                        value.calendarId,
                        calendars
                    )
                )
            }

            ConfigCycleRow(
                "Emoji",
                value.emoji
                    ?.let {
                        runCatching {
                            WidgetEmojiChoice.valueOf(it).label
                        }.getOrNull()
                    }
                    ?: "Global · ${globalEmoji.label}"
            ) {
                val choices =
                    WidgetEmojiChoice.entries.map { it.name }

                value = value.copy(
                    emoji = cycleString(
                        value.emoji,
                        choices
                    )
                )
            }

            ConfigCycleRow(
                "Font",
                value.font
                    ?: "Global · ${globalFont.name.lowercase()}"
            ) {
                val choices =
                    WidgetFontChoice.entries.map { it.name }

                value = value.copy(
                    font = cycleString(
                        value.font,
                        choices
                    )
                )
            }

            ConfigCycleRow(
                "Content",
                value.contentMode.name.lowercase()
            ) {
                val all = WidgetContentMode.entries
                value = value.copy(
                    contentMode = all[
                        (
                            all.indexOf(value.contentMode) + 1
                        ) % all.size
                    ]
                )
            }

            ConfigToggleRow(
                "Events",
                value.showEvents
            ) {
                value = value.copy(showEvents = it)
            }

            ConfigToggleRow(
                "Tasks",
                value.showTasks
            ) {
                value = value.copy(showTasks = it)
            }

            ConfigToggleRow(
                "Focus state",
                value.showFocusState
            ) {
                value = value.copy(showFocusState = it)
            }

            Spacer(Modifier.height(34.dp))

            Row(
                horizontalArrangement =
                    Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    modifier = Modifier.clickable {
                        onSave(value)
                    },
                    shape = CircleShape,
                    color =
                        MaterialTheme.colorScheme.onBackground,
                    contentColor =
                        MaterialTheme.colorScheme.background
                ) {
                    Text(
                        "Save",
                        modifier = Modifier.padding(
                            horizontal = 22.dp,
                            vertical = 11.dp
                        )
                    )
                }

                Text(
                    "Cancel",
                    modifier = Modifier
                        .clickable { onCancel() }
                        .padding(11.dp),
                    color =
                        MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun WidgetPreview(
    value: WidgetInstancePrefs,
    emoji: WidgetEmojiChoice,
    font: WidgetFontChoice,
    items: List<DaylineItem>
) {
    val context = LocalContext.current
    val now = LocalDateTime.now()
    val filtered = remember(
        value,
        items,
        now.toLocalDate()
    ) {
        items.filter { item ->
            val kindVisible = when (item.kind) {
                AgendaKind.EVENT -> value.showEvents
                AgendaKind.TASK -> value.showTasks
            }

            val spaceVisible =
                value.spaceId == null ||
                    item.spaceId == value.spaceId

            val calendarVisible =
                value.calendarId == null ||
                    item.calendarId == value.calendarId

            kindVisible &&
                spaceVisible &&
                calendarVisible
        }
    }

    val current = filtered.firstOrNull { item ->
        val start = item.startTime
        val end = item.endTime

        item.occursOn(now.toLocalDate()) &&
            start != null &&
            end != null &&
            !now.toLocalTime().isBefore(start) &&
            now.toLocalTime().isBefore(end)
    }

    val next = previewNextOccurrence(
        filtered,
        now
    )

    val previewItem = when (value.contentMode) {
        WidgetContentMode.CURRENT -> current
        WidgetContentMode.NEXT -> next
        WidgetContentMode.SMART -> current ?: next
    }

    val status = when {
        previewItem == null ->
            "YOUR DAY IS CLEAR"

        current?.id == previewItem.id &&
            previewItem.focusCycle != FocusCycle.OFF &&
            value.showFocusState ->
            "FOCUS > ${previewItem.title}"

        current?.id == previewItem.id ->
            "NOW > ${previewItem.title}"

        previewItem.kind == AgendaKind.TASK ->
            "TODO > ${previewItem.title}"

        else -> {
            val time =
                previewItem.startTime?.format(
                    DateTimeFormatter.ofPattern("HH:mm")
                ) ?: "ALL"
            "$time > ${previewItem.title}"
        }
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(116.dp),
        shape = RoundedCornerShape(26.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = 0.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    start = 14.dp,
                    end = 10.dp,
                    top = 12.dp,
                    bottom = 12.dp
                ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val emojiBitmap =
                remember(emoji, context) {
                    NotoEmojiRenderer.render(
                        context = context,
                        glyph = emoji.symbol,
                        sizeDp = 44
                    )
                }

            Image(
                bitmap = emojiBitmap.asImageBitmap(),
                contentDescription = emoji.label,
                modifier = Modifier.size(44.dp),
                colorFilter = ColorFilter.tint(
                    MaterialTheme.colorScheme.onSurface
                )
            )

            Spacer(Modifier.width(12.dp))

            Box(
                Modifier
                    .width(1.dp)
                    .height(58.dp)
                    .background(
                        MaterialTheme.colorScheme
                            .onSurfaceVariant
                            .copy(alpha = 0.24f)
                    )
            )

            Spacer(Modifier.width(12.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                val dateText = buildString {
                    append(
                        now.dayOfWeek.getDisplayName(
                            TextStyle.SHORT,
                            Locale.getDefault()
                        ).uppercase()
                    )
                    append("  ")
                    append(now.dayOfMonth)
                    append("  ")
                    append(
                        now.month.getDisplayName(
                            TextStyle.SHORT,
                            Locale.getDefault()
                        ).uppercase()
                    )
                }

                PreviewDotText(
                    text = dateText,
                    font = font,
                    scale = 0.72f
                )

                Spacer(Modifier.height(12.dp))

                PreviewDotText(
                    text = status,
                    font = font,
                    scale = 0.78f,
                    maxChars = 22
                )

                Spacer(Modifier.height(10.dp))

                Text(
                    when (value.contentMode) {
                        WidgetContentMode.SMART -> "SMART"
                        WidgetContentMode.CURRENT -> "CURRENT"
                        WidgetContentMode.NEXT -> "NEXT"
                    },
                    style =
                        MaterialTheme.typography.labelMedium,
                    color =
                        MaterialTheme.colorScheme
                            .onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun PreviewDotText(
    text: String,
    font: WidgetFontChoice,
    scale: Float,
    maxChars: Int = 24
) {
    val context = LocalContext.current
    val rendered = remember(
        text,
        font,
        scale,
        maxChars,
        context
    ) {
        DotMatrixRenderer.render(
            context = context,
            rawText = text,
            fontChoice = font,
            scale = scale,
            maxChars = maxChars
        )
    }

    Image(
        bitmap = rendered.bitmap.asImageBitmap(),
        contentDescription = text,
        modifier = Modifier
            .width(rendered.widthDp.dp)
            .height(rendered.heightDp.dp),
        colorFilter = ColorFilter.tint(
            MaterialTheme.colorScheme.onSurface
        )
    )
}

private fun previewNextOccurrence(
    items: List<DaylineItem>,
    now: LocalDateTime
): DaylineItem? {
    for (offset in 0L..30L) {
        val date = now.toLocalDate().plusDays(offset)

        val candidate = items
            .filter { it.occursOn(date) }
            .sortedWith(
                compareBy<DaylineItem> {
                    it.startTime == null
                }.thenBy { it.startTime }
            )
            .firstOrNull { item ->
                if (date.isAfter(now.toLocalDate())) {
                    true
                } else {
                    val start = item.startTime
                    val end = item.endTime

                    when {
                        start == null -> true
                        end != null &&
                            end.isAfter(now.toLocalTime()) -> true
                        else ->
                            !start.isBefore(now.toLocalTime())
                    }
                }
            }

        if (candidate != null) {
            return candidate
        }
    }

    return null
}

private fun resolvePreviewEmoji(
    value: WidgetInstancePrefs,
    fallback: WidgetEmojiChoice
): WidgetEmojiChoice =
    runCatching {
        WidgetEmojiChoice.valueOf(
            value.emoji.orEmpty()
        )
    }.getOrDefault(fallback)

private fun resolvePreviewFont(
    value: WidgetInstancePrefs,
    fallback: WidgetFontChoice
): WidgetFontChoice =
    runCatching {
        WidgetFontChoice.valueOf(
            value.font.orEmpty()
        )
    }.getOrDefault(fallback)

@Composable
private fun ConfigCycleRow(
    label: String,
    value: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f)
        )

        Text(
            value,
            style = MaterialTheme.typography.bodyMedium,
            color =
                MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(Modifier.width(8.dp))

        Text(
            "›",
            color =
                MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ConfigToggleRow(
    label: String,
    checked: Boolean,
    onChange: (Boolean) -> Unit
) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f)
        )

        Switch(
            checked = checked,
            onCheckedChange = onChange
        )
    }
}

private fun cycleSpace(
    current: String?,
    spaces: List<DaylineSpace>
): String? {
    if (spaces.isEmpty()) return null
    if (current == null) return spaces.first().id

    val index =
        spaces.indexOfFirst { it.id == current }

    return if (
        index < 0 ||
        index == spaces.lastIndex
    ) {
        null
    } else {
        spaces[index + 1].id
    }
}

private fun cycleCalendar(
    current: Long?,
    calendars: List<DeviceCalendar>
): Long? {
    if (calendars.isEmpty()) return null
    if (current == null) return calendars.first().id

    val index =
        calendars.indexOfFirst { it.id == current }

    return if (
        index < 0 ||
        index == calendars.lastIndex
    ) {
        null
    } else {
        calendars[index + 1].id
    }
}

private fun cycleString(
    current: String?,
    choices: List<String>
): String? {
    if (choices.isEmpty()) return null
    if (current == null) return choices.first()

    val index = choices.indexOf(current)

    return if (
        index < 0 ||
        index == choices.lastIndex
    ) {
        null
    } else {
        choices[index + 1]
    }
}
