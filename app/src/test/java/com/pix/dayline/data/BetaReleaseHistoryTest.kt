package com.pix.dayline.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Instant

class BetaReleaseHistoryTest {
    private fun release(version: String): BetaRelease = BetaRelease(
        tagName = "v$version",
        versionName = version,
        title = version,
        notes = "## Changed\n• $version",
        publishedAt = Instant.EPOCH,
        htmlUrl = "https://example.test/$version"
    )

    @Test
    fun returnsEveryExactMissedReleaseInUpgradeOrder() {
        val releases = listOf(
            release("0.18.8.beta"),
            release("0.18.7.beta"),
            release("0.18.6.beta"),
            release("0.18.0.beta"),
            release("0.17.4.beta"),
            release("0.17.3.beta"),
            release("0.17.2.beta"),
            release("0.17.1.beta"),
            release("0.17.0.beta"),
            release("0.16.1.beta")
        )

        val missed = BetaReleaseHistory.missedBetween(
            releases = releases,
            currentVersion = "0.17.0.beta",
            targetVersion = "0.18.8.beta"
        )

        assertEquals(
            listOf(
                "0.17.1.beta",
                "0.17.2.beta",
                "0.17.3.beta",
                "0.17.4.beta",
                "0.18.0.beta",
                "0.18.6.beta",
                "0.18.7.beta",
                "0.18.8.beta"
            ),
            missed.map { it.versionName }
        )
    }

    @Test
    fun pendingPostUpdateRangeSurvivesFailedFetchesAndSkipsFirstInstall() {
        assertEquals(
            "0.18.9.beta",
            BetaReleaseHistory.pendingFromVersion(
                lastLaunchedVersion = "0.18.9.beta",
                pendingFromVersion = null,
                currentVersion = "0.18.10.beta"
            )
        )
        assertEquals(
            "0.18.8.beta",
            BetaReleaseHistory.pendingFromVersion(
                lastLaunchedVersion = "0.18.10.beta",
                pendingFromVersion = "0.18.8.beta",
                currentVersion = "0.18.10.beta"
            )
        )
        assertNull(
            BetaReleaseHistory.pendingFromVersion(
                lastLaunchedVersion = null,
                pendingFromVersion = null,
                currentVersion = "0.18.10.beta"
            )
        )
        assertNull(
            BetaReleaseHistory.pendingFromVersion(
                lastLaunchedVersion = "0.18.10.beta",
                pendingFromVersion = null,
                currentVersion = "0.18.10.beta"
            )
        )
    }

    @Test
    fun newestInstallableRespectsNameAndVersionCode() {
        val releases = listOf(release("0.18.8.beta"), release("0.18.7.beta"))
        assertEquals(
            "0.18.8.beta",
            BetaReleaseHistory.newestInstallable(releases, "0.18.7.beta", 1807L)?.versionName
        )
        assertNull(BetaReleaseHistory.newestInstallable(releases, "0.18.8.beta", 1808L))
    }
}
