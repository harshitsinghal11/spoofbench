package com.spoofbench.defender.core.signals

import com.spoofbench.defender.core.LocationEvent
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class ProviderPlausibilitySignalTest {

    private fun event(provider: String, accuracyM: Float?, satellites: Int?) = LocationEvent(
        tNs = 0L, lat = 28.0, lon = 77.0, accuracyM = accuracyM,
        speedMps = 0f, bearingDeg = 0f, altM = 0.0, provider = provider,
        isMock = false, elapsedRealtimeNs = 0L, satellites = satellites
    )

    @Test
    fun `network provider with GPS-grade accuracy is flagged`() {
        val signal = ProviderPlausibilitySignal()
        assertEquals(1.0, signal.evaluate(event("network", 8f, null)))
    }

    @Test
    fun `strong satellite lock with poor accuracy is flagged`() {
        val signal = ProviderPlausibilitySignal()
        assertEquals(1.0, signal.evaluate(event("gps", 80f, 9)))
    }

    @Test
    fun `too few satellites for high precision is flagged`() {
        val signal = ProviderPlausibilitySignal()
        assertEquals(1.0, signal.evaluate(event("gps", 5f, 2)))
    }

    @Test
    fun `plausible gps fix is not flagged`() {
        val signal = ProviderPlausibilitySignal()
        assertEquals(0.0, signal.evaluate(event("gps", 12f, 7)))
    }

    @Test
    fun `missing satellite data is not falsely flagged`() {
        val signal = ProviderPlausibilitySignal()
        assertEquals(0.0, signal.evaluate(event("gps", 12f, null)))
    }
}