package com.carvision.vehicle.packs

/**
 * Acceso de solo lectura a los archivos de un pack, venga de los assets de la app (pack incluido)
 * o de la carpeta de packs importados. El resto del sistema no distingue entre ambos.
 */
interface PackSource {
    /** Descripción para logs y mensajes de error. */
    val description: String

    /** Lee un archivo del pack. [path] ya ha sido validado como ruta relativa segura. */
    fun read(path: String): ByteArray
}

/** Pack cargado y validado, listo para el renderizador. */
class LoadedVehiclePack(
    val manifest: VehiclePackManifest,
    val warnings: List<String>,
    val source: PackSource,
) {
    fun readModel(): ByteArray = source.read(manifest.model.file)
}

class PackLoadException(message: String) : Exception(message)

object VehiclePackLoader {
    const val MANIFEST = "manifest.json"

    fun load(source: PackSource): LoadedVehiclePack {
        val text = try {
            source.read(MANIFEST).decodeToString()
        } catch (e: Exception) {
            throw PackLoadException("No se encuentra $MANIFEST en ${source.description}")
        }
        return when (val r = VehiclePackParser.parse(text)) {
            is PackParseResult.Valid -> LoadedVehiclePack(r.manifest, r.warnings, source)
            is PackParseResult.Invalid -> throw PackLoadException(r.errors.joinToString("\n"))
        }
    }
}
