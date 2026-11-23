package com.carvision.core.maintenance

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class MaintenanceCalculatorTest {
    private val today = LocalDate.of(2026, 11, 12)

    private fun record(type: ServiceType, date: LocalDate, km: Long?, nextKm: Long? = null) =
        ServiceRecord("r-${type.name}-${date}", type, dateEpochDay = date.toEpochDay(), odometerKm = km, nextKm = nextKm)

    @Test
    fun `aceite reciente esta al dia y calcula km y fecha restantes`() {
        val book = MaintenanceBook(records = listOf(record(ServiceType.OIL_CHANGE, LocalDate.of(2026, 4, 20), 200_000)))
        val s = MaintenanceCalculator.status(book, ServiceType.OIL_CHANGE, currentKm = 205_000, today = today)
        assertEquals(DueState.OK, s.state)
        assertEquals(215_000L, s.dueKm)
        assertEquals(10_000L, s.remainingKm)
        assertEquals(LocalDate.of(2027, 4, 20), s.dueDate)
    }

    @Test
    fun `pasado de kilometros esta vencido aunque la fecha no`() {
        val book = MaintenanceBook(records = listOf(record(ServiceType.OIL_CHANGE, LocalDate.of(2026, 10, 1), 100_000)))
        val s = MaintenanceCalculator.status(book, ServiceType.OIL_CHANGE, currentKm = 116_000, today = today)
        assertEquals(DueState.OVERDUE, s.state)
        assertTrue(s.remainingKm!! < 0)
    }

    @Test
    fun `cerca del limite pasa a proximo`() {
        val book = MaintenanceBook(records = listOf(record(ServiceType.OIL_CHANGE, LocalDate.of(2026, 9, 1), 100_000)))
        val s = MaintenanceCalculator.status(book, ServiceType.OIL_CHANGE, currentKm = 114_200, today = today)
        assertEquals(DueState.SOON, s.state)
    }

    @Test
    fun `el kilometraje indicado por el taller tiene prioridad`() {
        val book = MaintenanceBook(records = listOf(record(ServiceType.OIL_CHANGE, LocalDate.of(2026, 9, 1), 100_000, nextKm = 120_000)))
        val s = MaintenanceCalculator.status(book, ServiceType.OIL_CHANGE, currentKm = 105_000, today = today)
        assertEquals(120_000L, s.dueKm)
    }

    @Test
    fun `sin kilometraje actual solo se usa la fecha`() {
        val book = MaintenanceBook(records = listOf(record(ServiceType.ITV, LocalDate.of(2024, 12, 1), null)))
        val s = MaintenanceCalculator.status(book, ServiceType.ITV, currentKm = null, today = today)
        assertNull(s.remainingKm)
        assertEquals(DueState.SOON, s.state) // vence el 2026-12-01: quedan menos de 30 días
    }

    @Test
    fun `sin registros no se inventa ningun vencimiento`() {
        val s = MaintenanceCalculator.status(MaintenanceBook(), ServiceType.TIMING_BELT, 150_000, today)
        assertEquals(DueState.NO_RECORD, s.state)
        assertNull(s.dueKm)
    }

    @Test
    fun `el carnet se guarda y se recupera igual`() {
        val book = MaintenanceBook(records = listOf(record(ServiceType.BRAKES, LocalDate.of(2025, 1, 2), 90_000)), manualOdometerKm = 123)
        assertEquals(book, MaintenanceCodec.decode(MaintenanceCodec.encode(book)))
    }
}
