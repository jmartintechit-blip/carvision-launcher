package com.carvision.vehicle.data

import com.carvision.core.model.VehicleSnapshot
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow

/**
 * Fuente de datos del vehículo (capa "CAN Adapters").
 *
 * Contrato:
 * - Solo LECTURA. Ninguna implementación envía comandos al vehículo.
 * - [start] nunca bloquea: si la conexión tarda o falla, [snapshot] sigue emitiendo datos "no disponibles".
 * - Cada señal indica su estado y origen; una implementación nunca marca como VALID un dato que no ha leído.
 */
interface VehicleDataProvider {
    /** Identificador estable (se guarda en preferencias). */
    val id: String

    /** Nombre legible para el usuario. */
    val displayName: String

    val snapshot: StateFlow<VehicleSnapshot>

    fun start(scope: CoroutineScope)

    fun stop()
}
