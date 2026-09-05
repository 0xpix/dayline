package com.pix.dayline.data

import androidx.annotation.DrawableRes
import com.pix.dayline.R

val WidgetEmojiChoice.label: String
    get() = when (this) {
        WidgetEmojiChoice.SMILE -> "Smile"
        WidgetEmojiChoice.GRIN -> "Grin"
        WidgetEmojiChoice.WINK -> "Wink"
        WidgetEmojiChoice.COOL -> "Cool"
        WidgetEmojiChoice.NERD -> "Nerd"
        WidgetEmojiChoice.PARTY -> "Party"
        WidgetEmojiChoice.SLEEPY -> "Sleepy"
        WidgetEmojiChoice.MELT -> "Melt"
        WidgetEmojiChoice.GHOST -> "Ghost"
        WidgetEmojiChoice.ROBOT -> "Robot"
        WidgetEmojiChoice.RELAXED -> "Relaxed"
        WidgetEmojiChoice.HEART_EYES -> "Heart eyes"
    }

@get:DrawableRes
val WidgetEmojiChoice.iconRes: Int
    get() = when (this) {
        WidgetEmojiChoice.SMILE -> R.drawable.emoji_smile
        WidgetEmojiChoice.GRIN -> R.drawable.emoji_grin
        WidgetEmojiChoice.WINK -> R.drawable.emoji_wink
        WidgetEmojiChoice.COOL -> R.drawable.emoji_cool
        WidgetEmojiChoice.NERD -> R.drawable.emoji_nerd
        WidgetEmojiChoice.PARTY -> R.drawable.emoji_party
        WidgetEmojiChoice.SLEEPY -> R.drawable.emoji_sleepy
        WidgetEmojiChoice.MELT -> R.drawable.emoji_melt
        WidgetEmojiChoice.GHOST -> R.drawable.emoji_ghost
        WidgetEmojiChoice.ROBOT -> R.drawable.emoji_robot
        WidgetEmojiChoice.RELAXED -> R.drawable.emoji_relaxed
        WidgetEmojiChoice.HEART_EYES -> R.drawable.emoji_heart_eyes
    }
