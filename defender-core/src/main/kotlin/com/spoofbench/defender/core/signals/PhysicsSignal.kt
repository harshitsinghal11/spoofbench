package com.spoofbench.defender.core.signals

import com.spoofbench.defender.core.LocationEvent
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Flags a location update that implies an impossible speed between
 * it and the previous fix (a "teleport").
 */
class PhysicsSignal(private val maxPlausibleSpeedMps: Double = 70.0) {

    private var previous: LocationEvent? = null

    /** Returns evidence in [0,1] — 0 = looks fine, 1 = clearly impossible. */
    fun evaluate(event: LocationEvent): Double {
        val prev = previous
        previous = event
        if (prev == null) return 0.0

        val dtSeconds = (event.tNs - prev.tNs) / 1_000_000_000.0
        if (dtSeconds <= 0.0) return 1.0

        val distanceM = haversineMeters(prev.lat, prev.lon, event.lat, event.lon)
        val impliedSpeed = distanceM / dtSeconds

        return if (impliedSpeed > maxPlausibleSpeedMps) 1.0 else 0.0
    }

    private fun haversineMeters(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 6371000.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2).pow(2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLon / 2).pow(2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return r * c
    }
}
