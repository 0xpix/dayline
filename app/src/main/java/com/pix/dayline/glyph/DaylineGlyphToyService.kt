package com.pix.dayline.glyph

import android.app.Service
import android.content.Intent
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.Messenger
import com.pix.dayline.data.DaylineStore
import com.pix.dayline.model.*
import kotlin.random.Random

/**
 * Phone (4a) Pro Always-on Glyph Toy. Nothing binds this service when the user
 * activates Dayline in Settings > Glyph Interface > Flip to Glyph > Always-on Glyph Toy.
 */
class DaylineGlyphToyService : Service() {
    private val handler = Handler(Looper.getMainLooper())
    private lateinit var store: DaylineStore
    private lateinit var runtime: GlyphRuntimeStore
    private lateinit var bridge: NothingGlyphBridge
    private var running = false
    private var nextGlanceAt = 0L
    private var nextBlinkAt = 0L
    private var blinking = false
    private var idleOverride: DaylineGlyphSignal? = null
    private var lastDerived = DaylineGlyphSignal.IDLE
    private var derivedDisplayUntil = 0L
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
        bridge = NothingGlyphBridge(applicationContext, appMatrix = false)
    }

    override fun onBind(intent: Intent?): IBinder? {
        running = true
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
                prefs.reduceMotion -> 30_000L
                prefs.mode == GlyphMode.OFF -> 30_000L
                else -> 1_000L
            }
            handler.postDelayed(this, delay)
        }
    }

    private fun renderNow() {
        val prefs = store.loadGlyphPreferences()
        if (!prefs.enabled) {
            bridge.show(IntArray(GlyphMatrixPatterns.SIZE * GlyphMatrixPatterns.SIZE))
            return
        }

        val nowMillis = System.currentTimeMillis()
        val quiet = prefs.quietHoursEnabled && GlyphStateResolver.inQuietHours(
            java.time.LocalTime.now(), prefs.quietStart, prefs.quietEnd
        )
        val brightness = if (prefs.dimAtNight && quiet) 96 else 255

        val queued = if (prefs.mode == GlyphMode.EYES_AND_STATES && prefs.showAppStates) {
            runtime.current(nowMillis)
        } else null

        val derivedRaw = GlyphStateResolver.resolve(
            context = applicationContext,
            items = store.loadItems(),
            preferences = prefs
        )
        val derived = if (derivedRaw == DaylineGlyphSignal.REST && !prefs.restAnimation) {
            GlyphStateResolver.idleSignal(prefs)
        } else {
            derivedRaw
        }

        // App states are signals, not a permanent replacement for the face. Show a
        // derived state once when it changes, then return to expressive eyes.
        if (derived != lastDerived) {
            lastDerived = derived
            if (derived.priority > 0) {
                val seconds = when (derived) {
                    DaylineGlyphSignal.REMINDER_SOON -> prefs.reminderFlashSeconds
                    else -> if (prefs.returnToEyes) prefs.stateDurationSeconds else 30
                }.coerceIn(1, 30)
                derivedDisplayUntil = nowMillis + seconds * 1_000L
            } else {
                derivedDisplayUntil = 0L
            }
        }

        val focusedBase = when {
            derived == DaylineGlyphSignal.FOCUS -> DaylineGlyphSignal.SQUINT
            derived == DaylineGlyphSignal.REST && prefs.restAnimation -> DaylineGlyphSignal.SLEEPY
            else -> GlyphStateResolver.idleSignal(prefs)
        }

        val signal = when {
            quiet -> DaylineGlyphSignal.SLEEPY
            queued != null -> queued
            derived.priority > 0 && nowMillis < derivedDisplayUntil -> derived
            prefs.reduceMotion -> focusedBase
            else -> naturalIdle(prefs, nowMillis, focusedBase)
        }

        val focusPulse = prefs.focusStyle == GlyphFocusStyle.ACTIVE &&
            derived == DaylineGlyphSignal.FOCUS && !prefs.reduceMotion && !quiet
        val effectiveBrightness = if (focusPulse && (nowMillis / 1_000L) % 2L == 1L) {
            (brightness * 0.62f).toInt()
        } else {
            brightness
        }

        if (!blinking) {
            bridge.show(GlyphMatrixPatterns.frame(signal, effectiveBrightness))
        }
    }

    private fun naturalIdle(
        prefs: GlyphPreferences,
        now: Long,
        base: DaylineGlyphSignal
    ): DaylineGlyphSignal {
        if (nextBlinkAt == 0L || nextGlanceAt == 0L) scheduleNaturalMotion(force = true)

        if (prefs.blinkEnabled && !blinking && now >= nextBlinkAt) {
            nextBlinkAt = now + Random.nextLong(5_000L, 11_000L)
            playBlink(base)
            return DaylineGlyphSignal.CENTER
        }

        if (prefs.randomGlancesEnabled && now >= nextGlanceAt) {
            idleOverride = if (Random.nextBoolean()) DaylineGlyphSignal.LOOK_LEFT else DaylineGlyphSignal.LOOK_RIGHT
            nextGlanceAt = now + glanceDelay(prefs.glanceFrequency)
            handler.postDelayed({ idleOverride = null }, 900L)
        }
        return idleOverride ?: base
    }

    /**
     * Keep the closed frame on screen long enough for the Glyph service/hardware
     * to actually render it. The old ~140 ms closed phase could disappear between
     * hardware frame updates and look like no blink at all.
     */
    private fun playBlink(base: DaylineGlyphSignal) {
        blinking = true
        val startPrefs = store.loadGlyphPreferences()
        bridge.show(
            GlyphMatrixPatterns.frame(
                DaylineGlyphSignal.CENTER,
                currentBrightness(startPrefs)
            )
        )

        handler.postDelayed({
            if (!running) return@postDelayed
            val prefs = store.loadGlyphPreferences()
            bridge.show(
                GlyphMatrixPatterns.frame(
                    DaylineGlyphSignal.BLINK,
                    currentBrightness(prefs)
                )
            )
        }, 120L)

        handler.postDelayed({
            if (!running) return@postDelayed
            val prefs = store.loadGlyphPreferences()
            bridge.show(
                GlyphMatrixPatterns.frame(
                    base,
                    currentBrightness(prefs)
                )
            )
            blinking = false
        }, 420L)
    }

    private fun currentBrightness(prefs: GlyphPreferences): Int {
        val quiet = prefs.quietHoursEnabled && GlyphStateResolver.inQuietHours(
            java.time.LocalTime.now(), prefs.quietStart, prefs.quietEnd
        )
        return if (prefs.dimAtNight && quiet) 96 else 255
    }

    private fun scheduleNaturalMotion(force: Boolean = false) {
        val now = System.currentTimeMillis()
        if (force || nextBlinkAt <= now) nextBlinkAt = now + Random.nextLong(5_000L, 11_000L)
        if (force || nextGlanceAt <= now) {
            nextGlanceAt = now + glanceDelay(store.loadGlyphPreferences().glanceFrequency)
        }
    }

    private fun glanceDelay(frequency: GlyphGlanceFrequency): Long = when (frequency) {
        GlyphGlanceFrequency.RARE -> Random.nextLong(12_000L, 25_000L)
        GlyphGlanceFrequency.NORMAL -> Random.nextLong(8_000L, 16_000L)
        GlyphGlanceFrequency.FREQUENT -> Random.nextLong(5_000L, 10_000L)
    }

    private fun stopLoop() {
        running = false
        blinking = false
        handler.removeCallbacksAndMessages(null)
        bridge.close()
    }
}
