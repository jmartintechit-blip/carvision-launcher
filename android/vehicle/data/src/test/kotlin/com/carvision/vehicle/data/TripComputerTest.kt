package com.carvision.vehicle.data

import com.carvision.core.model.SignalSource
import com.carvision.core.model.SignalStatus
import com.carvision.core.model.VehicleSignal
import com.carvision.core.model.VehicleSnapshot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TripComputerTest {
    private var now = 0L
    private val trip = TripComputer(clock = { now })

    private fun snap(speed: Double?, status: SignalStatus = SignalStatus.VALID, odo: Long? = null) =
        VehicleSnapshot.unavailable().copy(
            speedKmh = if (speed == null) VehicleSignal.unavailable("km/h") else VehicleSignal(speed, "km/h", now, status, SignalSource.CAN),
            odometerKm = if (odo == null) VehicleSignal.unavailable("km") else VehicleSignal(odo, "km", now, SignalStatus.VALID, SignalSource.CAN),
        )

    private fun drive(speed: Double, seconds: Int, status: SignalStatus = SignalStatus.VALID, odo: Long? = null) {
        repeat(seconds) { trip.update(snap(speed, status, odo)); now += 1000 }
    }

    @Test
    fun `60 kmh durante un minuto son 1 km`() {
        drive(60.0, 61)
        val s = trip.stats.value
        assertEquals(1.0, s.distanceKm, 0.01)
        assertEquals(60L, s.movingSeconds)
        assertEquals(60.0, s.avgSpeedKmh!!, 0.5)
        assertEquals(60.0, s.maxSpeedKmh!!, 0.0)
        assertFalse(s.simulated)
    }

    @Test
    fun `datos cada medio segundo tambien cuentan el tiempo`() {
        repeat(121) { trip.update(snap(60.0)); now += 500 }
        val s = trip.stats.value
        assertEquals(60L, s.elapsedSeconds)
        assertEquals(60L, s.movingSeconds)
        assertEquals(1.0, s.distanceKm, 0.01)
    }

    @Test
    fun `parado no suma tiempo en marcha ni distancia`() {
        drive(0.0, 30)
        val s = trip.stats.value
        assertEquals(0.0, s.distanceKm, 0.0001)
        assertEquals(0L, s.movingSeconds)
        assertNull(s.avgSpeedKmh)
    }

    @Test
    fun `un hueco sin datos no se rellena`() {
        drive(100.0, 2)
        now += 60_000 // un minuto sin datos
        drive(100.0, 2)
        assertTrue(trip.stats.value.distanceKm < 0.1)
    }

    @Test
    fun `sin velocidad no hay datos de viaje`() {
        repeat(5) { trip.update(snap(null)); now += 1000 }
        assertFalse(trip.stats.value.hasData)
    }

    @Test
    fun `los datos simulados marcan el viaje como simulado`() {
        drive(50.0, 5, SignalStatus.SIMULATED)
        assertTrue(trip.stats.value.simulated)
    }

    @Test
    fun `con cuentakilometros real se usa su diferencia`() {
        drive(90.0, 2, odo = 1000)
        drive(90.0, 2, odo = 1010)
        val s = trip.stats.value
        assertTrue(s.fromOdometer)
        assertEquals(10.0, s.distanceKm, 0.0)
    }

    @Test
    fun `nuevo viaje lo pone todo a cero`() {
        drive(80.0, 20)
        trip.reset()
        assertEquals(0.0, trip.stats.value.distanceKm, 0.0)
        assertFalse(trip.stats.value.hasData)
    }
}
