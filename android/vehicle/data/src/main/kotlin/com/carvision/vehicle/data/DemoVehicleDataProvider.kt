package com.carvision.vehicle.data

import com.carvision.core.model.ConnectionKind
import com.carvision.core.model.ConnectionState
import com.carvision.core.model.Opening
import com.carvision.core.model.VehicleSignal
import com.carvision.core.model.VehicleSnapshot
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.max
import kotlin.math.sin

/**
 * Modo demostración. Todas las señales salen con estado SIMULATED y origen DEMO,
 * de modo que la interfaz siempre puede etiquetarlas como simulación.
 *
 * Las aperturas se controlan a mano con [setOpening] para probar las animaciones sin vehículo.
 */
class DemoVehicleDataProvider(
    private val clock: () -> Long = System::currentTimeMillis,
) : VehicleDataProvider {
    override val id = "demo"
    override val displayName = "Modo demostración"

    private val openState = MutableStateFlow(Opening.entries.associateWith { false })
    private val lights = MutableStateFlow(false)
    private val state = MutableStateFlow(buildSnapshot(clock(), 0.0))
    override val snapshot: StateFlow<VehicleSnapshot> = state.asStateFlow()

    private var job: Job? = null

    override fun start(scope: CoroutineScope) {
        if (job?.isActive == true) return
        job = scope.launch {
            val t0 = clock()
            while (isActive) {
                val now = clock()
                state.value = buildSnapshot(now, (now - t0) / 1000.0)
                delay(TICK_MS)
            }
        }
    }

    override fun stop() {
        job?.cancel()
        job = null
    }

    fun setOpening(opening: Opening, open: Boolean) {
        openState.update { it + (opening to open) }
        state.update { snap -> snap.copy(openings = openingSignals(clock())) }
    }

    fun toggleOpening(opening: Opening) = setOpening(opening, !(openState.value[opening] ?: false))

    /** Simula encender/apagar las luces del coche (el tema automático reacciona). */
    fun toggleLights() {
        lights.value = !lights.value
        state.update { it.copy(lightsOn = VehicleSignal.simulated(lights.value, null, clock())) }
    }

    fun closeAll() {
        openState.value = Opening.entries.associateWith { false }
        state.update { snap -> snap.copy(openings = openingSignals(clock())) }
    }

    private fun openingSignals(now: Long) =
        openState.value.mapValues { (_, open) -> VehicleSignal.simulated(open, null, now) }

    private fun buildSnapshot(now: Long, seconds: Double): VehicleSnapshot {
        // Ciclo suave de conducción urbana simulada (0–62 km/h). No se detiene al abrir algo: así se puede
        // probar el aviso de apertura abierta en marcha.
        val wave = (sin(seconds * 2 * PI / 40.0) + 1) / 2
        val raw = max(0.0, wave * 62.0 - 6.0)
        val speed = if (raw < 2.0) 0.0 else raw
        val rpm = if (speed < 1) 820 else (1100 + speed * 28).toInt()
        return VehicleSnapshot(
            openings = openingSignals(now),
            speedKmh = VehicleSignal.simulated(speed, "km/h", now),
            rpm = VehicleSignal.simulated(rpm, "rpm", now),
            fuelLiters = VehicleSignal.simulated(31.0, "L", now),
            outsideTempC = VehicleSignal.simulated(14.5, "°C", now),
            batteryVolts = VehicleSignal.simulated(if (speed < 1) 12.6 else 14.2, "V", now),
            odometerKm = VehicleSignal.simulated(128_450L, "km", now),
            lightsOn = VehicleSignal.simulated(lights.value, null, now),
            connection = ConnectionState(ConnectionKind.DEMO, displayName, "Datos simulados"),
        )
    }

    private companion object {
        const val TICK_MS = 500L
    }
}
