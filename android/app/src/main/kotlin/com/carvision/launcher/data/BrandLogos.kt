package com.carvision.launcher.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.max
import kotlin.math.min

/**
 * Logotipos de marca por coche.
 * Prioridad: 1) logotipo elegido por el usuario para ese pack; 2) el que trae el pack (brand.logo);
 * 3) ninguno → la interfaz dibuja la marca en texto (nunca se queda el logotipo de otro coche).
 */
class BrandLogos(private val context: Context) {
    private val dir = File(context.filesDir, "brand")
    private val version = MutableStateFlow(0)
    /** Cambia cada vez que se guarda o borra un logotipo, para refrescar la interfaz. */
    val changes: StateFlow<Int> = version.asStateFlow()

    fun overrideFile(packId: String) = File(dir, "$packId.png")

    /** Logotipo para un pack ya cargado, o null si no hay ninguno. */
    suspend fun load(state: PackState.Ready): Bitmap? = withContext(Dispatchers.IO) {
        val id = state.pack.manifest.id
        val f = overrideFile(id)
        if (f.isFile) return@withContext BitmapFactory.decodeFile(f.path)
        val path = state.pack.manifest.brand?.logo ?: return@withContext null
        runCatching {
            val bytes = state.pack.source.read(path)
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        }.getOrNull()
    }

    /** Guarda una imagen elegida por el usuario como logotipo del pack, quitando el fondo si hace falta. */
    suspend fun setFromUri(packId: String, uri: Uri): Boolean = withContext(Dispatchers.IO) {
        val src = context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it) } ?: return@withContext false
        val clean = LogoCleaner.process(src)
        dir.mkdirs()
        overrideFile(packId).outputStream().use { clean.compress(Bitmap.CompressFormat.PNG, 100, it) }
        version.value++
        true
    }

    suspend fun clear(packId: String) = withContext(Dispatchers.IO) {
        overrideFile(packId).delete()
        version.value++
    }
}

/**
 * Limpieza automática de logotipos:
 * - si la imagen no tiene transparencia, quita el fondo claro uniforme conectado con los bordes
 *   (blanco o los "cuadros de transparencia" pintados que traen muchas webs de logotipos);
 * - suaviza el contorno recuperando la transparencia de los píxeles de borde mezclados con el fondo;
 * - recorta al contenido y limita el tamaño.
 */
object LogoCleaner {
    fun process(input: Bitmap): Bitmap {
        var src = input.copy(Bitmap.Config.ARGB_8888, false)
        val maxW = 1024
        if (src.width > maxW) src = Bitmap.createScaledBitmap(src, maxW, max(1, src.height * maxW / src.width), true)
        val w = src.width
        val h = src.height
        val px = IntArray(w * h)
        src.getPixels(px, 0, w, 0, 0, w, h)

        val hasAlpha = px.any { Color.alpha(it) < 250 }
        if (!hasAlpha) removeBackground(px, w, h)
        return crop(px, w, h)
    }

    private fun isBackgroundLike(c: Int): Boolean {
        val r = Color.red(c); val g = Color.green(c); val b = Color.blue(c)
        val mx = max(r, max(g, b)); val mn = min(r, min(g, b))
        return mn >= 200 && mx - mn <= 28            // claro y casi sin color
    }

    private fun removeBackground(px: IntArray, w: Int, h: Int) {
        val bg = BooleanArray(w * h)
        val queue = ArrayDeque<Int>()
        fun seed(i: Int) {
            if (!bg[i] && isBackgroundLike(px[i])) { bg[i] = true; queue.addLast(i) }
        }
        for (x in 0 until w) { seed(x); seed((h - 1) * w + x) }
        for (y in 0 until h) { seed(y * w); seed(y * w + w - 1) }
        while (queue.isNotEmpty()) {
            val i = queue.removeFirst()
            val x = i % w; val y = i / w
            if (x > 0) seed(i - 1)
            if (x < w - 1) seed(i + 1)
            if (y > 0) seed(i - w)
            if (y < h - 1) seed(i + w)
        }
        // Fondo fuera; en los 2 px de borde se estima la transparencia por la mezcla con el blanco.
        for (i in px.indices) if (bg[i]) px[i] = Color.TRANSPARENT
        repeat(2) {
            val edge = IntArray(px.size) { -1 }
            for (i in px.indices) {
                if (Color.alpha(px[i]) == 0) continue
                val x = i % w; val y = i / w
                val touches = (x > 0 && Color.alpha(px[i - 1]) == 0) || (x < w - 1 && Color.alpha(px[i + 1]) == 0) ||
                    (y > 0 && Color.alpha(px[i - w]) == 0) || (y < h - 1 && Color.alpha(px[i + w]) == 0)
                if (touches) edge[i] = i
            }
            for (i in edge) {
                if (i < 0) continue
                val c = px[i]
                val mn = min(Color.red(c), min(Color.green(c), Color.blue(c)))
                val a = ((255 - mn) / 255f * 1.4f).coerceIn(0.05f, 1f)
                fun un(v: Int) = ((v - (1 - a) * 255) / a).toInt().coerceIn(0, 255)
                px[i] = Color.argb((a * 255).toInt(), un(Color.red(c)), un(Color.green(c)), un(Color.blue(c)))
            }
        }
    }

    private fun crop(px: IntArray, w: Int, h: Int): Bitmap {
        var x0 = w; var y0 = h; var x1 = -1; var y1 = -1
        for (y in 0 until h) for (x in 0 until w) {
            if (Color.alpha(px[y * w + x]) > 12) {
                if (x < x0) x0 = x; if (x > x1) x1 = x; if (y < y0) y0 = y; if (y > y1) y1 = y
            }
        }
        val full = Bitmap.createBitmap(px, w, h, Bitmap.Config.ARGB_8888)
        if (x1 < 0) return full
        return Bitmap.createBitmap(full, x0, y0, x1 - x0 + 1, y1 - y0 + 1)
    }
}
