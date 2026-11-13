package com.spoofbench.defender.core.signals

import com.spoofbench.defender.core.LocationEvent
import kotlin.math.sqrt

/**
 * Flags a run of location updates whose reported accuracy barely varies.
 * Real GNSS accuracy jitters fix-to-fix (multipath, satellite geometry
 * shifting); many mock-location apps hardcode a fixed accuracy value.
 */
class SignalQualitySignal(
    private val windowSize: Int = 5,
    private val minStdDevM: Double = 0.3
) {
    private val history = ArrayDeque<Double>()

    /** Returns evidence in [0,1] — 0 = looks fine, 1 = suspiciously constant. */
    fun evaluate(event: LocationEvent): Double {
        val acc = event.accuracyM?.toDouble() ?: return 0.0
        history.addLast(acc)
        if (history.size > windowSize) history.removeFirst()
        if (history.size < windowSize) return 0.0

        val mean = history.average()
        val variance = history.sumOf { (it - mean) * (it - mean) } / history.size
        val stdDev = sqrt(variance)

        return if (stdDev < minStdDevM) 1.0 else 0.0
    }
}