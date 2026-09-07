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

    private data class ProviderSnapshot(
        val title: String,
        val startDate: LocalDate,
        val startTime: java.time.LocalTime?,
        val endTime: java.time.LocalTime?,
        val recurrence: Recurrence,
        val repeatDays: Set<Int>,
        val recurrenceEndDate: LocalDate?,
        val excludedDates: Set<LocalDate>,
        val calendarId: Long,
        val calendarName: String?
    )

    private sealed interface ProviderState {
        data class Present(val snapshot: ProviderSnapshot) : ProviderState
        data object Missing : ProviderState
        data object Unknown : ProviderState
    }

    /**
     * Reconcile every Dayline event that has a provider mapping.
     *
     * This is intentionally two-way. Dayline writes local edits through upsert(),
     * while provider edits flow back here through the Calendar ContentObserver:
     * title, date/time, all-day state, recurrence, exclusions and calendar moves
     * are pulled into the mapped Dayline row. Provider-side deletion removes the
     * mapped local row. Query failures remain conservative and keep local data.
     *
     * The historical method name is retained so older call sites stay source
     * compatible even though it now reconciles changes as well as deletions.
     */
    fun reconcileDeletedMappedItems(
        context: Context,
        local: List<DaylineItem>
    ): List<DaylineItem> {
        if (!hasReadPermission(context)) return local

        val cache = mutableMapOf<Long, ProviderState>()
        return local.mapNotNull { item ->
            val eventId = item.calendarEventId ?: return@mapNotNull item
            when (val state = cache.getOrPut(eventId) { providerState(context, eventId) }) {
                ProviderState.Missing -> null
                ProviderState.Unknown -> item
                is ProviderState.Present -> {
                    val snapshot = state.snapshot
                    item.copy(
                        title = snapshot.title,
                        startDate = snapshot.startDate,
                        startTime = snapshot.startTime,
                        endTime = snapshot.endTime,
                        recurrence = snapshot.recurrence,
                        repeatDays = snapshot.repeatDays,
                        recurrenceEndDate = snapshot.recurrenceEndDate,
                        excludedDates = snapshot.excludedDates,
                        calendarId = snapshot.calendarId,
                        calendarName = snapshot.calendarName
                    )
                }
            }
        }
    }

    private fun providerState(context: Context, eventId: Long): ProviderState = runCatching {
        val uri = ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, eventId)
        val projection = arrayOf(
            CalendarContract.Events._ID,
            CalendarContract.Events.TITLE,
            CalendarContract.Events.DTSTART,
            CalendarContract.Events.DTEND,
            CalendarContract.Events.DURATION,
            CalendarContract.Events.ALL_DAY,
            CalendarContract.Events.CALENDAR_ID,
            CalendarContract.Events.RRULE,
            CalendarContract.Events.EXDATE,
            CalendarContract.Events.DELETED
        )

        context.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
            if (!cursor.moveToFirst()) return@use ProviderState.Missing

            val deletedIx = cursor.getColumnIndex(CalendarContract.Events.DELETED)
            if (deletedIx >= 0 && cursor.getInt(deletedIx) != 0) {
                return@use ProviderState.Missing
            }

            val title = cursor.getString(cursor.getColumnIndexOrThrow(CalendarContract.Events.TITLE))
                ?.ifBlank { "Untitled" }
                ?: "Untitled"
            val startMillis = cursor.getLong(cursor.getColumnIndexOrThrow(CalendarContract.Events.DTSTART))
            val dtEndIx = cursor.getColumnIndex(CalendarContract.Events.DTEND)
            val durationIx = cursor.getColumnIndex(CalendarContract.Events.DURATION)
            val allDay = cursor.getInt(cursor.getColumnIndexOrThrow(CalendarContract.Events.ALL_DAY)) == 1
            val calendarId = cursor.getLong(cursor.getColumnIndexOrThrow(CalendarContract.Events.CALENDAR_ID))
            val rrule = cursor.getString(cursor.getColumnIndexOrThrow(CalendarContract.Events.RRULE))
            val exdate = cursor.getString(cursor.getColumnIndexOrThrow(CalendarContract.Events.EXDATE))

            val startZoned = Instant.ofEpochMilli(startMillis).atZone(if (allDay) ZoneOffset.UTC else zone)
            val endMillis = when {
                dtEndIx >= 0 && !cursor.isNull(dtEndIx) -> cursor.getLong(dtEndIx)
                durationIx >= 0 && !cursor.isNull(durationIx) -> {
                    val duration = runCatching { Duration.parse(cursor.getString(durationIx)) }.getOrNull()
                    startMillis + (duration?.toMillis() ?: if (allDay) 86_400_000L else 3_600_000L)
                }
                else -> startMillis + if (allDay) 86_400_000L else 3_600_000L
            }
            val endZoned = Instant.ofEpochMilli(endMillis).atZone(if (allDay) ZoneOffset.UTC else zone)
            val recurrenceInfo = parseRecurrence(rrule, startZoned.toLocalDate())

            ProviderState.Present(
                ProviderSnapshot(
                    title = title,
                    startDate = startZoned.toLocalDate(),
                    startTime = if (allDay) null else startZoned.toLocalTime(),
                    endTime = if (allDay) null else endZoned.toLocalTime(),
                    recurrence = recurrenceInfo.first,
                    repeatDays = recurrenceInfo.second,
                    recurrenceEndDate = parseUntil(rrule),
                    excludedDates = parseExdates(exdate, allDay),
                    calendarId = calendarId,
                    calendarName = calendarName(context, calendarId)
                )
            )
        } ?: ProviderState.Unknown
    }.getOrElse { ProviderState.Unknown }

    private fun parseRecurrence(rrule: String?, startDate: LocalDate): Pair<Recurrence, Set<Int>> {
        if (rrule.isNullOrBlank()) return Recurrence.ONCE to emptySet()
        val upper = rrule.uppercase()
        val byDay = Regex("(?:^|;)BYDAY=([^;]+)")
            .find(upper)
            ?.groupValues
            ?.getOrNull(1)
            ?.split(',')
            ?.mapNotNull(::rruleDayNumber)
            ?.toSet()
            .orEmpty()

        return when {
            "FREQ=DAILY" in upper -> Recurrence.DAILY to emptySet()
            "FREQ=MONTHLY" in upper -> Recurrence.MONTHLY to emptySet()
            "FREQ=WEEKLY" in upper && byDay == setOf(1, 2, 3, 4, 5) ->
                Recurrence.WEEKDAYS to emptySet()
            "FREQ=WEEKLY" in upper && byDay == setOf(6, 7) ->
                Recurrence.WEEKENDS to emptySet()
            "FREQ=WEEKLY" in upper && byDay.size == 1 && byDay.first() == startDate.dayOfWeek.value ->
                Recurrence.WEEKLY to emptySet()
            "FREQ=WEEKLY" in upper ->
                Recurrence.CUSTOM to byDay.ifEmpty { setOf(startDate.dayOfWeek.value) }
            else -> Recurrence.ONCE to emptySet()
        }
    }

    private fun rruleDayNumber(raw: String): Int? = when (raw.takeLast(2)) {
        "MO" -> 1
        "TU" -> 2
        "WE" -> 3
        "TH" -> 4
        "FR" -> 5
        "SA" -> 6
        "SU" -> 7
        else -> null
    }

    private fun parseUntil(rrule: String?): LocalDate? {
        if (rrule.isNullOrBlank()) return null
        val raw = Regex("(?:^|;)UNTIL=([^;]+)")
            .find(rrule.uppercase())
            ?.groupValues
            ?.getOrNull(1)
            ?: return null

        return runCatching {
            when {
                raw.length >= 16 && raw.endsWith("Z") ->
                    Instant.from(DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmssX").parse(raw))
                        .atZone(zone)
                        .toLocalDate()
                raw.length >= 8 -> LocalDate.parse(raw.take(8), DateTimeFormatter.BASIC_ISO_DATE)
                else -> null
            }
        }.getOrNull()
    }

    private fun parseExdates(raw: String?, allDay: Boolean): Set<LocalDate> {
        if (raw.isNullOrBlank()) return emptySet()
        return raw.split(',').mapNotNullTo(mutableSetOf()) { token ->
            val clean = token.substringAfter(':').trim()
            runCatching {
                when {
                    clean.length >= 16 && clean.endsWith("Z") ->
                        Instant.from(DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmssX").parse(clean))
                            .atZone(if (allDay) ZoneOffset.UTC else zone)
                            .toLocalDate()
                    clean.length >= 8 -> LocalDate.parse(clean.take(8), DateTimeFormatter.BASIC_ISO_DATE)
                    else -> null
                }
            }.getOrNull()
        }
    }

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
