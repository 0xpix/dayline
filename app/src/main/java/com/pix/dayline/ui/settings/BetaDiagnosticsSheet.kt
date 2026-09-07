package com.pix.dayline.ui.settings

import android.content.Intent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.pix.dayline.BuildConfig
import com.pix.dayline.data.DaylineStore
import com.pix.dayline.data.FocusRuntimeStore
import com.pix.dayline.data.UpdateUiState
import com.pix.dayline.glyph.GlyphDiagnosticsStore
import com.pix.dayline.model.DaylineItem
import com.pix.dayline.model.GlyphHardwareStatus
import com.pix.dayline.model.occursOn
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BetaDiagnosticsSheet(
    calendarSyncEnabled: Boolean,
    lastCalendarSyncAt: Long?,
    calendarSyncError: String?,
    glyphHardwareStatus: GlyphHardwareStatus,
    updateState: UpdateUiState,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val store = remember(context) { DaylineStore(context.applicationContext) }
    val glyph = remember(context) { GlyphDiagnosticsStore(context.applicationContext).snapshot() }
    val focus = remember(context) { FocusRuntimeStore(context.applicationContext).active() }
    val nextReminder = remember(context) { nextReminder(store.loadItems()) }
    val widgetAt = store.loadLastWidgetRefreshAt()

    val rows = buildList {
        add("VERSION" to BuildConfig.VERSION_NAME)
        add("BUILD" to "${BuildConfig.VERSION_CODE} · ${BuildConfig.GIT_COMMIT}")
        add("CHANNEL" to BuildConfig.UPDATE_CHANNEL)
        add("CALENDAR" to when {
            !calendarSyncEnabled -> "OFF"
            !calendarSyncError.isNullOrBlank() -> "ERROR · ${calendarSyncError.take(40)}"
            lastCalendarSyncAt != null -> "SYNCED · ${formatTime(lastCalendarSyncAt)}"
            else -> "WAITING"
        })
        add("WIDGET" to (widgetAt?.let { "UPDATED · ${formatTime(it)}" } ?: "NO REFRESH RECORDED"))
        add("NEXT REMINDER" to (nextReminder?.let { formatTime(it) } ?: "NONE IN 30 DAYS"))
        add("FOCUS" to when {
            focus == null -> "IDLE"
            focus.paused -> "PAUSED · ${if (focus.focus) "FOCUS" else "REST"}"
            else -> "${if (focus.focus) "FOCUS" else "REST"} · ${focus.sessionsCompleted} SESSIONS"
        })
        add("GLYPH HW" to if (glyphHardwareStatus.available) glyphHardwareStatus.detail ?: "AVAILABLE" else "UNAVAILABLE")
        add("GLYPH FRAME" to (glyph.lastFrameAt?.let(::formatTime) ?: "NONE"))
        add("GLYPH DISCONNECTS" to glyph.disconnectCount.toString())
        add("GLYPH RECOVERIES" to glyph.recoveryCount.toString())
        add("GLYPH SEND FAILS" to glyph.sendFailureCount.toString())
        glyph.lastError?.let { add("GLYPH LAST ERROR" to it) }
        add("UPDATER" to updateState.status.name)
        updateState.checkedAtMillis?.let { add("UPDATE CHECK" to formatTime(it)) }
    }
    val exportText = buildString {
        appendLine("Dayline beta diagnostics")
        appendLine("Generated: ${formatTime(System.currentTimeMillis())}")
        rows.forEach { (label, value) -> appendLine("$label: $value") }
    }

    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = MaterialTheme.colorScheme.background) {
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 28.dp)
                .padding(bottom = 36.dp)
        ) {
            Text("Beta diagnostics", style = MaterialTheme.typography.displaySmall)
            Spacer(Modifier.height(8.dp))
            Text("Local runtime facts for testing Dayline. No calendar titles or event contents are exported.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(22.dp))
            rows.forEach { (label, value) -> DiagnosticRow(label, value) }
            Spacer(Modifier.height(18.dp))
            Text(
                "EXPORT DEBUG LOG",
                modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp).clickable {
                    val intent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_SUBJECT, "Dayline ${BuildConfig.VERSION_NAME} diagnostics")
                        putExtra(Intent.EXTRA_TEXT, exportText)
                    }
                    context.startActivity(Intent.createChooser(intent, "Export Dayline diagnostics"))
                }.wrapContentHeight(),
                style = MaterialTheme.typography.labelLarge
            )
            Text("Glyph counters are diagnostic only; they do not change the conservative recovery algorithm.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun DiagnosticRow(label: String, value: String) {
    Column(Modifier.fillMaxWidth().padding(vertical = 7.dp)) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(2.dp))
        Text(value, style = MaterialTheme.typography.bodyLarge)
    }
}

private fun nextReminder(items: List<DaylineItem>): Long? {
    val now = LocalDateTime.now()
    var best: LocalDateTime? = null
    for (offset in 0L..30L) {
        val date = LocalDate.now().plusDays(offset)
        items.forEach { item ->
            val reminder = item.reminderMinutes ?: return@forEach
            val start = item.startTime ?: return@forEach
            if (!item.occursOn(date)) return@forEach
            val candidate = LocalDateTime.of(date, start).minusMinutes(reminder.toLong())
            if (candidate.isAfter(now) && (best == null || candidate.isBefore(best))) best = candidate
        }
    }
    return best?.atZone(ZoneId.systemDefault())?.toInstant()?.toEpochMilli()
}

private fun formatTime(epochMillis: Long): String = DateTimeFormatter.ofPattern("MMM d · HH:mm:ss")
    .withZone(ZoneId.systemDefault())
    .format(Instant.ofEpochMilli(epochMillis))
