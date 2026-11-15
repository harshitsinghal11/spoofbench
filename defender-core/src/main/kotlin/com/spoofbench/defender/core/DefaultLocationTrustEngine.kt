package com.spoofbench.defender.core

import com.spoofbench.defender.core.signals.MockFlagSignal
import com.spoofbench.defender.core.signals.PhysicsSignal
import com.spoofbench.defender.core.signals.ProviderPlausibilitySignal
import com.spoofbench.defender.core.signals.RegularitySignal
import com.spoofbench.defender.core.signals.SignalQualitySignal
import kotlin.math.roundToInt

class DefaultLocationTrustEngine(
    private val flagThreshold: Int = 50
) : LocationTrustEngine {

    private val physicsSignal = PhysicsSignal()
    private val mockFlagSignal = MockFlagSignal()
    private val regularitySignal = RegularitySignal()
    private val signalQualitySignal = SignalQualitySignal()
    private val providerPlausibilitySignal = ProviderPlausibilitySignal()

    override fun onLocation(event: LocationEvent): Verdict {
        val evidence = mapOf(
            "mockFlag" to mockFlagSignal.evaluate(event),
            "physics" to physicsSignal.evaluate(event),
            "regularity" to regularitySignal.evaluate(event),
            "signalQuality" to signalQualitySignal.evaluate(event),
            "providerPlausibility" to providerPlausibilitySignal.evaluate(event)
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