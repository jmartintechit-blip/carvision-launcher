package com.carvision.core.designsystem

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

private val FieldShape = RoundedCornerShape(14.dp)

/**
 * Campo de texto propio: etiqueta pequeña encima, superficie sutil y borde de acento al enfocar.
 * Sin el subrayado ni las animaciones de etiqueta de Material.
 */
@Composable
fun CvTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    keyboardType: KeyboardType = KeyboardType.Text,
    singleLine: Boolean = true,
    error: String? = null,
    suffix: String? = null,
) {
    val c = CvTheme.colors
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val border by animateColorAsState(
        when {
            error != null -> c.danger
            focused -> c.accent
            else -> c.surfaceBorder
        },
        label = "fieldBorder",
    )
    Column(modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(label.uppercase(), style = CvText.caption, color = if (error != null) c.danger else c.textTertiary)
        Box(
            Modifier
                .fillMaxWidth()
                .clip(FieldShape)
                .background(c.surface)
                .border(1.dp, border, FieldShape)
                .padding(horizontal = 14.dp, vertical = 12.dp),
        ) {
            if (value.isEmpty() && placeholder.isNotEmpty()) {
                Text(placeholder, style = CvText.body, color = c.textTertiary, maxLines = 1)
            }
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = singleLine,
                textStyle = CvText.body.copy(color = c.textPrimary),
                cursorBrush = SolidColor(c.accent),
                interactionSource = interaction,
                keyboardOptions = KeyboardOptions(keyboardType = keyboardType, imeAction = if (singleLine) ImeAction.Next else ImeAction.Default),
                modifier = Modifier.fillMaxWidth().padding(end = if (suffix != null) 36.dp else 0.dp),
            )
            if (suffix != null) {
                Text(suffix, style = CvText.label, color = c.textTertiary, modifier = Modifier.align(androidx.compose.ui.Alignment.CenterEnd))
            }
        }
        if (error != null) Text(error, style = CvText.label, color = c.danger)
    }
}

/** Botón de texto: principal (relleno) o secundario (superficie). */
@Composable
fun CvButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    primary: Boolean = false,
    destructive: Boolean = false,
    enabled: Boolean = true,
    horizontalPadding: androidx.compose.ui.unit.Dp = 22.dp,
) {
    val c = CvTheme.colors
    val bg = when {
        destructive -> c.danger.copy(alpha = 0.16f)
        primary -> c.primaryButton
        else -> c.surfaceStrong
    }
    val fg = when {
        destructive -> c.danger
        primary -> c.onPrimaryButton
        else -> c.textPrimary
    }
    Text(
        text,
        style = CvText.heading,
        color = if (enabled) fg else fg.copy(alpha = 0.4f),
        maxLines = 1,
        softWrap = false,
        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        modifier = modifier
            .clip(RoundedCornerShape(22.dp))
            .background(if (enabled) bg else bg.copy(alpha = bg.alpha * 0.5f))
            .pressable(enabled = enabled, onClick = onClick)
            .padding(horizontal = horizontalPadding, vertical = 12.dp),
    )
}
