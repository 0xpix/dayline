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
import com.pix.dayline.model.Recurrence
import com.pix.dayline.model.occursOn
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

object NotificationScheduler {
    private const val CHANNEL_ID = "dayline_reminders"

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(NotificationManager::class.java)
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Event reminders",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Upcoming Dayline events and tasks"
            }
            manager.createNotificationChannel(channel)
        }
    }

    fun syncAll(context: Context, items: List<DaylineItem>) {
        ensureChannel(context)
        items.forEach { scheduleNext(context, it) }
    }

    fun cancel(context: Context, item: DaylineItem) {
        val manager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        manager.cancel(pendingIntent(context, item.id))
    }

    fun scheduleNext(context: Context, item: DaylineItem, after: LocalDateTime = LocalDateTime.now()) {
        cancel(context, item)
        val reminder = item.reminderMinutes ?: return
        val startTime = item.startTime ?: return
        val trigger = nextTrigger(item, reminder, after) ?: return
        val triggerMillis = trigger.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val manager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = pendingIntent(context, item.id)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && manager.canScheduleExactAlarms()) {
            manager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerMillis, intent)
        } else if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            manager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerMillis, intent)
        } else {
            manager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerMillis, intent)
        }
    }

    fun showReminder(context: Context, itemId: String) {
        ensureChannel(context)
        val item = DaylineStore(context).loadItems().firstOrNull { it.id == itemId } ?: return
        val start = item.startTime ?: return
        val reminder = item.reminderMinutes ?: return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            scheduleNext(context, item)
            return
        }

        val contentIntent = PendingIntent.getActivity(
            context,
            requestCode(item.id),
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val range = buildString {
            append(start.format(DateTimeFormatter.ofPattern("HH:mm")))
            item.endTime?.takeIf { it.isAfter(start) }?.let {
                append("–")
                append(it.format(DateTimeFormatter.ofPattern("HH:mm")))
            }
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_dayline_notification)
            .setContentTitle(item.title)
            .setContentText("Starts in ${reminder} min · $range")
            .setContentIntent(contentIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        NotificationManagerCompat.from(context).notify(requestCode(item.id), notification)
        scheduleNext(context, item, LocalDateTime.now().plusSeconds(2))
    }

    private fun nextTrigger(item: DaylineItem, reminder: Int, after: LocalDateTime): LocalDateTime? {
        val startTime = item.startTime ?: return null

        if (item.recurrence == Recurrence.ONCE) {
            val trigger = item.startDate.atTime(startTime).minusMinutes(reminder.toLong())
            return trigger.takeIf { it.isAfter(after) }
        }

        var date = maxOf(item.startDate, after.toLocalDate())
        repeat(400) {
            if (item.occursOn(date)) {
                val trigger = date.atTime(startTime).minusMinutes(reminder.toLong())
                if (trigger.isAfter(after)) return trigger
            }
            date = date.plusDays(1)
        }
        return null
    }

    private fun pendingIntent(context: Context, itemId: String): PendingIntent {
        val intent = Intent(context, NotificationReceiver::class.java).putExtra(NotificationReceiver.EXTRA_ITEM_ID, itemId)
        return PendingIntent.getBroadcast(
            context,
            requestCode(itemId),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun requestCode(id: String): Int = id.hashCode() and 0x7fffffff
}
