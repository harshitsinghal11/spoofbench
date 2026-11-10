package com.spoofbench.defender.core.signals

import com.spoofbench.defender.core.LocationEvent
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class PhysicsSignalTest {

    private fun event(tNs: Long, lat: Double, lon: Double) = LocationEvent(
        tNs = tNs, lat = lat, lon = lon, accuracyM = 5f, speedMps = null,
        bearingDeg = null, altM = null, provider = "gps", isMock = false,
        elapsedRealtimeNs = tNs
    )

    @Test
    fun `normal walking speed is not flagged`() {
        val signal = PhysicsSignal()
        signal.evaluate(event(0L, 28.6139, 77.2090))
        val result = signal.evaluate(event(1_000_000_000L, 28.61391, 77.2090))
        assertEquals(0.0, result)
    }

    @Test
    fun `teleporting 15km in one second is flagged`() {
        val signal = PhysicsSignal()
        signal.evaluate(event(0L, 28.6139, 77.2090))
        val result = signal.evaluate(event(1_000_000_000L, 28.70, 77.30))
        assertEquals(1.0, result)
    }
}
