package com.pix.dayline.data

/**
 * Normalizes GitHub beta release notes into the compact Added / Changed / Fixed
 * structure rendered by the in-app updater.
 */
object BetaReleaseNotes {
    fun bundleBetween(
        currentVersion: String,
        targetVersion: String,
        releases: List<Pair<String, String>>
    ): String {
        val missed = releases
            .filter { (version, _) ->
                DaylineVersion.compare(version, currentVersion) > 0 &&
                    DaylineVersion.compare(version, targetVersion) <= 0
            }
            .distinctBy { it.first.lowercase() }
            .sortedWith(Comparator { left, right ->
                DaylineVersion.compare(right.first, left.first)
            })

        if (missed.isEmpty()) return clean("")

        return missed.joinToString("\n\n") { (version, notes) ->
            "# $version\n${clean(notes)}"
        }
    }

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
                // The updater renders sections as separate cards, so source
                // Markdown spacing should not create extra blank rows in the
                // normalized note payload.
                line.isBlank() -> Unit
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
