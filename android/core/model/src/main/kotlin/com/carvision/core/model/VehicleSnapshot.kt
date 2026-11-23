package com.carvision.core.model

/**
 * Elementos que se pueden abrir. [signalKey] es la clave estable que usan los packs
 * (manifest.json → parts[].signal) para enlazar una pieza 3D con una señal.
 */
enum class Opening(val signalKey: String) {
    FRONT_LEFT_DOOR("door.front_left"),
    FRONT_RIGHT_DOOR("door.front_right"),
    REAR_LEFT_DOOR("door.rear_left"),
    REAR_RIGHT_DOOR("door.rear_right"),
    TAILGATE("tailgate"),
    HOOD("hood");

    companion object {
        fun fromSignalKey(key: String): Opening? = entries.firstOrNull { it.signalKey == key }
    }
}

enum class ConnectionKind {
    /** No hay ningún adaptador de datos del vehículo configurado. Situación normal sin CAN. */
    NO_ADAPTER,
    /** Modo demostración activo: todos los datos son simulados. */
    DEMO,
    CONNECTING,
    CONNECTED,
    ERROR,
}

data class ConnectionState(
    val kind: ConnectionKind,
    val adapterName: String? = null,
    val message: String? = null,
)

/** Fotografía del estado del vehículo en un instante. Inmutable. */
data class VehicleSnapshot(
    val openings: Map<Opening, VehicleSignal<Boolean>>,
    val speedKmh: VehicleSignal<Double>,
    val rpm: VehicleSignal<Int>,
    val fuelLiters: VehicleSignal<Double>,
    val outsideTempC: VehicleSignal<Double>,
    val batteryVolts: VehicleSignal<Double>,
    val odometerKm: VehicleSignal<Long>,
    /** Luces de cruce/posición encendidas. Si hay dato, el tema automático lo usa (luces = oscuro). */
    val lightsOn: VehicleSignal<Boolean> = VehicleSignal.unavailable(),
    val connection: ConnectionState,
) {
    /** Aperturas cuyo estado conocido es "abierto". Las desconocidas no cuentan como abiertas. */
    val openOpenings: Set<Opening>
        get() = openings.filterValues { it.isDisplayable && it.value == true }.keys

    /** true solo si TODAS las aperturas tienen estado conocido y están cerradas. */
    val allKnownClosed: Boolean
        get() = openings.isNotEmpty() && openings.values.all { it.isDisplayable && it.value == false }

    companion object {
        fun unavailable(connection: ConnectionState = ConnectionState(ConnectionKind.NO_ADAPTER)) = VehicleSnapshot(
            openings = Opening.entries.associateWith { VehicleSignal.unavailable() },
            speedKmh = VehicleSignal.unavailable("km/h"),
            rpm = VehicleSignal.unavailable("rpm"),
            fuelLiters = VehicleSignal.unavailable("L"),
            outsideTempC = VehicleSignal.unavailable("°C"),
            batteryVolts = VehicleSignal.unavailable("V"),
            odometerKm = VehicleSignal.unavailable("km"),
            connection = connection,
        )
    }
}
