package com.carvision.vehicle.data

import com.carvision.core.model.Opening
import com.carvision.core.model.SignalSource
import com.carvision.core.model.SignalStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DemoVehicleDataProviderTest {

    @Test
    fun `todas las senales del modo demo estan marcadas como simuladas`() {
        val snap = DemoVehicleDataProvider { 1_000L }.snapshot.value
        val all = listOf(snap.speedKmh, snap.rpm, snap.fuelLiters, snap.outsideTempC, snap.batteryVolts, snap.odometerKm) +
            snap.openings.values
        assertTrue(all.all { it.status == SignalStatus.SIMULATED && it.source == SignalSource.DEMO })
    }

    @Test
    fun `abrir y cerrar una puerta se refleja en el estado`() {
        val demo = DemoVehicleDataProvider { 1_000L }
        demo.setOpening(Opening.FRONT_LEFT_DOOR, true)
        assertEquals(setOf(Opening.FRONT_LEFT_DOOR), demo.snapshot.value.openOpenings)
        demo.closeAll()
        assertTrue(demo.snapshot.value.allKnownClosed)
    }
}
