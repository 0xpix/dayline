package com.pix.dayline.data

import android.content.Context
import com.pix.dayline.data.room.DaylineRoomRepository
import com.pix.dayline.model.*
import org.json.JSONArray
import org.json.JSONObject
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID

class DaylineStore(context: Context) {
    private val prefs = context.getSharedPreferences("dayline", Context.MODE_PRIVATE)
    private val roomRepository = DaylineRoomRepository.get(context.applicationContext)

    @Volatile
    private var roomInitialized = false

    fun loadItems(): List<DaylineItem> {
        ensureRoomInitialized()
        return roomRepository.loadItems()
    }

    fun saveItems(items: List<DaylineItem>) {
        ensureRoomInitialized()
        val payload = itemsToLegacyJson(items)
        prefs.edit().putString(KEY_PENDING_ROOM_ITEMS, payload).apply()
        roomRepository.replaceItems(
            items = items,
            onPersisted = {
                clearPendingRoomWrite(KEY_PENDING_ROOM_ITEMS, payload)
            },
            onFailure = { error ->
                recordRoomWriteFailure("items", error)
            }
        )
    }

    fun loadSpaces(): List<DaylineSpace> {
        ensureRoomInitialized()
        return roomRepository.loadSpaces()
    }

    fun saveSpaces(spaces: List<DaylineSpace>) {
        ensureRoomInitialized()
        val payload = spacesToLegacyJson(spaces)
        prefs.edit().putString(KEY_PENDING_ROOM_SPACES, payload).apply()
        roomRepository.replaceSpaces(
            spaces = spaces,
            onPersisted = {
                clearPendingRoomWrite(KEY_PENDING_ROOM_SPACES, payload)
            },
            onFailure = { error ->
                recordRoomWriteFailure("spaces", error)
            }
        )
    }

    fun loadTemplates(): List<EventTemplate> {
        val raw = prefs.getString(KEY_TEMPLATES, null) ?: return defaultTemplates()
        val array = runCatching { JSONArray(raw) }.getOrNull() ?: return defaultTemplates()

        val loaded = buildList {
            for (index in 0 until array.length()) {
                val template = runCatching {
                    val json = array.getJSONObject(index)
                    EventTemplate(
                        id = json.optString("id").ifBlank { UUID.randomUUID().toString() },
                        title = json.optString("title"),
                        kind = enumValue(json.optString("kind"), AgendaKind.EVENT),
                        durationMinutes = json.optInt("durationMinutes", 60).coerceIn(15, 24 * 60),
                        startTime = json.optString("startTime").takeIf { it.isNotBlank() }?.let(LocalTime::parse),
                        spaceId = json.optString("spaceId").takeIf { it.isNotBlank() },
                        color = enumValue(json.optString("color"), ItemColor.MONO),
                        focusCycle = enumValue(json.optString("focusCycle"), FocusCycle.OFF),
                        customFocusMinutes = json.optInt("customFocusMinutes", 25).coerceIn(5, 180),
                        customBreakMinutes = json.optInt("customBreakMinutes", 5).coerceIn(1, 60),
                        bufferBeforeMinutes = json.optInt("bufferBeforeMinutes", 0).coerceIn(0, 180),
                        bufferAfterMinutes = json.optInt("bufferAfterMinutes", 0).coerceIn(0, 180),
                        priority = enumValue(json.optString("priority"), TaskPriority.NORMAL)
                    )
                }.getOrNull()
                if (template != null) add(template)
            }
        }

        val legacyIds = setOf("template-gym", "template-deep-work", "template-cs2", "template-0x00")
        return if (loaded.size == legacyIds.size && loaded.map { it.id }.toSet() == legacyIds) {
            defaultTemplates()
        } else {
            loaded
        }
    }

    fun saveTemplates(templates: List<EventTemplate>) {
        val array = JSONArray()
        templates.forEach { template ->
            array.put(
                JSONObject().put("id", template.id).put("title", template.title).put("kind", template.kind.name)
                    .put("durationMinutes", template.durationMinutes).put("startTime", template.startTime?.toString().orEmpty())
                    .put("spaceId", template.spaceId.orEmpty()).put("color", template.color.name)
                    .put("focusCycle", template.focusCycle.name).put("customFocusMinutes", template.customFocusMinutes)
                    .put("customBreakMinutes", template.customBreakMinutes).put("bufferBeforeMinutes", template.bufferBeforeMinutes)
                    .put("bufferAfterMinutes", template.bufferAfterMinutes).put("priority", template.priority.name)
            )
        }
        prefs.edit().putString(KEY_TEMPLATES, array.toString()).apply()
    }

    fun loadCalendarPreferences(): CalendarPreferences {
        val raw = prefs.getString(KEY_CALENDAR_PREFS, null) ?: return CalendarPreferences()
        val json = runCatching { JSONObject(raw) }.getOrNull() ?: return CalendarPreferences()
        val rulesArray = json.optJSONArray("rules") ?: JSONArray()
        val rules = buildList {
            for (index in 0 until rulesArray.length()) {
                val rule = runCatching {
                    val value = rulesArray.getJSONObject(index)
                    CalendarRule(
                        calendarId = value.getLong("calendarId"),
                        visible = value.optBoolean("visible", true),
                        editable = value.optBoolean("editable", false),
                        spaceId = value.optString("spaceId").takeIf { it.isNotBlank() },
                        color = enumValue(value.optString("color"), ItemColor.MONO)
                    )
                }.getOrNull()
                if (rule != null) add(rule)
            }
        }
        return CalendarPreferences(
            defaultCalendarId = json.optLong("defaultCalendarId", -1L).takeIf { it >= 0L },
            rules = rules
        )
    }

    fun saveCalendarPreferences(preferences: CalendarPreferences) {
        val rules = JSONArray()
        preferences.rules.forEach { rule ->
            rules.put(JSONObject().put("calendarId", rule.calendarId).put("visible", rule.visible)
                .put("editable", rule.editable).put("spaceId", rule.spaceId.orEmpty()).put("color", rule.color.name))
        }
        prefs.edit().putString(KEY_CALENDAR_PREFS,
            JSONObject().put("defaultCalendarId", preferences.defaultCalendarId ?: -1L).put("rules", rules).toString()
        ).apply()
    }

    fun loadWidgetInstancePrefs(appWidgetId: Int): WidgetInstancePrefs {
        val raw = prefs.getString(widgetKey(appWidgetId), null)
            ?: return WidgetInstancePrefs(appWidgetId = appWidgetId, autoSlideLongTitles = loadWidgetAutoSlide())
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
                backgroundMode = enumValue(json.optString("backgroundMode"), WidgetBackgroundMode.SYSTEM),
                contentMode = enumValue(json.optString("contentMode"), WidgetContentMode.SMART),
                showFocusState = json.optBoolean("showFocusState", true),
                autoSlideLongTitles = json.optBoolean("autoSlideLongTitles", loadWidgetAutoSlide())
            )
        }.getOrDefault(WidgetInstancePrefs(appWidgetId))
    }

    fun saveWidgetInstancePrefs(value: WidgetInstancePrefs) {
        val json = JSONObject().put("spaceId", value.spaceId.orEmpty()).put("calendarId", value.calendarId ?: -1L)
            .put("emoji", value.emoji.orEmpty()).put("font", value.font.orEmpty()).put("showTasks", value.showTasks)
            .put("showEvents", value.showEvents).put("backgroundMode", value.backgroundMode.name)
            .put("contentMode", value.contentMode.name).put("showFocusState", value.showFocusState)
            .put("autoSlideLongTitles", value.autoSlideLongTitles)
        prefs.edit().putString(widgetKey(value.appWidgetId), json.toString()).apply()
    }

    fun removeWidgetInstancePrefs(appWidgetId: Int) { prefs.edit().remove(widgetKey(appWidgetId)).apply() }

    fun loadAppearance(): Appearance = enumValue(prefs.getString(KEY_APPEARANCE, Appearance.SYSTEM.name), Appearance.SYSTEM)
    fun saveAppearance(appearance: Appearance) { prefs.edit().putString(KEY_APPEARANCE, appearance.name).apply() }
    fun loadFontChoice(): FontChoice = enumValue(prefs.getString(KEY_FONT, FontChoice.PIXELIFY.name), FontChoice.PIXELIFY)
    fun saveFontChoice(font: FontChoice) { prefs.edit().putString(KEY_FONT, font.name).apply() }
    fun loadWidgetFontChoice(): WidgetFontChoice = enumValue(prefs.getString(KEY_WIDGET_FONT, WidgetFontChoice.DOT_BOLD.name), WidgetFontChoice.DOT_BOLD)
    fun saveWidgetFontChoice(font: WidgetFontChoice) { prefs.edit().putString(KEY_WIDGET_FONT, font.name).apply() }
    fun loadWidgetEmojiChoice(): WidgetEmojiChoice = enumValue(prefs.getString(KEY_WIDGET_EMOJI, WidgetEmojiChoice.SMILE.name), WidgetEmojiChoice.SMILE)
    fun saveWidgetEmojiChoice(emoji: WidgetEmojiChoice) { prefs.edit().putString(KEY_WIDGET_EMOJI, emoji.name).apply() }
    fun loadWidgetAutoSlide(): Boolean = prefs.getBoolean(KEY_WIDGET_AUTO_SLIDE, true)
    fun saveWidgetAutoSlide(enabled: Boolean) { prefs.edit().putBoolean(KEY_WIDGET_AUTO_SLIDE, enabled).apply() }
    fun loadNowActivityEnabled(): Boolean = prefs.getBoolean(KEY_NOW_ACTIVITY, true)
    fun saveNowActivityEnabled(enabled: Boolean) { prefs.edit().putBoolean(KEY_NOW_ACTIVITY, enabled).apply() }
    fun loadCalendarSyncEnabled(): Boolean = prefs.getBoolean(KEY_CALENDAR_SYNC, false)
    fun saveCalendarSyncEnabled(enabled: Boolean) { prefs.edit().putBoolean(KEY_CALENDAR_SYNC, enabled).apply() }
    fun loadAutoBetaUpdates(): Boolean = prefs.getBoolean(KEY_AUTO_BETA_UPDATES, false)
    fun saveAutoBetaUpdates(enabled: Boolean) { prefs.edit().putBoolean(KEY_AUTO_BETA_UPDATES, enabled).apply() }

    fun loadLastLaunchedVersion(): String? =
        prefs.getString(KEY_LAST_LAUNCHED_VERSION, null)?.takeIf { it.isNotBlank() }

    fun saveLastLaunchedVersion(version: String) {
        prefs.edit().putString(KEY_LAST_LAUNCHED_VERSION, version).apply()
    }

    fun loadPendingWhatsNewFromVersion(): String? =
        prefs.getString(KEY_PENDING_WHATS_NEW_FROM_VERSION, null)?.takeIf { it.isNotBlank() }

    fun savePendingWhatsNewFromVersion(version: String?) {
        val editor = prefs.edit()
        if (version.isNullOrBlank()) editor.remove(KEY_PENDING_WHATS_NEW_FROM_VERSION)
        else editor.putString(KEY_PENDING_WHATS_NEW_FROM_VERSION, version)
        editor.apply()
    }

    fun loadLastUpdateCheckAt(): Long? = prefs.getLong(KEY_LAST_UPDATE_CHECK_AT, -1L).takeIf { it >= 0L }
    fun saveUpdateCheckResult(checkedAtMillis: Long, error: String?) {
        prefs.edit().putLong(KEY_LAST_UPDATE_CHECK_AT, checkedAtMillis)
            .putString(KEY_LAST_UPDATE_CHECK_ERROR, error.orEmpty()).apply()
    }
    fun loadLastUpdateCheckError(): String? = prefs.getString(KEY_LAST_UPDATE_CHECK_ERROR, null)?.takeIf { it.isNotBlank() }

    fun saveAvailableBetaRelease(release: BetaRelease?) {
        if (release == null) {
            prefs.edit().remove(KEY_AVAILABLE_BETA_RELEASE).apply(); return
        }
        val json = JSONObject().put("tagName", release.tagName).put("versionName", release.versionName)
            .put("title", release.title).put("notes", release.notes).put("publishedAt", release.publishedAt?.toString().orEmpty())
            .put("htmlUrl", release.htmlUrl).put("apkName", release.apkName.orEmpty()).put("apkUrl", release.apkUrl.orEmpty())
            .put("checksumUrl", release.checksumUrl.orEmpty()).put("apkSizeBytes", release.apkSizeBytes ?: -1L)
        prefs.edit().putString(KEY_AVAILABLE_BETA_RELEASE, json.toString()).apply()
    }

    fun loadAvailableBetaRelease(): BetaRelease? {
        val raw = prefs.getString(KEY_AVAILABLE_BETA_RELEASE, null) ?: return null
        return runCatching {
            val json = JSONObject(raw)
            BetaRelease(
                tagName = json.getString("tagName"), versionName = json.getString("versionName"),
                title = json.optString("title"), notes = json.optString("notes"),
                publishedAt = json.optString("publishedAt").takeIf { it.isNotBlank() }?.let(Instant::parse),
                htmlUrl = json.optString("htmlUrl"), apkName = json.optString("apkName").takeIf { it.isNotBlank() },
                apkUrl = json.optString("apkUrl").takeIf { it.isNotBlank() }, checksumUrl = json.optString("checksumUrl").takeIf { it.isNotBlank() },
                apkSizeBytes = json.optLong("apkSizeBytes", -1L).takeIf { it >= 0L }
            )
        }.getOrNull()
    }

    fun loadLastCalendarSyncAt(): Long? = prefs.getLong(KEY_LAST_CALENDAR_SYNC_AT, -1L).takeIf { it >= 0L }
    fun loadLastCalendarSyncError(): String? = prefs.getString(KEY_LAST_CALENDAR_SYNC_ERROR, null)?.takeIf { it.isNotBlank() }
    fun saveCalendarSyncHealth(syncedAtMillis: Long?, error: String?) {
        val editor = prefs.edit()
        if (syncedAtMillis == null) editor.remove(KEY_LAST_CALENDAR_SYNC_AT) else editor.putLong(KEY_LAST_CALENDAR_SYNC_AT, syncedAtMillis)
        editor.putString(KEY_LAST_CALENDAR_SYNC_ERROR, error.orEmpty()).apply()
    }

    fun loadLastWidgetRefreshAt(): Long? = prefs.getLong(KEY_LAST_WIDGET_REFRESH_AT, -1L).takeIf { it >= 0L }
    fun saveWidgetRefreshAt(epochMillis: Long = System.currentTimeMillis()) {
        prefs.edit().putLong(KEY_LAST_WIDGET_REFRESH_AT, epochMillis).apply()
    }

    fun loadGlyphPreferences(): GlyphPreferences {
        val defaults = GlyphPreferences()
        val raw = prefs.getString(KEY_GLYPH_PREFERENCES, null) ?: return defaults

        val loaded = runCatching {
            val json = JSONObject(raw)
            GlyphPreferences(
                mode = enumValue(json.optString("mode"), defaults.mode),
                idleExpression = enumValue(json.optString("idleExpression"), defaults.idleExpression),
                blinkEnabled = json.optBoolean("blinkEnabled", defaults.blinkEnabled),
                randomGlancesEnabled = json.optBoolean("randomGlancesEnabled", defaults.randomGlancesEnabled),
                glanceFrequency = enumValue(json.optString("glanceFrequency"), defaults.glanceFrequency),
                showAppStates = json.optBoolean("showAppStates", defaults.showAppStates),
                stateDurationSeconds = json.optInt("stateDurationSeconds", defaults.stateDurationSeconds).coerceIn(2, 10),
                returnToEyes = json.optBoolean("returnToEyes", defaults.returnToEyes),
                reminderFlashSeconds = json.optInt("reminderFlashSeconds", defaults.reminderFlashSeconds).coerceIn(2, 10),
                focusStyle = enumValue(json.optString("focusStyle"), defaults.focusStyle),
                restAnimation = json.optBoolean("restAnimation", defaults.restAnimation),
                quietHoursEnabled = json.optBoolean("quietHoursEnabled", defaults.quietHoursEnabled),
                quietStart = json.optString("quietStart", defaults.quietStart.toString()).let(LocalTime::parse),
                quietEnd = json.optString("quietEnd", defaults.quietEnd.toString()).let(LocalTime::parse),
                dimAtNight = json.optBoolean("dimAtNight", defaults.dimAtNight),
                reduceMotion = json.optBoolean("reduceMotion", defaults.reduceMotion)
            )
        }.getOrDefault(defaults)

        // EYES_AND_STATES/showAppStates/restAnimation only exist for loading old
        // preference JSON. Current Dayline is eyes-first, so upgraded installs
        // must behave exactly like fresh installs without requiring Settings to
        // be opened once to normalize those legacy flags.
        val normalized = loaded.normalizedForCurrentGlyph()
        if (normalized != loaded) saveGlyphPreferences(normalized)
        return normalized
    }

    fun saveGlyphPreferences(value: GlyphPreferences) {
        val json = JSONObject().put("mode", value.mode.name).put("idleExpression", value.idleExpression.name)
            .put("blinkEnabled", value.blinkEnabled).put("randomGlancesEnabled", value.randomGlancesEnabled)
            .put("glanceFrequency", value.glanceFrequency.name).put("showAppStates", value.showAppStates)
            .put("stateDurationSeconds", value.stateDurationSeconds.coerceIn(2, 10)).put("returnToEyes", value.returnToEyes)
            .put("reminderFlashSeconds", value.reminderFlashSeconds.coerceIn(2, 10)).put("focusStyle", value.focusStyle.name)
            .put("restAnimation", value.restAnimation).put("quietHoursEnabled", value.quietHoursEnabled)
            .put("quietStart", value.quietStart.toString()).put("quietEnd", value.quietEnd.toString())
            .put("dimAtNight", value.dimAtNight).put("reduceMotion", value.reduceMotion)
        prefs.edit().putString(KEY_GLYPH_PREFERENCES, json.toString()).apply()
    }

    fun loadShowOrb(): Boolean = prefs.getBoolean(KEY_SHOW_ORB, true)
    fun saveShowOrb(show: Boolean) { prefs.edit().putBoolean(KEY_SHOW_ORB, show).apply() }
    fun loadWeekStartsMonday(): Boolean = prefs.getBoolean(KEY_WEEK_STARTS_MONDAY, true)
    fun saveWeekStartsMonday(monday: Boolean) { prefs.edit().putBoolean(KEY_WEEK_STARTS_MONDAY, monday).apply() }
    fun loadOnboardingComplete(): Boolean =
        prefs.getBoolean(KEY_ONBOARDING_COMPLETE, loadItems().isNotEmpty())
    fun saveOnboardingComplete(complete: Boolean) { prefs.edit().putBoolean(KEY_ONBOARDING_COMPLETE, complete).apply() }

    fun exportState(): String {
        ensureRoomInitialized()
        val values = JSONObject()
            .put(KEY_ITEMS, itemsToLegacyJson(roomRepository.loadItems()))
            .put(KEY_SPACES, spacesToLegacyJson(roomRepository.loadSpaces()))

        prefs.all.forEach { (key, value) ->
            if (!shouldBackupKey(key) || key == KEY_ITEMS || key == KEY_SPACES) return@forEach
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
            .put("format", BACKUP_FORMAT)
            .put("version", BACKUP_FORMAT_VERSION)
            .put("values", values)
            .toString(2)
    }

    fun inspectBackup(raw: String): BackupPreview? = runCatching {
        val root = JSONObject(raw)
        require(root.optString("format") == BACKUP_FORMAT) { "Not a Dayline backup" }
        val version = root.optInt("version", 1)
        require(version in 1..BACKUP_FORMAT_VERSION) { "Unsupported Dayline backup version: $version" }

        val values = root.getJSONObject("values")
        val items = values.optString(KEY_ITEMS).takeIf { it.isNotBlank() }?.let(::JSONArray) ?: JSONArray()
        var events = 0
        var tasks = 0
        val itemIds = mutableSetOf<String>()
        for (index in 0 until items.length()) {
            val itemJson = items.getJSONObject(index)
            // Fully decode each item now so malformed dates/required fields are
            // rejected before restore can clear the existing local database.
            val decoded = itemFromJson(itemJson)
            require(decoded.id.isNotBlank()) { "Backup contains an item without an id" }
            require(itemIds.add(decoded.id)) { "Backup contains duplicate item id: ${decoded.id}" }
            when (decoded.kind) {
                AgendaKind.TASK -> tasks += 1
                else -> events += 1
            }
        }

        val spacesArray = values.optString(KEY_SPACES)
            .takeIf { it.isNotBlank() }
            ?.let(::JSONArray)
            ?: JSONArray()
        val spaceIds = mutableSetOf<String>()
        for (index in 0 until spacesArray.length()) {
            val space = spacesArray.getJSONObject(index)
            val spaceId = space.optString("id")
            require(spaceId.isNotBlank()) { "Backup contains a Space without an id" }
            require(spaceIds.add(spaceId)) { "Backup contains duplicate Space id: $spaceId" }
            require(space.optString("name").isNotBlank()) { "Backup contains a Space without a name" }
        }

        val templatesArray = values.optString(KEY_TEMPLATES)
            .takeIf { it.isNotBlank() }
            ?.let(::JSONArray)
            ?: JSONArray()
        val templateIds = mutableSetOf<String>()
        for (index in 0 until templatesArray.length()) {
            val template = templatesArray.getJSONObject(index)
            val templateId = template.optString("id")
            require(templateId.isNotBlank()) { "Backup contains a template without an id" }
            require(templateIds.add(templateId)) { "Backup contains duplicate template id: $templateId" }
        }

        val spaces = spacesArray.length()
        val templates = templatesArray.length()

        BackupPreview(
            version = version,
            events = events,
            tasks = tasks,
            spaces = spaces,
            templates = templates
        )
    }.getOrNull()

    fun importState(raw: String): Boolean = runCatching {
        require(inspectBackup(raw) != null) { "Backup contents could not be validated" }

        val root = JSONObject(raw)
        require(root.optString("format") == BACKUP_FORMAT) { "Not a Dayline backup" }
        val version = root.optInt("version", 1)
        require(version in 1..BACKUP_FORMAT_VERSION) { "Unsupported Dayline backup version: $version" }

        val values = root.getJSONObject("values")
        val importedItems = parseLegacyItems(values.optString(KEY_ITEMS))
        val importedSpaces = if (values.has(KEY_SPACES)) {
            parseLegacySpaces(values.optString(KEY_SPACES))
        } else {
            defaultSpaces()
        }

        val restored = mutableListOf<Pair<String, Any>>()
        val keys = values.keys()
        while (keys.hasNext()) {
            val key = keys.next()
            if (!shouldBackupKey(key) || key == KEY_ITEMS || key == KEY_SPACES) continue
            val value = values.get(key)
            when (value) {
                is String, is Boolean, is Int, is Long, is Double, is JSONArray -> restored += key to value
            }
        }

        ensureRoomInitialized()
        val previousItems = roomRepository.loadItems()
        val previousSpaces = roomRepository.loadSpaces()

        val editor = prefs.edit().clear()
        restored.forEach { (key, value) ->
            when (value) {
                is String -> editor.putString(key, value)
                is Boolean -> editor.putBoolean(key, value)
                is Int -> editor.putInt(key, value)
                is Long -> editor.putLong(key, value)
                is Double -> editor.putFloat(key, value.toFloat())
                is JSONArray -> editor.putStringSet(
                    key,
                    buildSet {
                        for (index in 0 until value.length()) add(value.getString(index))
                    }
                )
            }
        }

        try {
            roomRepository.replaceItemsBlocking(importedItems)
            roomRepository.replaceSpacesBlocking(importedSpaces)
            check(editor.commit()) { "Could not persist restored Dayline settings" }
        } catch (error: Throwable) {
            runCatching { roomRepository.replaceItemsBlocking(previousItems) }
            runCatching { roomRepository.replaceSpacesBlocking(previousSpaces) }
            throw error
        }

        true
    }.getOrDefault(false)

    private fun shouldBackupKey(key: String): Boolean =
        key !in VOLATILE_BACKUP_KEYS && !key.startsWith(WIDGET_INSTANCE_PREFIX)

    private fun ensureRoomInitialized() {
        if (roomInitialized) return

        val rawLegacyItems = prefs.getString(KEY_ITEMS, null)
        val rawLegacySpaces = prefs.getString(KEY_SPACES, null)
        val hadLegacyItems = prefs.contains(KEY_ITEMS)

        val migrationComplete = roomRepository.initialize(
            legacyItems = parseLegacyItems(rawLegacyItems),
            legacySpaces = rawLegacySpaces
                ?.let(::parseLegacySpaces)
                ?: defaultSpaces(),
            legacyItemsValid = legacyItemsAreValid(rawLegacyItems),
            legacySpacesValid = legacySpacesAreValid(rawLegacySpaces)
        )

        replayPendingRoomWrites()

        if (
            hadLegacyItems &&
            !prefs.contains(KEY_ONBOARDING_COMPLETE) &&
            roomRepository.loadItems().isNotEmpty()
        ) {
            prefs.edit().putBoolean(KEY_ONBOARDING_COMPLETE, true).apply()
        }

        // Delete the old source only after Room confirms the migration marker.
        // If a legacy record cannot be decoded completely, keep the raw JSON as
        // a forensic/recovery copy instead of silently declaring success.
        if (migrationComplete) {
            prefs.edit().remove(KEY_ITEMS).remove(KEY_SPACES).apply()
        }
        roomInitialized = true
    }

    private fun replayPendingRoomWrites() {
        prefs.getString(KEY_PENDING_ROOM_ITEMS, null)?.let { payload ->
            if (legacyItemsAreValid(payload)) {
                runCatching {
                    roomRepository.replaceItemsBlocking(parseLegacyItems(payload))
                }.onSuccess {
                    clearPendingRoomWrite(KEY_PENDING_ROOM_ITEMS, payload)
                }.onFailure { error ->
                    recordRoomWriteFailure("items replay", error)
                }
            } else {
                recordRoomWriteFailure("items replay", IllegalStateException("Pending item journal is malformed"))
            }
        }

        prefs.getString(KEY_PENDING_ROOM_SPACES, null)?.let { payload ->
            if (legacySpacesAreValid(payload)) {
                runCatching {
                    roomRepository.replaceSpacesBlocking(parseLegacySpaces(payload))
                }.onSuccess {
                    clearPendingRoomWrite(KEY_PENDING_ROOM_SPACES, payload)
                }.onFailure { error ->
                    recordRoomWriteFailure("spaces replay", error)
                }
            } else {
                recordRoomWriteFailure("spaces replay", IllegalStateException("Pending Space journal is malformed"))
            }
        }
    }

    private fun clearPendingRoomWrite(key: String, expectedPayload: String) {
        if (prefs.getString(key, null) != expectedPayload) return

        val otherKey = when (key) {
            KEY_PENDING_ROOM_ITEMS -> KEY_PENDING_ROOM_SPACES
            KEY_PENDING_ROOM_SPACES -> KEY_PENDING_ROOM_ITEMS
            else -> null
        }
        val editor = prefs.edit().remove(key)
        if (otherKey == null || prefs.getString(otherKey, null) == null) {
            editor.remove(KEY_LAST_ROOM_WRITE_ERROR)
        }
        editor.apply()
    }

    private fun recordRoomWriteFailure(kind: String, error: Throwable) {
        val detail = error.message?.takeIf { it.isNotBlank() }
            ?: error::class.java.simpleName
        prefs.edit()
            .putString(KEY_LAST_ROOM_WRITE_ERROR, "$kind · $detail")
            .apply()
    }

    fun loadLastRoomWriteError(): String? =
        prefs.getString(KEY_LAST_ROOM_WRITE_ERROR, null)?.takeIf { it.isNotBlank() }

    fun hasPendingRoomWrites(): Boolean =
        prefs.contains(KEY_PENDING_ROOM_ITEMS) || prefs.contains(KEY_PENDING_ROOM_SPACES)

    private fun legacyItemsAreValid(raw: String?): Boolean {
        if (raw.isNullOrBlank()) return true
        return runCatching {
            val array = JSONArray(raw)
            for (index in 0 until array.length()) {
                itemFromJson(array.getJSONObject(index))
            }
            true
        }.getOrDefault(false)
    }

    private fun legacySpacesAreValid(raw: String?): Boolean {
        if (raw.isNullOrBlank()) return true
        return runCatching {
            val array = JSONArray(raw)
            for (index in 0 until array.length()) {
                val json = array.getJSONObject(index)
                json.getString("id")
                json.getString("name")
            }
            true
        }.getOrDefault(false)
    }

    private fun parseLegacyItems(raw: String?): List<DaylineItem> {
        if (raw.isNullOrBlank()) return emptyList()
        val array = runCatching { JSONArray(raw) }.getOrNull() ?: return emptyList()
        return buildList {
            for (index in 0 until array.length()) {
                runCatching { itemFromJson(array.getJSONObject(index)) }
                    .getOrNull()
                    ?.let(::add)
            }
        }
    }

    private fun parseLegacySpaces(raw: String?): List<DaylineSpace> {
        if (raw.isNullOrBlank()) return emptyList()
        val array = runCatching { JSONArray(raw) }.getOrNull() ?: return emptyList()
        return buildList {
            for (index in 0 until array.length()) {
                runCatching {
                    val json = array.getJSONObject(index)
                    DaylineSpace(
                        id = json.getString("id"),
                        name = json.getString("name"),
                        color = enumValue(json.optString("color"), ItemColor.MONO),
                        calendarId = json.optLong("calendarId", -1L).takeIf { it >= 0L }
                    )
                }.getOrNull()?.let(::add)
            }
        }
    }

    private fun itemsToLegacyJson(items: List<DaylineItem>): String =
        JSONArray().apply { items.forEach { put(itemToJson(it)) } }.toString()

    private fun spacesToLegacyJson(spaces: List<DaylineSpace>): String =
        JSONArray().apply {
            spaces.forEach { space ->
                put(
                    JSONObject()
                        .put("id", space.id)
                        .put("name", space.name)
                        .put("color", space.color.name)
                        .put("calendarId", space.calendarId ?: -1L)
                )
            }
        }.toString()

    private fun defaultSpaces(): List<DaylineSpace> = listOf(
        DaylineSpace("personal", "Personal", ItemColor.BLUE),
        DaylineSpace("work", "Work", ItemColor.VIOLET)
    )

    private fun itemFromJson(json: JSONObject): DaylineItem {
        val completed = parseDates(json.optJSONArray("completedDates")); val excluded = parseDates(json.optJSONArray("excludedDates"))
        val details = buildList {
            val array = json.optJSONArray("details") ?: JSONArray()
            for (index in 0 until array.length()) {
                val detail = array.getJSONObject(index)
                add(TaskDetail(detail.optString("id").ifBlank { UUID.randomUUID().toString() }, detail.optString("text"), detail.optBoolean("done", false)))
            }
        }
        val startTimeRaw = json.optString("startTime").ifBlank { json.optString("time") }
        val startDate = LocalDate.parse(json.getString("startDate"))
        val kind = enumValue(json.optString("kind"), AgendaKind.EVENT)
        val rawRecurrence = json.optString("recurrence", Recurrence.ONCE.name)
        val storedRepeatDays = parseInts(json.optJSONArray("repeatDays"))
        val recurrence = when (rawRecurrence) { "SUNDAYS", "EXCEPT_SUNDAY" -> Recurrence.CUSTOM; else -> enumValue(rawRecurrence, Recurrence.ONCE) }
        val repeatDays = when (rawRecurrence) { "SUNDAYS" -> setOf(7); "EXCEPT_SUNDAY" -> setOf(1,2,3,4,5,6); else -> storedRepeatDays }

        return DaylineItem(
            id = json.getString("id"), title = json.getString("title"), kind = kind,
            startDate = startDate, startTime = startTimeRaw.takeIf { it.isNotBlank() }?.let(LocalTime::parse),
            endTime = json.optString("endTime").takeIf { it.isNotBlank() }?.let(LocalTime::parse), recurrence = recurrence,
            repeatDays = repeatDays, recurrenceEndDate = json.optString("recurrenceEndDate").takeIf { it.isNotBlank() }?.let(LocalDate::parse),
            excludedDates = excluded, seriesParentId = json.optString("seriesParentId").takeIf { it.isNotBlank() },
            reminderMinutes = json.optInt("reminderMinutes", -1).takeIf { it >= 0 }, focusCycle = enumValue(json.optString("focusCycle"), FocusCycle.OFF),
            customFocusMinutes = json.optInt("customFocusMinutes", 25).coerceIn(5,180), customBreakMinutes = json.optInt("customBreakMinutes", 5).coerceIn(1,60),
            focusSessionsCompleted = json.optInt("focusSessionsCompleted", 0).coerceAtLeast(0), focusedMinutesCompleted = json.optInt("focusedMinutesCompleted",0).coerceAtLeast(0),
            bufferBeforeMinutes = json.optInt("bufferBeforeMinutes",0).coerceIn(0,180), bufferAfterMinutes = json.optInt("bufferAfterMinutes",0).coerceIn(0,180),
            priority = enumValue(json.optString("priority"), TaskPriority.NORMAL),
            estimatedDurationMinutes = json.optInt("estimatedDurationMinutes", 30).coerceIn(15, 8 * 60),
            earliestDate = json.optString("earliestDate").takeIf { it.isNotBlank() }?.let(LocalDate::parse),
            deadlineDate = json.optString("deadlineDate").takeIf { it.isNotBlank() }?.let(LocalDate::parse),
            allDay = json.optBoolean("allDay", kind == AgendaKind.EVENT && startTimeRaw.isBlank()),
            timeZoneId = json.optString("timeZoneId").takeIf { it.isNotBlank() },
            color = enumValue(json.optString("color"), ItemColor.MONO), spaceId = json.optString("spaceId").takeIf { it.isNotBlank() },
            details = details, completedDates = completed, calendarEventId = json.optLong("calendarEventId", -1L).takeIf { it >= 0L },
            calendarId = json.optLong("calendarId", -1L).takeIf { it >= 0L }, calendarName = json.optString("calendarName").takeIf { it.isNotBlank() },
            calendarReadOnly = json.optBoolean("calendarReadOnly", false)
        )
    }

    private fun itemToJson(item: DaylineItem): JSONObject {
        val details = JSONArray()
        item.details.forEach { detail -> details.put(JSONObject().put("id", detail.id).put("text", detail.text).put("done", detail.done)) }
        return JSONObject().put("id", item.id).put("title", item.title).put("kind", item.kind.name)
            .put("startDate", item.startDate.toString()).put("startTime", item.startTime?.toString().orEmpty()).put("endTime", item.endTime?.toString().orEmpty())
            .put("recurrence", item.recurrence.name).put("repeatDays", intsArray(item.repeatDays)).put("recurrenceEndDate", item.recurrenceEndDate?.toString().orEmpty())
            .put("excludedDates", datesArray(item.excludedDates)).put("seriesParentId", item.seriesParentId.orEmpty()).put("reminderMinutes", item.reminderMinutes ?: -1)
            .put("focusCycle", item.focusCycle.name).put("customFocusMinutes", item.customFocusMinutes).put("customBreakMinutes", item.customBreakMinutes)
            .put("focusSessionsCompleted", item.focusSessionsCompleted).put("focusedMinutesCompleted", item.focusedMinutesCompleted)
            .put("bufferBeforeMinutes", item.bufferBeforeMinutes).put("bufferAfterMinutes", item.bufferAfterMinutes).put("priority", item.priority.name)
            .put("estimatedDurationMinutes", item.estimatedDurationMinutes)
            .put("earliestDate", item.earliestDate?.toString().orEmpty()).put("deadlineDate", item.deadlineDate?.toString().orEmpty())
            .put("allDay", item.allDay).put("timeZoneId", item.timeZoneId.orEmpty())
            .put("color", item.color.name).put("spaceId", item.spaceId.orEmpty()).put("details", details).put("completedDates", datesArray(item.completedDates))
            .put("calendarEventId", item.calendarEventId ?: -1L).put("calendarId", item.calendarId ?: -1L).put("calendarName", item.calendarName.orEmpty())
            .put("calendarReadOnly", item.calendarReadOnly)
    }

    private fun datesArray(dates: Set<LocalDate>): JSONArray = JSONArray().apply { dates.sorted().forEach { put(it.toString()) } }
    private fun intsArray(values: Set<Int>): JSONArray = JSONArray().apply { values.sorted().forEach(::put) }
    private fun parseInts(array: JSONArray?): Set<Int> = buildSet {
        val source = array ?: JSONArray(); for (index in 0 until source.length()) source.optInt(index, -1).takeIf { it in 1..7 }?.let(::add)
    }
    private fun parseDates(array: JSONArray?): Set<LocalDate> = buildSet {
        val source = array ?: JSONArray(); for (index in 0 until source.length()) runCatching { LocalDate.parse(source.getString(index)) }.getOrNull()?.let(::add)
    }

    private fun defaultTemplates(): List<EventTemplate> = listOf(
        EventTemplate("template-meeting", "Meeting", durationMinutes = 60, color = ItemColor.BLUE),
        EventTemplate("template-focus", "Focus block", durationMinutes = 90, color = ItemColor.VIOLET, focusCycle = FocusCycle.FOCUS_50_10),
        EventTemplate("template-workout", "Workout", durationMinutes = 60, color = ItemColor.SAGE),
        EventTemplate("template-appointment", "Appointment", durationMinutes = 60, color = ItemColor.AMBER),
        EventTemplate("template-errand", "Errand", durationMinutes = 45, color = ItemColor.ROSE)
    )

    private fun widgetKey(appWidgetId: Int): String = "$WIDGET_INSTANCE_PREFIX$appWidgetId"
    private inline fun <reified T : Enum<T>> enumValue(value: String?, fallback: T): T = runCatching { enumValueOf<T>(value.orEmpty()) }.getOrDefault(fallback)

    companion object {
        private const val BACKUP_FORMAT = "dayline-backup"
        private const val BACKUP_FORMAT_VERSION = 2
        private const val WIDGET_INSTANCE_PREFIX = "widget_instance_"
        private val VOLATILE_BACKUP_KEYS = setOf(
            "last_update_check_at",
            "last_update_check_error",
            "available_beta_release",
            "pending_whats_new_from_version",
            "last_launched_version",
            "last_calendar_sync_at",
            "last_calendar_sync_error",
            "last_widget_refresh_at",
            "pending_room_items",
            "pending_room_spaces",
            "last_room_write_error"
        )

        private const val KEY_ITEMS = "items"; private const val KEY_SPACES = "spaces"; private const val KEY_TEMPLATES = "templates"
        private const val KEY_CALENDAR_PREFS = "calendar_prefs"; private const val KEY_APPEARANCE = "appearance"; private const val KEY_FONT = "font"
        private const val KEY_WIDGET_FONT = "widget_font"; private const val KEY_WIDGET_EMOJI = "widget_emoji"; private const val KEY_WIDGET_AUTO_SLIDE = "widget_auto_slide"
        private const val KEY_NOW_ACTIVITY = "now_activity"; private const val KEY_CALENDAR_SYNC = "calendar_sync"; private const val KEY_AUTO_BETA_UPDATES = "auto_beta_updates"
        private const val KEY_LAST_UPDATE_CHECK_AT = "last_update_check_at"; private const val KEY_LAST_UPDATE_CHECK_ERROR = "last_update_check_error"
        private const val KEY_AVAILABLE_BETA_RELEASE = "available_beta_release"
        private const val KEY_LAST_LAUNCHED_VERSION = "last_launched_version"
        private const val KEY_PENDING_WHATS_NEW_FROM_VERSION = "pending_whats_new_from_version"
        private const val KEY_LAST_CALENDAR_SYNC_AT = "last_calendar_sync_at"
        private const val KEY_LAST_CALENDAR_SYNC_ERROR = "last_calendar_sync_error"; private const val KEY_GLYPH_PREFERENCES = "glyph_preferences"
        private const val KEY_LAST_WIDGET_REFRESH_AT = "last_widget_refresh_at"
        private const val KEY_PENDING_ROOM_ITEMS = "pending_room_items"
        private const val KEY_PENDING_ROOM_SPACES = "pending_room_spaces"
        private const val KEY_LAST_ROOM_WRITE_ERROR = "last_room_write_error"
        private const val KEY_SHOW_ORB = "show_orb"; private const val KEY_WEEK_STARTS_MONDAY = "week_starts_monday"; private const val KEY_ONBOARDING_COMPLETE = "onboarding_complete"
    }
}

data class BackupPreview(
    val version: Int,
    val events: Int,
    val tasks: Int,
    val spaces: Int,
    val templates: Int
) {
    val items: Int get() = events + tasks
}

enum class Appearance { SYSTEM, LIGHT, DARK }
enum class FontChoice { SYSTEM, GEIST, INTER, SPACE_GROTESK, IBM_PLEX_MONO, PIXELIFY, GEIST_PIXEL }
enum class WidgetFontChoice { DOT_BOLD, DOT_FINE, MONO }
enum class WidgetEmojiChoice { SMILE, GRIN, WINK, COOL, NERD, PARTY, SLEEPY, MELT, GHOST, ROBOT, RELAXED, HEART_EYES }
