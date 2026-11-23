package com.carvision.vehicle.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TripHistoryTest {
    private fun trip(km: Double, start: Long = 0) = TripRecord(start, start + 600_000, km, 600, 550, 90.0, simulated = false, fromOdometer = false)

    @Test
    fun `un viaje real se guarda el primero`() {
        val h = TripHistory(trips = listOf(trip(5.0, 1))).close(trip(12.0, 2))
        assertEquals(listOf(12.0, 5.0), h.trips.map { it.distanceKm })
        assertNull(h.inProgress)
    }

    @Test
    fun `los trayectos minimos no ensucian el historial`() {
        assertEquals(0, TripHistory().close(trip(0.05)).trips.size)
    }

    @Test
    fun `se limita el numero de viajes guardados`() {
        var h = TripHistory()
        repeat(10) { h = h.close(trip(1.0, it.toLong()), maxTrips = 5) }
        assertEquals(5, h.trips.size)
        assertEquals(9L, h.trips.first().startedAtMs)
    }

    @Test
    fun `el historial se guarda y se recupera igual`() {
        val h = TripHistory(trips = listOf(trip(3.3)), inProgress = trip(1.0))
        assertEquals(h, TripHistoryCodec.decode(TripHistoryCodec.encode(h)))
    }
}
