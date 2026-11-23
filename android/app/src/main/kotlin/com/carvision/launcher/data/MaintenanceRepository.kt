package com.carvision.launcher.data

import android.content.Context
import android.util.Log
import com.carvision.core.maintenance.MaintenanceBook
import com.carvision.core.maintenance.MaintenanceCodec
import com.carvision.core.maintenance.ServiceRecord
import com.carvision.core.maintenance.ServiceSchedule
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.File
import java.time.LocalDate
import java.util.UUID

/**
 * Carnet de mantenimiento guardado en la memoria privada de la app (files/maintenance.json).
 *
 * Escritura atómica: se escribe en un archivo temporal y se renombra. Así, si la radio se apaga de golpe al
 * quitar el contacto, el carnet anterior queda intacto. Se guarda una copia de seguridad del último estado válido.
 */
class MaintenanceRepository(context: Context, private val scope: CoroutineScope) {
    private val file = File(context.filesDir, "maintenance.json")
    private val backup = File(context.filesDir, "maintenance.bak.json")
    private val mutex = Mutex()

    private val state = MutableStateFlow(MaintenanceBook())
    val book: StateFlow<MaintenanceBook> = state.asStateFlow()

    init {
        scope.launch(Dispatchers.IO) { state.value = read() }
    }

    private fun read(): MaintenanceBook {
        for (f in listOf(file, backup)) {
            if (!f.isFile) continue
            try {
                return MaintenanceCodec.decode(f.readText())
            } catch (e: Exception) {
                Log.e(TAG, "No se pudo leer ${f.name}", e)
            }
        }
        return MaintenanceBook()
    }

    private suspend fun write(book: MaintenanceBook) = mutex.withLock {
        val tmp = File(file.parentFile, "maintenance.tmp")
        tmp.writeText(MaintenanceCodec.encode(book))
        if (file.isFile) file.copyTo(backup, overwrite = true)
        if (!tmp.renameTo(file)) {
            tmp.copyTo(file, overwrite = true)
            tmp.delete()
        }
    }

    private fun mutate(change: (MaintenanceBook) -> MaintenanceBook) {
        state.update(change)
        val snapshot = state.value
        scope.launch(Dispatchers.IO) { write(snapshot) }
    }

    fun newId(): String = UUID.randomUUID().toString()

    fun upsert(record: ServiceRecord) = mutate { b ->
        b.copy(records = b.records.filterNot { it.id == record.id } + record)
    }

    fun delete(id: String) = mutate { b -> b.copy(records = b.records.filterNot { it.id == id }) }

    fun updateSchedule(schedule: ServiceSchedule) = mutate { b ->
        b.copy(schedules = b.schedules.filterNot { it.type == schedule.type } + schedule)
    }

    fun setManualOdometer(km: Long) = mutate { b ->
        b.copy(manualOdometerKm = km, manualOdometerEpochDay = LocalDate.now().toEpochDay())
    }

    private companion object {
        const val TAG = "CarVisionMaint"
    }
}
