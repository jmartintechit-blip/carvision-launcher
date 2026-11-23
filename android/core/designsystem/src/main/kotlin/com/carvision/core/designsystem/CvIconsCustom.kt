package com.carvision.core.designsystem

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.dp

/**
 * Iconos propios de CarVision para la columna principal. Diseñados con los criterios de las interfaces de serie
 * de gama alta: monocromos, geométricos, trazo fino y uniforme (1,5), sin metáforas de "móvil" (casa, engranaje,
 * cuadrícula de cuadrados).
 */
object CvIconsCustom {
    private const val STROKE = 1.5f

    private fun icon(name: String, strokes: List<String>, fills: List<String> = emptyList()): ImageVector {
        val b = ImageVector.Builder(name = name, defaultWidth = 24.dp, defaultHeight = 24.dp, viewportWidth = 24f, viewportHeight = 24f)
        strokes.forEach {
            b.addPath(
                pathData = addPathNodes(it), fill = null, stroke = SolidColor(Color.Black), strokeLineWidth = STROKE,
                strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round,
            )
        }
        fills.forEach { b.addPath(pathData = addPathNodes(it), fill = SolidColor(Color.Black)) }
        return b.build()
    }

    private fun dot(cx: Float, cy: Float, r: Float) = "M${cx - r},$cy a$r,$r 0 1,0 ${2 * r},0 a$r,$r 0 1,0 ${-2 * r},0 Z"

    /** Flecha de navegación (estilo indicador de posición). */
    val Navigation by lazy { icon("Navigation", listOf("M12 2.8 L19.4 20.6 L12 16.9 L4.6 20.6 Z")) }

    /** Nota musical. */
    val Media by lazy {
        icon("Media", listOf("M9 17.5V5.2l11-2.2v12.4", "M3.5 17.5a2.75 2.75 0 1 0 5.5 0a2.75 2.75 0 1 0 -5.5 0", "M14.5 15.4a2.75 2.75 0 1 0 5.5 0a2.75 2.75 0 1 0 -5.5 0"))
    }

    /** Auricular de teléfono, trazo limpio. */
    val Phone by lazy {
        icon("Phone", listOf("M7.2 3.5h-2a1.7 1.7 0 0 0-1.7 1.9c.9 8.1 7.1 14.3 15.2 15.2a1.7 1.7 0 0 0 1.9-1.7v-2.1a1.4 1.4 0 0 0-1.1-1.4l-3-.7a1.4 1.4 0 0 0-1.4.5l-1.2 1.5a11.3 11.3 0 0 1-5.4-5.4l1.5-1.2a1.4 1.4 0 0 0 .5-1.4l-.7-3a1.4 1.4 0 0 0-1.4-1.1z"))
    }

    /** Silueta lateral de un compacto. */
    val Vehicle by lazy {
        icon(
            "Vehicle",
            listOf(
                "M4.3 16.2H2.8v-2.6c0-.8.5-1.5 1.3-1.7l2.9-.8 2.6-3c.5-.6 1.2-.9 1.9-.9h4.1c.8 0 1.5.4 2 1l2.2 2.8 1.3.3c.8.2 1.4.9 1.4 1.8v3.1h-1.6",
                "M9.5 16.2h5.1",
                "M4.6 16.2a2.45 2.45 0 1 0 4.9 0a2.45 2.45 0 1 0 -4.9 0",
                "M14.6 16.2a2.45 2.45 0 1 0 4.9 0a2.45 2.45 0 1 0 -4.9 0",
                "M8.6 11.2h10.2",
            ),
        )
    }

    /** Llave de servicio. */
    val Service by lazy {
        icon("Service", listOf("M14.6 6.3a1 1 0 0 0 0 1.4l1.7 1.7a1 1 0 0 0 1.4 0l3.4-3.4a5.6 5.6 0 0 1-7.4 7.4l-6.2 6.2a2.1 2.1 0 0 1-3-3l6.2-6.2a5.6 5.6 0 0 1 7.4-7.4z"))
    }

    private fun rrect(x: Float, y: Float, w: Float, h: Float, r: Float) =
        "M${x + r},$y h${w - 2 * r} a$r,$r 0 0 1 $r,$r v${h - 2 * r} a$r,$r 0 0 1 ${-r},$r h${-(w - 2 * r)} " +
            "a$r,$r 0 0 1 ${-r},${-r} v${-(h - 2 * r)} a$r,$r 0 0 1 $r,${-r} Z"

    /** Aplicaciones: cuatro módulos redondeados de trazo, con el mismo peso visual que el resto. */
    val Apps by lazy {
        icon("Apps", listOf(rrect(3.5f, 3.5f, 7f, 7f, 2.2f), rrect(13.5f, 3.5f, 7f, 7f, 2.2f), rrect(3.5f, 13.5f, 7f, 7f, 2.2f), rrect(13.5f, 13.5f, 7f, 7f, 2.2f)))
    }

    /** Ajustes: engranaje de perfil suave (Lucide, ISC) con el trazo fino de la columna. */
    val Settings by lazy {
        icon(
            "Settings",
            listOf(
                "M9.671 4.136a2.34 2.34 0 0 1 4.659 0 2.34 2.34 0 0 0 3.319 1.915 2.34 2.34 0 0 1 2.33 4.033 2.34 2.34 0 0 0 0 3.831 2.34 2.34 0 0 1-2.33 4.033 2.34 2.34 0 0 0-3.319 1.915 2.34 2.34 0 0 1-4.659 0 2.34 2.34 0 0 0-3.32-1.915 2.34 2.34 0 0 1-2.33-4.033 2.34 2.34 0 0 0 0-3.831A2.34 2.34 0 0 1 6.35 6.051a2.34 2.34 0 0 0 3.319-1.915",
                "M9 12a3 3 0 1 0 6 0a3 3 0 1 0 -6 0",
            ),
        )
    }

    /** Paleta de ambiente (temas de fondo). */
    val Ambient by lazy {
        icon(
            "Ambient",
            listOf(
                "M12 3a9 9 0 1 0 0 18c1.1 0 1.8-.9 1.8-1.9 0-.5-.2-.9-.5-1.3-.3-.3-.5-.8-.5-1.3 0-1 .8-1.8 1.8-1.8h2.1A4.3 4.3 0 0 0 21 10.4C21 6.3 17 3 12 3z",
            ),
            listOf(dot(7.6f, 11.6f, 1.25f), dot(10.4f, 7.6f, 1.25f), dot(15f, 7.9f, 1.25f)),
        )
    }

    /** Garaje de vehículos (cambiar o importar coche). */
    val Garage by lazy {
        icon(
            "Garage",
            listOf(
                "M3 20.5V9.2l9-5.7 9 5.7v11.3",
                "M6.6 20.5v-3.4c0-.6.3-1.1.8-1.3l1.8-.6 1.6-2c.3-.4.8-.6 1.3-.6h1.8c.5 0 1 .2 1.3.6l1.6 2 1.8.6c.5.2.8.7.8 1.3v3.4",
                "M6.6 18.7h10.8",
            ),
        )
    }

}
