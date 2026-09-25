package com.pix.dayline.data

/**
 * Normalizes updater-facing release notes and extracts exact per-version notes
 * from Dayline's repository changelog.
 */
object BetaReleaseNotes {
    fun fromChangelog(value: String): Map<String, String> {
        if (value.isBlank()) return emptyMap()

        val result = linkedMapOf<String, String>()
        val allowed = setOf("Added", "Changed", "Fixed")
        var version: String? = null
        var section: String? = null
        val lines = mutableListOf<String>()

        fun flushVersion() {
            val currentVersion = version ?: return
            val compact = lines.joinToString("\n").trim()
            if (compact.isNotBlank()) result[currentVersion] = clean(compact)
            lines.clear()
        }

        value.lineSequence().forEach { raw ->
            val line = raw.trim()
            when {
                line.startsWith("## ") && !line.startsWith("### ") -> {
                    flushVersion()
                    val heading = line.removePrefix("## ").trim()
                    version = heading
                        .substringBefore(" — ")
                        .substringBefore(" - ")
                        .trim()
                        .removePrefix("v")
                        .removePrefix("V")
                        .takeIf(DaylineVersion::hasNumericVersion)
                    section = null
                }
                line.startsWith("### ") -> {
                    val candidate = line.removePrefix("### ").trim()
                    section = candidate.takeIf { it in allowed }
                    if (version != null && section != null) lines += "## $section"
                }
                line.isBlank() -> Unit
                version != null && section != null -> {
                    val clean = cleanBullet(line)
                    if (clean.isNotBlank()) lines += "• $clean"
                }
            }
        }
        flushVersion()
        return result
    }

    fun clean(value: String): String {
        if (value.isBlank()) return FALLBACK

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
                line.isBlank() -> Unit
                line.contains("Full Changelog", ignoreCase = true) -> Unit
                line.startsWith("http://") || line.startsWith("https://") -> Unit
                current != null -> {
                    val clean = cleanBullet(line)
                    if (clean.isNotBlank()) output += "• $clean"
                }
            }
        }

        val compact = output.joinToString("\n").trim().take(MAX_NOTES_LENGTH)
        return if (
            compact.contains("## Added") ||
            compact.contains("## Changed") ||
            compact.contains("## Fixed")
        ) compact else FALLBACK
    }

    private fun cleanBullet(value: String): String = value
        .trimStart('-', '*', '•', ' ')
        .replace("**", "")
        .replace("`", "")
        .trim()

    private const val FALLBACK = "## Changed\n• Bug fixes and Dayline polish."
    private const val MAX_NOTES_LENGTH = 3_000
}
