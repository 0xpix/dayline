package com.pix.dayline.glyph

import android.app.Service
import android.content.Intent
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.Messenger
import com.pix.dayline.data.DaylineStore
import com.pix.dayline.data.FocusRuntimeStore
import com.pix.dayline.model.*
import java.time.LocalDateTime
import java.time.ZoneId
import kotlin.math.roundToInt
import kotlin.random.Random

/**
 * Phone (4a) Pro Always-on Glyph Toy.
 *
 * Dayline keeps expressive eyes as the permanent visual language. Focus mode
 * stays visually clean: normal eyes remain on screen, then at phase start,
 * every 5-minute checkpoint and 1:00 remaining the face briefly transitions
 * through CENTER into a full-screen MM:SS countdown for 30 seconds.
 */
class DaylineGlyphToyService : Service() {
    private val handler = Handler(Looper.getMainLooper())
    private lateinit var store: DaylineStore
    private lateinit var runtime: GlyphRuntimeStore
    private lateinit var focusRuntime: FocusRuntimeStore
    private lateinit var visualPrefs: GlyphVisualPreferencesStore
    private lateinit var bridge: NothingGlyphBridge

    private var running = false
    private var nextBlinkAt = 0L
    private var blinkUntil = 0L
    private var nextMotionAt = 0L
    private var motionUntil = 0L
    private var motionOverride: DaylineGlyphSignal? = null
    private var centerRecoveryUntil = 0L
    private var lastFrame: IntArray? = null

    private var announcementStage = AnnouncementStage.EYES
    private var announcementStageUntil = 0L
    private var activeAnnouncementKey: String? = null
    private var activeAnnouncementPhaseKey: String? = null
    private var lastAnnouncedKey: String? = null

    private val systemMessenger by lazy {
        Messenger(Handler(Looper.getMainLooper()) {
            renderNow()
            true
        })
    }

    override fun onCreate() {
        super.onCreate()
        store = DaylineStore(applicationContext)
        runtime = GlyphRuntimeStore(applicationContext)
        focusRuntime = FocusRuntimeStore(applicationContext)
        visualPrefs = GlyphVisualPreferencesStore(applicationContext)
        bridge = NothingGlyphBridge(applicationContext, appMatrix = false)
    }

    override fun onBind(intent: Intent?): IBinder? {
        running = true
        lastFrame = null
        resetAnnouncementState()
        scheduleNaturalMotion(force = true)
        bridge.connect { renderNow() }
        handler.post(tick)
        return systemMessenger.binder
    }

    override fun onUnbind(intent: Intent?): Boolean {
        stopLoop()
        return false
    }

    override fun onDestroy() {
        stopLoop()
        super.onDestroy()
    }

    private val tick = object : Runnable {
        override fun run() {
            if (!running) return
            renderNow()
            val prefs = store.loadGlyphPreferences()
            val delay = when {
                prefs.mode == GlyphMode.OFF -> 30_000L
                prefs.reduceMotion -> 1_000L
                else -> 250L
            }
            handler.postDelayed(this, delay)
        }
    }

    private fun renderNow() {
        val prefs = store.loadGlyphPreferences()
        if (!prefs.enabled) {
            showIfChanged(IntArray(GlyphMatrixPatterns.SIZE * GlyphMatrixPatterns.SIZE))
            return
        }

        val nowMillis = System.currentTimeMillis()
        val brightness = currentBrightness(prefs)
        val phase = focusPhase(nowMillis)
        val presentation = focusPresentation(phase, nowMillis)

        // Manual expression previews from Settings are allowed, but all
        // priority > 0 app-state signals remain intentionally ignored.
        val preview = runtime.current(nowMillis)?.takeIf { it.priority == 0 }
        val expression = when {
            presentation.forceCenter -> DaylineGlyphSignal.CENTER
            preview != null -> preview
            prefs.reduceMotion -> DaylineGlyphSignal.CENTER
            else -> naturalExpression(prefs, nowMillis)
        }

        val frame = GlyphMatrixPatterns.frame(
            signal = expression,
            brightness = brightness,
            focusRemainingSeconds = presentation.timerSeconds
        )
        showIfChanged(frame)
    }

    /**
     * A single calm state machine keeps hardware updates stable.
     *
     * Every non-centre animation is isolated by CENTER on both sides:
     * CENTER -> animation -> CENTER -> next animation.
     */
    private fun naturalExpression(
        prefs: GlyphPreferences,
        now: Long
    ): DaylineGlyphSignal {
        if (nextBlinkAt == 0L || nextMotionAt == 0L) scheduleNaturalMotion(force = true)

        if (motionUntil > 0L && now >= motionUntil) {
            motionUntil = 0L
            motionOverride = null
            beginCenterRecovery(now)
            return DaylineGlyphSignal.CENTER
        }

        if (now < centerRecoveryUntil) return DaylineGlyphSignal.CENTER

        motionOverride?.let { return it }

        if (blinkUntil > 0L) {
            if (now < blinkUntil) return DaylineGlyphSignal.BLINK
            blinkUntil = 0L
            beginCenterRecovery(now)
            return DaylineGlyphSignal.CENTER
        }

        if (prefs.blinkEnabled && now >= nextBlinkAt) {
            blinkUntil = now + BLINK_HOLD_MS
            nextBlinkAt = now + Random.nextLong(2_200L, 4_800L)
            return DaylineGlyphSignal.BLINK
        }

        if (prefs.randomGlancesEnabled && now >= nextMotionAt) {
            motionOverride = randomExpression()
            motionUntil = now + Random.nextLong(900L, 1_550L)
            nextMotionAt = now + motionDelay(prefs.glanceFrequency)
            return motionOverride ?: DaylineGlyphSignal.CENTER
        }

        return DaylineGlyphSignal.CENTER
    }

    private fun beginCenterRecovery(now: Long) {
        centerRecoveryUntil = now + CENTER_RECOVERY_MS

        // No blink or expression may start during the dedicated centre beat.
        if (nextBlinkAt <= centerRecoveryUntil) {
            nextBlinkAt = centerRecoveryUntil + Random.nextLong(350L, 900L)
        }
        if (nextMotionAt <= centerRecoveryUntil) {
            nextMotionAt = centerRecoveryUntil + Random.nextLong(350L, 900L)
        }
    }

    private fun randomExpression(): DaylineGlyphSignal {
        return when (Random.nextInt(100)) {
            in 0..21 -> DaylineGlyphSignal.LOOK_LEFT
            in 22..43 -> DaylineGlyphSignal.LOOK_RIGHT
            in 44..74 -> DaylineGlyphSignal.HAPPY
            in 75..89 -> DaylineGlyphSignal.WINK
            in 90..96 -> DaylineGlyphSignal.HEARTS
            in 97..98 -> DaylineGlyphSignal.SQUINT
            else -> DaylineGlyphSignal.SLEEPY
        }
    }

    private enum class AnnouncementStage {
        EYES,
        PRE_CENTER,
        TIME,
        POST_CENTER
    }

    private data class FocusPhase(
        val remainingSeconds: Long,
        val totalSeconds: Long,
        val phaseKey: String
    )

    private data class FocusPresentation(
        val timerSeconds: Long? = null,
        val forceCenter: Boolean = false
    )

    /**
     * Focus announcement contract:
     *
     * eyes -> CENTER -> time for 30s -> CENTER -> eyes
     *
     * Time is announced at phase start, every five-minute remaining checkpoint,
     * and at 1:00 remaining. The timer stays live during the 30-second window.
     */
    private fun focusPresentation(
        phase: FocusPhase?,
        nowMillis: Long
    ): FocusPresentation {
        if (phase == null) {
            return finishAnnouncementWhenFocusEnds(nowMillis)
        }

        // If Focus/Rest advanced to another persisted/fallback phase while an
        // announcement was active, finish the old visual cleanly before the new
        // phase-start checkpoint is allowed to announce.
        if (
            announcementStage != AnnouncementStage.EYES &&
            activeAnnouncementPhaseKey != null &&
            activeAnnouncementPhaseKey != phase.phaseKey
        ) {
            if (announcementStage != AnnouncementStage.POST_CENTER) {
                announcementStage = AnnouncementStage.POST_CENTER
                announcementStageUntil = nowMillis + CENTER_RECOVERY_MS
            }
            return FocusPresentation(forceCenter = true)
        }

        when (announcementStage) {
            AnnouncementStage.PRE_CENTER -> {
                if (nowMillis < announcementStageUntil) {
                    return FocusPresentation(forceCenter = true)
                }
                announcementStage = AnnouncementStage.TIME
                announcementStageUntil = nowMillis + ANNOUNCEMENT_DURATION_MS
                return FocusPresentation(timerSeconds = phase.remainingSeconds)
            }

            AnnouncementStage.TIME -> {
                if (nowMillis < announcementStageUntil) {
                    return FocusPresentation(timerSeconds = phase.remainingSeconds)
                }
                announcementStage = AnnouncementStage.POST_CENTER
                announcementStageUntil = nowMillis + CENTER_RECOVERY_MS
                return FocusPresentation(forceCenter = true)
            }

            AnnouncementStage.POST_CENTER -> {
                if (nowMillis < announcementStageUntil) {
                    return FocusPresentation(forceCenter = true)
                }
                announcementStage = AnnouncementStage.EYES
                announcementStageUntil = 0L
                activeAnnouncementKey = null
                activeAnnouncementPhaseKey = null
                scheduleNaturalMotion(force = true)
                return FocusPresentation(forceCenter = true)
            }

            AnnouncementStage.EYES -> Unit
        }

        val checkpoint = announcementCheckpoint(phase) ?: return FocusPresentation()
        val key = "${phase.phaseKey}:$checkpoint"
        if (key == lastAnnouncedKey) return FocusPresentation()

        lastAnnouncedKey = key
        activeAnnouncementKey = key
        activeAnnouncementPhaseKey = phase.phaseKey
        announcementStage = AnnouncementStage.PRE_CENTER
        announcementStageUntil = nowMillis + CENTER_RECOVERY_MS
        prepareCenterForAnnouncement()
        return FocusPresentation(forceCenter = true)
    }

    private fun finishAnnouncementWhenFocusEnds(nowMillis: Long): FocusPresentation {
        return when (announcementStage) {
            AnnouncementStage.EYES -> FocusPresentation()
            AnnouncementStage.POST_CENTER -> {
                if (nowMillis < announcementStageUntil) {
                    FocusPresentation(forceCenter = true)
                } else {
                    resetAnnouncementState()
                    scheduleNaturalMotion(force = true)
                    FocusPresentation(forceCenter = true)
                }
            }
            AnnouncementStage.PRE_CENTER,
            AnnouncementStage.TIME -> {
                announcementStage = AnnouncementStage.POST_CENTER
                announcementStageUntil = nowMillis + CENTER_RECOVERY_MS
                FocusPresentation(forceCenter = true)
            }
        }
    }

    private fun announcementCheckpoint(phase: FocusPhase): Long? {
        val total = phase.totalSeconds.coerceAtLeast(1L)
        val remaining = phase.remainingSeconds.coerceIn(0L, total)

        val checkpoints = buildList {
            // Option B: always announce the phase start.
            add(total)

            // Then every five-minute remaining checkpoint below the start.
            var nextFive = (total / FIVE_MINUTES_SECONDS) * FIVE_MINUTES_SECONDS
            if (nextFive == total) nextFive -= FIVE_MINUTES_SECONDS
            while (nextFive >= FIVE_MINUTES_SECONDS) {
                add(nextFive)
                nextFive -= FIVE_MINUTES_SECONDS
            }

            // And always 1:00 remaining when the phase is longer than a minute.
            if (total > ONE_MINUTE_SECONDS) add(ONE_MINUTE_SECONDS)
        }.distinct()

        return checkpoints.firstOrNull { checkpoint ->
            remaining <= checkpoint &&
                remaining > (checkpoint - CHECKPOINT_DETECTION_WINDOW_SECONDS).coerceAtLeast(0L)
        }
    }

    private fun prepareCenterForAnnouncement() {
        blinkUntil = 0L
        motionUntil = 0L
        motionOverride = null
        centerRecoveryUntil = 0L
    }

    private fun resetAnnouncementState() {
        announcementStage = AnnouncementStage.EYES
        announcementStageUntil = 0L
        activeAnnouncementKey = null
        activeAnnouncementPhaseKey = null
        lastAnnouncedKey = null
    }

    /** Resolve the currently active event's 25/5, 50/10 or custom phase. */
    private fun focusPhase(nowMillis: Long): FocusPhase? {
        val now = LocalDateTime.now()
        val today = now.toLocalDate()
        val time = now.toLocalTime()

        val active = store.loadItems().firstOrNull { item ->
            if (item.kind != AgendaKind.EVENT || item.focusCycle == FocusCycle.OFF) {
                return@firstOrNull false
            }
            if (!item.occursOn(today)) return@firstOrNull false
            val start = item.startTime ?: return@firstOrNull false
            val end = item.endTime?.takeIf { it.isAfter(start) } ?: start.plusHours(1)
            time >= start && time < end
        } ?: return null

        val runtimeState = focusRuntime.load(active.id)
        if (
            runtimeState != null &&
            runtimeState.occurrenceDate == today &&
            !runtimeState.finished
        ) {
            val totalSeconds = (
                (runtimeState.phaseEndEpochMillis - runtimeState.phaseStartedEpochMillis)
                    .coerceAtLeast(1_000L) + 999L
            ) / 1_000L

            val remainingSeconds = if (runtimeState.paused) {
                runtimeState.pausedRemainingSeconds.coerceAtLeast(0L)
            } else {
                val remainingMillis = (runtimeState.phaseEndEpochMillis - nowMillis)
                    .coerceAtLeast(0L)
                (remainingMillis + 999L) / 1_000L
            }

            return FocusPhase(
                remainingSeconds = remainingSeconds.coerceAtMost(totalSeconds),
                totalSeconds = totalSeconds,
                phaseKey = buildString {
                    append(active.id)
                    append(':')
                    append(runtimeState.phaseStartedEpochMillis)
                    append(':')
                    append(if (runtimeState.focus) 'F' else 'R')
                }
            )
        }

        // Fallback keeps the Glyph useful before the notification runtime has
        // created a persisted phase. The event supplies 25/5, 50/10 or custom.
        val start = active.startTime ?: return null
        val startMillis = LocalDateTime.of(today, start)
            .atZone(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()
        val elapsedSeconds = ((nowMillis - startMillis).coerceAtLeast(0L) / 1_000L)
        val focusSeconds = active.focusMinutes.coerceAtLeast(1) * 60L
        val breakSeconds = active.breakMinutes.coerceAtLeast(1) * 60L
        val cycleSeconds = focusSeconds + breakSeconds
        val cycleIndex = elapsedSeconds / cycleSeconds
        val cyclePosition = elapsedSeconds % cycleSeconds
        val cycleStartMillis = startMillis + cycleIndex * cycleSeconds * 1_000L

        return if (cyclePosition < focusSeconds) {
            FocusPhase(
                remainingSeconds = (focusSeconds - cyclePosition).coerceAtLeast(0L),
                totalSeconds = focusSeconds,
                phaseKey = "${active.id}:$cycleStartMillis:F"
            )
        } else {
            val breakElapsed = cyclePosition - focusSeconds
            val breakStartMillis = cycleStartMillis + focusSeconds * 1_000L
            FocusPhase(
                remainingSeconds = (breakSeconds - breakElapsed).coerceAtLeast(0L),
                totalSeconds = breakSeconds,
                phaseKey = "${active.id}:$breakStartMillis:R"
            )
        }
    }

    private fun currentBrightness(prefs: GlyphPreferences): Int {
        val uiBrightness = visualPrefs.loadBrightness()
        val rawBrightness = (
            uiBrightness / GlyphVisualPreferencesStore.MAX_BRIGHTNESS.toFloat() *
                GlyphMatrixPatterns.MAX_RAW_BRIGHTNESS
            ).roundToInt()
            .coerceIn(0, GlyphMatrixPatterns.MAX_RAW_BRIGHTNESS)

        val quiet = prefs.quietHoursEnabled && GlyphStateResolver.inQuietHours(
            java.time.LocalTime.now(), prefs.quietStart, prefs.quietEnd
        )
        return if (prefs.dimAtNight && quiet) {
            (rawBrightness * 0.65f).roundToInt().coerceAtLeast(128)
        } else {
            rawBrightness
        }
    }

    private fun showIfChanged(frame: IntArray) {
        val previous = lastFrame
        if (previous != null && previous.contentEquals(frame)) return
        lastFrame = frame.copyOf()
        bridge.show(frame)
    }

    private fun scheduleNaturalMotion(force: Boolean = false) {
        val now = System.currentTimeMillis()
        if (force || nextBlinkAt <= now) {
            nextBlinkAt = now + Random.nextLong(1_500L, 3_200L)
        }
        if (force || nextMotionAt <= now) {
            nextMotionAt = now + motionDelay(store.loadGlyphPreferences().glanceFrequency)
        }
    }

    private fun motionDelay(frequency: GlyphGlanceFrequency): Long = when (frequency) {
        GlyphGlanceFrequency.RARE -> Random.nextLong(8_000L, 15_000L)
        GlyphGlanceFrequency.NORMAL -> Random.nextLong(5_000L, 10_000L)
        GlyphGlanceFrequency.FREQUENT -> Random.nextLong(3_500L, 7_000L)
    }

    private fun stopLoop() {
        running = false
        blinkUntil = 0L
        motionUntil = 0L
        centerRecoveryUntil = 0L
        motionOverride = null
        lastFrame = null
        resetAnnouncementState()
        handler.removeCallbacksAndMessages(null)
        bridge.close()
    }

    private companion object {
        const val CENTER_RECOVERY_MS = 700L
        const val BLINK_HOLD_MS = 350L
        const val ANNOUNCEMENT_DURATION_MS = 30_000L
        const val CHECKPOINT_DETECTION_WINDOW_SECONDS = 30L
        const val FIVE_MINUTES_SECONDS = 5L * 60L
        const val ONE_MINUTE_SECONDS = 60L
    }
}
