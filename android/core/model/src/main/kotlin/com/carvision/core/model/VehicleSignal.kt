package com.carvision.core.model

/**
 * Estado de fiabilidad de una señal del vehículo.
 * Regla del proyecto: nunca se presenta un dato SIMULATED o STALE como si fuera VALID.
 */
enum class SignalStatus {
    /** Dato real, reciente, recibido de una fuente verificada. */
    VALID,
    /** No hay fuente que proporcione este dato. */
    UNAVAILABLE,
    /** Hubo un dato real, pero es más antiguo que su tiempo máximo de validez. */
    STALE,
    /** Dato generado por el modo demostración. */
    SIMULATED,
}

/**
 * Origen del dato. Se muestra al usuario para que sepa de dónde sale cada valor.
 * No hay origen GPS a propósito: los datos del vehículo salen siempre del propio coche (DECISIONS.md D-016).
 */
enum class SignalSource {
    NONE,
    DEMO,
    CAN,
    OBD,
    MANUAL,
}

/**
 * Señal normalizada: valor + unidad + marca temporal + estado + origen.
 * Todos los CAN Adapters producen este mismo formato.
 */
data class VehicleSignal<out T>(
    val value: T?,
    val unit: String?,
    val timestampMs: Long,
    val status: SignalStatus,
    val source: SignalSource,
) {
    /** true si hay un valor que se puede mostrar (real o simulado, nunca antiguo). */
    val isDisplayable: Boolean
        get() = value != null && (status == SignalStatus.VALID || status == SignalStatus.SIMULATED)

    /** Marca el dato como antiguo si supera [maxAgeMs]. Solo afecta a datos reales. */
    fun withStaleness(nowMs: Long, maxAgeMs: Long): VehicleSignal<T> =
        if (status == SignalStatus.VALID && nowMs - timestampMs > maxAgeMs) copy(status = SignalStatus.STALE) else this

    companion object {
        fun <T> unavailable(unit: String? = null): VehicleSignal<T> =
            VehicleSignal(null, unit, 0L, SignalStatus.UNAVAILABLE, SignalSource.NONE)

        fun <T> simulated(value: T, unit: String?, nowMs: Long): VehicleSignal<T> =
            VehicleSignal(value, unit, nowMs, SignalStatus.SIMULATED, SignalSource.DEMO)
    }
}
