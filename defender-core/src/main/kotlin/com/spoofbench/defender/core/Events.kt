package com.spoofbench.defender.core

data class LocationEvent(
    val tNs: Long,
    val lat: Double,
    val lon: Double,
    val accuracyM: Float?,
    val speedMps: Float?,
    val bearingDeg: Float?,
    val altM: Double?,
    val provider: String,
    val isMock: Boolean?,
    val elapsedRealtimeNs: Long,
    val satellites: Int? = null
)

data class ImuEvent(
    val tNs: Long,
    val type: ImuType,
    val x: Float,
    val y: Float,
    val z: Float
)

enum class ImuType { ACCEL, GYRO }

data class Verdict(
    val tNs: Long,
    val trust: Int,
    val flagged: Boolean,
    val evidence: Map<String, Double>
)
