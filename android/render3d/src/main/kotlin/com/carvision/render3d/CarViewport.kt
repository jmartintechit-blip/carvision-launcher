package com.carvision.render3d

import android.view.TextureView
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.carvision.vehicle.packs.LoadedVehiclePack

/**
 * Vista 3D del coche para Compose.
 *
 * @param openings estado de aperturas por clave de señal ("door.front_left" → true si está abierta).
 * @param resetToken cambia su valor para volver a la vista inicial.
 */
@Composable
fun CarViewport(
    pack: LoadedVehiclePack?,
    openings: Map<String, Boolean>,
    viewMode: CarViewMode,
    resetToken: Int,
    schematicColors: SchematicColors,
    speedKmh: Double? = null,
    active: Boolean = true,
    darkTheme: Boolean = true,
    /** true mientras se ve la animación de arranque; al pasar a false, la cámara hace la bienvenida. */
    introArmed: Boolean = false,
    /** Color de la carrocería elegido por el usuario (0xRRGGBB) o null = el del modelo. */
    paintColor: Int? = null,
    /** Proporción de cada borde que se funde (0 = sin fundido). */
    edgeFade: Float = 0.14f,
    modifier: Modifier = Modifier,
    onDoubleTap: () -> Unit = {},
    onRendererSupport: (Boolean) -> Unit = {},
    /** El coche ya se ve en pantalla (o no hay 3D y se muestra la vista en planta). */
    onCarShown: () -> Unit = {},
) {
    val context = LocalContext.current
    val renderer = remember { CarRenderer(context) }

    DisposableEffect(renderer) {
        onDispose { renderer.destroy() }
    }
    LaunchedEffect(renderer) { onRendererSupport(renderer.isSupported) }
    val shownCallback = androidx.compose.runtime.rememberUpdatedState(onCarShown)
    DisposableEffect(renderer) {
        renderer.onCarShown = { shownCallback.value() }
        onDispose { renderer.onCarShown = null }
    }

    // Sin OpenGL ES 3.0: vista en planta 2D (la app sigue funcionando en cualquier radio).
    if (!renderer.isSupported) {
        LaunchedEffect(Unit) { onCarShown() }
        CarSchematic(openings, schematicColors.body, schematicColors.line, schematicColors.open, modifier)
        return
    }
    val introWasArmed = remember { booleanArrayOf(false) }
    LaunchedEffect(introArmed) {
        if (introArmed) {
            renderer.armIntro()
            introWasArmed[0] = true
        } else if (introWasArmed[0]) {
            renderer.playIntro()
            introWasArmed[0] = false
        }
    }
    LaunchedEffect(pack) { pack?.let { renderer.loadPack(it) } }
    LaunchedEffect(openings, pack) { renderer.setOpenings(openings) }
    LaunchedEffect(paintColor, pack) { renderer.setPaintColor(paintColor) }
    LaunchedEffect(viewMode) { renderer.setViewMode(viewMode) }
    LaunchedEffect(resetToken) { if (resetToken > 0) renderer.resetView() }
    LaunchedEffect(speedKmh) { renderer.setSpeedKmh(speedKmh) }
    LaunchedEffect(active) { renderer.setActive(active) }
    LaunchedEffect(darkTheme) { renderer.setStageTheme(darkTheme) }

    // Fundido en los cuatro bordes: el suelo de luz y la sombra se desvanecen sin dejar ver el rectángulo del visor.
    Box(
        modifier
            .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
            .drawWithContent {
                drawContent()
                val fx = edgeFade.coerceIn(0f, 0.45f)
                drawRect(
                    Brush.horizontalGradient(0f to Color.Transparent, fx to Color.Black, 1f - fx to Color.Black, 1f to Color.Transparent),
                    blendMode = BlendMode.DstIn,
                )
                drawRect(
                    Brush.verticalGradient(0f to Color.Transparent, fx * 1.3f to Color.Black, 1f - fx * 1.1f to Color.Black, 1f to Color.Transparent),
                    blendMode = BlendMode.DstIn,
                )
            },
    ) {
        AndroidView(
            factory = { ctx -> TextureView(ctx).also { renderer.attachTo(it) } },
            modifier = Modifier.fillMaxSize(),
        )
        // Capa de gestos encima de la TextureView (más fiable que dejar el toque a la vista nativa).
        Box(
            Modifier
                .matchParentSize()
                .pointerInput(renderer) {
                val tracker = VelocityTracker()
                detectDragGestures(
                    onDragStart = { tracker.resetTracking() },
                    onDragEnd = { renderer.onDragEnd(tracker.calculateVelocity().x, size.width) },
                    onDragCancel = { renderer.onDragEnd(0f, size.width) },
                ) { change, drag ->
                    tracker.addPosition(change.uptimeMillis, change.position)
                    change.consume()
                    renderer.onDrag(drag.x, drag.y, size.width)
                }
            }
            .pointerInput(renderer) {
                detectTapGestures(onDoubleTap = { onDoubleTap() })
            },
        )
    }
}
