$root = "C:\Harshit\Hackathons\upcoming\spoof-bench\defender-core"
New-Item -ItemType Directory -Force -Path "$root\src\main\kotlin\com\spoofbench\defender\core\signals" | Out-Null
New-Item -ItemType Directory -Force -Path "$root\src\test\kotlin\com\spoofbench\defender\core\signals" | Out-Null

# Copy the Gradle wrapper from the already-working OwnTracks project (avoids re-downloading Gradle)
Copy-Item -Recurse -Force "C:\Harshit\Hackathons\upcoming\spoof-bench\target-owntracks\project\gradle" "$root\gradle"
Copy-Item -Force "C:\Harshit\Hackathons\upcoming\spoof-bench\target-owntracks\project\gradlew" "$root\gradlew"
Copy-Item -Force "C:\Harshit\Hackathons\upcoming\spoof-bench\target-owntracks\project\gradlew.bat" "$root\gradlew.bat"

@'
pluginManagement {
    repositories {
        gradlePluginPortal()
        google()
        mavenCentral()
    }
}
rootProject.name = "defender-core"
'@ | Set-Content "$root\settings.gradle.kts"

@'
plugins {
    kotlin("jvm") version "2.0.21"
}

repositories {
    mavenCentral()
}

dependencies {
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.3")
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(17)
}
'@ | Set-Content "$root\build.gradle.kts"

@'
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
'@ | Set-Content "$root\src\main\kotlin\com\spoofbench\defender\core\Events.kt"

@'
package com.spoofbench.defender.core

interface LocationTrustEngine {
    fun onLocation(event: LocationEvent): Verdict
    fun onImu(event: ImuEvent)
    fun reset()
}
'@ | Set-Content "$root\src\main\kotlin\com\spoofbench\defender\core\LocationTrustEngine.kt"

@'
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
'@ | Set-Content "$root\src\main\kotlin\com\spoofbench\defender\core\signals\PhysicsSignal.kt"

@'
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
'@ | Set-Content "$root\src\test\kotlin\com\spoofbench\defender\core\signals\PhysicsSignalTest.kt"

Write-Host "Done. Folder created at $root"