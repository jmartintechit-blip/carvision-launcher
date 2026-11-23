package com.carvision.core.designsystem

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.dp

/** Glifos rellenos para los controles multimedia (como en los sistemas de serie). Diseño propio. */
object CvIconsFilled {
    private fun filled(name: String, vararg paths: String): ImageVector {
        val b = ImageVector.Builder(name = name, defaultWidth = 24.dp, defaultHeight = 24.dp, viewportWidth = 24f, viewportHeight = 24f)
        paths.forEach { b.addPath(pathData = addPathNodes(it), fill = SolidColor(Color.Black)) }
        return b.build()
    }

    /** Triángulo con esquinas redondeadas, desplazado a la derecha para centrarlo ópticamente. */
    val Play by lazy { filled("Play", "M8.5 5.2c0-.9 1-1.5 1.8-1l9.2 6c.7.5.7 1.5 0 2l-9.2 6c-.8.5-1.8-.1-1.8-1z") }
    val Pause by lazy { filled("Pause", "M7 4.5h3a1 1 0 0 1 1 1v13a1 1 0 0 1-1 1H7a1 1 0 0 1-1-1v-13a1 1 0 0 1 1-1z", "M14 4.5h3a1 1 0 0 1 1 1v13a1 1 0 0 1-1 1h-3a1 1 0 0 1-1-1v-13a1 1 0 0 1 1-1z") }
    val Next by lazy { filled("Next", "M3.5 6.6c0-.8.9-1.3 1.6-.8l6.4 4.4c.6.4.6 1.2 0 1.6l-6.4 4.4c-.7.5-1.6 0-1.6-.8z", "M12 6.6c0-.8.9-1.3 1.6-.8l6.4 4.4c.6.4.6 1.2 0 1.6l-6.4 4.4c-.7.5-1.6 0-1.6-.8z") }
    val Previous by lazy { filled("Previous", "M20.5 6.6c0-.8-.9-1.3-1.6-.8l-6.4 4.4c-.6.4-.6 1.2 0 1.6l6.4 4.4c.7.5 1.6 0 1.6-.8z", "M12 6.6c0-.8-.9-1.3-1.6-.8L4 10.2c-.6.4-.6 1.2 0 1.6l6.4 4.4c.7.5 1.6 0 1.6-.8z") }
}
