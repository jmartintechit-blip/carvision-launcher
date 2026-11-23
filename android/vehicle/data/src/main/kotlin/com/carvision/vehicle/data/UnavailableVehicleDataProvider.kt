package com.carvision.vehicle.data

import com.carvision.core.model.VehicleSnapshot
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Proveedor por defecto cuando no hay adaptador: todos los datos "No disponible". */
class UnavailableVehicleDataProvider : VehicleDataProvider {
    override val id = "none"
    override val displayName = "Sin conexión con el vehículo"

    private val state = MutableStateFlow(VehicleSnapshot.unavailable())
    override val snapshot: StateFlow<VehicleSnapshot> = state.asStateFlow()

    override fun start(scope: CoroutineScope) = Unit
    override fun stop() = Unit
}
