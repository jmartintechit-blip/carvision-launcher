package com.carvision.core.designsystem

import androidx.compose.material3.Typography
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.sp

/** Manrope (SIL OFL 1.1, ver docs/third-party/Manrope-OFL.txt), fuente variable incluida en la app. */
@OptIn(ExperimentalTextApi::class)
private fun manrope(weight: Int) = Font(
    R.font.manrope_variable,
    weight = FontWeight(weight),
    variationSettings = FontVariation.Settings(FontVariation.weight(weight)),
)

val Manrope = FontFamily(manrope(200), manrope(300), manrope(400), manrope(500), manrope(600), manrope(700), manrope(800))

private val Tight = LineHeightStyle(LineHeightStyle.Alignment.Center, LineHeightStyle.Trim.Both)

/**
 * Escala tipográfica. Criterio de las pantallas de serie: cifras grandes y finas, títulos medios,
 * etiquetas pequeñas con algo de espaciado. Nada en negrita gruesa.
 */
object CvText {
    /** Hora grande. */
    val clock = TextStyle(fontFamily = Manrope, fontWeight = FontWeight.W300, fontSize = 64.sp, letterSpacing = (-2).sp, lineHeightStyle = Tight)
    /** Cifra protagonista (velocidad). */
    val hero = TextStyle(fontFamily = Manrope, fontWeight = FontWeight.W200, fontSize = 72.sp, letterSpacing = (-2.5).sp, lineHeightStyle = Tight)
    val display = TextStyle(fontFamily = Manrope, fontWeight = FontWeight.W300, fontSize = 34.sp, letterSpacing = (-0.8).sp)
    val title = TextStyle(fontFamily = Manrope, fontWeight = FontWeight.W500, fontSize = 26.sp, letterSpacing = (-0.4).sp)
    val heading = TextStyle(fontFamily = Manrope, fontWeight = FontWeight.W600, fontSize = 17.sp, letterSpacing = (-0.1).sp)
    val body = TextStyle(fontFamily = Manrope, fontWeight = FontWeight.W500, fontSize = 15.sp)
    /** Valor de un dato secundario. */
    val value = TextStyle(fontFamily = Manrope, fontWeight = FontWeight.W400, fontSize = 22.sp, letterSpacing = (-0.4).sp)
    val label = TextStyle(fontFamily = Manrope, fontWeight = FontWeight.W500, fontSize = 13.sp, letterSpacing = 0.1.sp)
    /** Texto pequeño en mayúsculas espaciadas. */
    val caption = TextStyle(fontFamily = Manrope, fontWeight = FontWeight.W600, fontSize = 10.sp, letterSpacing = 1.4.sp)
    val unit = TextStyle(fontFamily = Manrope, fontWeight = FontWeight.W500, fontSize = 14.sp, letterSpacing = 0.2.sp)
}

val CvTypography = Typography(
    bodyLarge = CvText.body,
    bodyMedium = CvText.body,
    titleLarge = CvText.title,
    labelLarge = CvText.label,
)
