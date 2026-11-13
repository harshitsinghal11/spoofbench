$root = "C:\Harshit\Hackathons\upcoming\spoof-bench\defender-core"
New-Item -ItemType Directory -Force -Path "$root\src\main\kotlin\com\spoofbench\defender\core\replay" | Out-Null
New-Item -ItemType Directory -Force -Path "$root\sample_data" | Out-Null

@'
plugins {
    kotlin("jvm") version "2.0.21"
    application
}

repositories {
    mavenCentral()
}

dependencies {
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.3")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(17)
}

application {
    mainClass.set("com.spoofbench.defender.core.replay.CsvReplayKt")
}
'@ | Set-Content "$root\build.gradle.kts"

@'
package com.spoofbench.defender.core.replay

import com.spoofbench.defender.core.DefaultLocationTrustEngine
import com.spoofbench.defender.core.LocationEvent
import java.io.File

/**
 * Reads a gps.csv file (schema: t_ns,lat,lon,alt,accuracy,speed,bearing,provider,is_mock,satellites)
 * and runs every row through the Defender engine, printing a verdict per row
 * plus a short summary.
 */
fun main(args: Array<String>) {
    if (args.isEmpty()) {
        println("Usage: run --args=\"<path-to-gps.csv>\"")
        return
    }
    val file = File(args[0])
    if (!file.exists()) {
        println("File not found: ${file.absolutePath}")
        return
    }

    val engine = DefaultLocationTrustEngine()
    var total = 0
    var flagged = 0

    file.readLines().drop(1)
        .filter { it.isNotBlank() }
        .forEach { line ->
            val cols = line.split(",")
            val event = LocationEvent(
                tNs = cols[0].trim().toLong(),
                lat = cols[1].trim().toDouble(),
                lon = cols[2].trim().toDouble(),
                altM = cols[3].trim().toDoubleOrNull(),
                accuracyM = cols[4].trim().toFloatOrNull(),
                speedMps = cols[5].trim().toFloatOrNull(),
                bearingDeg = cols[6].trim().toFloatOrNull(),
                provider = cols[7].trim(),
                isMock = cols[8].trim().toBooleanStrictOrNull(),
                elapsedRealtimeNs = cols[0].trim().toLong(),
                satellites = cols.getOrNull(9)?.trim()?.toIntOrNull()
            )
            val verdict = engine.onLocation(event)
            total++
            if (verdict.flagged) flagged++
            println("t=${event.tNs} trust=${verdict.trust} flagged=${verdict.flagged} evidence=${verdict.evidence}")
        }

    println("---")
    val pct = if (total > 0) (flagged * 100 / total) else 0
    println("Total rows: $total, flagged: $flagged ($pct%)")
}
'@ | Set-Content "$root\src\main\kotlin\com\spoofbench\defender\core\replay\CsvReplay.kt"

@'
t_ns,lat,lon,alt,accuracy,speed,bearing,provider,is_mock,satellites
0,28.6139,77.2090,216,5,1.2,90,gps,false,8
1000000000,28.61392,77.20902,216,5,1.3,91,gps,false,8
1980000000,28.61394,77.20905,216,5,1.2,90,gps,false,8
3050000000,28.61397,77.20907,216,5,1.3,92,gps,false,8
4300000000,28.70000,77.30000,216,10,1.2,90,gps,false,8
5300000000,28.70003,77.30002,216,10,1.3,91,gps,true,8
'@ | Set-Content "$root\sample_data\sample_gps.csv"

Write-Host "Done."