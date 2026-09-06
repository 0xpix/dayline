package com.pix.dayline.data

import android.content.Context
import com.pix.dayline.model.*
import org.json.JSONArray
import org.json.JSONObject
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID

class DaylineStore(context: Context) {
    private val prefs = context.getSharedPreferences("dayline", Context.MODE_PRIVATE)

    fun loadItems(): List<DaylineItem> {
        val raw = prefs.getString(KEY_ITEMS, null) ?: return emptyList()
        return runCatching {
            val array = JSONArray(raw)
            buildList {
                for (index in 0 until array.length()) {
                    val json = array.getJSONObject(index)
                    add(itemFromJson(json))
                }
            }
        }.getOrDefault(emptyList())
    }

    fun saveItems(items: List<DaylineItem>) {
        val array = JSONArray()
        items.forEach { array.put(itemToJson(it)) }
        // Commit is intentional: widgets can be refreshed immediately after this call.
        prefs.edit().putString(KEY_ITEMS, array.toString()).commit()
    }

    fun loadSpaces(): List<DaylineSpace> {
        val raw = prefs.getString(KEY_SPACES, null)
        if (raw == null) {
            val defaults = listOf(
                DaylineSpace("personal", "Personal", ItemColor.BLUE),
                DaylineSpace("work", "Work", ItemColor.VIOLET)
            )
            saveSpaces(defaults)
            return defaults
        }

        return runCatching {
            val array = JSONArray(raw)
            buildList {
                for (index in 0 until array.length()) {
                    val json = array.getJSONObject(index)
                    add(
                        DaylineSpace(
                            id = json.getString("id"),
                            name = json.getString("name"),
                            color = enumValue(
                                json.optString("color"),
                                ItemColor.MONO
                            ),
                            calendarId = json.optLong("calendarId", -1L)
                                .takeIf { it >= 0L }
                        )
                    )
                }
            }
        }.getOrDefault(emptyList())
    }

    fun saveSpaces(spaces: List<DaylineSpace>) {
        val array = JSONArray()
        spaces.forEach { space ->
            array.put(
                JSONObject()
                    .put("id", space.id)
                    .put("name", space.name)
                    .put("color", space.color.name)
                    .put("calendarId", space.calendarId ?: -1L)
            )
        }
        prefs.edit().putString(KEY_SPACES, array.toString()).apply()
    }

    fun loadTemplates(): List<EventTemplate> {
        val raw = prefs.getString(KEY_TEMPLATES, null) ?: return defaultTemplates()
        return runCatching {
            val array = JSONArray(raw)
            val loaded = buildList {
                for (index in 0 until array.length()) {
                    val json = array.getJSONObject(index)
                    add(
                        EventTemplate(
                            id = json.optString("id").ifBlank { UUID.randomUUID().toString() },
                            title = json.optString("title"),
                            kind = enumValue(json.optString("kind"), AgendaKind.EVENT),
                            durationMinutes = json.optInt("durationMinutes", 60).coerceIn(15, 24 * 60),
                            startTime = json.optString("startTime")
                                .takeIf { it.isNotBlank() }
                                ?.let(LocalTime::parse),
                            spaceId = json.optString("spaceId").takeIf { it.isNotBlank() },
                            color = enumValue(json.optString("color"), ItemColor.MONO),
                            focusCycle = enumValue(json.optString("focusCycle"), FocusCycle.OFF),
                            customFocusMinutes = json.optInt("customFocusMinutes", 25).coerceIn(5, 180),
                            customBreakMinutes = json.optInt("customBreakMinutes", 5).coerceIn(1, 60),
                            bufferBeforeMinutes = json.optInt("bufferBeforeMinutes", 0).coerceIn(0, 180),
                            bufferAfterMinutes = json.optInt("bufferAfterMinutes", 0).coerceIn(0, 180),
                            priority = enumValue(json.optString("priority"), TaskPriority.NORMAL)
                        )
                    )
                }
            }

            val legacyIds = setOf(
                "template-gym",
                "template-deep-work",
                "template-cs2",
                "template-0x00"
            )

            if (
                loaded.size == legacyIds.size &&
                loaded.map { it.id }.toSet() == legacyIds
            ) {
                defaultTemplates()
            } else {
                loaded
            }
        }.getOrDefault(defaultTemplates())
    }

    fun saveTemplates(templates: List<EventTemplate>) {
        val array = JSONArray()
        templates.forEach { template ->
            array.put(
                JSONObject()
                    .put("id", template.id)
                    .put("title", template.title)
                    .put("kind", template.kind.name)
                    .put("durationMinutes", template.durationMinutes)
                    .put("startTime", template.startTime?.toString().orEmpty())
                    .put("spaceId", template.spaceId.orEmpty())
                    .put("color", template.color.name)
                    .put("focusCycle", template.focusCycle.name)
                    .put("customFocusMinutes", template.customFocusMinutes)
                    .put("customBreakMinutes", template.customBreakMinutes)
                    .put("bufferBeforeMinutes", template.bufferBeforeMinutes)
                    .put("bufferAfterMinutes", template.bufferAfterMinutes)
                    .put("priority", template.priority.name)
            )
        }
        prefs.edit().putString(KEY_TEMPLATES, array.toString()).apply()
    }

    fun loadCalendarPreferences(): CalendarPreferences {
        val raw = prefs.getString(KEY_CALENDAR_PREFS, null) ?: return CalendarPreferences()
        return runCatching {
            val json = JSONObject(raw)
            val rulesArray = json.optJSONArray("rules") ?: JSONArray()
            val rules = buildList {
                for (index in 0 until rulesArray.length()) {
                    val rule = rulesArray.getJSONObject(index)
                    add(
                        CalendarRule(
                            calendarId = rule.getLong("calendarId"),
                            visible = rule.optBoolean("visible", true),
                            editable = rule.optBoolean("editable", false),
                            spaceId = rule.optString("spaceId").takeIf { it.isNotBlank() },
                            color = enumValue(rule.optString("color"), ItemColor.MONO)
                        )
                    )
                }
            }
            CalendarPreferences(
                defaultCalendarId = json.optLong("defaultCalendarId", -1L)
                    .takeIf { it >= 0L },
                rules = rules
            )
        }.getOrDefault(CalendarPreferences())
    }

    fun saveCalendarPreferences(preferences: CalendarPreferences) {
        val rules = JSONArray()
        preferences.rules.forEach { rule ->
            rules.put(
                JSONObject()
                    .put("calendarId", rule.calendarId)
                    .put("visible", rule.visible)
                    .put("editable", rule.editable)
                    .put("spaceId", rule.spaceId.orEmpty())
                    .put("color", rule.color.name)
            )
        }

        prefs.edit().putString(
            KEY_CALENDAR_PREFS,
            JSONObject()
                .put("defaultCalendarId", preferences.defaultCalendarId ?: -1L)
                .put("rules", rules)
                .toString()
        ).apply()
    }

    fun loadWidgetInstancePrefs(appWidgetId: Int): WidgetInstancePrefs {
        val raw = prefs.getString(widgetKey(appWidgetId), null)
            ?: return WidgetInstancePrefs(
                appWidgetId = appWidgetId,
                autoSlideLongTitles = loadWidgetAutoSlide()
            )
        return runCatching {
            val json = JSONObject(raw)
            WidgetInstancePrefs(
                appWidgetId = appWidgetId,
                spaceId = json.optString("spaceId").takeIf { it.isNotBlank() },
                calendarId = json.optLong("calendarId", -1L).takeIf { it >= 0L },
                emoji = json.optString("emoji").takeIf { it.isNotBlank() },
                font = json.optString("font").takeIf { it.isNotBlank() },
                showTasks = json.optBoolean("showTasks", true),
                showEvents = json.optBoolean("showEvents", true),
                backgroundMode = enumValue(
                    json.optString("backgroundMode"),
                    WidgetBackgroundMode.SYSTEM
                ),
                contentMode = enumValue(
                    json.optString("contentMode"),
                    WidgetContentMode.SMART
                ),
                showFocusState = json.optBoolean("showFocusState", true),
                autoSlideLongTitles = json.optBoolean(
                    "autoSlideLongTitles",
                    loadWidgetAutoSlide()
                )
            )
        }.getOrDefault(WidgetInstancePrefs(appWidgetId))
    }

    fun saveWidgetInstancePrefs(value: WidgetInstancePrefs) {
        val json = JSONObject()
            .put("spaceId", value.spaceId.orEmpty())
            .put("calendarId", value.calendarId ?: -1L)
            .put("emoji", value.emoji.orEmpty())
            .put("font", value.font.orEmpty())
            .put("showTasks", value.showTasks)
            .put("showEvents", value.showEvents)
            .put("backgroundMode", value.backgroundMode.name)
            .put("contentMode", value.contentMode.name)
            .put("showFocusState", value.showFocusState)
            .put("autoSlideLongTitles", value.autoSlideLongTitles)

        prefs.edit().putString(widgetKey(value.appWidgetId), json.toString()).apply()
    }

    fun removeWidgetInstancePrefs(appWidgetId: Int) {
        prefs.edit().remove(widgetKey(appWidgetId)).apply()
    }

    fun loadAppearance(): Appearance = enumValue(
        prefs.getString(KEY_APPEARANCE, Appearance.SYSTEM.name),
        Appearance.SYSTEM
    )

    fun saveAppearance(appearance: Appearance) {
        prefs.edit().putString(KEY_APPEARANCE, appearance.name).apply()
    }

    fun loadFontChoice(): FontChoice = enumValue(
        prefs.getString(KEY_FONT, FontChoice.PIXELIFY.name),
        FontChoice.PIXELIFY
    )

    fun saveFontChoice(font: FontChoice) {
        prefs.edit().putString(KEY_FONT, font.name).apply()
    }

    fun loadWidgetFontChoice(): WidgetFontChoice = enumValue(
        prefs.getString(KEY_WIDGET_FONT, WidgetFontChoice.DOT_BOLD.name),
        WidgetFontChoice.DOT_BOLD
    )

    fun saveWidgetFontChoice(font: WidgetFontChoice) {
        prefs.edit().putString(KEY_WIDGET_FONT, font.name).apply()
    }

    fun loadWidgetEmojiChoice(): WidgetEmojiChoice = enumValue(
        prefs.getString(KEY_WIDGET_EMOJI, WidgetEmojiChoice.SMILE.name),
        WidgetEmojiChoice.SMILE
    )

    fun saveWidgetEmojiChoice(emoji: WidgetEmojiChoice) {
        prefs.edit().putString(KEY_WIDGET_EMOJI, emoji.name).apply()
    }

    fun loadWidgetAutoSlide(): Boolean = prefs.getBoolean(KEY_WIDGET_AUTO_SLIDE, true)
    fun saveWidgetAutoSlide(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_WIDGET_AUTO_SLIDE, enabled).apply()
    }

    fun loadNowActivityEnabled(): Boolean = prefs.getBoolean(KEY_NOW_ACTIVITY, true)
    fun saveNowActivityEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_NOW_ACTIVITY, enabled).apply()
    }

    fun loadCalendarSyncEnabled(): Boolean = prefs.getBoolean(KEY_CALENDAR_SYNC, false)
    fun saveCalendarSyncEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_CALENDAR_SYNC, enabled).apply()
    }


    fun loadAutoBetaUpdates(): Boolean = prefs.getBoolean(KEY_AUTO_BETA_UPDATES, false)
    fun saveAutoBetaUpdates(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_AUTO_BETA_UPDATES, enabled).apply()
    }

    fun loadLastUpdateCheckAt(): Long? = prefs.getLong(KEY_LAST_UPDATE_CHECK_AT, -1L)
        .takeIf { it >= 0L }

    fun saveUpdateCheckResult(checkedAtMillis: Long, error: String?) {
        prefs.edit()
            .putLong(KEY_LAST_UPDATE_CHECK_AT, checkedAtMillis)
            .putString(KEY_LAST_UPDATE_CHECK_ERROR, error.orEmpty())
            .apply()
    }

    fun loadLastUpdateCheckError(): String? = prefs
        .getString(KEY_LAST_UPDATE_CHECK_ERROR, null)
        ?.takeIf { it.isNotBlank() }

    fun saveAvailableBetaRelease(release: BetaRelease?) {
        if (release == null) {
            prefs.edit().remove(KEY_AVAILABLE_BETA_RELEASE).apply()
            return
        }
        val json = JSONObject()
            .put("tagName", release.tagName)
            .put("versionName", release.versionName)
            .put("title", release.title)
            .put("notes", release.notes)
            .put("publishedAt", release.publishedAt?.toString().orEmpty())
            .put("htmlUrl", release.htmlUrl)
            .put("apkName", release.apkName.orEmpty())
            .put("apkUrl", release.apkUrl.orEmpty())
            .put("checksumUrl", release.checksumUrl.orEmpty())
        prefs.edit().putString(KEY_AVAILABLE_BETA_RELEASE, json.toString()).apply()
    }

    fun loadAvailableBetaRelease(): BetaRelease? {
        val raw = prefs.getString(KEY_AVAILABLE_BETA_RELEASE, null) ?: return null
        return runCatching {
            val json = JSONObject(raw)
            BetaRelease(
                tagName = json.getString("tagName"),
                versionName = json.getString("versionName"),
                title = json.optString("title"),
                notes = json.optString("notes"),
                publishedAt = json.optString("publishedAt")
                    .takeIf { it.isNotBlank() }
                    ?.let(Instant::parse),
                htmlUrl = json.optString("htmlUrl"),
                apkName = json.optString("apkName").takeIf { it.isNotBlank() },
                apkUrl = json.optString("apkUrl").takeIf { it.isNotBlank() },
                checksumUrl = json.optString("checksumUrl").takeIf { it.isNotBlank() }
            )
        }.getOrNull()
    }

    fun loadLastCalendarSyncAt(): Long? = prefs.getLong(KEY_LAST_CALENDAR_SYNC_AT, -1L)
        .takeIf { it >= 0L }

    fun loadLastCalendarSyncError(): String? = prefs
        .getString(KEY_LAST_CALENDAR_SYNC_ERROR, null)
        ?.takeIf { it.isNotBlank() }

    fun saveCalendarSyncHealth(syncedAtMillis: Long?, error: String?) {
        val editor = prefs.edit()
        if (syncedAtMillis == null) editor.remove(KEY_LAST_CALENDAR_SYNC_AT)
        else editor.putLong(KEY_LAST_CALENDAR_SYNC_AT, syncedAtMillis)
        editor.putString(KEY_LAST_CALENDAR_SYNC_ERROR, error.orEmpty())
        editor.apply()
    }

    fun loadShowOrb(): Boolean = prefs.getBoolean(KEY_SHOW_ORB, true)
    fun saveShowOrb(show: Boolean) {
        prefs.edit().putBoolean(KEY_SHOW_ORB, show).apply()
    }

    fun loadWeekStartsMonday(): Boolean = prefs.getBoolean(KEY_WEEK_STARTS_MONDAY, true)
    fun saveWeekStartsMonday(monday: Boolean) {
        prefs.edit().putBoolean(KEY_WEEK_STARTS_MONDAY, monday).apply()
    }

    /** Existing GitHub installs skip onboarding; fresh installs get the 3-step setup. */
    fun loadOnboardingComplete(): Boolean =
        prefs.getBoolean(KEY_ONBOARDING_COMPLETE, prefs.contains(KEY_ITEMS))

    fun saveOnboardingComplete(complete: Boolean) {
        prefs.edit().putBoolean(KEY_ONBOARDING_COMPLETE, complete).apply()
    }

    fun exportState(): String {
        val values = JSONObject()
        prefs.all.forEach { (key, value) ->
            when (value) {
                is String -> values.put(key, value)
                is Boolean -> values.put(key, value)
                is Int -> values.put(key, value)
                is Long -> values.put(key, value)
                is Float -> values.put(key, value.toDouble())
                is Set<*> -> values.put(key, JSONArray(value.toList()))
            }
        }

        return JSONObject()
            .put("format", "dayline-backup")
            .put("version", 1)
            .put("values", values)
            .toString(2)
    }

    fun importState(raw: String): Boolean = runCatching {
        val root = JSONObject(raw)
        require(root.optString("format") == "dayline-backup")
        val values = root.getJSONObject("values")
        val editor = prefs.edit().clear()
        val keys = values.keys()
        while (keys.hasNext()) {
            val key = keys.next()
            val value = values.get(key)
            when (value) {
                is String -> editor.putString(key, value)
                is Boolean -> editor.putBoolean(key, value)
                is Int -> editor.putInt(key, value)
                is Long -> editor.putLong(key, value)
                is Double -> editor.putFloat(key, value.toFloat())
                is JSONArray -> {
                    val set = buildSet {
                        for (index in 0 until value.length()) {
                            add(value.getString(index))
                        }
                    }
                    editor.putStringSet(key, set)
                }
            }
        }
        editor.commit()
    }.getOrDefault(false)

    private fun itemFromJson(json: JSONObject): DaylineItem {
        val completed = parseDates(json.optJSONArray("completedDates"))
        val excluded = parseDates(json.optJSONArray("excludedDates"))
        val details = buildList {
            val array = json.optJSONArray("details") ?: JSONArray()
            for (index in 0 until array.length()) {
                val detail = array.getJSONObject(index)
                add(
                    TaskDetail(
                        id = detail.optString("id")
                            .ifBlank { UUID.randomUUID().toString() },
                        text = detail.optString("text"),
                        done = detail.optBoolean("done", false)
                    )
                )
            }
        }

        val startTimeRaw = json.optString("startTime")
            .ifBlank { json.optString("time") }

        val startDate =
            LocalDate.parse(
                json.getString("startDate")
            )

        val rawRecurrence =
            json.optString(
                "recurrence",
                Recurrence.ONCE.name
            )

        val storedRepeatDays =
            parseInts(
                json.optJSONArray("repeatDays")
            )

        val recurrence =
            when (rawRecurrence) {
                "SUNDAYS",
                "EXCEPT_SUNDAY" ->
                    Recurrence.CUSTOM

                else ->
                    enumValue(
                        rawRecurrence,
                        Recurrence.ONCE
                    )
            }

        val repeatDays =
            when (rawRecurrence) {
                "SUNDAYS" ->
                    setOf(7)

                "EXCEPT_SUNDAY" ->
                    setOf(1, 2, 3, 4, 5, 6)

                else ->
                    storedRepeatDays
            }

        return DaylineItem(
            id = json.getString("id"),
            title = json.getString("title"),
            kind = enumValue(json.optString("kind"), AgendaKind.EVENT),
            startDate = startDate,
            startTime = startTimeRaw.takeIf { it.isNotBlank() }?.let(LocalTime::parse),
            endTime = json.optString("endTime")
                .takeIf { it.isNotBlank() }
                ?.let(LocalTime::parse),
            recurrence = recurrence,
            repeatDays = repeatDays,
            recurrenceEndDate = json.optString("recurrenceEndDate")
                .takeIf { it.isNotBlank() }
                ?.let(LocalDate::parse),
            excludedDates = excluded,
            seriesParentId = json.optString("seriesParentId").takeIf { it.isNotBlank() },
            reminderMinutes = json.optInt("reminderMinutes", -1).takeIf { it >= 0 },
            focusCycle = enumValue(json.optString("focusCycle"), FocusCycle.OFF),
            customFocusMinutes = json.optInt("customFocusMinutes", 25).coerceIn(5, 180),
            customBreakMinutes = json.optInt("customBreakMinutes", 5).coerceIn(1, 60),
            focusSessionsCompleted = json.optInt("focusSessionsCompleted", 0).coerceAtLeast(0),
            focusedMinutesCompleted = json.optInt("focusedMinutesCompleted", 0).coerceAtLeast(0),
            bufferBeforeMinutes = json.optInt("bufferBeforeMinutes", 0).coerceIn(0, 180),
            bufferAfterMinutes = json.optInt("bufferAfterMinutes", 0).coerceIn(0, 180),
            priority = enumValue(json.optString("priority"), TaskPriority.NORMAL),
            color = enumValue(json.optString("color"), ItemColor.MONO),
            spaceId = json.optString("spaceId").takeIf { it.isNotBlank() },
            details = details,
            completedDates = completed,
            calendarEventId = json.optLong("calendarEventId", -1L).takeIf { it >= 0L },
            calendarId = json.optLong("calendarId", -1L).takeIf { it >= 0L },
            calendarName = json.optString("calendarName").takeIf { it.isNotBlank() },
            calendarReadOnly = json.optBoolean("calendarReadOnly", false)
        )
    }

    private fun itemToJson(item: DaylineItem): JSONObject {
        val details = JSONArray()
        item.details.forEach { detail ->
            details.put(
                JSONObject()
                    .put("id", detail.id)
                    .put("text", detail.text)
                    .put("done", detail.done)
            )
        }

        return JSONObject()
            .put("id", item.id)
            .put("title", item.title)
            .put("kind", item.kind.name)
            .put("startDate", item.startDate.toString())
            .put("startTime", item.startTime?.toString().orEmpty())
            .put("endTime", item.endTime?.toString().orEmpty())
            .put("recurrence", item.recurrence.name)
            .put("repeatDays", intsArray(item.repeatDays))
            .put("recurrenceEndDate", item.recurrenceEndDate?.toString().orEmpty())
            .put("excludedDates", datesArray(item.excludedDates))
            .put("seriesParentId", item.seriesParentId.orEmpty())
            .put("reminderMinutes", item.reminderMinutes ?: -1)
            .put("focusCycle", item.focusCycle.name)
            .put("customFocusMinutes", item.customFocusMinutes)
            .put("customBreakMinutes", item.customBreakMinutes)
            .put("focusSessionsCompleted", item.focusSessionsCompleted)
            .put("focusedMinutesCompleted", item.focusedMinutesCompleted)
            .put("bufferBeforeMinutes", item.bufferBeforeMinutes)
            .put("bufferAfterMinutes", item.bufferAfterMinutes)
            .put("priority", item.priority.name)
            .put("color", item.color.name)
            .put("spaceId", item.spaceId.orEmpty())
            .put("details", details)
            .put("completedDates", datesArray(item.completedDates))
            .put("calendarEventId", item.calendarEventId ?: -1L)
            .put("calendarId", item.calendarId ?: -1L)
            .put("calendarName", item.calendarName.orEmpty())
            .put("calendarReadOnly", item.calendarReadOnly)
    }

    private fun datesArray(
        dates: Set<LocalDate>
    ): JSONArray =
        JSONArray().apply {
            dates.sorted().forEach {
                put(it.toString())
            }
        }

    private fun intsArray(
        values: Set<Int>
    ): JSONArray =
        JSONArray().apply {
            values.sorted().forEach(::put)
        }

    private fun parseInts(
        array: JSONArray?
    ): Set<Int> = buildSet {
        val source = array ?: JSONArray()

        for (index in 0 until source.length()) {
            val value = source.optInt(index, -1)

            if (value in 1..7) {
                add(value)
            }
        }
    }

    private fun parseDates(array: JSONArray?): Set<LocalDate> = buildSet {
        val source = array ?: JSONArray()
        for (index in 0 until source.length()) {
            runCatching { LocalDate.parse(source.getString(index)) }
                .getOrNull()
                ?.let(::add)
        }
    }

    private fun defaultTemplates(): List<EventTemplate> = listOf(
        EventTemplate(
            id = "template-meeting",
            title = "Meeting",
            durationMinutes = 60,
            color = ItemColor.BLUE
        ),
        EventTemplate(
            id = "template-focus",
            title = "Focus block",
            durationMinutes = 90,
            color = ItemColor.VIOLET,
            focusCycle = FocusCycle.FOCUS_50_10
        ),
        EventTemplate(
            id = "template-workout",
            title = "Workout",
            durationMinutes = 60,
            color = ItemColor.SAGE
        ),
        EventTemplate(
            id = "template-appointment",
            title = "Appointment",
            durationMinutes = 60,
            color = ItemColor.AMBER
        ),
        EventTemplate(
            id = "template-errand",
            title = "Errand",
            durationMinutes = 45,
            color = ItemColor.ROSE
        )
    )

    private fun widgetKey(appWidgetId: Int): String = "widget_instance_$appWidgetId"

    private inline fun <reified T : Enum<T>> enumValue(value: String?, fallback: T): T =
        runCatching { enumValueOf<T>(value.orEmpty()) }.getOrDefault(fallback)

    companion object {
        private const val KEY_ITEMS = "items"
        private const val KEY_SPACES = "spaces"
        private const val KEY_TEMPLATES = "templates"
        private const val KEY_CALENDAR_PREFS = "calendar_prefs"
        private const val KEY_APPEARANCE = "appearance"
        private const val KEY_FONT = "font"
        private const val KEY_WIDGET_FONT = "widget_font"
        private const val KEY_WIDGET_EMOJI = "widget_emoji"
        private const val KEY_WIDGET_AUTO_SLIDE = "widget_auto_slide"
        private const val KEY_NOW_ACTIVITY = "now_activity"
        private const val KEY_CALENDAR_SYNC = "calendar_sync"

        private const val KEY_AUTO_BETA_UPDATES = "auto_beta_updates"
        private const val KEY_LAST_UPDATE_CHECK_AT = "last_update_check_at"
        private const val KEY_LAST_UPDATE_CHECK_ERROR = "last_update_check_error"
        private const val KEY_AVAILABLE_BETA_RELEASE = "available_beta_release"
        private const val KEY_LAST_CALENDAR_SYNC_AT = "last_calendar_sync_at"
        private const val KEY_LAST_CALENDAR_SYNC_ERROR = "last_calendar_sync_error"
        private const val KEY_SHOW_ORB = "show_orb"
        private const val KEY_WEEK_STARTS_MONDAY = "week_starts_monday"
        private const val KEY_ONBOARDING_COMPLETE = "onboarding_complete"
    }
}

enum class Appearance { SYSTEM, LIGHT, DARK }
enum class FontChoice { SYSTEM, GEIST, INTER, SPACE_GROTESK, IBM_PLEX_MONO, PIXELIFY, GEIST_PIXEL }
enum class WidgetFontChoice { DOT_BOLD, DOT_FINE, MONO }

enum class WidgetEmojiChoice {
    SMILE,
    GRIN,
    WINK,
    COOL,
    NERD,
    PARTY,
    SLEEPY,
    MELT,
    GHOST,
    ROBOT,
    RELAXED,
    HEART_EYES
}
