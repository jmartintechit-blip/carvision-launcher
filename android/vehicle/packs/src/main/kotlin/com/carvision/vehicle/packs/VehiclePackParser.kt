package com.carvision.vehicle.packs

import com.carvision.core.model.Opening
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

/** Resultado de leer un pack: o un manifiesto válido, o la lista de problemas encontrados. */
sealed interface PackParseResult {
    data class Valid(val manifest: VehiclePackManifest, val warnings: List<String>) : PackParseResult
    data class Invalid(val errors: List<String>) : PackParseResult
}

object VehiclePackParser {
    const val SUPPORTED_FORMAT = 1
    private val ID_REGEX = Regex("^[a-z0-9][a-z0-9_.-]{2,63}$")
    private val SAFE_PATH = Regex("^[A-Za-z0-9_][A-Za-z0-9_./-]*$")

    private val json = Json { ignoreUnknownKeys = true }

    fun parse(text: String): PackParseResult {
        val manifest = try {
            json.decodeFromString(VehiclePackManifest.serializer(), text)
        } catch (e: SerializationException) {
            return PackParseResult.Invalid(listOf("manifest.json no es válido: ${e.message}"))
        } catch (e: IllegalArgumentException) {
            return PackParseResult.Invalid(listOf("manifest.json no es válido: ${e.message}"))
        }
        val errors = mutableListOf<String>()
        val warnings = mutableListOf<String>()

        if (manifest.formatVersion != SUPPORTED_FORMAT) {
            errors += "Versión de formato ${manifest.formatVersion} no soportada (se espera $SUPPORTED_FORMAT)."
        }
        if (!ID_REGEX.matches(manifest.id)) errors += "Identificador de pack inválido: '${manifest.id}'."
        if (!isSafeRelativePath(manifest.model.file) || !manifest.model.file.endsWith(".glb", ignoreCase = true)) {
            errors += "model.file debe ser una ruta relativa a un .glb dentro del pack."
        }
        val ids = manifest.parts.map { it.id }
        if (ids.size != ids.toSet().size) errors += "Hay piezas con el mismo id."
        manifest.parts.forEach { p ->
            if (p.axis.size != 3 || p.axis.all { it == 0f }) errors += "Pieza '${p.id}': eje inválido."
            if (p.openAngleDeg !in -150f..150f) errors += "Pieza '${p.id}': ángulo fuera de rango (±150°)."
            if (p.durationMs !in 100..5000) errors += "Pieza '${p.id}': duración fuera de rango (100–5000 ms)."
            if (Opening.fromSignalKey(p.signal) == null) warnings += "Pieza '${p.id}': señal desconocida '${p.signal}'."
        }
        manifest.wheels.forEach { w ->
            if (w.radiusM !in 0.15f..0.6f) errors += "Rueda '${w.node}': radio fuera de rango (0,15–0,6 m)."
            if (w.axis.size != 3 || w.axis.all { it == 0f }) errors += "Rueda '${w.node}': eje inválido."
        }
        manifest.brand?.logo?.let { logo ->
            val ext = logo.substringAfterLast('.', "").lowercase()
            if (!isSafeRelativePath(logo) || ext !in setOf("png", "webp")) {
                errors += "brand.logo debe ser una ruta relativa a un .png o .webp dentro del pack."
            }
        }
        with(manifest.camera) {
            if (minPitchDeg > maxPitchDeg) errors += "camera: minPitchDeg > maxPitchDeg."
            if (fovDeg !in 10f..90f) errors += "camera: fovDeg fuera de rango."
        }
        if (!manifest.license.commercialRedistribution) {
            warnings += "La licencia del modelo no permite redistribución comercial: el pack es solo para uso personal."
        }
        return if (errors.isEmpty()) PackParseResult.Valid(manifest, warnings) else PackParseResult.Invalid(errors)
    }

    /** Rechaza rutas absolutas o que intenten salir de la carpeta del pack (../). */
    fun isSafeRelativePath(path: String): Boolean =
        SAFE_PATH.matches(path) && path.split('/').none { it == ".." || it.isEmpty() }
}
