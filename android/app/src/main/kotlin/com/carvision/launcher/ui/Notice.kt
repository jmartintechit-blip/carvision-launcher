package com.carvision.launcher.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.carvision.core.designsystem.CvText
import com.carvision.core.designsystem.CvTheme
import kotlinx.coroutines.delay

/** Avisos breves propios (sustituyen a los Toast de Android, que delatan el sistema). */
class Notifier {
    var message by mutableStateOf<String?>(null)
        private set
    private var serial by mutableStateOf(0)

    fun show(text: String) {
        message = text
        serial++
    }

    internal val key get() = serial
    internal fun clear() { message = null }
}

val LocalNotifier = staticCompositionLocalOf { Notifier() }

/** Muestra el aviso actual arriba, centrado, durante 2,5 s. */
@Composable
fun NoticeHost(notifier: Notifier) {
    val c = CvTheme.colors
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(notifier.key) {
        if (notifier.message != null) {
            visible = true
            delay(2500)
            visible = false
            delay(300)
            notifier.clear()
        }
    }
    Box(Modifier.fillMaxWidth().padding(top = 18.dp), contentAlignment = Alignment.TopCenter) {
        AnimatedVisibility(
            visible = visible && notifier.message != null,
            enter = fadeIn() + slideInVertically { -it / 2 },
            exit = fadeOut() + slideOutVertically { -it / 2 },
        ) {
            Text(
                notifier.message.orEmpty(),
                style = CvText.body,
                color = c.textPrimary,
                modifier = Modifier
                    .clip(RoundedCornerShape(22.dp))
                    .background(if (c.isDark) c.backgroundTop.copy(alpha = 0.96f) else c.surfaceStrong)
                    .border(1.dp, c.surfaceBorder, RoundedCornerShape(22.dp))
                    .padding(horizontal = 22.dp, vertical = 12.dp),
            )
        }
    }
}
