package com.carvision.launcher.ui

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.size
import com.carvision.launcher.data.BrandLogos
import com.carvision.launcher.data.PackState
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * Logotipo de la marca del coche activo (elegido por el usuario o el del pack), o null si no tiene.
 * Se reinicia a null al cambiar de coche: nunca se queda el logotipo del coche anterior.
 */
@Composable
fun rememberBrandLogo(packState: PackState, logos: BrandLogos): Bitmap? {
    val version by logos.changes.collectAsStateWithLifecycle()
    val logo by produceState<Bitmap?>(null, packState, version) {
        value = null
        val ready = packState as? PackState.Ready ?: return@produceState
        value = logos.load(ready)
    }
    return logo
}

/**
 * Reducción de alta calidad: divide a la mitad con filtro hasta acercarse al tamaño final y termina con un
 * último paso exacto. Reducir de golpe (p. ej. 743 → 60 px) con el filtro bilineal de Android deja el
 * logotipo pixelado y con dientes de sierra.
 */
fun highQualityScale(src: Bitmap, targetW: Int): Bitmap {
    if (targetW <= 0 || targetW >= src.width) return src
    var b = src
    while (b.width / 2 >= targetW) {
        b = Bitmap.createScaledBitmap(b, b.width / 2, max(1, b.height / 2), true)
    }
    val h = max(1, (b.height * targetW.toFloat() / b.width).roundToInt())
    return Bitmap.createScaledBitmap(b, targetW, h, true)
}

/** Imagen nítida a un ancho fijo: se reescala una sola vez al tamaño exacto en píxeles de la pantalla. */
@Composable
fun CrispImage(bitmap: Bitmap, width: Dp, contentDescription: String?, modifier: Modifier = Modifier) {
    val px = with(LocalDensity.current) { width.roundToPx() }
    val scaled = remember(bitmap, px) { highQualityScale(bitmap, px) }
    val heightDp = with(LocalDensity.current) { (scaled.height).toDp() }
    Image(
        scaled.asImageBitmap(),
        contentDescription = contentDescription,
        contentScale = ContentScale.FillBounds,
        modifier = modifier.size(width, heightDp),
    )
}

/**
 * Marca en texto cuando el coche no tiene logotipo: mayúsculas espaciadas tipo emblema.
 * El tamaño se ajusta a la longitud para que nunca se corte.
 */
@Composable
fun BrandWordmark(name: String, color: androidx.compose.ui.graphics.Color, modifier: Modifier = Modifier) {
    val n = name.trim().uppercase()
    val (size, spacing) = when {
        n.length <= 4 -> 16 to 3.5f
        n.length <= 6 -> 14 to 3f
        n.length <= 8 -> 12 to 2.2f
        else -> 10 to 1.4f
    }
    val base = com.carvision.core.designsystem.CvText.heading.copy(
        fontWeight = androidx.compose.ui.text.font.FontWeight.W600,
    )
    val measurer = androidx.compose.ui.text.rememberTextMeasurer()
    androidx.compose.foundation.layout.BoxWithConstraints(modifier, contentAlignment = androidx.compose.ui.Alignment.Center) {
        // Se ajusta al hueco disponible (barra lateral, tarjetas del Garaje…): reduce letra y espaciado hasta que cabe.
        val maxPx = constraints.maxWidth
        val style = androidx.compose.runtime.remember(n, maxPx, size, spacing) {
            var scale = 1f
            fun styleFor(k: Float) = base.copy(
                fontSize = androidx.compose.ui.unit.TextUnit(size * k, androidx.compose.ui.unit.TextUnitType.Sp),
                letterSpacing = androidx.compose.ui.unit.TextUnit(spacing * k, androidx.compose.ui.unit.TextUnitType.Sp),
            )
            if (maxPx != androidx.compose.ui.unit.Constraints.Infinity) {
                while (scale > 0.55f && measurer.measure(n, styleFor(scale), maxLines = 1, softWrap = false).size.width > maxPx) {
                    scale -= 0.05f
                }
            }
            styleFor(scale)
        }
        androidx.compose.material3.Text(n, style = style, color = color, maxLines = 1, softWrap = false)
    }
}
