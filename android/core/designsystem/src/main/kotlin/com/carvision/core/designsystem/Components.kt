package com.carvision.core.designsystem

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

val CardShape = RoundedCornerShape(26.dp)
val ChipShape = RoundedCornerShape(16.dp)

/** Forma "squircle" aproximada para iconos de apps (esquinas suaves, como en los sistemas de serie). */
val SquircleShape = RoundedCornerShape(percent = 26)

/** Toque sin el destello de Android: el elemento se atenúa y encoge levemente al pulsarlo. */
@Composable
fun Modifier.pressable(enabled: Boolean = true, onClick: () -> Unit): Modifier {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val s by animateFloatAsState(if (pressed) 0.96f else 1f, tween(120), label = "press")
    val a by animateFloatAsState(if (pressed) 0.7f else 1f, tween(120), label = "pressAlpha")
    return this
        .graphicsLayer { scaleX = s; scaleY = s; alpha = a }
        .clickable(interaction, indication = null, enabled = enabled, onClick = onClick)
}

/**
 * Fondo: degradado casi negro con una luz ambiente muy suave detrás del coche.
 * Sin fotografías ni desenfoque (igual en cualquier GPU y versión de Android).
 */
@Composable
fun CvBackground(modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit) {
    val c = CvTheme.colors
    val p = c.ambientPalette
    Box(
        modifier
            .background(Brush.verticalGradient(listOf(c.backgroundTop, c.backgroundBottom)))
            .drawBehind {
                fun glow(color: Color, at: Offset, rel: Float) {
                    val center = Offset(size.width * at.x, size.height * at.y)
                    val r = size.width * rel
                    drawCircle(Brush.radialGradient(listOf(color, Color.Transparent), center = center, radius = r), radius = r, center = center)
                }
                glow(p.glow1, p.glow1At, p.glow1Radius)
                glow(p.glow2, p.glow2At, p.glow2Radius)
            },
        content = content,
    )
}

/** Tarjeta: superficie translúcida mínima y borde de un píxel. */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    padding: PaddingValues = PaddingValues(20.dp),
    strong: Boolean = false,
    onClick: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit,
) {
    val c = CvTheme.colors
    Box(
        modifier
            .then(if (onClick != null) Modifier.pressable(onClick = onClick) else Modifier)
            .clip(CardShape)
            .background(if (strong) c.surfaceStrong else c.surface)
            .border(1.dp, c.surfaceBorder, CardShape)
            .padding(padding),
        content = content,
    )
}

/** Elemento de estado: icono fino + título + valor. Sin fondos de color. */
@Composable
fun StatusChip(
    icon: ImageVector,
    title: String,
    subtitle: String,
    tint: Color,
    modifier: Modifier = Modifier,
    highlighted: Boolean = false,
    onClick: (() -> Unit)? = null,
) {
    val c = CvTheme.colors
    Row(
        modifier
            .then(if (onClick != null) Modifier.pressable(onClick = onClick) else Modifier)
            .clip(ChipShape)
            .background(if (highlighted) tint.copy(alpha = 0.14f) else c.surface)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Icon(icon, contentDescription = null, tint = if (highlighted) tint else c.textSecondary, modifier = Modifier.size(20.dp))
        Column {
            Text(title, style = CvText.label, color = c.textPrimary, maxLines = 1)
            Text(subtitle, style = CvText.label, color = if (highlighted) tint else c.textTertiary, maxLines = 1)
        }
    }
}

/** Etiqueta discreta en mayúsculas espaciadas (origen del dato, estado). Punto de color opcional. */
@Composable
fun Tag(text: String, color: Color, modifier: Modifier = Modifier, dot: Boolean = true) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
        if (dot) Box(Modifier.size(5.dp).clip(CircleShape).background(color))
        Text(text.uppercase(), style = CvText.caption, color = color, maxLines = 1)
    }
}

/** Botón de icono. `filled` = botón principal (círculo sólido, como el de reproducir). */
@Composable
fun RoundIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
    filled: Boolean = false,
    enabled: Boolean = true,
    iconScale: Float = 0.5f,
) {
    val c = CvTheme.colors
    Box(
        modifier
            .size(size)
            .pressable(enabled = enabled, onClick = onClick)
            .clip(CircleShape)
            .background(if (filled) c.primaryButton.copy(alpha = if (enabled) 1f else 0.25f) else Color.Transparent),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            icon,
            contentDescription = contentDescription,
            tint = when {
                filled -> c.onPrimaryButton
                !enabled -> c.textTertiary
                else -> c.textPrimary
            },
            modifier = Modifier.size(size * iconScale),
        )
    }
}

/** Control segmentado: pista sutil y segmento elegido resaltado en blanco translúcido. */
@Composable
fun SegmentedRow(modifier: Modifier = Modifier, content: @Composable RowScope.() -> Unit) {
    val c = CvTheme.colors
    Row(
        modifier
            .clip(ChipShape)
            .background(c.surface)
            .padding(3.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.CenterVertically,
        content = content,
    )
}

@Composable
fun Segment(text: String, selected: Boolean, onClick: () -> Unit, icon: ImageVector? = null) {
    val c = CvTheme.colors
    val bg by animateColorAsState(if (selected) c.selection else Color.Transparent, tween(180), label = "seg")
    Row(
        Modifier
            .clip(RoundedCornerShape(13.dp))
            .background(bg)
            .pressable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        if (icon != null) Icon(icon, null, tint = if (selected) c.textPrimary else c.textTertiary, modifier = Modifier.size(17.dp))
        Text(text, style = CvText.label, color = if (selected) c.textPrimary else c.textSecondary, maxLines = 1, softWrap = false)
    }
}

/** Separador de un píxel. */
@Composable
fun Hairline(modifier: Modifier = Modifier) {
    Box(modifier.height(1.dp).background(CvTheme.colors.surfaceBorder))
}

/** Contenedor de icono de app con forma squircle (para el cajón y el dock). */
@Composable
fun AppIconFrame(size: Dp, modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit) {
    Box(modifier.size(size).clip(SquircleShape), contentAlignment = Alignment.Center, content = content)
}

/** Padding vertical cómodo para filas tocables. */
val RowPadding = PaddingValues(horizontal = 4.dp, vertical = 14.dp)
