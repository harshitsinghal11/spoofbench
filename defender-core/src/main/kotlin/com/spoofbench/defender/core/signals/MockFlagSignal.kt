package com.spoofbench.defender.core.signals

import com.spoofbench.defender.core.LocationEvent

/** Flags a location update where the OS itself reports it as coming from a mock provider. */
class MockFlagSignal {
    fun evaluate(event: LocationEvent): Double = if (event.isMock == true) 1.0 else 0.0
}
