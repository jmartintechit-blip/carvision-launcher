package com.carvision.launcher.data

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.core.content.pm.PackageInfoCompat
import com.carvision.vehicle.packs.FilePackSource
import com.carvision.vehicle.packs.LoadedVehiclePack
import com.carvision.vehicle.packs.PackImportResult
import com.carvision.vehicle.packs.PackSource
import com.carvision.vehicle.packs.PackZipImporter
import com.carvision.vehicle.packs.VehiclePackLoader
import com.carvision.vehicle.packs.VehiclePackParser
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

sealed interface PackState {
    data object Loading : PackState
    data class Ready(val pack: LoadedVehiclePack) : PackState
    data class Failed(val message: String) : PackState
}

/** Pack incluido en la app (assets/vehicle-packs/<id>/). */
class AssetPackSource(private val context: Context, private val folder: String) : PackSource {
    override val description = "assets/$folder"
    override fun read(path: String): ByteArray {
        require(VehiclePackParser.isSafeRelativePath(path)) { "Ruta no permitida: $path" }
        return context.assets.open("$folder/$path").use { it.readBytes() }
    }
}

/** Pack disponible para elegir. */
data class PackEntry(
    val id: String,
    val name: String,
    val make: String,
    val model: String,
    val imported: Boolean,
    /** Ruta del logotipo dentro del pack (manifest → brand.logo), si lo trae. */
    val logoPath: String? = null,
)

/**
 * Packs de vehículo: los incluidos en la app y los importados por el usuario (.zip), que se guardan en
 * la memoria privada de la app. Ambos se cargan con el mismo cargador; un pack importado con el mismo id
 * que uno incluido lo sustituye.
 *
 * Coches de prueba ([TEST_PACKS], p. ej. el Ford Focus con su marca): viajan dentro de la app, pero en el primer
 * arranque se instalan como si el usuario los hubiera importado (aparecen como «Importado» y se pueden borrar).
 * Así se ve cómo queda un coche con su marca sin que sea el coche por defecto. Si el usuario lo borra, no vuelve.
 */
class PackRepository(private val context: Context, private val scope: CoroutineScope) {
    private val state = MutableStateFlow<PackState>(PackState.Loading)
    val current: StateFlow<PackState> = state.asStateFlow()

    private val entries = MutableStateFlow<List<PackEntry>>(emptyList())
    val available: StateFlow<List<PackEntry>> = entries.asStateFlow()

    private val importedRoot = File(context.filesDir, "packs")
    private val importer = PackZipImporter(importedRoot, context.cacheDir)

    /** Carpeta visible por el usuario (Android/data/<app>/files/import) para copiar .zip sin selector de archivos. */
    val importFolder: File? get() = context.getExternalFilesDir("import")

    private fun bundledIds(): List<String> =
        context.assets.list(BUNDLED_DIR)?.sorted().orEmpty().filterNot { it in TEST_PACKS }
    private fun importedDirs(): List<File> = importedRoot.listFiles()?.filter { File(it, VehiclePackLoader.MANIFEST).isFile }.orEmpty()

    /**
     * Instala los coches de prueba como packs importados: una vez, y de nuevo cada vez que se actualiza la app
     * (si el usuario no los ha borrado), para que la copia instalada no se quede con una versión antigua del modelo.
     * Antes se instalaban solo una vez y una copia vieja del Focus tenía las ruedas traseras fijas (2026-11-13).
     */
    private fun installTestPacks() {
        val marker = File(importedRoot, ".test_packs_installed")
        val versionFile = File(importedRoot, ".test_packs_version")
        val done = if (marker.isFile) marker.readLines().toSet() else emptySet()
        val appVersion = runCatching {
            PackageInfoCompat.getLongVersionCode(context.packageManager.getPackageInfo(context.packageName, 0))
        }.getOrDefault(0L)
        val upgraded = versionFile.takeIf { it.isFile }?.readText()?.trim()?.toLongOrNull() != appVersion
        val refresh = if (upgraded) {
            TEST_PACKS.filter { it in done && File(File(importedRoot, it), VehiclePackLoader.MANIFEST).isFile }.toSet()
        } else emptySet()
        val pending = TEST_PACKS - done
        if (pending.isEmpty() && refresh.isEmpty()) {
            if (upgraded) runCatching { importedRoot.mkdirs(); versionFile.writeText(appVersion.toString()) }
            return
        }
        importedRoot.mkdirs()
        (pending + refresh).forEach { id ->
            val dest = File(importedRoot, id)
            runCatching {
                if (id in refresh || !File(dest, VehiclePackLoader.MANIFEST).isFile) {
                    val tmp = File(importedRoot, ".$id.tmp").apply { deleteRecursively() }
                    copyAssetTree("$BUNDLED_DIR/$id", tmp)
                    VehiclePackLoader.load(FilePackSource(tmp))      // comprobar antes de instalar
                    dest.deleteRecursively()
                    if (!tmp.renameTo(dest)) { tmp.copyRecursively(dest, overwrite = true); tmp.deleteRecursively() }
                }
            }.onFailure { Log.e(TAG, "No se pudo instalar el coche de prueba $id", it) }
        }
        marker.writeText((done + pending).joinToString("\n"))
        versionFile.writeText(appVersion.toString())
    }

    private fun copyAssetTree(assetPath: String, dest: File) {
        val children = context.assets.list(assetPath).orEmpty()
        if (children.isEmpty()) {
            dest.parentFile?.mkdirs()
            context.assets.open(assetPath).use { input -> dest.outputStream().use { input.copyTo(it) } }
        } else {
            dest.mkdirs()
            children.forEach { copyAssetTree("$assetPath/$it", File(dest, it)) }
        }
    }

    private fun sourceFor(id: String): PackSource? {
        importedDirs().firstOrNull { it.name == id }?.let { return FilePackSource(it) }
        if (id in bundledIds()) return AssetPackSource(context, "$BUNDLED_DIR/$id")
        return null
    }

    /** Recarga la lista de packs disponibles. */
    suspend fun refresh() = withContext(Dispatchers.IO) {
        installTestPacks()
        val imported = importedDirs().mapNotNull { dir ->
            runCatching { VehiclePackLoader.load(FilePackSource(dir)).manifest }.getOrNull()
                ?.let { PackEntry(it.id, it.name, it.vehicle.make, it.vehicle.model, imported = true, logoPath = it.brand?.logo) }
        }
        val importedIds = imported.map { it.id }.toSet()
        val bundled = bundledIds().filterNot { it in importedIds }.mapNotNull { id ->
            runCatching { VehiclePackLoader.load(AssetPackSource(context, "$BUNDLED_DIR/$id")).manifest }.getOrNull()
                ?.let { PackEntry(it.id, it.name, it.vehicle.make, it.vehicle.model, imported = false, logoPath = it.brand?.logo) }
        }
        entries.value = bundled + imported
    }

    fun load(preferredId: String? = null) {
        scope.launch {
            state.value = PackState.Loading
            refresh()
            state.value = withContext(Dispatchers.IO) {
                val ids = entries.value.map { it.id }
                val id = preferredId?.takeIf { it in ids } ?: ids.firstOrNull()
                    ?: return@withContext PackState.Failed("No hay ningún pack de vehículo instalado")
                try {
                    val pack = VehiclePackLoader.load(sourceFor(id) ?: error("Pack no encontrado: $id"))
                    pack.warnings.forEach { Log.w(TAG, "Pack $id: $it") }
                    PackState.Ready(pack)
                } catch (e: Exception) {
                    Log.e(TAG, "No se pudo cargar el pack $id", e)
                    PackState.Failed(e.message ?: "Error al cargar el pack")
                }
            }
        }
    }

    /** Importa un .zip elegido con el selector de archivos. */
    suspend fun importFromUri(uri: Uri): PackImportResult = withContext(Dispatchers.IO) {
        val input = context.contentResolver.openInputStream(uri)
            ?: return@withContext PackImportResult.Failure(listOf("No se puede abrir el archivo"))
        input.use { importer.import(it) }.also { refresh() }
    }

    /** Importa todos los .zip de la carpeta de importación (para radios sin selector de archivos). */
    suspend fun importFromFolder(): List<Pair<String, PackImportResult>> = withContext(Dispatchers.IO) {
        val dir = importFolder ?: return@withContext emptyList()
        val zips = dir.listFiles { f -> f.isFile && f.name.endsWith(".zip", ignoreCase = true) }.orEmpty()
        zips.map { f -> f.name to f.inputStream().use { importer.import(it) } }.also { refresh() }
    }

    /** Logotipo de cualquier pack (para el Garaje): el elegido por el usuario o el del pack. */
    suspend fun logoFor(entry: PackEntry, logos: BrandLogos): android.graphics.Bitmap? = withContext(Dispatchers.IO) {
        val f = logos.overrideFile(entry.id)
        if (f.isFile) return@withContext android.graphics.BitmapFactory.decodeFile(f.path)
        val path = entry.logoPath ?: return@withContext null
        runCatching {
            val bytes = sourceFor(entry.id)?.read(path) ?: return@runCatching null
            android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        }.getOrNull()
    }

    /** Elimina un pack importado (los incluidos no se pueden borrar). */
    suspend fun deleteImported(id: String) = withContext(Dispatchers.IO) {
        importedDirs().firstOrNull { it.name == id }?.deleteRecursively()
        refresh()
    }

    private companion object {
        const val BUNDLED_DIR = "vehicle-packs"
        /** Coches de prueba: se instalan como importados en el primer arranque (ver la clase). */
        val TEST_PACKS = setOf("ford_focus_mk3_5p")
        const val TAG = "CarVisionPacks"
    }
}
