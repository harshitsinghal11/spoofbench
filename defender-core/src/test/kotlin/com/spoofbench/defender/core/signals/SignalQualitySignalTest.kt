package com.spoofbench.defender.core.signals

import com.spoofbench.defender.core.LocationEvent
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class SignalQualitySignalTest {

    private fun event(tNs: Long, accuracyM: Float?) = LocationEvent(
        tNs = tNs, lat = 28.0, lon = 77.0, accuracyM = accuracyM,
        speedMps = 0f, bearingDeg = 0f, altM = 0.0, provider = "gps",
        isMock = false, elapsedRealtimeNs = tNs, satellites = null
    )

    @Test
    fun `constant accuracy over window is flagged`() {
        val signal = SignalQualitySignal(windowSize = 5)
        var last = 0.0
        repeat(5) { i ->
            last = signal.evaluate(event(i * 1_000_000_000L, 5.0f))
        }
        assertEquals(1.0, last)
    }

    @Test
    fun `naturally jittering accuracy is not flagged`() {
        val signal = SignalQualitySignal(windowSize = 5)
        val values = listOf(4.2f, 6.8f, 3.1f, 7.5f, 5.0f)
        var last = 0.0
        values.forEachIndexed { i, v ->
            last = signal.evaluate(event(i * 1_000_000_000L, v))
        }
        assertEquals(0.0, last)
    }

    @Test
    fun `null accuracy is never flagged`() {
        val signal = SignalQualitySignal()
        assertEquals(0.0, signal.evaluate(event(0L, null)))
    }
}