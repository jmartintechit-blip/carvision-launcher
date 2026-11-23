package com.carvision.launcher.ui

import android.content.ActivityNotFoundException
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalContext
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.carvision.core.designsystem.CvText
import com.carvision.core.designsystem.CvTheme
import com.carvision.core.designsystem.GlassCard
import com.carvision.core.model.Opening
import kotlinx.coroutines.delay
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

val SpanishLocale: Locale = Locale("es", "ES")
val TimeFormat: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm", SpanishLocale)
val DateFormat: DateTimeFormatter = DateTimeFormatter.ofPattern("EEEE, d 'de' MMMM", SpanishLocale)

/**
 * Hora actual, actualizada cada minuto y al instante cuando cambia la hora o la zona horaria
 * (las radios suelen ajustar la hora por GPS o red unos segundos después de arrancar).
 */
@Composable
fun rememberClock(): LocalDateTime {
    val ctx = LocalContext.current
    var now by remember { mutableStateOf(LocalDateTime.now()) }
    DisposableEffect(ctx) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(c: Context, i: Intent) {
                if (i.action == Intent.ACTION_TIMEZONE_CHANGED) {
                    java.util.TimeZone.setDefault(null) // fuerza a releer la zona del sistema
                }
                now = LocalDateTime.now()
            }
        }
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_TIME_TICK)
            addAction(Intent.ACTION_TIME_CHANGED)
            addAction(Intent.ACTION_TIMEZONE_CHANGED)
        }
        ctx.registerReceiver(receiver, filter)
        now = LocalDateTime.now()
        onDispose { ctx.unregisterReceiver(receiver) }
    }
    // Respaldo por si algún firmware no emite TIME_TICK.
    LaunchedEffect(Unit) {
        while (true) {
            delay(60_000L - (System.currentTimeMillis() % 60_000L) + 50)
            now = LocalDateTime.now()
        }
    }
    return now
}

fun Opening.label(): String = when (this) {
    Opening.FRONT_LEFT_DOOR -> "Puerta delantera izquierda"
    Opening.FRONT_RIGHT_DOOR -> "Puerta delantera derecha"
    Opening.REAR_LEFT_DOOR -> "Puerta trasera izquierda"
    Opening.REAR_RIGHT_DOOR -> "Puerta trasera derecha"
    Opening.TAILGATE -> "Maletero"
    Opening.HOOD -> "Capó"
}

/** La interfaz está en español de España: cifras con coma decimal y punto de millares, sea cual sea el idioma de la radio. */
val UiLocale: java.util.Locale = java.util.Locale("es", "ES")

/** "abierta"/"abierto" según el género del nombre. */
fun Opening.openLabel(): String = when (this) {
    Opening.TAILGATE, Opening.HOOD -> "${label()} abierto"
    else -> "${label()} abierta"
}

/** Nombre compacto para listas estrechas bajo el título «Aperturas». */
fun Opening.compactLabel(): String = when (this) {
    Opening.FRONT_LEFT_DOOR -> "Delantera izq."
    Opening.FRONT_RIGHT_DOOR -> "Delantera der."
    Opening.REAR_LEFT_DOOR -> "Trasera izq."
    Opening.REAR_RIGHT_DOOR -> "Trasera der."
    Opening.TAILGATE -> "Maletero"
    Opening.HOOD -> "Capó"
}

fun Opening.shortLabel(): String = when (this) {
    Opening.FRONT_LEFT_DOOR -> "Del. I"
    Opening.FRONT_RIGHT_DOOR -> "Del. D"
    Opening.REAR_LEFT_DOOR -> "Tras. I"
    Opening.REAR_RIGHT_DOOR -> "Tras. D"
    Opening.TAILGATE -> "Maletero"
    Opening.HOOD -> "Capó"
}

/** Lanzadores de aplicaciones externas. Devuelven false si no hay ninguna app que lo resuelva. */
object ExternalApps {
    private fun start(ctx: Context, intent: Intent): Boolean = try {
        ctx.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        true
    } catch (e: ActivityNotFoundException) {
        false
    } catch (e: SecurityException) {
        false
    }

    fun navigation(ctx: Context) = start(ctx, Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q=")))
    fun music(ctx: Context) = start(ctx, Intent.makeMainSelectorActivity(Intent.ACTION_MAIN, Intent.CATEGORY_APP_MUSIC))
    fun phone(ctx: Context) = start(ctx, Intent(Intent.ACTION_DIAL))
    fun homeSettings(ctx: Context) = start(ctx, Intent(android.provider.Settings.ACTION_HOME_SETTINGS))
}

/** Pantalla de sección todavía no construida. Dice claramente que está pendiente. */
@Composable
fun PlaceholderScreen(title: String, message: String) {
    val c = CvTheme.colors
    Column(Modifier.fillMaxSize().padding(horizontal = 14.dp, vertical = 6.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(title, style = CvText.display, color = c.textPrimary)
        Text("Pendiente de desarrollo", style = CvText.heading, color = c.textSecondary)
        Text(message, style = CvText.body, color = c.textTertiary)
    }
}
