package com.carvision.launcher.ui

import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.flow.first
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.rememberUpdatedState
import android.graphics.BitmapFactory
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.carvision.core.designsystem.Manrope
import com.carvision.launcher.data.PackState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlin.math.max
import kotlin.math.min

private const val TOTAL_MS = 2800
/** Punto de la animación en que empieza el fundido a la pantalla de inicio. */
private const val FADE_START = 0.84f
/** Máximo que el logotipo espera a que se vea el coche antes de fundirse. */
private const val WAIT_CAR_MS = 2500L
private val Accent = Color(0xFF34D1E0)

/** Fracción 0..1 de [p] dentro del tramo [a]..[b], con suavizado. */
private fun seg(p: Float, a: Float, b: Float): Float {
    val t = ((p - a) / (b - a)).coerceIn(0f, 1f)
    return t * t * (3f - 2f * t)
}

/**
 * Animación de arranque estilo fabricante: fondo negro, el logotipo de la marca aparece y crece levemente,
 * un destello de luz lo recorre, se dibuja una línea de luz bajo él y todo se funde con la pantalla de inicio.
 *
 * El logotipo sale del pack del vehículo (manifest → brand.logo). Si el pack no lo trae, se muestra "CarVision".
 * Duración total ≈ 2,8 s. Se puede desactivar en Ajustes.
 */
@Composable
fun BootSplash(
    packState: PackState,
    logos: com.carvision.launcher.data.BrandLogos,
    /** true cuando el coche ya se ve detrás: hasta entonces el logotipo espera quieto antes de fundirse. */
    carReady: Boolean = true,
    onFinished: () -> Unit,
) {
    // Esperar a que el pack esté listo (o falle) para saber qué logotipo mostrar; máximo 1,5 s.
    val resolved by produceState<Pair<android.graphics.Bitmap?, String?>?>(null, packState) {
        when (packState) {
            is PackState.Ready -> {
                val m = packState.pack.manifest
                value = logos.load(packState) to (m.brand?.name ?: m.vehicle.make)
            }
            is PackState.Failed -> value = null to null
            PackState.Loading -> {
                delay(1500)
                value = null to null
            }
        }
    }
    val progress = remember { Animatable(0f) }
    val ready by rememberUpdatedState(carReady)
    LaunchedEffect(resolved != null) {
        if (resolved == null) return@LaunchedEffect
        // Logotipo, destello y pausa hasta el fundido…
        progress.animateTo(FADE_START, tween((TOTAL_MS * FADE_START).toInt(), easing = LinearEasing))
        // …el logotipo espera quieto a que el coche esté dibujado (como mucho [WAIT_CAR_MS]): así al fundirse se ve el
        // coche y no un hueco. Con el logotipo quieto, el motor 3D tiene la tarjeta gráfica libre para ponerse al día.
        withTimeoutOrNull(WAIT_CAR_MS) { snapshotFlow { ready }.first { it } }
        progress.animateTo(1f, tween((TOTAL_MS * (1f - FADE_START)).toInt(), easing = LinearEasing))
        onFinished()
    }
    val p = progress.value
    val logo = resolved?.first

    Box(
        Modifier
            .fillMaxSize()
            .graphicsLayer { alpha = 1f - seg(p, FADE_START, 1f) }
            .background(Color.Black)
            .drawBehind {
                // Halo suave detrás del logotipo.
                val a = seg(p, 0.1f, 0.45f) * 0.35f
                drawCircle(
                    Brush.radialGradient(listOf(Accent.copy(alpha = a * 0.5f), Color.Transparent), center = center, radius = size.minDimension * 0.6f),
                    radius = size.minDimension * 0.6f, center = center,
                )
            },
        contentAlignment = Alignment.Center,
    ) {
        if (resolved == null) return@Box
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            val appear = seg(p, 0.04f, 0.36f)
            Box(
                Modifier
                    .graphicsLayer {
                        alpha = appear
                        val s = 0.86f + 0.14f * appear
                        scaleX = s
                        scaleY = s
                        compositingStrategy = CompositingStrategy.Offscreen // necesario para el destello enmascarado
                    },
                contentAlignment = Alignment.Center,
            ) {
                if (logo != null) {
                    CrispImage(logo, 340.dp, contentDescription = resolved?.second)
                } else {
                    Text(
                        (resolved?.second ?: "CarVision").uppercase(),
                        style = TextStyle(fontFamily = Manrope, fontWeight = FontWeight.W800, fontSize = 56.sp, letterSpacing = 10.sp, color = Color.White),
                    )
                }
                // Destello: banda de luz diagonal que solo se pinta sobre el logotipo (SrcAtop).
                Canvas(Modifier.matchParentSize()) {
                    val t = seg(p, 0.34f, 0.72f)
                    if (t <= 0f || t >= 1f) return@Canvas
                    val x = -size.width * 0.4f + (size.width * 1.8f) * t
                    drawRect(
                        Brush.linearGradient(
                            0f to Color.Transparent, 0.5f to Color.White.copy(alpha = 0.85f), 1f to Color.Transparent,
                            start = Offset(x - size.width * 0.18f, 0f), end = Offset(x + size.width * 0.18f, size.height),
                        ),
                        blendMode = BlendMode.SrcAtop,
                    )
                }
            }
            Spacer(Modifier.height(28.dp))
            // Línea de luz que se abre desde el centro.
            val line = seg(p, 0.42f, 0.74f)
            Canvas(Modifier.width(320.dp).height(2.dp)) {
                val w = size.width * line
                val x0 = (size.width - w) / 2
                drawRect(
                    Brush.horizontalGradient(listOf(Color.Transparent, Accent, Color.White, Accent, Color.Transparent), startX = x0, endX = x0 + max(w, 1f)),
                    topLeft = Offset(x0, 0f),
                    size = androidx.compose.ui.geometry.Size(min(w, size.width), size.height),
                    alpha = 1f - seg(p, 0.76f, 0.86f) * 0.6f,
                )
            }
        }
    }
}
