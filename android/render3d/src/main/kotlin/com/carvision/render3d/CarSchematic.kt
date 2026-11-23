package com.carvision.render3d

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import kotlin.math.min

data class SchematicColors(val body: Color, val line: Color, val open: Color)

/**
 * Vista en planta 2D del coche (sin GPU 3D). Se usa:
 * - como respaldo cuando la radio no tiene OpenGL ES 3.0;
 * - en el futuro, en widgets pequeños y en la ventana flotante.
 * El frontal apunta hacia arriba. Las aperturas abiertas se dibujan con [openColor].
 */
@Composable
fun CarSchematic(
    openings: Map<String, Boolean>,
    bodyColor: Color,
    lineColor: Color,
    openColor: Color,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier) {
        val h = size.height * 0.86f
        val w = min(h * 0.42f, size.width * 0.6f)
        val left = (size.width - w) / 2
        val top = (size.height - h) / 2
        val stroke = Stroke(width = w * 0.025f)

        // Carrocería
        drawRoundRect(bodyColor, Offset(left, top), Size(w, h), CornerRadius(w * 0.28f), alpha = 0.9f)
        drawRoundRect(lineColor, Offset(left, top), Size(w, h), CornerRadius(w * 0.28f), style = stroke)
        // Parabrisas, techo y luneta
        drawRoundRect(lineColor, Offset(left + w * 0.14f, top + h * 0.27f), Size(w * 0.72f, h * 0.46f), CornerRadius(w * 0.16f), style = stroke, alpha = 0.6f)

        fun door(key: String, x: Float, y0: Float, y1: Float, side: Float) {
            val open = openings[key] == true
            val c = if (open) openColor else lineColor
            val dx = if (open) side * w * 0.22f else 0f
            drawLine(c, Offset(x, top + h * y0), Offset(x + dx, top + h * y1), strokeWidth = w * if (open) 0.06f else 0.03f)
        }
        door("door.front_left", left, 0.30f, 0.50f, -1f)
        door("door.rear_left", left, 0.51f, 0.70f, -1f)
        door("door.front_right", left + w, 0.30f, 0.50f, 1f)
        door("door.rear_right", left + w, 0.51f, 0.70f, 1f)

        val hatchOpen = openings["tailgate"] == true
        drawLine(
            if (hatchOpen) openColor else lineColor,
            Offset(left + w * 0.2f, top + h * (if (hatchOpen) 1.04f else 0.985f)),
            Offset(left + w * 0.8f, top + h * (if (hatchOpen) 1.04f else 0.985f)),
            strokeWidth = w * if (hatchOpen) 0.06f else 0.03f,
        )
    }
}
