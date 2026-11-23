package com.carvision.core.maintenance

import kotlinx.serialization.Serializable

/**
 * Tipos de intervención del carnet. Los intervalos por defecto son ORIENTATIVOS: cada coche y motor tiene los
 * suyos (manual del fabricante) y el usuario los puede cambiar o desactivar. Nunca se presentan como universales.
 */
@Serializable
enum class ServiceType(val label: String, val defaultKm: Int?, val defaultMonths: Int?, val enabledByDefault: Boolean = true) {
    OIL_CHANGE("Aceite y filtro de aceite", 15_000, 12),
    INSPECTION("Revisión general", 15_000, 12),
    AIR_FILTER("Filtro de aire", 30_000, 24),
    CABIN_FILTER("Filtro de habitáculo", 15_000, 12),
    FUEL_FILTER("Filtro de combustible", 60_000, 48),
    TIMING_BELT("Distribución", 150_000, 120),
    WATER_PUMP("Bomba de agua", 150_000, 120, enabledByDefault = false),
    COOLANT("Líquido refrigerante", null, 60),
    BRAKE_FLUID("Líquido de frenos", null, 24),
    BRAKES("Pastillas y discos de freno", 40_000, null),
    TIRES("Neumáticos", 40_000, 60),
    BATTERY("Batería", null, 60),
    ITV("ITV", null, 24),
    CUSTOM("Otra intervención", null, null, enabledByDefault = false),
}

/** Una intervención realizada. Fechas en días desde 1970-01-01 (LocalDate.toEpochDay). */
@Serializable
data class ServiceRecord(
    val id: String,
    val type: ServiceType,
    /** Nombre libre (solo para CUSTOM o para precisar, p. ej. "Neumáticos delanteros"). */
    val customName: String? = null,
    val dateEpochDay: Long,
    val odometerKm: Long? = null,
    val description: String = "",
    val cost: Double? = null,
    val workshop: String = "",
    val notes: String = "",
    /** Próxima fecha indicada por el taller (tiene prioridad sobre el intervalo). */
    val nextDateEpochDay: Long? = null,
    /** Próximo kilometraje indicado por el taller (tiene prioridad sobre el intervalo). */
    val nextKm: Long? = null,
) {
    val title: String get() = customName?.takeIf { it.isNotBlank() } ?: type.label
}

/** Intervalo de una intervención, editable por el usuario. */
@Serializable
data class ServiceSchedule(
    val type: ServiceType,
    val enabled: Boolean,
    val intervalKm: Int?,
    val intervalMonths: Int?,
)

/** Carnet completo, tal cual se guarda en disco (JSON versionado). */
@Serializable
data class MaintenanceBook(
    val formatVersion: Int = 1,
    val records: List<ServiceRecord> = emptyList(),
    val schedules: List<ServiceSchedule> = defaultSchedules(),
    /** Kilometraje introducido a mano (cuando no hay dato real del coche). */
    val manualOdometerKm: Long? = null,
    val manualOdometerEpochDay: Long? = null,
) {
    fun schedule(type: ServiceType): ServiceSchedule =
        schedules.firstOrNull { it.type == type } ?: ServiceSchedule(type, type.enabledByDefault, type.defaultKm, type.defaultMonths)

    companion object {
        fun defaultSchedules(): List<ServiceSchedule> = ServiceType.entries
            .filter { it != ServiceType.CUSTOM }
            .map { ServiceSchedule(it, it.enabledByDefault, it.defaultKm, it.defaultMonths) }
    }
}
