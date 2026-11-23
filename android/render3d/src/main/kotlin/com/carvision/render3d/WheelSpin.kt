package com.carvision.render3d

import kotlin.math.exp
import kotlin.math.min

/**
 * Giro visible de las ruedas. No es un velocímetro: es decorativo y está pensado para que SIEMPRE se vea avanzar.
 *
 * Con la velocidad real (a 56 km/h, unos 2700°/s) los radios saltan 45° o más entre fotogramas y el ojo los ve ir
 * hacia delante o hacia atrás según caiga cada fotograma (efecto "rueda de carreta" del cine). Por eso:
 * - el giro visible se limita a [maxDegPerSec] (con ruedas de 0,33 m se alcanza a unos 23 km/h);
 * - cada fotograma avanza como mucho [maxStepDeg], aunque llegue tarde;
 * - acelera y frena con suavidad: la velocidad del coche llega a saltos (p. ej. cada medio segundo).
 */
class WheelSpin(
    private val maxDegPerSec: Float = 600f,
    private val maxStepDeg: Float = 12f,
    private val response: Float = 2.5f,
) {
    /** Velocidad de giro que se ve ahora, en grados por segundo. */
    var degPerSec = 0f
        private set

    /** Ángulo acumulado de la rueda, 0–360°. */
    var angleDeg = 0f
        private set

    /**
     * Avanza un fotograma. [speedMs] es la velocidad del coche (m/s), [radiusM] el radio de la rueda.
     * Devuelve true mientras la rueda sigue girando (hay que dibujar otro fotograma).
     */
    fun update(dt: Float, speedMs: Float, radiusM: Float): Boolean {
        val target = if (speedMs <= 0f || radiusM <= 0f) 0f
        else min(Math.toDegrees((speedMs / radiusM).toDouble()).toFloat(), maxDegPerSec)
        degPerSec += (target - degPerSec) * (1f - exp(-response * dt.coerceAtLeast(0f)))
        if (target == 0f && degPerSec < 2f) {
            degPerSec = 0f
            return false
        }
        val step = min(degPerSec * dt.coerceAtLeast(0f), maxStepDeg)
        angleDeg = (angleDeg + step) % 360f
        return true
    }
}
