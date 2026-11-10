package com.spoofbench.defender.core

interface LocationTrustEngine {
    fun onLocation(event: LocationEvent): Verdict
    fun onImu(event: ImuEvent)
    fun reset()
}
