package com.carvision.launcher.data

import android.content.Context
import android.util.Log
import com.carvision.vehicle.data.TripComputer
import com.carvision.vehicle.data.TripHistory
import com.carvision.vehicle.data.TripHistoryCodec
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.File

/**
 * Historial de viajes (files/trips.json, escritura atómica).
 * - Cada 20 s se guarda el viaje en curso: si la radio se apaga de golpe al quitar el contacto, no se pierde.
 * - Al arrancar, el viaje que quedó en curso pasa al historial (era el del último trayecto).
 * - «Nuevo viaje» cierra el actual y empieza otro.
 */
class TripRepository(context: Context, private val scope: CoroutineScope, private val trip: TripComputer) {
    private val file = File(context.filesDir, "trips.json")
    private val mutex = Mutex()
    private val state = MutableStateFlow(TripHistory())
    val history: StateFlow<TripHistory> = state.asStateFlow()

    init {
        scope.launch(Dispatchers.IO) {
            val loaded = runCatching { if (file.isFile) TripHistoryCodec.decode(file.readText()) else TripHistory() }
                .onFailure { Log.e("CarVisionTrips", "No se pudo leer trips.json", it) }
                .getOrDefault(TripHistory())
            // El viaje que quedó en curso en el arranque anterior se cierra ahora.
            state.value = loaded.close(loaded.inProgress)
            write()
            while (isActive) {
                delay(20_000)
                checkpoint()
            }
        }
    }

    private suspend fun write() = mutex.withLock {
        val tmp = File(file.parentFile, "trips.tmp")
        tmp.writeText(TripHistoryCodec.encode(state.value))
        if (!tmp.renameTo(file)) { tmp.copyTo(file, overwrite = true); tmp.delete() }
    }

    private suspend fun checkpoint() {
        val current = TripHistory.fromStats(trip.stats.value) ?: return
        state.update { it.copy(inProgress = current) }
        write()
    }

    /** Cierra el viaje actual (al historial) y empieza uno nuevo. */
    fun newTrip() {
        val current = TripHistory.fromStats(trip.stats.value)
        trip.reset()
        state.update { it.close(current) }
        scope.launch(Dispatchers.IO) { write() }
    }

    fun clear() {
        state.update { TripHistory() }
        scope.launch(Dispatchers.IO) { write() }
    }
}
