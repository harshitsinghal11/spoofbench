$root = "C:\Harshit\Hackathons\upcoming\spoof-bench\defender-core"

@'
package com.spoofbench.defender.core.signals

import com.spoofbench.defender.core.LocationEvent

/** Flags a location update where the OS itself reports it as coming from a mock provider. */
class MockFlagSignal {
    fun evaluate(event: LocationEvent): Double = if (event.isMock == true) 1.0 else 0.0
}
'@ | Set-Content "$root\src\main\kotlin\com\spoofbench\defender\core\signals\MockFlagSignal.kt"

@'
package com.spoofbench.defender.core

import com.spoofbench.defender.core.signals.MockFlagSignal
import com.spoofbench.defender.core.signals.PhysicsSignal
import kotlin.math.roundToInt

class DefaultLocationTrustEngine(
    private val flagThreshold: Int = 50
) : LocationTrustEngine {

    private val physicsSignal = PhysicsSignal()
    private val mockFlagSignal = MockFlagSignal()

    override fun onLocation(event: LocationEvent): Verdict {
        val evidence = mapOf(
            "mockFlag" to mockFlagSignal.evaluate(event),
            "physics" to physicsSignal.evaluate(event)
        )
        val worst = evidence.values.maxOrNull() ?: 0.0
        val trust = ((1.0 - worst) * 100).roundToInt().coerceIn(0, 100)
        return Verdict(
            tNs = event.tNs,
            trust = trust,
            flagged = trust < flagThreshold,
            evidence = evidence
        )
    }

    override fun onImu(event: ImuEvent) {
        // not used by any signal yet - wired in for Phase 7 (sensor-consistency signal)
    }

    override fun reset() {
        // revisited once signals carry more history
    }
}
'@ | Set-Content "$root\src\main\kotlin\com\spoofbench\defender\core\DefaultLocationTrustEngine.kt"

@'
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
'@ | Set-Content "$root\src\test\kotlin\com\spoofbench\defender\core\DefaultLocationTrustEngineTest.kt"

Write-Host "Done."