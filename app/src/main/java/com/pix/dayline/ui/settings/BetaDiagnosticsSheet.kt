package com.pix.dayline.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.pix.dayline.BuildConfig
import com.pix.dayline.model.GlyphHardwareStatus
import com.pix.dayline.model.UpdateUiState
import java.time.Instant
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
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = MaterialTheme.colorScheme.background) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 28.dp).padding(bottom = 36.dp)) {
            Text("Beta diagnostics", style = MaterialTheme.typography.displaySmall)
            Spacer(Modifier.height(8.dp))
            Text(
                "Read-only runtime facts for testing Dayline beta builds.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(24.dp))
            DiagnosticRow("VERSION", BuildConfig.VERSION_NAME)
            DiagnosticRow("BUILD", "${BuildConfig.VERSION_CODE} · ${BuildConfig.GIT_COMMIT}")
            DiagnosticRow("CHANNEL", BuildConfig.UPDATE_CHANNEL)
            DiagnosticRow(
                "CALENDAR",
                when {
                    !calendarSyncEnabled -> "OFF"
                    !calendarSyncError.isNullOrBlank() -> "ERROR · ${calendarSyncError.take(36)}"
                    lastCalendarSyncAt != null -> "SYNCED · ${formatTime(lastCalendarSyncAt)}"
                    else -> "WAITING"
                }
            )
            DiagnosticRow(
                "GLYPH HW",
                if (glyphHardwareStatus.available) glyphHardwareStatus.detail ?: "AVAILABLE" else "UNAVAILABLE"
            )
            DiagnosticRow("UPDATER", updateState.status.name)
            updateState.checkedAtMillis?.let { DiagnosticRow("UPDATE CHECK", formatTime(it)) }
            Spacer(Modifier.height(18.dp))
            Text(
                "This page intentionally reports hardware availability, not a fake connected state. Glyph transport diagnostics can be added once Nothing exposes a reliable connection signal.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
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

private fun formatTime(epochMillis: Long): String = DateTimeFormatter.ofPattern("MMM d · HH:mm:ss")
    .withZone(ZoneId.systemDefault())
    .format(Instant.ofEpochMilli(epochMillis))
