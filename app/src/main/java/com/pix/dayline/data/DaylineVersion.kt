package com.pix.dayline.data

/** Small, Android-independent version helpers used by the GitHub beta channel. */
object DaylineVersion {
    fun compare(left: String, right: String): Int {
        val a = numeric(left)
        val b = numeric(right)
        val length = maxOf(a.size, b.size)
        for (index in 0 until length) {
            val av = a.getOrElse(index) { 0 }
            val bv = b.getOrElse(index) { 0 }
            if (av != bv) return av.compareTo(bv)
        }

        // For the same numeric version, a stable build is newer than its beta.
        val aBeta = left.contains("beta", ignoreCase = true)
        val bBeta = right.contains("beta", ignoreCase = true)
        return when {
            aBeta == bBeta -> 0
            aBeta -> -1
            else -> 1
        }
    }

    fun hasNumericVersion(value: String): Boolean = numeric(value).isNotEmpty()

    /**
     * Dayline's current beta versionCode convention is minor*100 + patch:
     * 0.14.7 -> 1407, 0.13.1 -> 1301.
     *
     * Older pre-0.13 betas used a different scheme, so callers should only use
     * this as an installability guard for current/newer releases.
     */
    fun betaVersionCode(value: String): Long? {
        val parts = numeric(value)
        if (parts.size < 3) return null
        val major = parts[0].toLong()
        val minor = parts[1].toLong()
        val patch = parts[2].toLong()
        return major * 1_000_000L + minor * 100L + patch
    }

    fun isInstallableUpdate(
        candidateVersion: String,
        currentVersion: String,
        currentVersionCode: Long
    ): Boolean {
        if (compare(candidateVersion, currentVersion) <= 0) return false
        val candidateCode = betaVersionCode(candidateVersion) ?: return true
        return candidateCode > currentVersionCode
    }

    private fun numeric(value: String): List<Int> = normalize(value)
        .split('.')
        .mapNotNull { token -> token.takeWhile { it.isDigit() }.toIntOrNull() }

    private fun normalize(value: String): String = value
        .trim()
        .removePrefix("v")
        .removePrefix("V")
}
