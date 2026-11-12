$root = "C:\Harshit\Hackathons\upcoming\spoof-bench\defender-core"

@'
package com.spoofbench.defender.core.signals

import com.spoofbench.defender.core.LocationEvent

/**
 * Flags location updates that arrive at suspiciously perfect, unvarying
 * intervals -- real GNSS fixes have natural jitter; many synthetic/mocked
 * feeds don't.
 */
class RegularitySignal(
    private val historySize: Int = 5,
    private val minIntervalStdDevMs: Double = 15.0
) {
    private var previousTNs: Long? = null
    private val intervalsMs = ArrayDeque<Double>()

    fun evaluate(event: LocationEvent): Double {
        val prev = previousTNs
        previousTNs = event.tNs
        if (prev == null) return 0.0

        val intervalMs = (event.tNs - prev) / 1_000_000.0
        intervalsMs.addLast(intervalMs)
        if (intervalsMs.size > historySize) intervalsMs.removeFirst()

        if (intervalsMs.size < historySize) return 0.0

        val mean = intervalsMs.average()
        val variance = intervalsMs.sumOf { (it - mean) * (it - mean) } / intervalsMs.size
        val stdDev = Math.sqrt(variance)

        return if (stdDev < minIntervalStdDevMs) 1.0 else 0.0
    }
}
'@ | Set-Content "$root\src\main\kotlin\com\spoofbench\defender\core\signals\RegularitySignal.kt"

@'
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
'@ | Set-Content "$root\src\test\kotlin\com\spoofbench\defender\core\signals\RegularitySignalTest.kt"

@'
package com.spoofbench.defender.core

import com.spoofbench.defender.core.signals.MockFlagSignal
import com.spoofbench.defender.core.signals.PhysicsSignal
import com.spoofbench.defender.core.signals.RegularitySignal
import kotlin.math.roundToInt

class DefaultLocationTrustEngine(
    private val flagThreshold: Int = 50
) : LocationTrustEngine {

    private val physicsSignal = PhysicsSignal()
    private val mockFlagSignal = MockFlagSignal()
    private val regularitySignal = RegularitySignal()

    override fun onLocation(event: LocationEvent): Verdict {
        val evidence = mapOf(
            "mockFlag" to mockFlagSignal.evaluate(event),
            "physics" to physicsSignal.evaluate(event),
            "regularity" to regularitySignal.evaluate(event)
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
        // wired in for Phase 7 (sensor-consistency signal)
    }

    override fun reset() {
        // revisited once signals carry more history
    }
}
'@ | Set-Content "$root\src\main\kotlin\com\spoofbench\defender\core\DefaultLocationTrustEngine.kt"

Write-Host "Done."