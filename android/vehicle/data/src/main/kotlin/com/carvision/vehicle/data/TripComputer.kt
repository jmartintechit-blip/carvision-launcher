package com.carvision.vehicle.data

import com.carvision.core.model.SignalStatus
import com.carvision.core.model.VehicleSnapshot
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Datos del viaje actual. Todo es CALCULADO a partir de la velocidad que informa el coche
 * (o de la diferencia de cuentakilómetros cuando es fiable); nunca del GPS.
 */
data class TripStats(
    val startedAtMs: Long,
    val distanceKm: Double,
    /** Tiempo con el coche en movimiento (> 1 km/h). */
    val movingSeconds: Long,
    /** Tiempo desde el inicio del viaje con datos llegando. */
    val elapsedSeconds: Long,
    val maxSpeedKmh: Double?,
    /** Hay al menos un dato de velocidad utilizable. */
    val hasData: Boolean,
    /** Calculado con datos del modo demostración. */
    val simulated: Boolean,
    /** La distancia sale del cuentakilómetros del coche (más precisa que integrar la velocidad). */
    val fromOdometer: Boolean,
    /** Momento del último dato utilizable (fin provisional del viaje). */
    val lastUpdateMs: Long = startedAtMs,
) {
    /** Velocidad media en marcha. Null hasta tener al menos 30 s en movimiento. */
    val avgSpeedKmh: Double?
        get() = if (movingSeconds >= 30) distanceKm / (movingSeconds / 3600.0) else null

    companion object {
        fun empty(now: Long) = TripStats(now, 0.0, 0, 0, null, hasData = false, simulated = false, fromOdometer = false)
    }
}

/**
 * Ordenador de viaje. Recibe instantáneas del vehículo y acumula distancia y tiempos.
 * - Integra la velocidad (regla del trapecio); los huecos de más de [maxGapMs] no cuentan (sin datos = sin inventar).
 * - Si el cuentakilómetros es un dato real y ha avanzado al menos [odometerMinKm], usa esa diferencia.
 * - Un viaje empieza con cada arranque del launcher (al dar el contacto) o al pulsar "Nuevo viaje".
 */
class TripComputer(
    private val clock: () -> Long = System::currentTimeMillis,
    private val maxGapMs: Long = 5_000,
    private val odometerMinKm: Long = 3,
) {
    private val state = MutableStateFlow(TripStats.empty(clock()))
    val stats: StateFlow<TripStats> = state.asStateFlow()

    private var lastTs: Long? = null
    // Tiempos en milisegundos: los datos pueden llegar cada 0,5 s y sumar segundos enteros los perdería.
    private var movingMs = 0L
    private var elapsedMs = 0L
    private var lastSpeed: Double? = null
    private var integratedKm = 0.0
    private var odometerStart: Long? = null

    fun reset() {
        lastTs = null
        lastSpeed = null
        integratedKm = 0.0
        odometerStart = null
        movingMs = 0L
        elapsedMs = 0L
        state.value = TripStats.empty(clock())
    }

    fun update(snapshot: VehicleSnapshot) {
        val now = clock()
        val sig = snapshot.speedKmh
        val speed = sig.value
        if (!sig.isDisplayable || speed == null) {
            // Sin dato de velocidad: se corta la integración, no se supone nada.
            lastTs = null
            lastSpeed = null
            return
        }
        val prev = state.value
        val lt = lastTs
        val ls = lastSpeed
        if (lt != null && ls != null) {
            val dt = now - lt
            if (dt in 1..maxGapMs) {
                integratedKm += (ls + speed) / 2.0 * (dt / 3_600_000.0)
                elapsedMs += dt
                if (speed > 1.0 || ls > 1.0) movingMs += dt
            }
        }
        lastTs = now
        lastSpeed = speed

        // Cuentakilómetros real: diferencia desde el inicio del viaje.
        val odo = snapshot.odometerKm
        var fromOdo = false
        var distance = integratedKm
        if (odo.status == SignalStatus.VALID && odo.value != null) {
            val start = odometerStart ?: odo.value.also { odometerStart = it }
            val delta = odo.value!! - start!!
            if (delta >= odometerMinKm) {
                distance = delta.toDouble()
                fromOdo = true
            }
        }
        state.value = prev.copy(
            distanceKm = distance,
            movingSeconds = movingMs / 1000,
            elapsedSeconds = elapsedMs / 1000,
            maxSpeedKmh = maxOf(prev.maxSpeedKmh ?: 0.0, speed),
            hasData = true,
            simulated = prev.simulated || sig.status == SignalStatus.SIMULATED,
            fromOdometer = fromOdo,
            lastUpdateMs = now,
        )
    }
}
