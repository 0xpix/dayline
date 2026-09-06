package com.pix.dayline.notifications

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.pix.dayline.MainActivity
import com.pix.dayline.R
import com.pix.dayline.data.DaylineStore
import com.pix.dayline.data.FocusRuntimeState
import com.pix.dayline.data.FocusRuntimeStore
import com.pix.dayline.model.DaylineItem
import com.pix.dayline.model.FocusCycle
import com.pix.dayline.model.occursOn
import com.pix.dayline.widgets.DaylineWidgetUpdater
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

object NowActivityScheduler {
    private const val CHANNEL_ID = "dayline_now"
    private const val NOTIFICATION_OFFSET = 620_000

    data class LiveState(
        val item: DaylineItem,
        val active: Boolean,
        val focus: Boolean? = null,
        val paused: Boolean = false,
        val minutesRemaining: Long = 0L,
        val sessionsCompleted: Int = 0
    )

    private data class Occurrence(
        val item: DaylineItem,
        val date: LocalDate,
        val start: LocalDateTime,
        val end: LocalDateTime
    )

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_ID,
                    "Now activity",
                    NotificationManager.IMPORTANCE_LOW
                ).apply {
                    description = "Current event and optional focus/rest cycle"
                    setShowBadge(false)
                }
            )
        }
    }

    fun syncAll(context: Context, items: List<DaylineItem>) {
        ensureChannel(context)
        items.forEach { item ->
            cancelAlarms(context, item)
            cancelNotification(context, item)

            val now = LocalDateTime.now()
            val occurrence = currentOrNext(item, now) ?: return@forEach

            if (occurrence.start <= now && occurrence.end > now) {
                showNow(context, occurrence)
                scheduleEnd(context, occurrence)
                scheduleNextStartAfter(context, item, occurrence.end.plusSeconds(1))
            } else {
                scheduleStart(context, occurrence)
            }
        }
    }

    fun cancel(context: Context, item: DaylineItem) {
        cancelAlarms(context, item)
        cancelNotification(context, item)
        FocusRuntimeStore(context).clear(item.id)
    }

    fun cancelAll(context: Context, items: List<DaylineItem>) {
        items.forEach { cancel(context, it) }
    }

    fun handleStart(context: Context, itemId: String) {
        val item = DaylineStore(context).loadItems().firstOrNull { it.id == itemId } ?: return
        val now = LocalDateTime.now()
        val occurrence = currentOrNext(item, now.minusMinutes(2)) ?: return

        if (occurrence.end > now && occurrence.start <= now.plusMinutes(2)) {
            showNow(context, occurrence)
            scheduleEnd(context, occurrence)
            scheduleNextStartAfter(context, item, occurrence.end.plusSeconds(1))
        } else {
            scheduleStart(context, occurrence)
        }
    }

    fun handlePhase(context: Context, itemId: String) {
        val item = DaylineStore(context).loadItems().firstOrNull { it.id == itemId } ?: return
        val now = LocalDateTime.now()
        val occurrence = currentOrNext(item, now.minusMinutes(1)) ?: return
        if (occurrence.end <= now) {
            handleEnd(context, itemId)
            return
        }

        val runtimeStore = FocusRuntimeStore(context)
        val current = ensureRuntime(context, occurrence, now)
        if (current.finished || current.paused) {
            showNow(context, occurrence)
            return
        }

        val nextFocus = !current.focus
        val focusSeconds = item.focusMinutes.coerceAtLeast(1) * 60L
        val breakSeconds = item.breakMinutes.coerceAtLeast(1) * 60L
        val duration = if (nextFocus) focusSeconds else breakSeconds
        val eventEndMillis = occurrence.end.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

        val nowMillis = System.currentTimeMillis()
        val focusElapsed = if (current.focus) {
            ((nowMillis - current.phaseStartedEpochMillis) / 1000L)
                .coerceIn(0L, focusSeconds)
        } else {
            0L
        }
        val next = current.copy(
            focus = nextFocus,
            phaseStartedEpochMillis = nowMillis,
            phaseEndEpochMillis = minOf(
                nowMillis + duration * 1000L,
                eventEndMillis
            ),
            sessionsCompleted = current.sessionsCompleted + if (current.focus) 1 else 0,
            focusedSeconds = current.focusedSeconds + focusElapsed,
            paused = false,
            pausedRemainingSeconds = 0L
        )
        runtimeStore.save(next)
        showNow(context, occurrence, next)
        refreshWidgets(context)
    }

    fun handleRefresh(context: Context, itemId: String) {
        val item = DaylineStore(context).loadItems().firstOrNull { it.id == itemId } ?: return
        val now = LocalDateTime.now()
        val occurrence = occurrenceForNow(item, now)

        if (occurrence == null) {
            cancelDisplayRefresh(context, itemId)
            return
        }

        showNow(context, occurrence)
        refreshWidgets(context)
    }

    fun handleEnd(context: Context, itemId: String) {
        val store = DaylineStore(context)
        val item = store.loadItems().firstOrNull { it.id == itemId }

        if (item != null) {
            val runtimeStore = FocusRuntimeStore(context)
            val runtime = runtimeStore.load(itemId)
            val finalized = if (runtime != null && item.focusCycle != FocusCycle.OFF) {
                val totalFocused = finalizedFocusedSeconds(item, runtime)
                val completed = runtime.sessionsCompleted + if (runtime.focus && !runtime.paused) {
                    val elapsed = ((System.currentTimeMillis() - runtime.phaseStartedEpochMillis) / 1000L)
                    if (elapsed >= item.focusMinutes.coerceAtLeast(1) * 60L) 1 else 0
                } else 0
                item.copy(
                    focusSessionsCompleted = item.focusSessionsCompleted + completed,
                    focusedMinutesCompleted = item.focusedMinutesCompleted +
                        (totalFocused / 60L).toInt()
                )
            } else {
                item
            }
            if (finalized != item) {
                store.saveItems(store.loadItems().map { if (it.id == itemId) finalized else it })
            }
            runtimeStore.clear(itemId)
            cancelNotification(context, item)
            currentOrNext(finalized, LocalDateTime.now().plusSeconds(1))?.let {
                scheduleStart(context, it)
            }
            refreshWidgets(context)
        } else {
            NotificationManagerCompat.from(context).cancel(notificationId(itemId))
        }
    }

    fun togglePause(context: Context, itemId: String) {
        val item = DaylineStore(context).loadItems().firstOrNull { it.id == itemId } ?: return
        val now = LocalDateTime.now()
        val occurrence = currentOrNext(item, now.minusMinutes(1)) ?: return
        if (item.focusCycle == FocusCycle.OFF) return

        val runtimeStore = FocusRuntimeStore(context)
        val state = ensureRuntime(context, occurrence, now)
        val nowMillis = System.currentTimeMillis()
        val updated = if (state.paused) {
            state.copy(
                paused = false,
                phaseStartedEpochMillis = nowMillis,
                phaseEndEpochMillis = minOf(
                    nowMillis + state.pausedRemainingSeconds.coerceAtLeast(1L) * 1000L,
                    occurrence.end.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
                ),
                pausedRemainingSeconds = 0L
            )
        } else {
            val remaining = ((state.phaseEndEpochMillis - nowMillis) / 1000L)
                .coerceAtLeast(1L)
            val focusElapsed = if (state.focus) {
                ((nowMillis - state.phaseStartedEpochMillis) / 1000L)
                    .coerceAtLeast(0L)
            } else 0L
            cancelPhaseAlarm(context, itemId)
            state.copy(
                paused = true,
                phaseStartedEpochMillis = nowMillis,
                pausedRemainingSeconds = remaining,
                focusedSeconds = state.focusedSeconds + focusElapsed
            )
        }
        runtimeStore.save(updated)
        showNow(context, occurrence, updated)
        refreshWidgets(context)
    }

    fun skipRest(context: Context, itemId: String) {
        val item = DaylineStore(context).loadItems().firstOrNull { it.id == itemId } ?: return
        val now = LocalDateTime.now()
        val occurrence = currentOrNext(item, now.minusMinutes(1)) ?: return
        if (item.focusCycle == FocusCycle.OFF) return

        val runtimeStore = FocusRuntimeStore(context)
        val state = ensureRuntime(context, occurrence, now)
        if (state.focus || state.finished) return

        val endMillis = occurrence.end.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val updated = state.copy(
            focus = true,
            phaseStartedEpochMillis = System.currentTimeMillis(),
            paused = false,
            pausedRemainingSeconds = 0L,
            phaseEndEpochMillis = minOf(
                System.currentTimeMillis() + item.focusMinutes.coerceAtLeast(1) * 60_000L,
                endMillis
            )
        )
        runtimeStore.save(updated)
        showNow(context, occurrence, updated)
        refreshWidgets(context)
    }

    fun extendPhase(context: Context, itemId: String, minutes: Int) {
        val item = DaylineStore(context).loadItems().firstOrNull { it.id == itemId } ?: return
        val now = LocalDateTime.now()
        val occurrence = currentOrNext(item, now.minusMinutes(1)) ?: return
        if (item.focusCycle == FocusCycle.OFF) return

        val runtimeStore = FocusRuntimeStore(context)
        val state = ensureRuntime(context, occurrence, now)
        val endMillis = occurrence.end.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val updated = if (state.paused) {
            state.copy(
                pausedRemainingSeconds = state.pausedRemainingSeconds + minutes.coerceAtLeast(1) * 60L
            )
        } else {
            state.copy(
                phaseEndEpochMillis = minOf(
                    state.phaseEndEpochMillis + minutes.coerceAtLeast(1) * 60_000L,
                    endMillis
                )
            )
        }
        runtimeStore.save(updated)
        showNow(context, occurrence, updated)
        refreshWidgets(context)
    }

    fun finishNow(context: Context, itemId: String) {
        val store = DaylineStore(context)
        val item = store.loadItems().firstOrNull { it.id == itemId } ?: return
        val now = LocalDateTime.now()
        val occurrence = occurrenceForNow(item, now)
        val runtimeStore = FocusRuntimeStore(context)
        val state = runtimeStore.load(itemId)

        val finalized = if (state != null && item.focusCycle != FocusCycle.OFF) {
            item.copy(
                focusSessionsCompleted = item.focusSessionsCompleted + state.sessionsCompleted,
                focusedMinutesCompleted = item.focusedMinutesCompleted +
                    (finalizedFocusedSeconds(item, state) / 60L).toInt()
            )
        } else item

        if (finalized != item) {
            store.saveItems(store.loadItems().map { if (it.id == itemId) finalized else it })
        }

        runtimeStore.clear(itemId)
        cancelAlarms(context, item)
        cancelNotification(context, item)
        occurrence?.let {
            currentOrNext(finalized, it.end.plusSeconds(1))?.let { next ->
                scheduleStart(context, next)
            }
        }
        refreshWidgets(context)
    }

    fun liveState(context: Context, item: DaylineItem, now: LocalDateTime = LocalDateTime.now()): LiveState? {
        val occurrence = occurrenceForNow(item, now) ?: return null
        if (item.focusCycle == FocusCycle.OFF) {
            return LiveState(
                item = item,
                active = true,
                minutesRemaining = Duration.between(now, occurrence.end).toMinutes().coerceAtLeast(0)
            )
        }

        val state = ensureRuntime(context, occurrence, now)
        if (state.finished) return null
        val remaining = if (state.paused) {
            state.pausedRemainingSeconds / 60L
        } else {
            (state.phaseEndEpochMillis - System.currentTimeMillis()).coerceAtLeast(0L) / 60_000L
        }
        return LiveState(
            item = item,
            active = true,
            focus = state.focus,
            paused = state.paused,
            minutesRemaining = remaining,
            sessionsCompleted = state.sessionsCompleted
        )
    }

    private fun showNow(
        context: Context,
        occurrence: Occurrence,
        suppliedRuntime: FocusRuntimeState? = null
    ) {
        if (
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        ensureChannel(context)
        val now = LocalDateTime.now()
        val item = occurrence.item
        val runtime = if (item.focusCycle == FocusCycle.OFF) {
            null
        } else {
            suppliedRuntime ?: ensureRuntime(context, occurrence, now)
        }

        if (runtime?.finished == true) return

        val eventEndMillis = occurrence.end.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val timeout = (eventEndMillis - System.currentTimeMillis()).coerceAtLeast(1_000L)

        val contentIntent = PendingIntent.getActivity(
            context,
            notificationId(item.id),
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val format = DateTimeFormatter.ofPattern("HH:mm")
        val title: String
        val content: String
        val progressMax: Int
        val progressValue: Int

        if (runtime == null) {
            val totalMinutes = Duration.between(occurrence.start, occurrence.end)
                .toMinutes()
                .coerceAtLeast(1L)
            val elapsedMinutes = Duration.between(occurrence.start, now)
                .toMinutes()
                .coerceIn(0L, totalMinutes)
            val remainingMinutes = (totalMinutes - elapsedMinutes).coerceAtLeast(0L)

            title = item.title
            content = buildString {
                append("END ")
                append(occurrence.end.toLocalTime().format(format))
                append("  ·  ")
                append(compactRemaining(remainingMinutes))
                append(" LEFT")
            }
            progressMax = 100
            progressValue = ((elapsedMinutes * 100L) / totalMinutes).toInt().coerceIn(0, 100)
        } else {
            val mode = if (runtime.focus) "FOCUS" else "REST"
            val totalSessions = expectedFocusSessions(item, occurrence)
            val currentSession = (runtime.sessionsCompleted + if (runtime.focus) 1 else 0)
                .coerceAtLeast(1)
                .coerceAtMost(totalSessions.coerceAtLeast(1))

            val phaseTotalSeconds = if (runtime.focus) {
                item.focusMinutes.coerceAtLeast(1) * 60L
            } else {
                item.breakMinutes.coerceAtLeast(1) * 60L
            }
            val remainingSeconds = if (runtime.paused) {
                runtime.pausedRemainingSeconds.coerceAtLeast(0L)
            } else {
                ((runtime.phaseEndEpochMillis - System.currentTimeMillis()) / 1000L)
                    .coerceAtLeast(0L)
            }
            val remainingMinutes = ((remainingSeconds + 59L) / 60L).coerceAtLeast(0L)
            val elapsedSeconds = (phaseTotalSeconds - remainingSeconds)
                .coerceIn(0L, phaseTotalSeconds)

            title = "${if (runtime.paused) "PAUSED" else mode} · ${item.title}"
            content = buildString {
                append("END ")
                append(occurrence.end.toLocalTime().format(format))
                append("  ·  ")
                append(compactRemaining(remainingMinutes))
                append("  ·  SESSION ")
                append(currentSession)
                append("/")
                append(totalSessions)
            }
            progressMax = 100
            progressValue = if (runtime.paused) {
                ((elapsedSeconds * 100L) / phaseTotalSeconds).toInt().coerceIn(0, 100)
            } else {
                ((elapsedSeconds * 100L) / phaseTotalSeconds).toInt().coerceIn(0, 100)
            }
        }

        val eventRange =
            occurrence.start.toLocalTime().format(format) +
                "  →  " +
                occurrence.end.toLocalTime().format(format)

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_dayline_notification)
            .setContentTitle(title)
            .setContentText(content)
            .setSubText(eventRange)
            .setContentIntent(contentIntent)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setCategory(NotificationCompat.CATEGORY_EVENT)
            .setTimeoutAfter(timeout)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setShowWhen(false)
            .setProgress(progressMax, progressValue, false)

        if (runtime != null) {
            builder.addAction(
                R.drawable.ic_dayline_notification,
                if (runtime.paused) "Resume" else "Pause",
                actionPendingIntent(context, item.id, NowActivityReceiver.ACTION_PAUSE, "pause")
            )
            if (!runtime.focus) {
                builder.addAction(
                    R.drawable.ic_dayline_notification,
                    "Skip rest",
                    actionPendingIntent(context, item.id, NowActivityReceiver.ACTION_SKIP_REST, "skip")
                )
            }
            builder.addAction(
                R.drawable.ic_dayline_notification,
                "+5 min",
                actionPendingIntent(context, item.id, NowActivityReceiver.ACTION_PLUS_FIVE, "plus")
            )
            builder.addAction(
                R.drawable.ic_dayline_notification,
                "Finish",
                actionPendingIntent(context, item.id, NowActivityReceiver.ACTION_FINISH, "finish")
            )
        }

        NotificationManagerCompat.from(context).notify(notificationId(item.id), builder.build())

        // Avoid the system HH:MM:SS chronometer chip. Refresh the clean text periodically instead.
        if (runtime?.paused != true) {
            scheduleDisplayRefresh(
                context = context,
                itemId = item.id,
                eventEndMillis = eventEndMillis,
                phaseEndMillis = runtime?.phaseEndEpochMillis
            )
        } else {
            cancelDisplayRefresh(context, item.id)
        }

        if (runtime != null && !runtime.paused && runtime.phaseEndEpochMillis < eventEndMillis) {
            scheduleAlarmMillis(
                context,
                runtime.phaseEndEpochMillis,
                phasePendingIntent(context, item.id)
            )
        }
    }

    private fun compactRemaining(minutes: Long): String = when {
        minutes <= 0L -> "ENDING"
        minutes < 60L -> "${minutes}M"
        minutes % 60L == 0L -> "${minutes / 60L}H"
        else -> "${minutes / 60L}H ${minutes % 60L}M"
    }

    private fun scheduleDisplayRefresh(
        context: Context,
        itemId: String,
        eventEndMillis: Long,
        phaseEndMillis: Long?
    ) {
        val nowMillis = System.currentTimeMillis()
        val cadence = if (phaseEndMillis != null) 5L * 60_000L else 15L * 60_000L
        val target = listOfNotNull(
            nowMillis + cadence,
            phaseEndMillis,
            eventEndMillis
        ).minOrNull() ?: return

        if (target <= nowMillis + 5_000L || target >= eventEndMillis) return
        scheduleAlarmMillis(context, target, refreshPendingIntent(context, itemId))
    }

    private fun ensureRuntime(
        context: Context,
        occurrence: Occurrence,
        now: LocalDateTime
    ): FocusRuntimeState {
        val store = FocusRuntimeStore(context)
        val existing = store.load(occurrence.item.id)
        if (existing != null && existing.occurrenceDate == occurrence.date) {
            return existing
        }

        val focusSeconds = occurrence.item.focusMinutes.coerceAtLeast(1) * 60L
        val breakSeconds = occurrence.item.breakMinutes.coerceAtLeast(1) * 60L
        val cycleSeconds = focusSeconds + breakSeconds
        val elapsed = Duration.between(occurrence.start, now).seconds.coerceAtLeast(0L)
        val fullCycles = if (cycleSeconds > 0) elapsed / cycleSeconds else 0L
        val offset = if (cycleSeconds > 0) elapsed % cycleSeconds else 0L
        val focus = offset < focusSeconds
        val remaining = if (focus) focusSeconds - offset else cycleSeconds - offset
        val eventEndMillis = occurrence.end.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

        val nowMillis = System.currentTimeMillis()
        val elapsedInPhase = if (focus) offset else (offset - focusSeconds).coerceAtLeast(0L)
        val state = FocusRuntimeState(
            itemId = occurrence.item.id,
            occurrenceDate = occurrence.date,
            focus = focus,
            phaseStartedEpochMillis = nowMillis - elapsedInPhase * 1000L,
            phaseEndEpochMillis = minOf(
                nowMillis + remaining.coerceAtLeast(1L) * 1000L,
                eventEndMillis
            ),
            sessionsCompleted = (fullCycles + if (focus) 0L else 1L).toInt(),
            focusedSeconds = fullCycles * focusSeconds + if (focus) 0L else focusSeconds
        )
        store.save(state)
        return state
    }

    private fun finalizedFocusedSeconds(
        item: DaylineItem,
        state: FocusRuntimeState
    ): Long {
        if (!state.focus || state.paused) return state.focusedSeconds
        val elapsed = ((System.currentTimeMillis() - state.phaseStartedEpochMillis) / 1000L)
            .coerceAtLeast(0L)
        return state.focusedSeconds +
            elapsed.coerceAtMost(item.focusMinutes.coerceAtLeast(1) * 60L)
    }

    private fun expectedFocusSessions(
        item: DaylineItem,
        occurrence: Occurrence
    ): Int {
        val totalSeconds = Duration.between(occurrence.start, occurrence.end)
            .seconds
            .coerceAtLeast(1L)
        val cycleSeconds = (item.focusMinutes.coerceAtLeast(1) +
            item.breakMinutes.coerceAtLeast(1)) * 60L
        return ((totalSeconds + cycleSeconds - 1L) / cycleSeconds)
            .toInt()
            .coerceAtLeast(1)
    }


    private fun occurrenceForNow(item: DaylineItem, now: LocalDateTime): Occurrence? {
        val start = item.startTime ?: return null
        val end = item.endTime?.takeIf { it.isAfter(start) } ?: return null
        val date = now.toLocalDate()
        if (!item.occursOn(date)) return null
        val occurrence = Occurrence(item, date, date.atTime(start), date.atTime(end))
        return occurrence.takeIf { now >= it.start && now < it.end }
    }

    private fun currentOrNext(item: DaylineItem, from: LocalDateTime): Occurrence? {
        val startTime = item.startTime ?: return null
        val endTime = item.endTime?.takeIf { it.isAfter(startTime) } ?: return null
        var date = maxOf(item.startDate, from.toLocalDate())

        repeat(400) {
            if (item.occursOn(date)) {
                val occurrence = Occurrence(item, date, date.atTime(startTime), date.atTime(endTime))
                if (occurrence.end > from) return occurrence
            }
            date = date.plusDays(1)
        }
        return null
    }

    private fun scheduleNextStartAfter(context: Context, item: DaylineItem, after: LocalDateTime) {
        currentOrNext(item, after)?.let { scheduleStart(context, it) }
    }

    private fun scheduleStart(context: Context, occurrence: Occurrence) {
        scheduleAlarm(context, occurrence.start, startPendingIntent(context, occurrence.item.id))
    }

    private fun scheduleEnd(context: Context, occurrence: Occurrence) {
        scheduleAlarm(context, occurrence.end, endPendingIntent(context, occurrence.item.id))
    }

    private fun scheduleAlarm(context: Context, at: LocalDateTime, pendingIntent: PendingIntent) {
        scheduleAlarmMillis(
            context,
            at.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli(),
            pendingIntent
        )
    }

    private fun scheduleAlarmMillis(context: Context, triggerMillis: Long, pendingIntent: PendingIntent) {
        if (triggerMillis <= System.currentTimeMillis()) return
        val manager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && manager.canScheduleExactAlarms()) {
            manager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerMillis, pendingIntent)
        } else if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            manager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerMillis, pendingIntent)
        } else {
            manager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerMillis, pendingIntent)
        }
    }

    private fun cancelAlarms(context: Context, item: DaylineItem) {
        val manager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        manager.cancel(startPendingIntent(context, item.id))
        manager.cancel(endPendingIntent(context, item.id))
        manager.cancel(phasePendingIntent(context, item.id))
        manager.cancel(refreshPendingIntent(context, item.id))
    }

    private fun cancelPhaseAlarm(context: Context, itemId: String) {
        val manager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        manager.cancel(phasePendingIntent(context, itemId))
    }

    private fun cancelDisplayRefresh(context: Context, itemId: String) {
        val manager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        manager.cancel(refreshPendingIntent(context, itemId))
    }

    private fun cancelNotification(context: Context, item: DaylineItem) {
        NotificationManagerCompat.from(context).cancel(notificationId(item.id))
    }

    private fun startPendingIntent(context: Context, itemId: String): PendingIntent =
        actionPendingIntent(context, itemId, NowActivityReceiver.ACTION_START, "start")

    private fun phasePendingIntent(context: Context, itemId: String): PendingIntent =
        actionPendingIntent(context, itemId, NowActivityReceiver.ACTION_PHASE, "phase")

    private fun endPendingIntent(context: Context, itemId: String): PendingIntent =
        actionPendingIntent(context, itemId, NowActivityReceiver.ACTION_END, "end")

    private fun refreshPendingIntent(context: Context, itemId: String): PendingIntent =
        actionPendingIntent(context, itemId, NowActivityReceiver.ACTION_REFRESH, "refresh")

    private fun actionPendingIntent(
        context: Context,
        itemId: String,
        action: String,
        kind: String
    ): PendingIntent = PendingIntent.getBroadcast(
        context,
        requestCode(itemId, kind),
        Intent(context, NowActivityReceiver::class.java).apply {
            this.action = action
            putExtra(NowActivityReceiver.EXTRA_ITEM_ID, itemId)
        },
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    private fun requestCode(itemId: String, kind: String): Int =
        "$kind:$itemId".hashCode() and 0x7fffffff

    private fun notificationId(itemId: String): Int =
        NOTIFICATION_OFFSET + (itemId.hashCode() and 0x0fffffff)

    private fun refreshWidgets(context: Context) {
        CoroutineScope(Dispatchers.Default).launch {
            DaylineWidgetUpdater.updateAll(context)
        }
    }
}
