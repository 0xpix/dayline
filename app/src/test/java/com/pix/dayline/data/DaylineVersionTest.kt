package com.pix.dayline.data

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.assertEquals
import org.junit.Test

class DaylineVersionTest {
    @Test
    fun comparesNumericVersions() {
        assertTrue(DaylineVersion.compare("0.17.1.beta", "0.17.0.beta") > 0)
        assertTrue(DaylineVersion.compare("0.18.0.beta", "0.17.9.beta") > 0)
        assertEquals(0, DaylineVersion.compare("v0.17.1.beta", "0.17.1.beta"))
    }

    @Test
    fun stableBuildWinsAtSameNumericVersion() {
        assertTrue(DaylineVersion.compare("0.17.1", "0.17.1.beta") > 0)
        assertTrue(DaylineVersion.compare("0.17.1.beta", "0.17.1") < 0)
    }

    @Test
    fun derivesCurrentBetaVersionCodeConvention() {
        assertEquals(1701L, DaylineVersion.betaVersionCode("0.17.1.beta"))
        assertEquals(1800L, DaylineVersion.betaVersionCode("v0.18.0.beta"))
    }

    @Test
    fun installabilityRequiresNewerNameAndCode() {
        assertTrue(
            DaylineVersion.isInstallableUpdate(
                candidateVersion = "0.17.2.beta",
                currentVersion = "0.17.1.beta",
                currentVersionCode = 1701L
            )
        )
        assertFalse(
            DaylineVersion.isInstallableUpdate(
                candidateVersion = "0.17.1.beta",
                currentVersion = "0.17.1.beta",
                currentVersionCode = 1701L
            )
        )
        assertFalse(
            DaylineVersion.isInstallableUpdate(
                candidateVersion = "0.17.2.beta",
                currentVersion = "0.17.1.beta",
                currentVersionCode = 1702L
            )
        )
    }

    @Test
    fun malformedVersionDoesNotProduceBetaCode() {
        assertEquals(null, DaylineVersion.betaVersionCode("banana"))
    }
}
