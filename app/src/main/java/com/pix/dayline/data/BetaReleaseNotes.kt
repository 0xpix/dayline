package com.pix.dayline.data

/**
 * Normalizes GitHub beta release notes into the compact Added / Changed / Fixed
 * structure rendered by the in-app updater.
 */
object BetaReleaseNotes {
    fun clean(value: String): String {
        if (value.isBlank()) return "## Changed\n• Bug fixes and Dayline polish."

        val allowed = setOf("Added", "Changed", "Fixed")
        val output = mutableListOf<String>()
        var current: String? = null

        value.lineSequence().forEach { raw ->
            val line = raw.trim()
            when {
                line.startsWith("## ") && line.removePrefix("## ").trim() in allowed -> {
                    current = line.removePrefix("## ").trim()
                    output += "## $current"
                }
                line.startsWith("## ") -> current = null
                line.startsWith("#") -> Unit
                line.isBlank() -> if (output.lastOrNull()?.isNotBlank() == true) output += ""
                line.contains("Full Changelog", ignoreCase = true) -> Unit
                line.startsWith("http://") || line.startsWith("https://") -> Unit
                current != null -> {
                    val clean = line
                        .trimStart('-', '*', '•', ' ')
                        .replace("**", "")
                        .replace("`", "")
                        .trim()
                    if (clean.isNotBlank()) output += "• $clean"
                }
            }
        }

        val compact = output.joinToString("\n").trim().take(MAX_NOTES_LENGTH)
        return if (
            compact.contains("## Added") ||
            compact.contains("## Changed") ||
            compact.contains("## Fixed")
        ) {
            compact
        } else {
            "## Changed\n• Bug fixes and Dayline polish."
        }
    }

    private const val MAX_NOTES_LENGTH = 3_000
}
