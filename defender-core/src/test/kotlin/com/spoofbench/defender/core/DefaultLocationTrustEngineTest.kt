package com.spoofbench.defender.core

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class DefaultLocationTrustEngineTest {

    private fun event(
        tNs: Long, lat: Double, lon: Double, isMock: Boolean? = false
    ) = LocationEvent(
        tNs = tNs, lat = lat, lon = lon, accuracyM = 5f, speedMps = null,
        bearingDeg = null, altM = null, provider = "gps", isMock = isMock,
        elapsedRealtimeNs = tNs
    )

    @Test
    fun `genuine walking updates are trusted`() {
        val engine = DefaultLocationTrustEngine()
        engine.onLocation(event(0L, 28.6139, 77.2090))
        val verdict = engine.onLocation(event(1_000_000_000L, 28.61391, 77.2090))
        assertFalse(verdict.flagged)
        assertTrue(verdict.trust > 50)
    }

    @Test
    fun `mock provider is flagged`() {
        val engine = DefaultLocationTrustEngine()
        val verdict = engine.onLocation(event(0L, 28.6139, 77.2090, isMock = true))
        assertTrue(verdict.flagged)
    }

    @Test
    fun `teleporting is flagged`() {
        val engine = DefaultLocationTrustEngine()
        engine.onLocation(event(0L, 28.6139, 77.2090))
        val verdict = engine.onLocation(event(1_000_000_000L, 28.70, 77.30))
        assertTrue(verdict.flagged)
    }
}
