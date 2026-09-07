package com.pix.dayline.model

import java.time.LocalTime

enum class GlyphMode { OFF, EYES_ONLY, EYES_AND_STATES }
enum class GlyphIdleExpression { CENTER, CURIOUS, SLEEPY, HAPPY }
enum class GlyphGlanceFrequency { RARE, NORMAL, FREQUENT }
enum class GlyphFocusStyle { SUBTLE, ACTIVE }

data class GlyphPreferences(
    // EYES_AND_STATES is kept only for backwards-compatible preference loading.
    // The Glyph runtime is eyes-first; Focus only interrupts them briefly with
    // 30-second MM:SS announcements at start / 5-minute / 1-minute checkpoints.
    val mode: GlyphMode = GlyphMode.EYES_ONLY,
    val idleExpression: GlyphIdleExpression = GlyphIdleExpression.CENTER,
    val blinkEnabled: Boolean = true,
    val randomGlancesEnabled: Boolean = true,
    val glanceFrequency: GlyphGlanceFrequency = GlyphGlanceFrequency.NORMAL,
    val showAppStates: Boolean = false,
    val stateDurationSeconds: Int = 3,
    val returnToEyes: Boolean = true,
    val reminderFlashSeconds: Int = 5,
    val focusStyle: GlyphFocusStyle = GlyphFocusStyle.SUBTLE,
    val restAnimation: Boolean = false,
    val quietHoursEnabled: Boolean = true,
    val quietStart: LocalTime = LocalTime.of(23, 0),
    val quietEnd: LocalTime = LocalTime.of(7, 0),
    val dimAtNight: Boolean = true,
    val reduceMotion: Boolean = false
) {
    val enabled: Boolean get() = mode != GlyphMode.OFF
}

enum class DaylineGlyphSignal(val priority: Int) {
    IDLE(0),
    CENTER(0),
    LOOK_LEFT(0),
    LOOK_RIGHT(0),
    BLINK(0),
    WINK(0),
    HAPPY(0),
    EXCITED(0),
    SLEEPY(0),
    SURPRISED(0),
    PLAYFUL(0),
    CURIOUS(0),
    SIDE_EYE(0),
    ROLLING(0),
    SQUINT(0),
    HEARTS(0),

    NO_PLANS(9),
    DAY_OPEN(10),
    FREE_NOW(12),
    SYNC_OK(15),
    MOVED(18),
    EVENT_ENDED(20),
    TASK_DONE(30),
    NEXT_EVENT(40),
    EVENT_STARTED(50),
    FOCUS(55),
    REST(55),
    REMINDER_SOON(70),
    GO(75),
    MISSED(80),
    SYNC_ERROR(85),
    CONFLICT(100)
}

data class GlyphHardwareStatus(
    val available: Boolean,
    val matrixSize: Int? = null,
    val deviceLabel: String = "Unavailable",
    val detail: String? = null
)
