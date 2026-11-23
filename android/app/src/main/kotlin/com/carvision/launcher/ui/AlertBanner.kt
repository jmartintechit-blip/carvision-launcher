package com.carvision.launcher.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.carvision.core.designsystem.CvIcons
import com.carvision.core.designsystem.CvText
import com.carvision.core.designsystem.CvTheme
import com.carvision.launcher.alerts.DriveAlert

/**
 * Aviso de conducción arriba, centrado. Sobrio: cápsula oscura/clara con un punto ámbar que respira; sin rojo
 * chillón ni pantallas completas (es un aviso de ayuda, no una función de seguridad certificada).
 */
@Composable
fun AlertBanner(alert: DriveAlert?) {
    val c = CvTheme.colors
    // Conserva el último aviso durante la animación de salida.
    val last = remember { arrayOfNulls<DriveAlert>(1) }
    if (alert != null) last[0] = alert
    val shown = alert ?: last[0]
    // Ocupa toda la franja de cabecera de la zona del coche (título y botones) mientras dura el aviso:
    // así no se solapa a medias con nada.
    Box(Modifier.fillMaxWidth().padding(top = 2.dp, start = 4.dp), contentAlignment = Alignment.TopCenter) {
        AnimatedVisibility(
            visible = alert != null,
            enter = fadeIn(tween(220)) + slideInVertically(tween(320)) { -it / 2 },
            exit = fadeOut(tween(260)) + slideOutVertically(tween(260)) { -it / 2 },
        ) {
            val pulse by rememberInfiniteTransition(label = "alertPulse").animateFloat(
                initialValue = 0.45f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
                label = "alertPulseValue",
            )
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(22.dp))
                    .background(c.backgroundTop)
                    .background(c.surfaceStrong)
                    .border(1.dp, c.warning.copy(alpha = 0.55f), RoundedCornerShape(22.dp))
                    .padding(start = 18.dp, end = 26.dp, top = 17.dp, bottom = 17.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Box(
                    Modifier.size(40.dp).clip(CircleShape).background(c.warning.copy(alpha = 0.16f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        CvIcons.DoorOpen,
                        contentDescription = null,
                        tint = c.warning,
                        modifier = Modifier.size(22.dp).graphicsLayer { alpha = pulse },
                    )
                }
                Column {
                    Text(shown?.title.orEmpty(), style = CvText.heading, color = c.textPrimary)
                    Text(shown?.detail.orEmpty(), style = CvText.label, color = c.textSecondary)
                }
            }
        }
    }
}
