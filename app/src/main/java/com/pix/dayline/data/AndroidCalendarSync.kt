package com.pix.dayline.data

import android.Manifest
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.provider.CalendarContract
import androidx.core.content.ContextCompat
import com.pix.dayline.model.*
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

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


    fun probe(context: Context): Result<Unit> {
        if (!hasReadPermission(context)) {
            return Result.failure(SecurityException("Calendar permission is not granted."))
        }
        return runCatching {
            context.contentResolver.query(
                CalendarContract.Calendars.CONTENT_URI,
                arrayOf(CalendarContract.Calendars._ID),
                null,
                null,
                null
            )?.use { cursor ->
                if (cursor.moveToFirst()) cursor.getLong(0)
            }
            Unit
        }
    }

    fun listCalendars(context: Context): List<DeviceCalendar> {
        if (!hasReadPermission(context)) return emptyList()

        val projection = arrayOf(
            CalendarContract.Calendars._ID,
            CalendarContract.Calendars.CALENDAR_DISPLAY_NAME,
            CalendarContract.Calendars.ACCOUNT_NAME,
            CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL,
            CalendarContract.Calendars.VISIBLE,
            CalendarContract.Calendars.IS_PRIMARY
        )

        return runCatching {
            context.contentResolver.query(
                CalendarContract.Calendars.CONTENT_URI,
                projection,
                "${CalendarContract.Calendars.VISIBLE}=1",
                null,
                "${CalendarContract.Calendars.IS_PRIMARY} DESC, " +
                    "${CalendarContract.Calendars.CALENDAR_DISPLAY_NAME} ASC"
            )?.use { cursor ->
                buildList {
                    while (cursor.moveToNext()) {
                        val access = cursor.getInt(3)
                        add(
                            DeviceCalendar(
                                id = cursor.getLong(0),
                                name = cursor.getString(1)?.ifBlank { "Calendar" }
                                    ?: "Calendar",
                                accountName = cursor.getString(2).orEmpty(),
                                writable = access >= CalendarContract.Calendars.CAL_ACCESS_CONTRIBUTOR,
                                primary = cursor.getInt(5) == 1
                            )
                        )
                    }
                }
            } ?: emptyList()
        }.getOrDefault(emptyList())
    }

    /**
     * Read concrete generated instances so external recurrence rules do not
     * need to be reimplemented inside Dayline.
     */
    fun loadOccurrences(
        context: Context,
        preferences: CalendarPreferences = CalendarPreferences(),
        from: LocalDate = LocalDate.now().minusDays(14),
        to: LocalDate = LocalDate.now().plusDays(370)
    ): List<DaylineItem> {
        if (!hasReadPermission(context)) return emptyList()

        // Editing a concrete instance of an external recurring series through
        // Events would otherwise mutate the master series. Keep those
        // occurrences read-only until Dayline has provider-native exception
        // editing. One-off external events may still be editable per calendar.
        val recurringEventIds = recurringEventIds(context)

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
            CalendarContract.Instances.CALENDAR_DISPLAY_NAME,
            CalendarContract.Instances.CALENDAR_ID
        )

        return runCatching {
            context.contentResolver.query(
                builder.build(),
                projection,
                null,
                null,
                "${CalendarContract.Instances.BEGIN} ASC"
            )?.use { cursor ->
                val eventIdIx = cursor.getColumnIndexOrThrow(CalendarContract.Instances.EVENT_ID)
                val titleIx = cursor.getColumnIndexOrThrow(CalendarContract.Instances.TITLE)
                val beginIx = cursor.getColumnIndexOrThrow(CalendarContract.Instances.BEGIN)
                val endIx = cursor.getColumnIndexOrThrow(CalendarContract.Instances.END)
                val allDayIx = cursor.getColumnIndexOrThrow(CalendarContract.Instances.ALL_DAY)
                val calendarNameIx = cursor.getColumnIndexOrThrow(
                    CalendarContract.Instances.CALENDAR_DISPLAY_NAME
                )
                val calendarIdIx = cursor.getColumnIndexOrThrow(
                    CalendarContract.Instances.CALENDAR_ID
                )

                buildList {
                    while (cursor.moveToNext()) {
                        val calendarId = cursor.getLong(calendarIdIx)
                        if (!preferences.isVisible(calendarId)) continue

                        val eventId = cursor.getLong(eventIdIx)
                        val begin = cursor.getLong(beginIx)
                        val end = cursor.getLong(endIx)
                        val allDay = cursor.getInt(allDayIx) == 1
                        val rule = preferences.ruleFor(calendarId)

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
                                startTime = if (allDay) null else beginDateTime.toLocalTime(),
                                endTime = if (allDay) null else endDateTime.toLocalTime(),
                                recurrence = Recurrence.ONCE,
                                color = rule?.color ?: ItemColor.MONO,
                                spaceId = rule?.spaceId,
                                calendarEventId = eventId,
                                calendarId = calendarId,
                                calendarName = cursor.getString(calendarNameIx),
                                calendarReadOnly = !preferences.isEditable(calendarId) || eventId in recurringEventIds
                            )
                        )
                    }
                }
            } ?: emptyList()
        }.getOrDefault(emptyList())
    }

    /**
     * Reconcile local Dayline events that were previously published to an
     * Android calendar. If that provider event was deleted externally, remove
     * the mapped local event too.
     *
     * Provider failures are treated conservatively: a local event is kept
     * unless the provider explicitly confirms that the row is missing/deleted.
     */
    fun reconcileDeletedMappedItems(
        context: Context,
        local: List<DaylineItem>
    ): List<DaylineItem> {
        if (!hasReadPermission(context)) return local

        val statusCache = mutableMapOf<Long, Boolean?>()

        return local.filter { item ->
            val eventId = item.calendarEventId ?: return@filter true

            val exists = statusCache.getOrPut(eventId) {
                providerEventExists(context, eventId)
            }

            exists != false
        }
    }

    private fun providerEventExists(
        context: Context,
        eventId: Long
    ): Boolean? = runCatching {
        context.contentResolver.query(
            ContentUris.withAppendedId(
                CalendarContract.Events.CONTENT_URI,
                eventId
            ),
            arrayOf(
                CalendarContract.Events._ID,
                CalendarContract.Events.DELETED
            ),
            null,
            null,
            null
        )?.use { cursor ->
            if (!cursor.moveToFirst()) {
                false
            } else {
                val deletedIndex =
                    cursor.getColumnIndex(
                        CalendarContract.Events.DELETED
                    )

                deletedIndex < 0 ||
                    cursor.getInt(deletedIndex) == 0
            }
        } ?: false
    }.getOrNull()

    fun mergedItems(
        context: Context,
        local: List<DaylineItem>,
        enabled: Boolean,
        preferences: CalendarPreferences = CalendarPreferences(),
        from: LocalDate = LocalDate.now().minusDays(14),
        to: LocalDate = LocalDate.now().plusDays(370)
    ): List<DaylineItem> {
        if (!enabled || !hasReadPermission(context)) return local

        val mappedIds = local.mapNotNull { it.calendarEventId }.toSet()
        val external = loadOccurrences(context, preferences, from, to)
            .filterNot { it.calendarEventId in mappedIds }

        return local + external
    }

    fun upsert(
        context: Context,
        item: DaylineItem,
        preferences: CalendarPreferences = CalendarPreferences(),
        spaces: List<DaylineSpace> = emptyList()
    ): DaylineItem {
        if (
            item.kind != AgendaKind.EVENT ||
            item.calendarReadOnly ||
            !hasWritePermission(context)
        ) {
            return item
        }

        val existingId = item.calendarEventId
        val targetCalendarId = item.calendarId
            ?: spaces.firstOrNull { it.id == item.spaceId }?.calendarId
            ?: preferences.defaultCalendarId
            ?: firstWritableCalendar(context)
            ?: return item

        val values = eventValues(
            item = item,
            calendarId = if (existingId == null) targetCalendarId else null
        )

        val eventId = runCatching {
            if (existingId != null) {
                context.contentResolver.update(
                    ContentUris.withAppendedId(
                        CalendarContract.Events.CONTENT_URI,
                        existingId
                    ),
                    values,
                    null,
                    null
                )
                existingId
            } else {
                val uri = context.contentResolver.insert(
                    CalendarContract.Events.CONTENT_URI,
                    values
                ) ?: return@runCatching null
                ContentUris.parseId(uri)
            }
        }.getOrNull() ?: return item

        val resolvedCalendarId = item.calendarId ?: targetCalendarId

        return item.copy(
            calendarEventId = eventId,
            calendarId = resolvedCalendarId,
            calendarName = calendarName(context, resolvedCalendarId),
            calendarReadOnly = false
        )
    }

    fun publishExisting(
        context: Context,
        items: List<DaylineItem>,
        preferences: CalendarPreferences = CalendarPreferences(),
        spaces: List<DaylineSpace> = emptyList()
    ): List<DaylineItem> = items.map { item ->
        if (item.kind == AgendaKind.EVENT) {
            upsert(context, item, preferences, spaces)
        } else {
            item
        }
    }

    fun deleteMappedEvent(context: Context, item: DaylineItem) {
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

    private fun firstWritableCalendar(context: Context): Long? =
        listCalendars(context)
            .sortedWith(compareByDescending<DeviceCalendar> { it.primary }.thenBy { it.name })
            .firstOrNull { it.writable }
            ?.id

    private fun eventValues(item: DaylineItem, calendarId: Long?): ContentValues {
        val values = ContentValues()
        calendarId?.let { values.put(CalendarContract.Events.CALENDAR_ID, it) }

        values.put(CalendarContract.Events.TITLE, item.title)

        val startTime = item.startTime
        val allDay = startTime == null
        values.put(
            CalendarContract.Events.EVENT_TIMEZONE,
            if (allDay) "UTC" else zone.id
        )
        values.put(CalendarContract.Events.ALL_DAY, if (allDay) 1 else 0)

        val startMillis = if (allDay) {
            item.startDate.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        } else {
            item.startDate.atTime(startTime!!).atZone(zone).toInstant().toEpochMilli()
        }
        values.put(CalendarContract.Events.DTSTART, startMillis)

        val rrule = recurrenceRule(item)
        if (rrule == null) {
            values.putNull(CalendarContract.Events.RRULE)
            values.putNull(CalendarContract.Events.EXDATE)
            values.putNull(CalendarContract.Events.DURATION)

            val endMillis = if (allDay) {
                item.startDate.plusDays(1)
                    .atStartOfDay(ZoneOffset.UTC)
                    .toInstant()
                    .toEpochMilli()
            } else {
                val end = item.endTime
                    ?.takeIf { it.isAfter(startTime!!) }
                    ?: startTime!!.plusHours(1)
                item.startDate.atTime(end).atZone(zone).toInstant().toEpochMilli()
            }
            values.put(CalendarContract.Events.DTEND, endMillis)
        } else {
            values.put(CalendarContract.Events.RRULE, rrule)
            values.putNull(CalendarContract.Events.DTEND)

            val exdates = item.excludedDates
                .sorted()
                .joinToString(",") { excludedDate ->
                    val instant = if (allDay) {
                        excludedDate.atStartOfDay(ZoneOffset.UTC).toInstant()
                    } else {
                        excludedDate.atTime(startTime!!).atZone(zone).toInstant()
                    }
                    EXDATE_FORMATTER.format(instant)
                }
            if (exdates.isBlank()) {
                values.putNull(CalendarContract.Events.EXDATE)
            } else {
                values.put(CalendarContract.Events.EXDATE, exdates)
            }

            val duration = if (allDay) {
                "P1D"
            } else {
                val end = item.endTime
                    ?.takeIf { it.isAfter(startTime!!) }
                    ?: startTime!!.plusHours(1)
                val seconds = Duration.between(startTime, end).seconds.coerceAtLeast(60)
                "PT${seconds}S"
            }
            values.put(CalendarContract.Events.DURATION, duration)
        }

        return values
    }

    private fun recurrenceRule(item: DaylineItem): String? {
        val core = when (item.recurrence) {
            Recurrence.ONCE -> return null
            Recurrence.DAILY -> "FREQ=DAILY"
            Recurrence.WEEKDAYS ->
                "FREQ=WEEKLY;BYDAY=MO,TU,WE,TH,FR"
            Recurrence.WEEKENDS ->
                "FREQ=WEEKLY;BYDAY=SA,SU"
            Recurrence.CUSTOM -> {
                val days =
                    item.repeatDays
                        .ifEmpty {
                            setOf(
                                item.startDate
                                    .dayOfWeek
                                    .value
                            )
                        }

                "FREQ=WEEKLY;BYDAY=" +
                    days.toRruleDays()
            }
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
            Recurrence.MONTHLY -> "FREQ=MONTHLY;BYMONTHDAY=${item.startDate.dayOfMonth}"
        }

        val until = item.recurrenceEndDate?.let { endDate ->
            val stamp = endDate.plusDays(1)
                .atStartOfDay(ZoneOffset.UTC)
                .minusSeconds(1)
            ";UNTIL=${stamp.format(DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'"))}"
        }.orEmpty()

        return core + until
    }

    private fun Set<Int>.toRruleDays(): String =
        sorted()
            .mapNotNull { value ->
                when (value) {
                    1 -> "MO"
                    2 -> "TU"
                    3 -> "WE"
                    4 -> "TH"
                    5 -> "FR"
                    6 -> "SA"
                    7 -> "SU"
                    else -> null
                }
            }
            .joinToString(",")

    private fun recurringEventIds(context: Context): Set<Long> {
        if (!hasReadPermission(context)) return emptySet()

        return runCatching {
            context.contentResolver.query(
                CalendarContract.Events.CONTENT_URI,
                arrayOf(CalendarContract.Events._ID),
                "${CalendarContract.Events.RRULE} IS NOT NULL AND ${CalendarContract.Events.RRULE} != ''",
                null,
                null
            )?.use { cursor ->
                buildSet {
                    while (cursor.moveToNext()) add(cursor.getLong(0))
                }
            } ?: emptySet()
        }.getOrDefault(emptySet())
    }

    private val EXDATE_FORMATTER: DateTimeFormatter =
        DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'")
            .withZone(ZoneOffset.UTC)

    private fun calendarName(context: Context, calendarId: Long): String? = runCatching {
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
