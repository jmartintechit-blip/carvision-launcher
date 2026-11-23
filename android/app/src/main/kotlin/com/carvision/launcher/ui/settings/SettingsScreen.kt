package com.carvision.launcher.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.carvision.core.designsystem.CvIcons
import com.carvision.core.designsystem.CvText
import com.carvision.core.designsystem.CvTheme
import com.carvision.core.designsystem.GlassCard
import com.carvision.core.designsystem.Hairline
import com.carvision.core.designsystem.Segment
import com.carvision.core.designsystem.SegmentedRow
import com.carvision.core.designsystem.pressable
import com.carvision.launcher.data.AppContainer
import com.carvision.launcher.data.PackState
import com.carvision.launcher.data.ThemeMode
import com.carvision.launcher.ui.ExternalApps
import com.carvision.launcher.ui.LocalNotifier
import com.carvision.launcher.ui.launchOrNotify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Ajustes con estilo de lista agrupada de los sistemas de serie. */
@Composable
fun SettingsScreen(container: AppContainer) {
    val c = CvTheme.colors
    val ctx = LocalContext.current
    val notifier = LocalNotifier.current
    val scope = rememberCoroutineScope()
    val theme by container.settings.themeMode.collectAsStateWithLifecycle(ThemeMode.AUTO)
    val demo by container.settings.demoMode.collectAsStateWithLifecycle(false)
    val scale by container.settings.uiScale.collectAsStateWithLifecycle(1f)
    val bootAnim by container.settings.bootAnimation.collectAsStateWithLifecycle(true)
    val alertDoor by container.settings.alertDoorMoving.collectAsStateWithLifecycle(true)
    val alertSound by container.settings.alertSound.collectAsStateWithLifecycle(true)
    val alertVoice by container.settings.alertVoice.collectAsStateWithLifecycle(false)
    val pack by container.packs.current.collectAsStateWithLifecycle()
    val ambientName = container.settings.ambient.collectAsStateWithLifecycle(com.carvision.core.designsystem.Ambient.GRAPHITE).value.label

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 6.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        Text("Ajustes", style = CvText.display, color = c.textPrimary, modifier = Modifier.padding(start = 8.dp, top = 6.dp))

        Group("Pantalla") {
            SettingRow(CvIcons.SunMoon, "Tema", "Automático: según las luces del coche; si no hay dato, por la hora") {
                SegmentedRow {
                    Segment("Auto", theme == ThemeMode.AUTO, { scope.launch { container.settings.setThemeMode(ThemeMode.AUTO) } })
                    Segment("Claro", theme == ThemeMode.LIGHT, { scope.launch { container.settings.setThemeMode(ThemeMode.LIGHT) } })
                    Segment("Oscuro", theme == ThemeMode.DARK, { scope.launch { container.settings.setThemeMode(ThemeMode.DARK) } })
                }
            }
            Hairline(Modifier.fillMaxWidth())
            SettingRow(CvIcons.Apps, "Tamaño de la interfaz", null) {
                SegmentedRow {
                    listOf(0.9f to "Compacto", 1f to "Normal", 1.15f to "Grande").forEach { (v, label) ->
                        Segment(label, kotlin.math.abs(scale - v) < 0.01f, { scope.launch { container.settings.setUiScale(v) } })
                    }
                }
            }
            Hairline(Modifier.fillMaxWidth())
            SettingRow(CvIcons.ShieldCheck, "Animación de arranque", "Logotipo de la marca al encender la radio") {
                SegmentedRow {
                    Segment("Sí", bootAnim, { scope.launch { container.settings.setBootAnimation(true) } })
                    Segment("No", !bootAnim, { scope.launch { container.settings.setBootAnimation(false) } })
                }
            }
        }

        Group("Vehículo") {
            NavRow(CvIcons.Car, "Vehículo 3D", "Elegir o importar un coche (.zip)") { container.sheet = com.carvision.launcher.ui.Sheet.GARAGE }
            Hairline(Modifier.fillMaxWidth())
            NavRow(CvIcons.SunMoon, "Ambiente", "Fondo de la pantalla: ${ambientName}") { container.sheet = com.carvision.launcher.ui.Sheet.AMBIENT }
            Hairline(Modifier.fillMaxWidth())
            SettingRow(CvIcons.CanData, "Datos del coche", "Sin adaptador CAN todavía. La demostración usa datos simulados, siempre marcados") {
                SegmentedRow {
                    Segment("Sin conexión", !demo, { scope.launch { container.settings.setDemoMode(false) } })
                    Segment("Demostración", demo, { scope.launch { container.settings.setDemoMode(true) } })
                }
            }
        }

        Group("Avisos") {
            SettingRow(CvIcons.DoorOpen, "Apertura abierta en marcha", "Puerta, maletero o capó abiertos a más de 5 km/h. Ayuda, no sistema de seguridad") {
                SegmentedRow {
                    Segment("Sí", alertDoor, { scope.launch { container.settings.setAlertDoorMoving(true) } })
                    Segment("No", !alertDoor, { scope.launch { container.settings.setAlertDoorMoving(false) } })
                }
            }
            Hairline(Modifier.fillMaxWidth())
            SettingRow(CvIcons.Bell, "Sonido", "Tono suave al aparecer un aviso") {
                SegmentedRow {
                    Segment("Sí", alertSound, { scope.launch { container.settings.setAlertSound(true) } })
                    Segment("No", !alertSound, { scope.launch { container.settings.setAlertSound(false) } })
                }
            }
            Hairline(Modifier.fillMaxWidth())
            SettingRow(CvIcons.Info, "Voz", "Lee el aviso en voz alta con la voz en español de la radio, si la tiene") {
                SegmentedRow {
                    Segment("Sí", alertVoice, { scope.launch { container.settings.setAlertVoice(true) } })
                    Segment("No", !alertVoice, { scope.launch { container.settings.setAlertVoice(false) } })
                }
            }
        }

        Group("Sistema") {
            NavRow(CvIcons.ShieldCheck, "Asistente de inicio", "Pantalla de inicio, tu coche y la música, paso a paso") {
                container.wizardVisible = true
            }
            Hairline(Modifier.fillMaxWidth())
            NavRow(CvIcons.Home, "Volver al launcher original", "Abre el ajuste de Android: elige el launcher de tu radio. CarVision no se borra") {
                launchOrNotify(ctx, notifier, ExternalApps::homeSettings)
            }
            Hairline(Modifier.fillMaxWidth())
            NavRow(CvIcons.Music, "Música en reproducción", "Permiso para mostrar lo que suena") {
                runCatching { ctx.startActivity(container.media.permissionIntent()) }
            }
        }

        Group("Información") {
            when (val p = pack) {
                is PackState.Ready -> {
                    val m = p.pack.manifest
                    InfoRow("Vehículo", "${m.name} · v${m.version}")
                    Hairline(Modifier.fillMaxWidth())
                    InfoRow("Modelo 3D", "${m.license.attribution} (${m.license.model})")
                    p.pack.warnings.forEach { InfoRow("Aviso", it) }
                }
                is PackState.Failed -> InfoRow("Vehículo", p.message)
                PackState.Loading -> InfoRow("Vehículo", "Cargando…")
            }
            Hairline(Modifier.fillMaxWidth())
            InfoRow("Licencias", "Manrope (OFL 1.1) · Lucide (ISC) · Filament (Apache 2.0)")
        }
        Spacer(Modifier.height(12.dp))
    }
}

@Composable
private fun Group(title: String, content: @Composable () -> Unit) {
    val c = CvTheme.colors
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title.uppercase(), style = CvText.caption, color = c.textTertiary, modifier = Modifier.padding(start = 12.dp))
        GlassCard(Modifier.fillMaxWidth(), padding = PaddingValues(horizontal = 18.dp, vertical = 4.dp)) {
            Column { content() }
        }
    }
}

@Composable
private fun SettingRow(icon: ImageVector, title: String, subtitle: String?, control: @Composable () -> Unit) {
    val c = CvTheme.colors
    Row(
        Modifier.fillMaxWidth().padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Icon(icon, null, tint = c.textSecondary, modifier = Modifier.size(22.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = CvText.heading, color = c.textPrimary)
            if (subtitle != null) Text(subtitle, style = CvText.label, color = c.textTertiary)
        }
        control()
    }
}

@Composable
private fun NavRow(icon: ImageVector, title: String, subtitle: String, onClick: () -> Unit) {
    val c = CvTheme.colors
    Box(Modifier.fillMaxWidth().pressable(onClick = onClick)) {
        SettingRow(icon, title, subtitle) {
            Icon(CvIcons.ChevronRight, null, tint = c.textTertiary, modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
private fun InfoRow(title: String, value: String) {
    val c = CvTheme.colors
    Row(Modifier.fillMaxWidth().padding(vertical = 12.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(title, style = CvText.body, color = c.textPrimary)
        Text(value, style = CvText.label, color = c.textTertiary, modifier = Modifier.weight(1f), textAlign = TextAlign.End)
    }
}
