package com.carvision.vehicle.packs

import kotlinx.serialization.Serializable

/**
 * manifest.json de un Vehicle Pack (formato versionado, ver docs/VEHICLE_PACKS.md).
 *
 * Un pack describe SOLO la representación visual del vehículo. No contiene código ejecutable
 * ni información de protocolos CAN: las piezas se enlazan con señales normalizadas por su clave
 * (por ejemplo "door.front_left").
 */
@Serializable
data class VehiclePackManifest(
    val formatVersion: Int,
    val id: String,
    val name: String,
    val version: String,
    val vehicle: VehicleInfo,
    val model: ModelInfo,
    val parts: List<AnimatedPart> = emptyList(),
    val wheels: List<WheelInfo> = emptyList(),
    val camera: CameraConfig = CameraConfig(),
    /** Marca: logotipo para la animación de arranque. Opcional. */
    val brand: BrandInfo? = null,
    /** Pintura de la carrocería: la app permite cambiar su color al instante. Opcional. */
    val paint: PaintInfo = PaintInfo(),
    val license: LicenseInfo,
)

@Serializable
data class PaintInfo(
    /** Nombre del material de la pintura dentro del GLB. Si no existe, el color no se puede cambiar. */
    val material: String = "paint",
)

@Serializable
data class BrandInfo(
    val name: String,
    /** PNG o WebP con transparencia, relativo a la carpeta del pack (p. ej. "brand/logo.png"). */
    val logo: String? = null,
)

@Serializable
data class VehicleInfo(
    val make: String,
    val model: String,
    val generation: String? = null,
    val bodyStyle: String,
    val doors: Int,
) {
    /** "Marca Modelo"; si el modelo está vacío (p. ej. el coche por defecto «CarVision»), solo la marca. */
    val displayName: String get() = listOf(make, model).filter { it.isNotBlank() }.joinToString(" ")
}

@Serializable
data class ModelInfo(
    /** Ruta del .glb relativa a la carpeta del pack. */
    val file: String,
    /** Nombre del nodo raíz del coche dentro del GLB. */
    val rootNode: String = "car_root",
    /** Ángulo (grados, eje vertical) que hay que sumar para que el frontal mire a +Z. */
    val forwardYawDeg: Float = 0f,
)

@Serializable
data class AnimatedPart(
    /** Identificador de la pieza dentro del pack. */
    val id: String,
    /** Nombre del nodo en el GLB. Su origen debe estar en la bisagra. */
    val node: String,
    /** Clave de señal normalizada (ver Opening.signalKey). */
    val signal: String,
    /** Eje de giro en el espacio local del nodo. */
    val axis: List<Float>,
    /** Ángulo de apertura completa en grados (con signo). */
    val openAngleDeg: Float,
    val durationMs: Int = 700,
)

/** Rueda que gira según la velocidad. Su origen debe estar en el centro del buje. */
@Serializable
data class WheelInfo(
    val node: String,
    /** Radio exterior del neumático en metros (para convertir velocidad en giro). */
    val radiusM: Float,
    /** Eje de giro local; con frontal en +Z, avanzar = giro positivo sobre +X. */
    val axis: List<Float> = listOf(1f, 0f, 0f),
)

@Serializable
data class CameraConfig(
    val homeYawDeg: Float = 35f,
    val homePitchDeg: Float = 16f,
    val minPitchDeg: Float = 4f,
    val maxPitchDeg: Float = 55f,
    val fovDeg: Float = 28f,
    /** >1 aleja la cámara, <1 la acerca respecto al encuadre automático. */
    val zoom: Float = 1f,
)

@Serializable
data class LicenseInfo(
    /** Identificador SPDX o texto corto (p. ej. "CC-BY-4.0"). */
    val model: String,
    val author: String,
    val source: String,
    /** Texto de atribución que la app muestra en Ajustes › Licencias. */
    val attribution: String,
    /** true si la licencia permite redistribuir el archivo del modelo en un producto comercial. */
    val commercialRedistribution: Boolean,
)
