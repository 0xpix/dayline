package com.pix.dayline.data

val WidgetEmojiChoice.symbol: String
    get() = when (this) {
        WidgetEmojiChoice.SMILE -> "🙂"
        WidgetEmojiChoice.GRIN -> "😁"
        WidgetEmojiChoice.WINK -> "😉"
        WidgetEmojiChoice.COOL -> "😎"
        WidgetEmojiChoice.NERD -> "🤓"
        WidgetEmojiChoice.PARTY -> "🥳"
        WidgetEmojiChoice.SLEEPY -> "😴"
        WidgetEmojiChoice.MELT -> "🫠"
        WidgetEmojiChoice.GHOST -> "👻"
        WidgetEmojiChoice.ROBOT -> "🤖"
        WidgetEmojiChoice.RELAXED -> "😌"
        WidgetEmojiChoice.HEART_EYES -> "😍"
    }

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
