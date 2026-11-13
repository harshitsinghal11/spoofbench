package com.spoofbench.defender.core.signals

import com.spoofbench.defender.core.LocationEvent

/**
 * Flags a location update whose provider claim contradicts its own
 * accuracy/satellite numbers — e.g. "network" provider reporting
 * GPS-grade precision, or "gps" provider reporting a strong satellite
 * lock alongside poor accuracy.
 */
class ProviderPlausibilitySignal {

    fun evaluate(event: LocationEvent): Double {
        val accuracy = event.accuracyM
        val satellites = event.satellites
        var evidence = 0.0

        // Cell/Wi-Fi triangulation can't normally beat ~15m.
        if (event.provider == "network" && accuracy != null && accuracy < 15f) {
            evidence = maxOf(evidence, 1.0)
        }

        // Strong satellite lock but poor accuracy is contradictory.
        if (event.provider == "gps" && satellites != null && satellites >= 6 &&
            accuracy != null && accuracy > 50f) {
            evidence = maxOf(evidence, 1.0)
        }

        // Too few satellites to justify sub-10m precision.
        if (satellites != null && satellites in 1..3 && accuracy != null && accuracy < 10f) {
            evidence = maxOf(evidence, 1.0)
        }

        return evidence
    }
}