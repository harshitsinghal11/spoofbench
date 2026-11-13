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
