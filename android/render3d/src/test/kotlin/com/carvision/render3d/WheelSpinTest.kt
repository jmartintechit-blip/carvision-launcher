package com.carvision.render3d

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class WheelSpinTest {
    private val radius = 0.331f

    /** Avance de un fotograma en grados, siempre en sentido de marcha (0–360). */
    private fun stepOf(before: Float, after: Float) = ((after - before) % 360f + 360f) % 360f

    @Test
    fun `a velocidad de autopista nunca avanza mas de 12 grados por fotograma`() {
        val w = WheelSpin()
        val rnd = Random(1)
        repeat(2000) {
            // Fotogramas irregulares (emulador o radio lenta): de 8 a 100 ms.
            val dt = 0.008f + rnd.nextFloat() * 0.092f
            val before = w.angleDeg
            w.update(dt, speedMs = 130 / 3.6f, radiusM = radius)
            val step = stepOf(before, w.angleDeg)
            assertTrue("paso $step", step <= 12f + 1e-3f)
        }
    }

    @Test
    fun `el giro visible esta limitado aunque el coche vaya rapido`() {
        val w = WheelSpin()
        repeat(600) { w.update(1 / 60f, speedMs = 120 / 3.6f, radiusM = radius) }
        assertEquals(600f, w.degPerSec, 1f)
    }

    @Test
    fun `a poca velocidad gira a la velocidad real`() {
        val w = WheelSpin()
        val speed = 10 / 3.6f
        repeat(600) { w.update(1 / 60f, speed, radius) }
        val real = Math.toDegrees((speed / radius).toDouble()).toFloat()
        assertEquals(real, w.degPerSec, 1f)
    }

    @Test
    fun `los saltos de velocidad se suavizan y nunca gira hacia atras`() {
        val w = WheelSpin()
        var last = 0f
        // La velocidad cambia a saltos cada 30 fotogramas, como la demo (cada 0,5 s).
        for (i in 0 until 600) {
            val speed = if ((i / 30) % 2 == 0) 5f else 25f
            w.update(1 / 60f, speed, radius)
            assertTrue(w.degPerSec >= 0f)
            assertTrue("cambio brusco", kotlin.math.abs(w.degPerSec - last) < 40f)
            last = w.degPerSec
        }
    }

    @Test
    fun `al parar frena suave y luego deja de pedir fotogramas`() {
        val w = WheelSpin()
        repeat(300) { w.update(1 / 60f, 15f, radius) }
        val enMarcha = w.degPerSec
        assertTrue(w.update(1 / 60f, 0f, radius))
        assertTrue("frena poco a poco", w.degPerSec > enMarcha * 0.9f)
        var frames = 0
        while (w.update(1 / 60f, 0f, radius)) frames++
        assertTrue("acaba parando", frames in 30..600)
        assertFalse(w.update(1 / 60f, 0f, radius))
        assertEquals(0f, w.degPerSec, 0f)
    }
}
