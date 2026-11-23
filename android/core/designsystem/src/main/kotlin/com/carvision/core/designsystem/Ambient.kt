package com.carvision.core.designsystem

import androidx.compose.runtime.Immutable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color

/**
 * Ambientes de fondo (diseño propio de CarVision). Cada uno define un degradado de base, dos luces de color
 * difuminadas y un acento a juego, con variante oscura y clara. Se dibujan con degradados: cero coste de GPU extra.
 */
enum class Ambient(val label: String) {
    GRAPHITE("Grafito"),
    MIDNIGHT("Medianoche"),
    AURORA("Aurora"),
    SUNSET("Atardecer"),
    BORDEAUX("Burdeos"),
    TITANIUM("Titanio"),
}

@Immutable
data class AmbientPalette(
    val top: Color,
    val bottom: Color,
    /** Luz principal (detrás del coche). */
    val glow1: Color,
    /** Posición relativa (0..1) de la luz principal. */
    val glow1At: Offset,
    val glow1Radius: Float,
    val glow2: Color,
    val glow2At: Offset,
    val glow2Radius: Float,
    val accent: Color,
)

fun Ambient.palette(dark: Boolean): AmbientPalette = when (this) {
    Ambient.GRAPHITE -> if (dark) {
        AmbientPalette(Color(0xFF0A0C10), Color(0xFF020304), Color(0x2E7AA7FF), Offset(0.36f, 0.62f), 0.42f, Color(0x14FFFFFF), Offset(0.85f, 0.05f), 0.35f, Color(0xFF7AA7FF))
    } else {
        AmbientPalette(Color(0xFFF5F6F8), Color(0xFFE4E7EC), Color(0x263D6FE0), Offset(0.36f, 0.62f), 0.42f, Color(0x33FFFFFF), Offset(0.85f, 0.05f), 0.35f, Color(0xFF2F62D8))
    }
    Ambient.MIDNIGHT -> if (dark) {
        AmbientPalette(Color(0xFF0B1430), Color(0xFF02050D), Color(0x4D2F5BFF), Offset(0.3f, 0.75f), 0.55f, Color(0x2E8A5BFF), Offset(0.9f, 0.1f), 0.45f, Color(0xFF8DB0FF))
    } else {
        AmbientPalette(Color(0xFFEEF2FB), Color(0xFFD5DEF4), Color(0x404A72F0), Offset(0.3f, 0.75f), 0.55f, Color(0x268A5BFF), Offset(0.9f, 0.1f), 0.45f, Color(0xFF3557D6))
    }
    Ambient.AURORA -> if (dark) {
        AmbientPalette(Color(0xFF05161B), Color(0xFF020608), Color(0x4019C2B0), Offset(0.28f, 0.8f), 0.55f, Color(0x332E7BFF), Offset(0.92f, 0.08f), 0.5f, Color(0xFF5FE0D0))
    } else {
        AmbientPalette(Color(0xFFEAF6F5), Color(0xFFD3EAE8), Color(0x3319B0A0), Offset(0.28f, 0.8f), 0.55f, Color(0x262E7BFF), Offset(0.92f, 0.08f), 0.5f, Color(0xFF128A7E))
    }
    Ambient.SUNSET -> if (dark) {
        AmbientPalette(Color(0xFF1A0D12), Color(0xFF060309), Color(0x40FF6A3D), Offset(0.75f, 0.85f), 0.55f, Color(0x2E8A3DFF), Offset(0.1f, 0.05f), 0.5f, Color(0xFFFFA06F))
    } else {
        AmbientPalette(Color(0xFFFBF0EA), Color(0xFFF0DBD0), Color(0x38FF7A45), Offset(0.75f, 0.85f), 0.55f, Color(0x1F8A3DFF), Offset(0.1f, 0.05f), 0.5f, Color(0xFFD2622E))
    }
    Ambient.BORDEAUX -> if (dark) {
        AmbientPalette(Color(0xFF18060B), Color(0xFF060204), Color(0x40C2183D), Offset(0.35f, 0.8f), 0.55f, Color(0x1FFF8A5B), Offset(0.9f, 0.08f), 0.45f, Color(0xFFFF7A90))
    } else {
        AmbientPalette(Color(0xFFF8EDEF), Color(0xFFEBD3D8), Color(0x30C2183D), Offset(0.35f, 0.8f), 0.55f, Color(0x1FFF8A5B), Offset(0.9f, 0.08f), 0.45f, Color(0xFFB0213F))
    }
    Ambient.TITANIUM -> if (dark) {
        AmbientPalette(Color(0xFF17191C), Color(0xFF0A0B0D), Color(0x1FD8DEE6), Offset(0.4f, 0.7f), 0.5f, Color(0x14FFFFFF), Offset(0.85f, 0.0f), 0.45f, Color(0xFFD3DAE3))
    } else {
        AmbientPalette(Color(0xFFF3F3F1), Color(0xFFDADAD6), Color(0x26FFFFFF), Offset(0.4f, 0.7f), 0.5f, Color(0x1A6B7380), Offset(0.85f, 0.0f), 0.45f, Color(0xFF4A5462))
    }
}
