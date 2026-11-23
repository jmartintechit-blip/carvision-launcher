package com.carvision.core.maintenance

import kotlinx.serialization.json.Json
import java.time.LocalDate
import kotlin.math.max

enum class DueState {
    /** Pasado de km o de fecha. */
    OVERDUE,
    /** Quedan menos de ~1.000 km (o el 10 % del intervalo) o menos de 30 días. */
    SOON,
    OK,
    /** Activado pero sin ningún registro: no se puede calcular. */
    NO_RECORD,
}

/** Estado de una intervención programada respecto al kilometraje y la fecha actuales. */
data class ServiceStatus(
    val type: ServiceType,
    val title: String,
    val state: DueState,
    val last: ServiceRecord?,
    val dueKm: Long?,
    val dueDate: LocalDate?,
    /** Negativo = vencido. Null si no se conoce el km actual o la intervención no va por km. */
    val remainingKm: Long?,
    /** Negativo = vencido. */
    val remainingDays: Long?,
    /** Parte del intervalo consumida (0..1, puede superar 1). Null si no se puede calcular. */
    val progress: Float?,
)

object MaintenanceCalculator {

    /**
     * Calcula el estado de cada intervención activada, ordenadas de más a menos urgente.
     * @param currentKm kilometraje actual REAL (del coche o introducido a mano). Nunca un dato simulado.
     */
    fun compute(book: MaintenanceBook, currentKm: Long?, today: LocalDate): List<ServiceStatus> {
        val types = book.schedules.filter { it.enabled }.map { it.type }
        return types.map { type -> status(book, type, currentKm, today) }
            .sortedWith(compareBy<ServiceStatus> { it.state.ordinal }.thenBy { urgencyKey(it) })
    }

    fun status(book: MaintenanceBook, type: ServiceType, currentKm: Long?, today: LocalDate): ServiceStatus {
        val sched = book.schedule(type)
        val last = book.records.filter { it.type == type }.maxWithOrNull(compareBy({ it.dateEpochDay }, { it.odometerKm ?: 0 }))
        if (last == null) {
            return ServiceStatus(type, type.label, DueState.NO_RECORD, null, null, null, null, null, null)
        }
        val lastDate = LocalDate.ofEpochDay(last.dateEpochDay)
        val dueKm = last.nextKm ?: if (last.odometerKm != null && sched.intervalKm != null) last.odometerKm + sched.intervalKm else null
        val dueDate = last.nextDateEpochDay?.let { LocalDate.ofEpochDay(it) }
            ?: sched.intervalMonths?.let { lastDate.plusMonths(it.toLong()) }
        val remainingKm = if (dueKm != null && currentKm != null) dueKm - currentKm else null
        val remainingDays = dueDate?.let { it.toEpochDay() - today.toEpochDay() }

        val kmProgress = if (dueKm != null && currentKm != null && last.odometerKm != null && dueKm > last.odometerKm) {
            (currentKm - last.odometerKm).toFloat() / (dueKm - last.odometerKm)
        } else null
        val dayProgress = if (dueDate != null && dueDate.toEpochDay() > last.dateEpochDay) {
            (today.toEpochDay() - last.dateEpochDay).toFloat() / (dueDate.toEpochDay() - last.dateEpochDay)
        } else null
        val progress = listOfNotNull(kmProgress, dayProgress).maxOrNull()?.coerceAtLeast(0f)

        val kmSoon = sched.intervalKm?.let { max(1_000L, it / 10L) } ?: 1_000L
        val state = when {
            (remainingKm != null && remainingKm < 0) || (remainingDays != null && remainingDays < 0) -> DueState.OVERDUE
            (remainingKm != null && remainingKm <= kmSoon) || (remainingDays != null && remainingDays <= 30) -> DueState.SOON
            else -> DueState.OK
        }
        return ServiceStatus(type, last.title, state, last, dueKm, dueDate, remainingKm, remainingDays, progress)
    }

    private fun urgencyKey(s: ServiceStatus): Float = -(s.progress ?: -1f)
}

/** Lectura y escritura del carnet en JSON. Tolera campos desconocidos (versiones futuras). */
object MaintenanceCodec {
    private val json = Json { ignoreUnknownKeys = true; prettyPrint = true; encodeDefaults = true }

    fun encode(book: MaintenanceBook): String = json.encodeToString(MaintenanceBook.serializer(), book)

    fun decode(text: String): MaintenanceBook = json.decodeFromString(MaintenanceBook.serializer(), text)
}
