package com.carvision.launcher.data

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.storage.StorageManager
import android.os.storage.StorageVolume
import android.provider.DocumentsContract
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.zip.ZipInputStream

/** Pack encontrado en un pendrive. */
data class UsbPack(val fileName: String, val title: String, val uri: Uri)

/**
 * Pendrive automático (D-028): detecta un USB conectado y busca packs (.zip) en él, sin permisos de
 * almacenamiento. Android pide al usuario permitir el acceso a ese USB una sola vez (selector del sistema);
 * después se recuerda.
 */
class UsbPacks(private val context: Context, private val scope: kotlinx.coroutines.CoroutineScope) {
    private val sm = context.getSystemService(StorageManager::class.java)
    private val present = MutableStateFlow<List<StorageVolume>>(emptyList())
    /** Memorias extraíbles conectadas ahora mismo. */
    val volumes: StateFlow<List<StorageVolume>> = present.asStateFlow()

    private val ask = MutableStateFlow<StorageVolume?>(null)
    /** Pendrive recién conectado sobre el que conviene preguntar «¿Buscar mi coche?». */
    val prompt: StateFlow<StorageVolume?> = ask.asStateFlow()
    private val prefs = context.getSharedPreferences("usb", Context.MODE_PRIVATE)

    /**
     * Se ha conectado un USB. Se pregunta solo si puede tener un coche: si ya hay permiso, se mira antes en
     * silencio (un pendrive de música siempre conectado no molesta en cada arranque); si el usuario dijo
     * «No preguntar» para ese pendrive, nunca.
     */
    fun onMounted() {
        refresh()
        val v = present.value.firstOrNull() ?: run { ask.value = null; return }
        val uuid = v.uuid ?: return
        if (uuid in prefs.getStringSet("ignored", emptySet()).orEmpty()) return
        val tree = grantedTree(v)
        if (tree == null) { ask.value = v; return }
        scope.launch { if (scan(tree).isNotEmpty()) ask.value = v }
    }

    /** Cierra la pregunta; con [forever], no vuelve a preguntar por ese pendrive. */
    fun dismiss(forever: Boolean) {
        val uuid = ask.value?.uuid
        ask.value = null
        if (forever && uuid != null) {
            prefs.edit().putStringSet("ignored", prefs.getStringSet("ignored", emptySet()).orEmpty() + uuid).apply()
        }
    }

    /** Vuelve a mirar qué memorias extraíbles hay (al montar un USB y al volver a la app). */
    fun refresh() {
        present.value = runCatching {
            sm.storageVolumes.filter { it.isRemovable && it.state == Environment.MEDIA_MOUNTED }
        }.getOrDefault(emptyList())
        if (present.value.isEmpty()) ask.value = null
    }

    /** Acceso ya concedido a la raíz de esta memoria (se recuerda entre arranques). */
    fun grantedTree(volume: StorageVolume): Uri? {
        val uuid = volume.uuid ?: return null
        return context.contentResolver.persistedUriPermissions
            .firstOrNull { it.isReadPermission && DocumentsContract.getTreeDocumentId(it.uri) == "$uuid:" }?.uri
    }

    /** Pantalla del sistema para permitir el acceso a esta memoria (una sola vez). */
    fun accessIntent(volume: StorageVolume): Intent? = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q -> volume.createOpenDocumentTreeIntent()
        else -> @Suppress("DEPRECATION") volume.createAccessIntent(null)
    }

    /** Guarda el permiso de lectura de la carpeta elegida para no volver a preguntarlo. */
    fun remember(tree: Uri) {
        runCatching { context.contentResolver.takePersistableUriPermission(tree, Intent.FLAG_GRANT_READ_URI_PERMISSION) }
    }

    /** Busca packs en la raíz del USB y en sus carpetas de primer nivel. Solo devuelve .zip con manifest.json. */
    suspend fun scan(tree: Uri): List<UsbPack> = withContext(Dispatchers.IO) {
        val found = mutableListOf<UsbPack>()
        val rootId = DocumentsContract.getTreeDocumentId(tree)
        fun children(docId: String): List<Triple<String, String, String>> {
            val uri = DocumentsContract.buildChildDocumentsUriUsingTree(tree, docId)
            val cols = arrayOf(
                DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                DocumentsContract.Document.COLUMN_MIME_TYPE,
            )
            return runCatching {
                context.contentResolver.query(uri, cols, null, null, null)?.use { c ->
                    buildList { while (c.moveToNext()) add(Triple(c.getString(0), c.getString(1) ?: "", c.getString(2) ?: "")) }
                }.orEmpty()
            }.getOrDefault(emptyList())
        }
        fun check(id: String, name: String) {
            if (!name.endsWith(".zip", ignoreCase = true)) return
            val doc = DocumentsContract.buildDocumentUriUsingTree(tree, id)
            title(doc)?.let { found += UsbPack(name, it, doc) }
        }
        children(rootId).forEach { (id, name, mime) ->
            if (mime == DocumentsContract.Document.MIME_TYPE_DIR) {
                if (!name.startsWith(".") && name !in SKIP_DIRS) children(id).forEach { (cid, cname, _) -> check(cid, cname) }
            } else check(id, name)
        }
        found
    }

    /** Nombre del coche si el .zip es un pack (tiene manifest.json en la raíz); si no, null. */
    private fun title(doc: Uri): String? = runCatching {
        context.contentResolver.openInputStream(doc)?.use { input ->
            ZipInputStream(input.buffered()).use { zip ->
                generateSequence { zip.nextEntry }.firstOrNull { it.name == "manifest.json" } ?: return@runCatching null
                val json = org.json.JSONObject(zip.readBytes().toString(Charsets.UTF_8))
                json.optString("name").ifBlank { json.optString("id") }.ifBlank { null }
            }
        }
    }.getOrNull()

    private companion object {
        /** Carpetas típicas de un pendrive que no merece la pena recorrer. */
        val SKIP_DIRS = setOf("LOST.DIR", "System Volume Information", "Android", "DCIM", "Music", "Movies")
    }
}
