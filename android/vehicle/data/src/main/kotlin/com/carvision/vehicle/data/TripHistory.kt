package com.carvision.vehicle.data

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** Viaje terminado, tal cual se guarda en el historial. */
@Serializable
data class TripRecord(
    val startedAtMs: Long,
    val endedAtMs: Long,
    val distanceKm: Double,
    val elapsedSeconds: Long,
    val movingSeconds: Long,
    val maxSpeedKmh: Double?,
    val simulated: Boolean,
    val fromOdometer: Boolean,
) {
    val avgSpeedKmh: Double? get() = if (movingSeconds >= 30) distanceKm / (movingSeconds / 3600.0) else null
}

/**
 * Historial de viajes + el viaje en curso (se guarda cada poco para no perderlo si la radio se apaga de golpe).
 * Al arrancar, el viaje en curso que quedó guardado se pasa al historial si merece la pena.
 */
@Serializable
data class TripHistory(
    val formatVersion: Int = 1,
    val trips: List<TripRecord> = emptyList(),
    val inProgress: TripRecord? = null,
) {
    /** Cierra un viaje: lo añade si tiene sentido (mínimo [minKm] km) y mantiene como mucho [maxTrips]. */
    fun close(trip: TripRecord?, minKm: Double = MIN_KM, maxTrips: Int = MAX_TRIPS): TripHistory {
        val keep = trip?.takeIf { it.distanceKm >= minKm }
        val list = if (keep != null) (listOf(keep) + trips).take(maxTrips) else trips
        return copy(trips = list, inProgress = null)
    }

    companion object {
        const val MIN_KM = 0.2
        const val MAX_TRIPS = 300

        fun fromStats(s: TripStats): TripRecord? =
            if (!s.hasData) null else TripRecord(s.startedAtMs, s.lastUpdateMs, s.distanceKm, s.elapsedSeconds, s.movingSeconds, s.maxSpeedKmh, s.simulated, s.fromOdometer)
    }
}

object TripHistoryCodec {
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    fun encode(h: TripHistory): String = json.encodeToString(TripHistory.serializer(), h)
    fun decode(text: String): TripHistory = json.decodeFromString(TripHistory.serializer(), text)
}
