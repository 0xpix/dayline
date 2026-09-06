package com.pix.dayline.widgets

import android.content.Context

object NotoEmojiCatalog {
    @Volatile
    private var cached: List<String>? = null

    fun all(context: Context): List<String> {
        cached?.let { return it }

        val candidates = buildList {
            addAll(
                listOf(
                    "©", "®", "™", "‼", "⁉", "ℹ",
                    "↔", "↕", "↖", "↗", "↘", "↙",
                    "↩", "↪", "⌚", "⌛", "⌨",
                    "⏏", "⏩", "⏪", "⏫", "⏬",
                    "⏰", "⏳", "◀", "▶", "◻", "◼"
                )
            )

            addRange(0x2300, 0x23FF)
            addRange(0x2500, 0x25FF)
            addRange(0x2600, 0x26FF)
            addRange(0x2700, 0x27BF)
            addRange(0x2B00, 0x2BFF)
            addRange(0x1F000, 0x1FAFF)

            for (char in "0123456789#*") {
                add("$char\uFE0F\u20E3")
            }

            for (first in 'A'..'Z') {
                for (second in 'A'..'Z') {
                    add(
                        regionalIndicator(first) +
                            regionalIndicator(second)
                    )
                }
            }
        }

        val result = candidates
            .asSequence()
            .filter {
                NotoEmojiRenderer.hasGlyph(
                    context,
                    it
                )
            }
            .distinct()
            .toList()

        cached = result
        return result
    }

    private fun MutableList<String>.addRange(
        start: Int,
        end: Int
    ) {
        for (codePoint in start..end) {
            if (
                Character.isDefined(codePoint) &&
                !Character.isISOControl(codePoint)
            ) {
                add(
                    String(
                        Character.toChars(codePoint)
                    )
                )
            }
        }
    }

    private fun regionalIndicator(
        letter: Char
    ): String {
        val codePoint =
            0x1F1E6 +
                (letter.code - 'A'.code)

        return String(
            Character.toChars(codePoint)
        )
    }
}
