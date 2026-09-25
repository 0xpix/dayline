package com.pix.dayline.data

/** Pure release-history selection used by the beta updater and unit tests. */
object BetaReleaseHistory {
    fun newestFirst(releases: List<BetaRelease>): List<BetaRelease> = releases
        .distinctBy { it.versionName.lowercase() }
        .sortedWith(Comparator { left, right ->
            DaylineVersion.compare(right.versionName, left.versionName)
        })

    fun newestInstallable(
        releases: List<BetaRelease>,
        currentVersion: String,
        currentVersionCode: Long
    ): BetaRelease? = newestFirst(releases).firstOrNull { release ->
        DaylineVersion.isInstallableUpdate(
            candidateVersion = release.versionName,
            currentVersion = currentVersion,
            currentVersionCode = currentVersionCode
        )
    }

    fun missedBetween(
        releases: List<BetaRelease>,
        currentVersion: String,
        targetVersion: String
    ): List<BetaRelease> = releases
        .filter { release ->
            DaylineVersion.compare(release.versionName, currentVersion) > 0 &&
                DaylineVersion.compare(release.versionName, targetVersion) <= 0
        }
        .distinctBy { it.versionName.lowercase() }
        .sortedWith(Comparator { left, right ->
            DaylineVersion.compare(left.versionName, right.versionName)
        })
}
