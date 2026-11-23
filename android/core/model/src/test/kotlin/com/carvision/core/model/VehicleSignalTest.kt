package com.carvision.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VehicleSignalTest {

    @Test
    fun `un dato real antiguo pasa a STALE y deja de mostrarse`() {
        val s = VehicleSignal(12.5, "V", timestampMs = 1_000, status = SignalStatus.VALID, source = SignalSource.CAN)
        val old = s.withStaleness(nowMs = 10_000, maxAgeMs = 5_000)
        assertEquals(SignalStatus.STALE, old.status)
        assertFalse(old.isDisplayable)
    }

    @Test
    fun `un dato simulado nunca se convierte en real ni caduca`() {
        val s = VehicleSignal.simulated(50.0, "km/h", nowMs = 0)
        assertEquals(SignalStatus.SIMULATED, s.withStaleness(nowMs = 1_000_000, maxAgeMs = 1).status)
    }

    @Test
    fun `sin datos no se afirma que todo este cerrado`() {
        val snap = VehicleSnapshot.unavailable()
        assertFalse(snap.allKnownClosed)
        assertTrue(snap.openOpenings.isEmpty())
    }
}
