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
