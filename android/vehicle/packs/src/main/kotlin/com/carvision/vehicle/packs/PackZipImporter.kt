package com.carvision.vehicle.packs

import java.io.File
import java.io.InputStream
import java.util.zip.ZipInputStream

/** Resultado de importar un pack desde un .zip. */
sealed interface PackImportResult {
    data class Success(val manifest: VehiclePackManifest, val dir: File, val warnings: List<String>) : PackImportResult
    data class Failure(val errors: List<String>) : PackImportResult
}

/**
 * Importa un Vehicle Pack desde un .zip de forma segura:
 * - solo extensiones de datos (json, glb, png, webp, jpg, md, txt): nunca código;
 * - rutas relativas sin "..", sin rutas absolutas (protección contra "zip slip");
 * - límites de tamaño total, por archivo y número de entradas (protección contra "zip bombs");
 * - acepta el pack en la raíz del zip o dentro de una única carpeta;
 * - valida el manifiesto y que el modelo y el logotipo existan antes de instalar.
 * Se extrae primero a una carpeta temporal; solo si todo es correcto se mueve a [packsRoot]/<id>.
 */
class PackZipImporter(
    private val packsRoot: File,
    private val tempRoot: File,
    private val maxTotalBytes: Long = 80L * 1024 * 1024,
    private val maxEntryBytes: Long = 60L * 1024 * 1024,
    private val maxEntries: Int = 400,
) {
    private val allowedExt = setOf("json", "glb", "png", "webp", "jpg", "jpeg", "md", "txt")

    fun import(input: InputStream): PackImportResult {
        val tmp = File(tempRoot, "pack-import-${System.nanoTime()}").apply { mkdirs() }
        try {
            val err = extract(input, tmp)
            if (err != null) return PackImportResult.Failure(listOf(err))

            // El pack puede estar en la raíz o dentro de una única carpeta.
            val root = if (File(tmp, VehiclePackLoader.MANIFEST).isFile) tmp else {
                val dirs = tmp.listFiles()?.filter { it.isDirectory }.orEmpty()
                dirs.singleOrNull()?.takeIf { File(it, VehiclePackLoader.MANIFEST).isFile }
                    ?: return PackImportResult.Failure(listOf("El .zip no contiene manifest.json (¿se ha descomprimido o renombrado?)"))
            }
            val parsed = VehiclePackParser.parse(File(root, VehiclePackLoader.MANIFEST).readText())
            val manifest = when (parsed) {
                is PackParseResult.Invalid -> return PackImportResult.Failure(parsed.errors)
                is PackParseResult.Valid -> parsed.manifest
            }
            val missing = listOfNotNull(manifest.model.file, manifest.brand?.logo).filterNot { File(root, it).isFile }
            if (missing.isNotEmpty()) return PackImportResult.Failure(missing.map { "Falta el archivo '$it' indicado en el manifiesto." })

            val dest = File(packsRoot, manifest.id)
            packsRoot.mkdirs()
            if (dest.exists()) dest.deleteRecursively()
            if (!root.renameTo(dest)) {
                root.copyRecursively(dest, overwrite = true)
            }
            return PackImportResult.Success(manifest, dest, (parsed as PackParseResult.Valid).warnings)
        } catch (e: Exception) {
            return PackImportResult.Failure(listOf("No se pudo leer el .zip: ${e.message}"))
        } finally {
            tmp.deleteRecursively()
        }
    }

    /** Devuelve un mensaje de error o null si todo es correcto. */
    private fun extract(input: InputStream, dest: File): String? {
        var total = 0L
        var count = 0
        val canonicalDest = dest.canonicalPath + File.separator
        ZipInputStream(input.buffered()).use { zip ->
            while (true) {
                val entry = zip.nextEntry ?: break
                count++
                if (count > maxEntries) return "El .zip tiene demasiados archivos (máximo $maxEntries)."
                val name = entry.name.replace('\\', '/')
                if (name.startsWith("/") || name.split('/').any { it == ".." }) return "Ruta no permitida en el .zip: $name"
                if (name.startsWith("__MACOSX/") || name.substringAfterLast('/').startsWith(".")) continue
                val out = File(dest, name)
                if (!out.canonicalPath.startsWith(canonicalDest)) return "Ruta no permitida en el .zip: $name"
                if (entry.isDirectory) {
                    out.mkdirs()
                    continue
                }
                val ext = name.substringAfterLast('.', "").lowercase()
                if (ext !in allowedExt) return "Tipo de archivo no permitido en un pack: $name"
                out.parentFile?.mkdirs()
                var written = 0L
                out.outputStream().use { os ->
                    val buf = ByteArray(64 * 1024)
                    while (true) {
                        val n = zip.read(buf)
                        if (n < 0) break
                        written += n
                        total += n
                        if (written > maxEntryBytes) return "Archivo demasiado grande en el .zip: $name"
                        if (total > maxTotalBytes) return "El pack es demasiado grande (máximo ${maxTotalBytes / 1024 / 1024} MB)."
                        os.write(buf, 0, n)
                    }
                }
            }
        }
        if (count == 0) return "El archivo no es un .zip válido o está vacío."
        return null
    }
}

/** Pack instalado en una carpeta (packs importados). Solo lee dentro de esa carpeta. */
class FilePackSource(private val dir: File) : PackSource {
    override val description = dir.path
    override fun read(path: String): ByteArray {
        require(VehiclePackParser.isSafeRelativePath(path)) { "Ruta no permitida: $path" }
        val f = File(dir, path)
        require(f.canonicalPath.startsWith(dir.canonicalPath + File.separator)) { "Ruta no permitida: $path" }
        return f.readBytes()
    }
}
