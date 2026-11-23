package com.carvision.core.designsystem

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Paleta de CarVision, estilo pantalla de serie de gama alta: negro profundo, superficies grafito casi
 * imperceptibles, texto blanco con jerarquía por opacidad y un único acento azul hielo, usado con moderación.
 * Todo color de la interfaz sale de aquí.
 */
@Immutable
data class CvColors(
    val isDark: Boolean,
    val backgroundTop: Color,
    val backgroundBottom: Color,
    /** Luz ambiente detrás del coche. */
    val ambient: Color,
    /** Ambiente de fondo elegido (degradado + luces). */
    val ambientPalette: AmbientPalette = Ambient.GRAPHITE.palette(isDark),
    val surface: Color,
    val surfaceStrong: Color,
    val surfaceBorder: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textTertiary: Color,
    val accent: Color,
    val onAccent: Color,
    /** Botón principal (reproducir): blanco sobre oscuro, negro sobre claro, como en los sistemas de serie. */
    val primaryButton: Color,
    val onPrimaryButton: Color,
    val ok: Color,
    val warning: Color,
    val danger: Color,
    val demo: Color,
    val selection: Color,
)

val DarkCvColors = CvColors(
    isDark = true,
    backgroundTop = Color(0xFF0A0C10),
    backgroundBottom = Color(0xFF020304),
    ambient = Color(0x2E7AA7FF),
    surface = Color(0x0FFFFFFF),
    surfaceStrong = Color(0x1AFFFFFF),
    surfaceBorder = Color(0x12FFFFFF),
    textPrimary = Color(0xFFF4F6F9),
    textSecondary = Color(0x9EE9EDF3),
    textTertiary = Color(0x61E9EDF3),
    accent = Color(0xFF7AA7FF),
    onAccent = Color(0xFF07101F),
    primaryButton = Color(0xFFF4F6F9),
    onPrimaryButton = Color(0xFF07090C),
    ok = Color(0xFF6FD3A1),
    warning = Color(0xFFF2B55E),
    danger = Color(0xFFFF6B6B),
    demo = Color(0xFFB4A3FF),
    selection = Color(0x24FFFFFF),
)

val LightCvColors = CvColors(
    isDark = false,
    backgroundTop = Color(0xFFF5F6F8),
    backgroundBottom = Color(0xFFE4E7EC),
    ambient = Color(0x263D6FE0),
    surface = Color(0x99FFFFFF),
    surfaceStrong = Color(0xD9FFFFFF),
    surfaceBorder = Color(0x0F000000),
    textPrimary = Color(0xFF0B0D10),
    textSecondary = Color(0x990B0D10),
    textTertiary = Color(0x610B0D10),
    accent = Color(0xFF2F62D8),
    onAccent = Color(0xFFFFFFFF),
    primaryButton = Color(0xFF0B0D10),
    onPrimaryButton = Color(0xFFFFFFFF),
    ok = Color(0xFF1E9E66),
    warning = Color(0xFFC9801C),
    danger = Color(0xFFD23D3D),
    demo = Color(0xFF6B4FD8),
    selection = Color(0x14000000),
)

val LocalCvColors = staticCompositionLocalOf { DarkCvColors }

object CvTheme {
    val colors: CvColors
        @Composable get() = LocalCvColors.current
}

@Composable
fun CarVisionTheme(dark: Boolean, ambient: Ambient = Ambient.GRAPHITE, content: @Composable () -> Unit) {
    val p = ambient.palette(dark)
    val base = if (dark) DarkCvColors else LightCvColors
    val cv = base.copy(
        backgroundTop = p.top,
        backgroundBottom = p.bottom,
        ambient = p.glow1,
        ambientPalette = p,
        accent = p.accent,
    )
    val material = if (dark) {
        darkColorScheme(primary = cv.accent, onPrimary = cv.onAccent, background = cv.backgroundBottom, surface = cv.backgroundTop)
    } else {
        lightColorScheme(primary = cv.accent, onPrimary = cv.onAccent, background = cv.backgroundBottom, surface = cv.backgroundTop)
    }
    CompositionLocalProvider(LocalCvColors provides cv) {
        MaterialTheme(colorScheme = material, typography = CvTypography, content = content)
    }
}
