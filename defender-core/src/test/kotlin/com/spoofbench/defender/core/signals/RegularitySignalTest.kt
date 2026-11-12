package com.spoofbench.defender.core.signals

import com.spoofbench.defender.core.LocationEvent
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class RegularitySignalTest {

    private fun event(tNs: Long) = LocationEvent(
        tNs = tNs, lat = 28.6139, lon = 77.2090, accuracyM = 5f, speedMps = null,
        bearingDeg = null, altM = null, provider = "gps", isMock = false,
        elapsedRealtimeNs = tNs
    )

    @Test
    fun `jittery real-world intervals are not flagged`() {
        val signal = RegularitySignal()
        val stampsMs = listOf(0L, 980L, 2015L, 2990L, 4060L, 5005L)
        var result = 0.0
        for (ms in stampsMs) {
            result = signal.evaluate(event(ms * 1_000_000))
        }
        assertEquals(0.0, result)
    }

    @Test
    fun `perfectly uniform synthetic intervals are flagged`() {
        val signal = RegularitySignal()
        var result = 0.0
        for (i in 0..6) {
            result = signal.evaluate(event(i * 1_000_000_000L))
        }
        assertEquals(1.0, result)
    }
}
