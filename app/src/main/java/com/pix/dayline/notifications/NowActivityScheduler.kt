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
import com.pix.dayline.model.DaylineItem
import com.pix.dayline.model.FocusCycle
import com.pix.dayline.model.occursOn
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

object NowActivityScheduler {
    private const val CHANNEL_ID = "dayline_now"
    private const val NOTIFICATION_OFFSET = 620_000

    private data class Occurrence(
        val item: DaylineItem,
        val date: LocalDate,
        val start: LocalDateTime,
        val end: LocalDateTime
    )

    private data class FocusPhase(
        val focus: Boolean,
        val phaseEnd: LocalDateTime
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
                    description = "Persistent Dayline activity while a timed block is in progress"
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
                scheduleNextStartAfter(
                    context,
                    item,
                    occurrence.end.plusSeconds(1)
                )
            } else {
                scheduleStart(context, occurrence)
            }
        }
    }

    fun cancel(context: Context, item: DaylineItem) {
        cancelAlarms(context, item)
        cancelNotification(context, item)
    }

    fun cancelAll(context: Context, items: List<DaylineItem>) {
        items.forEach { cancel(context, it) }
    }

    fun handleStart(context: Context, itemId: String) {
        val item = DaylineStore(context)
            .loadItems()
            .firstOrNull { it.id == itemId }
            ?: return

        val now = LocalDateTime.now()
        val occurrence = currentOrNext(
            item,
            now.minusMinutes(2)
        ) ?: return

        if (
            occurrence.end > now &&
            occurrence.start <= now.plusMinutes(2)
        ) {
            showNow(context, occurrence)
            scheduleEnd(context, occurrence)
            scheduleNextStartAfter(
                context,
                item,
                occurrence.end.plusSeconds(1)
            )
        } else {
            scheduleStart(context, occurrence)
        }
    }

    fun handleEnd(context: Context, itemId: String) {
        val item = DaylineStore(context)
            .loadItems()
            .firstOrNull { it.id == itemId }

        if (item != null) {
            cancelNotification(context, item)

            currentOrNext(
                item,
                LocalDateTime.now().plusSeconds(1)
            )?.let { scheduleStart(context, it) }
        } else {
            NotificationManagerCompat.from(context)
                .cancel(notificationId(itemId))
        }
    }

    private fun showNow(
        context: Context,
        occurrence: Occurrence
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

        val phase = if (
            occurrence.item.focusCycle == FocusCycle.POMODORO_25_5
        ) {
            focusPhase(occurrence, now)
        } else {
            null
        }

        val countdownEnd = phase?.phaseEnd ?: occurrence.end

        val endMillis = countdownEnd
            .atZone(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()

        val remaining = (endMillis - System.currentTimeMillis())
            .coerceAtLeast(1_000L)

        val contentIntent = PendingIntent.getActivity(
            context,
            notificationId(occurrence.item.id),
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val format = DateTimeFormatter.ofPattern("HH:mm")
        val range =
            "${occurrence.start.toLocalTime().format(format)}–" +
            occurrence.end.toLocalTime().format(format)

        val title = if (phase == null) {
            occurrence.item.title
        } else {
            "${if (phase.focus) "FOCUS" else "REST"} · ${occurrence.item.title}"
        }

        val content = if (phase == null) {
            "$range · in progress"
        } else {
            val minutes = Duration
                .between(now, countdownEnd)
                .toMinutes()
                .coerceAtLeast(1)
            "${if (phase.focus) "25 min focus" else "5 min rest"} · ${minutes}m left"
        }

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_dayline_notification)
            .setContentTitle(title)
            .setContentText(content)
            .setContentIntent(contentIntent)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setCategory(NotificationCompat.CATEGORY_EVENT)
            .setShowWhen(true)
            .setWhen(endMillis)
            .setUsesChronometer(true)
            .setChronometerCountDown(true)
            .setTimeoutAfter(remaining)
            .setRequestPromotedOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)

        NotificationManagerCompat.from(context).notify(
            notificationId(occurrence.item.id),
            builder.build()
        )

        if (phase != null && phase.phaseEnd < occurrence.end) {
            scheduleAlarm(
                context = context,
                at = phase.phaseEnd,
                pendingIntent = phasePendingIntent(
                    context,
                    occurrence.item.id
                )
            )
        }
    }

    private fun focusPhase(
        occurrence: Occurrence,
        now: LocalDateTime
    ): FocusPhase {
        val elapsedSeconds = Duration
            .between(occurrence.start, now)
            .seconds
            .coerceAtLeast(0)

        val cycleSeconds = 30L * 60L
        val focusSeconds = 25L * 60L
        val offset = elapsedSeconds % cycleSeconds
        val cycleStart = now.minusSeconds(offset)

        val focus = offset < focusSeconds

        val rawEnd = if (focus) {
            cycleStart.plusSeconds(focusSeconds)
        } else {
            cycleStart.plusSeconds(cycleSeconds)
        }

        return FocusPhase(
            focus = focus,
            phaseEnd = minOf(rawEnd, occurrence.end)
        )
    }

    private fun currentOrNext(
        item: DaylineItem,
        from: LocalDateTime
    ): Occurrence? {
        val startTime = item.startTime ?: return null
        val endTime = item.endTime
            ?.takeIf { it.isAfter(startTime) }
            ?: return null

        var date = maxOf(item.startDate, from.toLocalDate())

        repeat(400) {
            if (item.occursOn(date)) {
                val start = date.atTime(startTime)
                val end = date.atTime(endTime)

                if (end > from) {
                    return Occurrence(
                        item = item,
                        date = date,
                        start = start,
                        end = end
                    )
                }
            }

            date = date.plusDays(1)
        }

        return null
    }

    private fun scheduleNextStartAfter(
        context: Context,
        item: DaylineItem,
        after: LocalDateTime
    ) {
        currentOrNext(item, after)?.let {
            scheduleStart(context, it)
        }
    }

    private fun scheduleStart(
        context: Context,
        occurrence: Occurrence
    ) {
        scheduleAlarm(
            context = context,
            at = occurrence.start,
            pendingIntent = startPendingIntent(
                context,
                occurrence.item.id
            )
        )
    }

    private fun scheduleEnd(
        context: Context,
        occurrence: Occurrence
    ) {
        scheduleAlarm(
            context = context,
            at = occurrence.end,
            pendingIntent = endPendingIntent(
                context,
                occurrence.item.id
            )
        )
    }

    private fun scheduleAlarm(
        context: Context,
        at: LocalDateTime,
        pendingIntent: PendingIntent
    ) {
        val triggerMillis = at
            .atZone(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()

        if (triggerMillis <= System.currentTimeMillis()) return

        val manager =
            context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        if (
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
            manager.canScheduleExactAlarms()
        ) {
            manager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerMillis,
                pendingIntent
            )
        } else if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            manager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerMillis,
                pendingIntent
            )
        } else {
            manager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerMillis,
                pendingIntent
            )
        }
    }

    private fun cancelAlarms(
        context: Context,
        item: DaylineItem
    ) {
        val manager =
            context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        manager.cancel(startPendingIntent(context, item.id))
        manager.cancel(endPendingIntent(context, item.id))
        manager.cancel(phasePendingIntent(context, item.id))
    }

    private fun cancelNotification(
        context: Context,
        item: DaylineItem
    ) {
        NotificationManagerCompat.from(context).cancel(
            notificationId(item.id)
        )
    }

    private fun startPendingIntent(
        context: Context,
        itemId: String
    ): PendingIntent =
        PendingIntent.getBroadcast(
            context,
            requestCode(itemId, "start"),
            Intent(
                context,
                NowActivityReceiver::class.java
            ).apply {
                action = NowActivityReceiver.ACTION_START
                putExtra(
                    NowActivityReceiver.EXTRA_ITEM_ID,
                    itemId
                )
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

    private fun phasePendingIntent(
        context: Context,
        itemId: String
    ): PendingIntent =
        PendingIntent.getBroadcast(
            context,
            requestCode(itemId, "phase"),
            Intent(
                context,
                NowActivityReceiver::class.java
            ).apply {
                action = NowActivityReceiver.ACTION_PHASE
                putExtra(
                    NowActivityReceiver.EXTRA_ITEM_ID,
                    itemId
                )
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

    private fun endPendingIntent(
        context: Context,
        itemId: String
    ): PendingIntent =
        PendingIntent.getBroadcast(
            context,
            requestCode(itemId, "end"),
            Intent(
                context,
                NowActivityReceiver::class.java
            ).apply {
                action = NowActivityReceiver.ACTION_END
                putExtra(
                    NowActivityReceiver.EXTRA_ITEM_ID,
                    itemId
                )
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

    private fun requestCode(
        itemId: String,
        kind: String
    ): Int =
        "$kind:$itemId".hashCode() and 0x7fffffff

    private fun notificationId(itemId: String): Int =
        NOTIFICATION_OFFSET + (
            itemId.hashCode() and 0x0fffffff
        )
}
