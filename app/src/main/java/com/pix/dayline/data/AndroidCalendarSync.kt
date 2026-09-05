package com.pix.dayline.data

import android.Manifest
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.provider.CalendarContract
import androidx.core.content.ContextCompat
import com.pix.dayline.model.AgendaKind
import com.pix.dayline.model.DaylineItem
import com.pix.dayline.model.FocusCycle
import com.pix.dayline.model.ItemColor
import com.pix.dayline.model.Recurrence
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset

object AndroidCalendarSync {
    private val zone: ZoneId
        get() = ZoneId.systemDefault()

    fun hasReadPermission(context: Context): Boolean =
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_CALENDAR
        ) == PackageManager.PERMISSION_GRANTED

    fun hasWritePermission(context: Context): Boolean =
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.WRITE_CALENDAR
        ) == PackageManager.PERMISSION_GRANTED

    fun hasPermissions(context: Context): Boolean =
        hasReadPermission(context) && hasWritePermission(context)

    /**
     * Read concrete Calendar Provider instances rather than raw Events.
     * Recurring external events therefore appear on the exact dates generated
     * by the user's calendar app.
     */
    fun loadOccurrences(
        context: Context,
        from: LocalDate = LocalDate.now().minusDays(14),
        to: LocalDate = LocalDate.now().plusDays(370)
    ): List<DaylineItem> {
        if (!hasReadPermission(context)) return emptyList()

        val beginMillis = from.atStartOfDay(zone).toInstant().toEpochMilli()
        val endMillis = to.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()

        val builder = CalendarContract.Instances.CONTENT_URI.buildUpon()
        ContentUris.appendId(builder, beginMillis)
        ContentUris.appendId(builder, endMillis)

        val projection = arrayOf(
            CalendarContract.Instances.EVENT_ID,
            CalendarContract.Instances.TITLE,
            CalendarContract.Instances.BEGIN,
            CalendarContract.Instances.END,
            CalendarContract.Instances.ALL_DAY,
            CalendarContract.Instances.CALENDAR_DISPLAY_NAME
        )

        return runCatching {
            context.contentResolver.query(
                builder.build(),
                projection,
                null,
                null,
                "${CalendarContract.Instances.BEGIN} ASC"
            )?.use { cursor ->
                val eventIdIx = cursor.getColumnIndexOrThrow(
                    CalendarContract.Instances.EVENT_ID
                )
                val titleIx = cursor.getColumnIndexOrThrow(
                    CalendarContract.Instances.TITLE
                )
                val beginIx = cursor.getColumnIndexOrThrow(
                    CalendarContract.Instances.BEGIN
                )
                val endIx = cursor.getColumnIndexOrThrow(
                    CalendarContract.Instances.END
                )
                val allDayIx = cursor.getColumnIndexOrThrow(
                    CalendarContract.Instances.ALL_DAY
                )
                val calendarIx = cursor.getColumnIndexOrThrow(
                    CalendarContract.Instances.CALENDAR_DISPLAY_NAME
                )

                buildList {
                    while (cursor.moveToNext()) {
                        val eventId = cursor.getLong(eventIdIx)
                        val begin = cursor.getLong(beginIx)
                        val end = cursor.getLong(endIx)
                        val allDay = cursor.getInt(allDayIx) == 1

                        val beginDateTime = Instant.ofEpochMilli(begin).atZone(
                            if (allDay) ZoneOffset.UTC else zone
                        )
                        val endDateTime = Instant.ofEpochMilli(end).atZone(
                            if (allDay) ZoneOffset.UTC else zone
                        )

                        add(
                            DaylineItem(
                                id = "android:$eventId:$begin",
                                title = cursor.getString(titleIx)
                                    ?.ifBlank { "Untitled" }
                                    ?: "Untitled",
                                kind = AgendaKind.EVENT,
                                startDate = beginDateTime.toLocalDate(),
                                startTime = if (allDay) null
                                else beginDateTime.toLocalTime(),
                                endTime = if (allDay) null
                                else endDateTime.toLocalTime(),
                                recurrence = Recurrence.ONCE,
                                reminderMinutes = null,
                                focusCycle = FocusCycle.OFF,
                                color = ItemColor.MONO,
                                calendarEventId = eventId,
                                calendarName = cursor.getString(calendarIx),
                                calendarReadOnly = true
                            )
                        )
                    }
                }
            } ?: emptyList()
        }.getOrDefault(emptyList())
    }

    fun mergedItems(
        context: Context,
        local: List<DaylineItem>,
        enabled: Boolean,
        from: LocalDate = LocalDate.now().minusDays(14),
        to: LocalDate = LocalDate.now().plusDays(370)
    ): List<DaylineItem> {
        if (!enabled || !hasReadPermission(context)) return local

        val mappedIds = local.mapNotNull { it.calendarEventId }.toSet()
        val external = loadOccurrences(context, from, to)
            .filterNot { it.calendarEventId in mappedIds }

        return local + external
    }

    /**
     * Publish a Dayline event to the user's first visible writable calendar.
     * Tasks stay Dayline-only.
     */
    fun upsert(
        context: Context,
        item: DaylineItem
    ): DaylineItem {
        if (
            item.kind != AgendaKind.EVENT ||
            item.calendarReadOnly ||
            !hasWritePermission(context)
        ) {
            return item
        }

        val existingId = item.calendarEventId
        val calendarId = if (existingId == null) {
            firstWritableCalendar(context) ?: return item
        } else {
            null
        }

        val values = eventValues(
            item = item,
            calendarId = calendarId
        )

        val eventId = runCatching {
            if (existingId != null) {
                val uri = ContentUris.withAppendedId(
                    CalendarContract.Events.CONTENT_URI,
                    existingId
                )
                context.contentResolver.update(uri, values, null, null)
                existingId
            } else {
                val uri = context.contentResolver.insert(
                    CalendarContract.Events.CONTENT_URI,
                    values
                ) ?: return@runCatching null
                ContentUris.parseId(uri)
            }
        }.getOrNull() ?: return item

        val calendarName = item.calendarName ?: calendarName(context, eventId)

        return item.copy(
            calendarEventId = eventId,
            calendarName = calendarName,
            calendarReadOnly = false
        )
    }

    fun publishExisting(
        context: Context,
        items: List<DaylineItem>
    ): List<DaylineItem> =
        items.map { item ->
            if (item.kind == AgendaKind.EVENT) upsert(context, item) else item
        }

    fun deleteMappedEvent(
        context: Context,
        item: DaylineItem
    ) {
        val eventId = item.calendarEventId ?: return
        if (!hasWritePermission(context) || item.calendarReadOnly) return

        runCatching {
            context.contentResolver.delete(
                ContentUris.withAppendedId(
                    CalendarContract.Events.CONTENT_URI,
                    eventId
                ),
                null,
                null
            )
        }
    }

    private fun firstWritableCalendar(context: Context): Long? {
        val projection = arrayOf(
            CalendarContract.Calendars._ID,
            CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL,
            CalendarContract.Calendars.VISIBLE,
            CalendarContract.Calendars.IS_PRIMARY
        )

        val selection =
            "${CalendarContract.Calendars.VISIBLE}=1 AND " +
            "${CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL}>=?"

        val args = arrayOf(
            CalendarContract.Calendars.CAL_ACCESS_CONTRIBUTOR.toString()
        )

        return runCatching {
            context.contentResolver.query(
                CalendarContract.Calendars.CONTENT_URI,
                projection,
                selection,
                args,
                "${CalendarContract.Calendars.IS_PRIMARY} DESC, " +
                    "${CalendarContract.Calendars._ID} ASC"
            )?.use { cursor ->
                if (cursor.moveToFirst()) cursor.getLong(0) else null
            }
        }.getOrNull()
    }

    private fun eventValues(
        item: DaylineItem,
        calendarId: Long?
    ): ContentValues {
        val values = ContentValues()

        calendarId?.let {
            values.put(CalendarContract.Events.CALENDAR_ID, it)
        }

        values.put(CalendarContract.Events.TITLE, item.title)

        val startTime = item.startTime
        val allDay = startTime == null

        values.put(
            CalendarContract.Events.EVENT_TIMEZONE,
            if (allDay) "UTC" else zone.id
        )
        values.put(
            CalendarContract.Events.ALL_DAY,
            if (allDay) 1 else 0
        )

        val startMillis = if (allDay) {
            item.startDate
                .atStartOfDay(ZoneOffset.UTC)
                .toInstant()
                .toEpochMilli()
        } else {
            item.startDate
                .atTime(startTime!!)
                .atZone(zone)
                .toInstant()
                .toEpochMilli()
        }

        values.put(CalendarContract.Events.DTSTART, startMillis)

        val rrule = recurrenceRule(item)

        if (rrule == null) {
            values.putNull(CalendarContract.Events.RRULE)
            values.putNull(CalendarContract.Events.DURATION)

            val endMillis = if (allDay) {
                item.startDate
                    .plusDays(1)
                    .atStartOfDay(ZoneOffset.UTC)
                    .toInstant()
                    .toEpochMilli()
            } else {
                val end = item.endTime
                    ?.takeIf { it.isAfter(startTime!!) }
                    ?: startTime!!.plusHours(1)

                item.startDate
                    .atTime(end)
                    .atZone(zone)
                    .toInstant()
                    .toEpochMilli()
            }

            values.put(CalendarContract.Events.DTEND, endMillis)
        } else {
            values.put(CalendarContract.Events.RRULE, rrule)
            values.putNull(CalendarContract.Events.DTEND)

            val duration = if (allDay) {
                "P1D"
            } else {
                val end = item.endTime
                    ?.takeIf { it.isAfter(startTime!!) }
                    ?: startTime!!.plusHours(1)
                val seconds = Duration
                    .between(startTime, end)
                    .seconds
                    .coerceAtLeast(60)
                "PT${seconds}S"
            }

            values.put(CalendarContract.Events.DURATION, duration)
        }

        return values
    }

    private fun recurrenceRule(item: DaylineItem): String? =
        when (item.recurrence) {
            Recurrence.ONCE -> null
            Recurrence.DAILY -> "FREQ=DAILY"
            Recurrence.WEEKDAYS -> "FREQ=WEEKLY;BYDAY=MO,TU,WE,TH,FR"
            Recurrence.WEEKLY -> {
                val day = when (item.startDate.dayOfWeek.value) {
                    1 -> "MO"
                    2 -> "TU"
                    3 -> "WE"
                    4 -> "TH"
                    5 -> "FR"
                    6 -> "SA"
                    else -> "SU"
                }
                "FREQ=WEEKLY;BYDAY=$day"
            }
            Recurrence.MONTHLY ->
                "FREQ=MONTHLY;BYMONTHDAY=${item.startDate.dayOfMonth}"
        }

    private fun calendarName(
        context: Context,
        eventId: Long
    ): String? {
        val eventProjection = arrayOf(CalendarContract.Events.CALENDAR_ID)

        val calendarId = runCatching {
            context.contentResolver.query(
                ContentUris.withAppendedId(
                    CalendarContract.Events.CONTENT_URI,
                    eventId
                ),
                eventProjection,
                null,
                null,
                null
            )?.use { cursor ->
                if (cursor.moveToFirst()) cursor.getLong(0) else null
            }
        }.getOrNull() ?: return null

        return runCatching {
            context.contentResolver.query(
                ContentUris.withAppendedId(
                    CalendarContract.Calendars.CONTENT_URI,
                    calendarId
                ),
                arrayOf(CalendarContract.Calendars.CALENDAR_DISPLAY_NAME),
                null,
                null,
                null
            )?.use { cursor ->
                if (cursor.moveToFirst()) cursor.getString(0) else null
            }
        }.getOrNull()
    }
}
