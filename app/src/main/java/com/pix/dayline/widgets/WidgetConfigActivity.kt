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
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.emoji2.emojipicker.EmojiPickerView
import androidx.lifecycle.lifecycleScope
import com.pix.dayline.R
import com.pix.dayline.data.AndroidCalendarSync
import com.pix.dayline.data.DaylineStore
import com.pix.dayline.data.WidgetEmojiChoice
import com.pix.dayline.data.WidgetFontChoice
import com.pix.dayline.data.label
import com.pix.dayline.data.symbol
import com.pix.dayline.model.*
import com.pix.dayline.ui.theme.DaylineTheme
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

private enum class WidgetConfigSheet {
    SPACE,
    CALENDAR,
    EMOJI,
    FONT,
    CONTENT
}

class WidgetConfigActivity : ComponentActivity() {
    override fun onCreate(
        savedInstanceState: Bundle?
    ) {
        super.onCreate(savedInstanceState)

        val appWidgetId =
            intent?.extras?.getInt(
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

        val store =
            DaylineStore(applicationContext)

        val previewItems =
            AndroidCalendarSync.mergedItems(
                context = applicationContext,
                local = store.loadItems(),
                enabled =
                    store.loadCalendarSyncEnabled(),
                preferences =
                    store.loadCalendarPreferences(),
                from =
                    LocalDate.now().minusDays(1),
                to =
                    LocalDate.now().plusDays(31)
            )

        setContent {
            DaylineTheme(
                darkTheme =
                    androidx.compose.foundation
                        .isSystemInDarkTheme(),
                fontChoice =
                    store.loadFontChoice(),
                dynamicColor = true
            ) {
                WidgetConfigScreen(
                    initial =
                        store.loadWidgetInstancePrefs(
                            appWidgetId
                        ),
                    spaces = store.loadSpaces(),
                    calendars =
                        AndroidCalendarSync
                            .listCalendars(
                                applicationContext
                            ),
                    items = previewItems,
                    defaultEmoji =
                        store.loadWidgetEmojiChoice()
                            .symbol,
                    defaultFont =
                        store.loadWidgetFontChoice(),
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
                                AppWidgetManager
                                    .EXTRA_APPWIDGET_ID,
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WidgetConfigScreen(
    initial: WidgetInstancePrefs,
    spaces: List<DaylineSpace>,
    calendars: List<DeviceCalendar>,
    items: List<DaylineItem>,
    defaultEmoji: String,
    defaultFont: WidgetFontChoice,
    onSave: (WidgetInstancePrefs) -> Unit,
    onCancel: () -> Unit
) {
    var value by remember {
        mutableStateOf(initial)
    }

    var openSheet by remember {
        mutableStateOf<WidgetConfigSheet?>(null)
    }

    val emoji =
        resolveEmojiGlyph(
            value.emoji,
            defaultEmoji
        )

    val font =
        resolvePreviewFont(
            value = value,
            fallback = defaultFont
        )

    Box(
        Modifier
            .fillMaxSize()
            .background(
                MaterialTheme.colorScheme.background
            )
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
                .verticalScroll(
                    rememberScrollState()
                )
                .padding(
                    horizontal = 28.dp,
                    vertical = 42.dp
                )
        ) {
            Text(
                "Widget",
                style =
                    MaterialTheme.typography.displayMedium
            )

            Spacer(Modifier.height(10.dp))

            Text(
                "PREVIEW",
                style =
                    MaterialTheme.typography.labelMedium,
                color =
                    MaterialTheme.colorScheme
                        .onSurfaceVariant
            )

            Spacer(Modifier.height(10.dp))

            WidgetPreview(
                value = value,
                emoji = emoji,
                font = font,
                items = items
            )

            Spacer(Modifier.height(28.dp))

            ConfigSelectorRow(
                "Space",
                spaces.firstOrNull {
                    it.id == value.spaceId
                }?.name ?: "All"
            ) {
                openSheet =
                    WidgetConfigSheet.SPACE
            }

            ConfigSelectorRow(
                "Calendar",
                calendars.firstOrNull {
                    it.id == value.calendarId
                }?.name ?: "All"
            ) {
                openSheet =
                    WidgetConfigSheet.CALENDAR
            }

            ConfigSelectorRow(
                "Emoji",
                emoji
            ) {
                openSheet =
                    WidgetConfigSheet.EMOJI
            }

            ConfigSelectorRow(
                "Font",
                widgetFontLabel(font)
            ) {
                openSheet =
                    WidgetConfigSheet.FONT
            }

            ConfigSelectorRow(
                "Content",
                contentModeLabel(
                    value.contentMode
                )
            ) {
                openSheet =
                    WidgetConfigSheet.CONTENT
            }

            ConfigToggleRow(
                "Events",
                value.showEvents
            ) {
                value =
                    value.copy(showEvents = it)
            }

            ConfigToggleRow(
                "Tasks",
                value.showTasks
            ) {
                value =
                    value.copy(showTasks = it)
            }

            ConfigToggleRow(
                "Focus state",
                value.showFocusState
            ) {
                value =
                    value.copy(showFocusState = it)
            }

            ConfigToggleRow(
                "Slide long titles",
                value.autoSlideLongTitles
            ) {
                value =
                    value.copy(
                        autoSlideLongTitles = it
                    )
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
                        MaterialTheme.colorScheme
                            .onBackground,
                    contentColor =
                        MaterialTheme.colorScheme
                            .background
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
                        MaterialTheme.colorScheme
                            .onSurfaceVariant
                )
            }
        }
    }

    when (openSheet) {
        WidgetConfigSheet.EMOJI ->
            EmojiPickerSheet(
                selected = emoji,
                onSelect = { selected ->
                    value =
                        value.copy(
                            emoji = selected
                        )
                    openSheet = null
                },
                onDismiss = {
                    openSheet = null
                }
            )

        WidgetConfigSheet.FONT ->
            WidgetSelectionSheet(
                title = "Widget font",
                options =
                    WidgetFontChoice.entries
                        .map {
                            widgetFontLabel(it) to
                                it.name
                        },
                selected = font.name,
                onSelect = {
                    value =
                        value.copy(font = it)
                    openSheet = null
                },
                onDismiss = {
                    openSheet = null
                }
            )

        WidgetConfigSheet.SPACE ->
            WidgetSelectionSheet(
                title = "Space",
                options =
                    listOf("All" to "") +
                        spaces.map {
                            it.name to it.id
                        },
                selected =
                    value.spaceId.orEmpty(),
                onSelect = {
                    value =
                        value.copy(
                            spaceId =
                                it.takeIf(
                                    String::isNotBlank
                                )
                        )
                    openSheet = null
                },
                onDismiss = {
                    openSheet = null
                }
            )

        WidgetConfigSheet.CALENDAR ->
            WidgetSelectionSheet(
                title = "Calendar",
                options =
                    listOf("All" to "") +
                        calendars.map {
                            it.name to
                                it.id.toString()
                        },
                selected =
                    value.calendarId
                        ?.toString()
                        .orEmpty(),
                onSelect = {
                    value =
                        value.copy(
                            calendarId =
                                it.toLongOrNull()
                        )
                    openSheet = null
                },
                onDismiss = {
                    openSheet = null
                }
            )

        WidgetConfigSheet.CONTENT ->
            WidgetSelectionSheet(
                title = "Content",
                options =
                    WidgetContentMode.entries
                        .map {
                            contentModeLabel(it) to
                                it.name
                        },
                selected =
                    value.contentMode.name,
                onSelect = { raw ->
                    value =
                        value.copy(
                            contentMode =
                                WidgetContentMode.valueOf(
                                    raw
                                )
                        )
                    openSheet = null
                },
                onDismiss = {
                    openSheet = null
                }
            )

        null -> Unit
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EmojiPickerSheet(
    selected: String,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor =
            MaterialTheme.colorScheme.background
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 22.dp)
                .padding(bottom = 26.dp)
        ) {
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment =
                    Alignment.CenterVertically
            ) {
                Text(
                    "Emoji",
                    style =
                        MaterialTheme.typography
                            .headlineLarge,
                    modifier = Modifier.weight(1f)
                )

                Text(
                    selected,
                    style =
                        MaterialTheme.typography
                            .headlineLarge
                )
            }

            Spacer(Modifier.height(8.dp))

            Text(
                "All Emoji 16.0 categories and variants.",
                style =
                    MaterialTheme.typography.bodyMedium,
                color =
                    MaterialTheme.colorScheme
                        .onSurfaceVariant
            )

            Spacer(Modifier.height(16.dp))

            AndroidView(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(390.dp),
                factory = { context ->
                    EmojiPickerView(
                        context,
                        null,
                        0
                    ).apply {
                        emojiGridColumns = 8
                        emojiGridRows = 5.5f
                        setOnEmojiPickedListener {
                            onSelect(it.emoji)
                        }
                    }
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WidgetSelectionSheet(
    title: String,
    options: List<Pair<String, String>>,
    selected: String,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor =
            MaterialTheme.colorScheme.background
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 26.dp)
                .padding(bottom = 30.dp)
        ) {
            Text(
                title,
                style =
                    MaterialTheme.typography
                        .headlineLarge
            )

            Spacer(Modifier.height(16.dp))

            options.forEach { (label, value) ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            onSelect(value)
                        }
                        .padding(vertical = 13.dp),
                    verticalAlignment =
                        Alignment.CenterVertically
                ) {
                    Text(
                        label,
                        style =
                            MaterialTheme.typography
                                .bodyLarge,
                        modifier =
                            Modifier.weight(1f)
                    )

                    Text(
                        if (selected == value) {
                            "●"
                        } else {
                            "○"
                        },
                        color =
                            MaterialTheme.colorScheme
                                .onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun WidgetPreview(
    value: WidgetInstancePrefs,
    emoji: String,
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
            val kindVisible =
                when (item.kind) {
                    AgendaKind.EVENT ->
                        value.showEvents
                    AgendaKind.TASK ->
                        value.showTasks
                }

            val spaceVisible =
                value.spaceId == null ||
                    item.spaceId ==
                    value.spaceId

            val calendarVisible =
                value.calendarId == null ||
                    item.calendarId ==
                    value.calendarId

            kindVisible &&
                spaceVisible &&
                calendarVisible
        }
    }

    val current =
        filtered.firstOrNull { item ->
            val start = item.startTime
            val end = item.endTime

            item.occursOn(
                now.toLocalDate()
            ) &&
                start != null &&
                end != null &&
                !now.toLocalTime()
                    .isBefore(start) &&
                now.toLocalTime()
                    .isBefore(end)
        }

    val next =
        previewNextOccurrence(
            filtered,
            now
        )

    val previewItem =
        when (value.contentMode) {
            WidgetContentMode.CURRENT ->
                current
            WidgetContentMode.NEXT ->
                next
            WidgetContentMode.SMART ->
                current ?: next
        }

    val status = when {
        previewItem == null ->
            "YOUR DAY IS CLEAR"

        current?.id == previewItem.id &&
            previewItem.focusCycle !=
            FocusCycle.OFF &&
            value.showFocusState ->
            "FOCUS > ${previewItem.title}"

        current?.id == previewItem.id ->
            "NOW > ${previewItem.title}"

        previewItem.kind ==
            AgendaKind.TASK ->
            "TODO > ${previewItem.title}"

        else -> {
            val time =
                previewItem.startTime
                    ?.format(
                        DateTimeFormatter
                            .ofPattern("HH:mm")
                    ) ?: "ALL"

            "$time > ${previewItem.title}"
        }
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(116.dp),
        shape = RoundedCornerShape(26.dp),
        color =
            colorResource(
                R.color.widget_system_surface
            ),
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
            verticalAlignment =
                Alignment.CenterVertically
        ) {
            val emojiBitmap =
                remember(emoji, context) {
                    NotoEmojiRenderer.render(
                        context = context,
                        glyph = emoji,
                        sizeDp = 44
                    )
                }

            Image(
                bitmap =
                    emojiBitmap.asImageBitmap(),
                contentDescription =
                    "Selected emoji",
                modifier = Modifier.size(44.dp),
                colorFilter =
                    ColorFilter.tint(
                        MaterialTheme.colorScheme
                            .onSurface
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
                val dateText =
                    buildString {
                        append(
                            now.dayOfWeek
                                .getDisplayName(
                                    TextStyle.SHORT,
                                    Locale.getDefault()
                                )
                                .uppercase()
                        )
                        append("  ")
                        append(now.dayOfMonth)
                        append("  ")
                        append(
                            now.month
                                .getDisplayName(
                                    TextStyle.SHORT,
                                    Locale.getDefault()
                                )
                                .uppercase()
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
                    contentModeLabel(
                        value.contentMode
                    ).uppercase(),
                    style =
                        MaterialTheme.typography
                            .labelMedium,
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
        bitmap =
            rendered.bitmap.asImageBitmap(),
        contentDescription = text,
        modifier = Modifier
            .width(rendered.widthDp.dp)
            .height(rendered.heightDp.dp),
        colorFilter =
            ColorFilter.tint(
                MaterialTheme.colorScheme.onSurface
            )
    )
}

@Composable
private fun ConfigSelectorRow(
    label: String,
    value: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                interactionSource =
                    remember {
                        MutableInteractionSource()
                    },
                indication = null,
                onClick = onClick
            )
            .padding(vertical = 12.dp),
        verticalAlignment =
            Alignment.CenterVertically
    ) {
        Text(
            label,
            style =
                MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f)
        )

        Text(
            value,
            style =
                MaterialTheme.typography.bodyMedium,
            color =
                MaterialTheme.colorScheme
                    .onSurfaceVariant,
            maxLines = 1
        )

        Spacer(Modifier.width(8.dp))

        Text(
            "›",
            color =
                MaterialTheme.colorScheme
                    .onSurfaceVariant
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
        verticalAlignment =
            Alignment.CenterVertically
    ) {
        Text(
            label,
            style =
                MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f)
        )

        Switch(
            checked = checked,
            onCheckedChange = onChange
        )
    }
}

private fun previewNextOccurrence(
    items: List<DaylineItem>,
    now: LocalDateTime
): DaylineItem? {
    for (offset in 0L..30L) {
        val date =
            now.toLocalDate()
                .plusDays(offset)

        val candidate =
            items
                .filter {
                    it.occursOn(date)
                }
                .sortedWith(
                    compareBy<DaylineItem> {
                        it.startTime == null
                    }.thenBy {
                        it.startTime
                    }
                )
                .firstOrNull { item ->
                    if (
                        date.isAfter(
                            now.toLocalDate()
                        )
                    ) {
                        true
                    } else {
                        val start =
                            item.startTime
                        val end =
                            item.endTime

                        when {
                            start == null ->
                                true
                            end != null &&
                                end.isAfter(
                                    now.toLocalTime()
                                ) ->
                                true
                            else ->
                                !start.isBefore(
                                    now.toLocalTime()
                                )
                        }
                    }
                }

        if (candidate != null) {
            return candidate
        }
    }

    return null
}

private fun resolveEmojiGlyph(
    raw: String?,
    fallback: String
): String {
    val saved =
        raw?.takeIf { it.isNotBlank() }
            ?: return fallback

    return runCatching {
        WidgetEmojiChoice
            .valueOf(saved)
            .symbol
    }.getOrDefault(saved)
}

private fun resolvePreviewFont(
    value: WidgetInstancePrefs,
    fallback: WidgetFontChoice
): WidgetFontChoice =
    runCatching {
        WidgetFontChoice.valueOf(
            value.font.orEmpty()
        )
    }.getOrDefault(fallback)

private fun widgetFontLabel(
    font: WidgetFontChoice
): String = when (font) {
    WidgetFontChoice.DOT_BOLD ->
        "Nothing dots · Bold"
    WidgetFontChoice.DOT_FINE ->
        "Nothing dots · Fine"
    WidgetFontChoice.MONO ->
        "Monospace · Bold"
}

private fun contentModeLabel(
    mode: WidgetContentMode
): String = when (mode) {
    WidgetContentMode.SMART -> "Smart"
    WidgetContentMode.CURRENT -> "Current"
    WidgetContentMode.NEXT -> "Next"
}
