package com.pix.dayline.data

/** Small, Android-independent version comparator used by the GitHub beta channel. */
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

    private fun numeric(value: String): List<Int> = normalize(value)
        .split('.')
        .mapNotNull { token -> token.takeWhile { it.isDigit() }.toIntOrNull() }

    private fun normalize(value: String): String = value
        .trim()
        .removePrefix("v")
        .removePrefix("V")
}
