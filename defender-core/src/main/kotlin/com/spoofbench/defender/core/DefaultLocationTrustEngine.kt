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
