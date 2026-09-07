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
import kotlin.random.Random

/**
 * Phone (4a) Pro Always-on Glyph Toy.
 *
 * Dayline keeps expressive eyes as the permanent visual language. The only
 * automatic app overlay is Focus Mode: phase progress lights a circular path
 * around the eyes while the face keeps its natural motion.
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

        // Manual expression previews from Settings are allowed, but all
        // priority > 0 app-state signals remain intentionally ignored.
        val preview = runtime.current(nowMillis)?.takeIf { it.priority == 0 }
        val expression = when {
            preview != null -> preview
            prefs.reduceMotion -> DaylineGlyphSignal.CENTER
            else -> naturalExpression(prefs, nowMillis)
        }

        val focus = focusOverlay(nowMillis)
        val frame = GlyphMatrixPatterns.frame(
            signal = expression,
            brightness = brightness,
            focusProgress = focus?.progress,
            focusBreak = focus?.isBreak == true
        )
        showIfChanged(frame)
    }

    /**
     * A single calm state machine keeps hardware updates stable. Left/right
     * glances always return to CENTER before blink or another expression starts.
     */
    private fun naturalExpression(
        prefs: GlyphPreferences,
        now: Long
    ): DaylineGlyphSignal {
        if (nextBlinkAt == 0L || nextMotionAt == 0L) scheduleNaturalMotion(force = true)

        // Finish the current expression first. A side glance gets a dedicated
        // centre recovery beat, so LEFT -> HAPPY / BLINK never looks like a jump.
        if (motionUntil > 0L && now >= motionUntil) {
            val completed = motionOverride
            motionUntil = 0L
            motionOverride = null
            if (completed == DaylineGlyphSignal.LOOK_LEFT || completed == DaylineGlyphSignal.LOOK_RIGHT) {
                centerRecoveryUntil = now + CENTER_RECOVERY_MS
                if (nextBlinkAt <= centerRecoveryUntil) {
                    nextBlinkAt = centerRecoveryUntil + Random.nextLong(350L, 900L)
                }
                return DaylineGlyphSignal.CENTER
            }
        }

        if (now < centerRecoveryUntil) return DaylineGlyphSignal.CENTER

        // Do not let a blink interrupt an expression mid-frame. Finish the
        // expression, centre if required, then blink.
        motionOverride?.let { return it }

        if (blinkUntil > 0L) {
            if (now < blinkUntil) return DaylineGlyphSignal.BLINK
            blinkUntil = 0L
        }

        if (prefs.blinkEnabled && now >= nextBlinkAt) {
            blinkUntil = now + 350L
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

    private fun randomExpression(): DaylineGlyphSignal {
        // Keep only the expressions that read well on the 13×13 matrix.
        // Happy gets more screen time; sleepy remains deliberately rare.
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

    private data class FocusOverlay(
        val progress: Float,
        val isBreak: Boolean
    )

    /** Resolve the currently active event's 25/5, 50/10 or custom focus phase. */
    private fun focusOverlay(nowMillis: Long): FocusOverlay? {
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
            val durationMillis = (
                runtimeState.phaseEndEpochMillis - runtimeState.phaseStartedEpochMillis
            ).coerceAtLeast(1_000L)
            val elapsedMillis = if (runtimeState.paused) {
                durationMillis - runtimeState.pausedRemainingSeconds.coerceAtLeast(0L) * 1_000L
            } else {
                nowMillis - runtimeState.phaseStartedEpochMillis
            }.coerceIn(0L, durationMillis)

            return FocusOverlay(
                progress = elapsedMillis.toFloat() / durationMillis.toFloat(),
                isBreak = !runtimeState.focus
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
        val cyclePosition = elapsedSeconds % cycleSeconds

        return if (cyclePosition < focusSeconds) {
            FocusOverlay(
                progress = cyclePosition.toFloat() / focusSeconds.toFloat(),
                isBreak = false
            )
        } else {
            val breakElapsed = cyclePosition - focusSeconds
            FocusOverlay(
                progress = breakElapsed.toFloat() / breakSeconds.toFloat(),
                isBreak = true
            )
        }
    }

    private fun currentBrightness(prefs: GlyphPreferences): Int {
        val base = visualPrefs.loadBrightness()
        val quiet = prefs.quietHoursEnabled && GlyphStateResolver.inQuietHours(
            java.time.LocalTime.now(), prefs.quietStart, prefs.quietEnd
        )
        return if (prefs.dimAtNight && quiet) {
            (base * 0.65f).toInt().coerceAtLeast(GlyphVisualPreferencesStore.MIN_BRIGHTNESS)
        } else {
            base
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
        handler.removeCallbacksAndMessages(null)
        bridge.close()
    }

    private companion object {
        const val CENTER_RECOVERY_MS = 500L
    }
}
