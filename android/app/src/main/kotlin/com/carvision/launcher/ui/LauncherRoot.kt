package com.carvision.launcher.ui

import android.content.Context
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.sp
import com.carvision.core.designsystem.CvIconsCustom
import com.carvision.core.designsystem.CvText
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.carvision.core.designsystem.CarVisionTheme
import com.carvision.core.designsystem.CvBackground
import com.carvision.core.designsystem.CvIcons
import com.carvision.core.designsystem.CvTheme
import com.carvision.core.designsystem.pressable
import com.carvision.launcher.data.AppContainer
import com.carvision.launcher.data.ThemeMode
import com.carvision.launcher.ui.apps.AppsScreen
import com.carvision.launcher.ui.home.HomeScreen
import com.carvision.launcher.ui.settings.SettingsScreen
import kotlinx.coroutines.flow.map

enum class Destination { HOME, APPS, VEHICLE, MAINTENANCE, SETTINGS }

/** Altura de diseño: toda la interfaz se diseña para 600 dp de alto y se escala a la pantalla real. */
private const val DESIGN_HEIGHT_DP = 600f

@Composable
fun LauncherRoot(container: AppContainer, destination: Destination, onNavigate: (Destination) -> Unit) {
    val themeMode by container.settings.themeMode.collectAsStateWithLifecycle(ThemeMode.AUTO)
    val userScale by container.settings.uiScale.collectAsStateWithLifecycle(1f)
    val now = rememberClock()
    val vehicle by container.vehicle.collectAsStateWithLifecycle()
    val packState by container.packs.current.collectAsStateWithLifecycle()
    val bootAnimation by remember { container.settings.bootAnimation.map<Boolean, Boolean?> { it } }.collectAsStateWithLifecycle(null)
    var splashVisible by remember { mutableStateOf(container.bootSplashPending) }
    val dark = when (themeMode) {
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
        // Automático: si el coche informa de las luces, luces encendidas = tema oscuro.
        // Si no hay ese dato, por la hora (oscuro de 20:00 a 08:00): muchas radios no cambian el modo noche del sistema.
        ThemeMode.AUTO -> vehicle.lightsOn.takeIf { it.isDisplayable }?.value
            ?: (now.hour >= 20 || now.hour < 8)
    }
    val brandLogo = rememberBrandLogo(packState, container.brandLogos)
    val brandName = (packState as? com.carvision.launcher.data.PackState.Ready)?.pack?.manifest?.let { it.brand?.name ?: it.vehicle.make }
    val ambient by container.settings.ambient.collectAsStateWithLifecycle(com.carvision.core.designsystem.Ambient.GRAPHITE)

    CarVisionTheme(dark = dark, ambient = ambient) {
        CompositionLocalProvider(LocalNotifier provides container.notifier) {
            CvBackground(Modifier.fillMaxSize()) {
                BoxWithConstraints(Modifier.fillMaxSize()) {
                    val base = LocalDensity.current
                    val scale = (maxHeight.value / DESIGN_HEIGHT_DP).coerceIn(0.7f, 1.8f) * userScale
                    CompositionLocalProvider(LocalDensity provides Density(base.density * scale, base.fontScale)) {
                        Row(Modifier.fillMaxSize().padding(start = 4.dp, top = 12.dp, end = 14.dp, bottom = 12.dp)) {
                            Dock(destination, onNavigate, brandLogo, brandName)
                            Spacer(Modifier.width(14.dp))
                            Box(Modifier.weight(1f).fillMaxHeight()) {
                                // Inicio siempre está compuesto: el motor 3D y el coche no se recargan al volver.
                                // Mientras otra pantalla lo tapa, se oculta y deja de dibujar.
                                val onHome = destination == Destination.HOME
                                Box(Modifier.fillMaxSize().graphicsLayer { alpha = if (onHome) 1f else 0f }) {
                                    HomeScreen(container, now, onNavigate, active = onHome)
                                }
                                if (!onHome) {
                                    // Bloquea los toques hacia Inicio, que sigue debajo aunque invisible.
                                    Box(Modifier.fillMaxSize().pointerInput(Unit) { detectTapGestures { } })
                                }
                                AnimatedContent(
                                    targetState = destination,
                                    transitionSpec = { fadeIn(tween(220)) togetherWith fadeOut(tween(160)) },
                                    label = "destination",
                                    modifier = Modifier.fillMaxSize(),
                                ) { d ->
                                    when (d) {
                                        Destination.HOME -> Unit
                                        Destination.APPS -> AppsScreen()
                                        Destination.SETTINGS -> SettingsScreen(container)
                                        Destination.VEHICLE -> com.carvision.launcher.ui.vehicle.VehicleScreen(container)
                                        Destination.MAINTENANCE -> com.carvision.launcher.ui.maintenance.MaintenanceScreen(container)
                                    }
                                }
                                // Aviso de conducción centrado sobre la zona del coche (no tapa el reloj ni las tarjetas).
                                val driveAlert by container.alerts.active.collectAsStateWithLifecycle()
                                Box(Modifier.fillMaxWidth(HERO_FRACTION)) { AlertBanner(driveAlert) }
                            }
                        }
                        SheetHost(container)
                        // Asistente de primer arranque: después de la animación de arranque, hasta que se termine o se descarte.
                        val wizardDone by remember { container.settings.wizardDone.map<Boolean, Boolean?> { it } }.collectAsStateWithLifecycle(null)
                        LaunchedEffect(splashVisible, wizardDone) {
                            if (!splashVisible && wizardDone == false && !container.wizardOffered) {
                                container.wizardOffered = true
                                container.wizardVisible = true
                            }
                        }
                        LaunchedEffect(container.sheet) {
                            if (container.sheet == null && container.wizardReturnAfterSheet) {
                                container.wizardReturnAfterSheet = false
                                container.wizardVisible = true
                            }
                        }
                        if (container.wizardVisible && !splashVisible) SetupWizard(container) { container.wizardVisible = false }
                        // Pendrive recién conectado: «¿Buscar mi coche?»
                        val usbAsk by container.usb.prompt.collectAsStateWithLifecycle()
                        if (usbAsk != null && container.sheet == null && !container.wizardVisible && !splashVisible) {
                            UsbPrompt(
                                onOpen = { container.usb.dismiss(false); container.sheet = Sheet.USB },
                                onIgnore = { container.usb.dismiss(true) },
                                onClose = { container.usb.dismiss(false) },
                            )
                        }
                        NoticeHost(container.notifier)
                        // Animación de arranque (logotipo de la marca), una vez por arranque del proceso.
                        if (splashVisible) {
                            val finish = {
                                splashVisible = false
                                container.bootSplashPending = false
                                container.introPending = false
                            }
                            when (bootAnimation) {
                                false -> LaunchedEffect(Unit) { finish() }
                                else -> BootSplash(packState, container.brandLogos, carReady = container.carShown, onFinished = finish)
                            }
                        } else {
                            // Aviso discreto de mantenimiento al arrancar (una vez por arranque del proceso).
                            val book by container.maintenance.book.collectAsStateWithLifecycle()
                            LaunchedEffect(book.records.size) {
                                if (container.maintenanceNoticeShown || book.records.isEmpty()) return@LaunchedEffect
                                val (text, state) = com.carvision.launcher.ui.maintenance.maintenanceSummary(book, vehicle)
                                if (state == com.carvision.core.maintenance.DueState.OVERDUE || state == com.carvision.core.maintenance.DueState.SOON) {
                                    container.notifier.show("Mantenimiento: $text")
                                }
                                container.maintenanceNoticeShown = true
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Columna lateral al estilo de las marcas premium (Audi, Porsche, Mercedes): integrada en el fondo y separada
 * solo por una línea fina; logotipo de la marca arriba (vuelve a Inicio); iconos propios monocromos con su
 * texto debajo; el acceso activo se marca con un halo de luz suave, sin cajas ni "pastillas".
 */
@Composable
private fun Dock(current: Destination, onNavigate: (Destination) -> Unit, brandLogo: android.graphics.Bitmap?, brandName: String?) {
    val c = CvTheme.colors
    val ctx = LocalContext.current
    val notifier = LocalNotifier.current
    Row(Modifier.fillMaxHeight()) {
        Column(
            Modifier.width(86.dp).fillMaxHeight().padding(vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                Modifier.height(48.dp).fillMaxWidth().pressable { onNavigate(Destination.HOME) },
                contentAlignment = Alignment.Center,
            ) {
                when {
                    brandLogo != null -> CrispImage(brandLogo, 60.dp, contentDescription = "Inicio")
                    // Sin logotipo: la marca en texto, con espaciado de emblema (automático para cualquier coche).
                    !brandName.isNullOrBlank() -> BrandWordmark(brandName, c.textPrimary)
                    else -> Icon(CvIconsCustom.Vehicle, "Inicio", tint = c.textPrimary, modifier = Modifier.size(28.dp))
                }
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.SpaceEvenly, horizontalAlignment = Alignment.CenterHorizontally) {
                DockItem(CvIconsCustom.Navigation, "Navegación", false) { launchOrNotify(ctx, notifier, ExternalApps::navigation) }
                DockItem(CvIconsCustom.Media, "Música", false) { launchOrNotify(ctx, notifier, ExternalApps::music) }
                DockItem(CvIconsCustom.Phone, "Teléfono", false) { launchOrNotify(ctx, notifier, ExternalApps::phone) }
                DockItem(CvIconsCustom.Vehicle, "Vehículo", current == Destination.VEHICLE) { onNavigate(Destination.VEHICLE) }
                DockItem(CvIconsCustom.Service, "Servicio", current == Destination.MAINTENANCE) { onNavigate(Destination.MAINTENANCE) }
                DockItem(CvIconsCustom.Apps, "Apps", current == Destination.APPS) { onNavigate(Destination.APPS) }
                DockItem(CvIconsCustom.Settings, "Ajustes", current == Destination.SETTINGS) { onNavigate(Destination.SETTINGS) }
            }
        }
        // Separador vertical que se desvanece en los extremos.
        Box(
            Modifier
                .width(1.dp)
                .fillMaxHeight()
                .padding(vertical = 24.dp)
                .background(Brush.verticalGradient(listOf(Color.Transparent, c.surfaceBorder, c.surfaceBorder, Color.Transparent))),
        )
    }
}

@Composable
private fun DockItem(icon: ImageVector, label: String, active: Boolean, onClick: () -> Unit) {
    val c = CvTheme.colors
    val glow by animateFloatAsState(if (active) 1f else 0f, tween(260), label = "dockGlow")
    Column(
        Modifier.width(86.dp).pressable(onClick = onClick).padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier
                .size(42.dp)
                .drawBehind {
                    if (glow > 0f) {
                        drawCircle(
                            Brush.radialGradient(listOf(c.accent.copy(alpha = 0.32f * glow), Color.Transparent), radius = size.minDimension * 0.75f),
                            radius = size.minDimension * 0.75f,
                        )
                    }
                },
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = label, tint = lerp(c.textTertiary, c.textPrimary, glow), modifier = Modifier.size(26.dp))
        }
        Text(
            label, style = CvText.label.copy(fontSize = 11.sp, letterSpacing = 0.2.sp),
            color = lerp(c.textTertiary, c.textPrimary, glow), maxLines = 1,
        )
    }
}

/** Aviso arriba al conectar un pendrive. */
@Composable
private fun UsbPrompt(onOpen: () -> Unit, onIgnore: () -> Unit, onClose: () -> Unit) {
    val c = CvTheme.colors
    Box(Modifier.fillMaxSize().padding(top = 18.dp), contentAlignment = Alignment.TopCenter) {
        Row(
            Modifier
                .clip(RoundedCornerShape(26.dp))
                .background(if (c.isDark) c.backgroundTop.copy(alpha = 0.97f) else c.surfaceStrong)
                .padding(start = 20.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(CvIconsCustom.Garage, null, tint = c.accent, modifier = Modifier.size(24.dp))
            Text("Pendrive conectado", style = CvText.heading, color = c.textPrimary)
            Text(
                "Buscar mi coche", style = CvText.heading, color = c.onPrimaryButton,
                modifier = Modifier.clip(RoundedCornerShape(20.dp)).background(c.primaryButton).pressable(onClick = onOpen)
                    .padding(horizontal = 18.dp, vertical = 10.dp),
            )
            Text(
                "No preguntar", style = CvText.label, color = c.textTertiary,
                modifier = Modifier.pressable(onClick = onIgnore).padding(horizontal = 8.dp, vertical = 10.dp),
            )
            Icon(CvIcons.Close, "Cerrar", tint = c.textTertiary, modifier = Modifier.size(36.dp).pressable(onClick = onClose).padding(8.dp))
        }
    }
}

fun launchOrNotify(ctx: Context, notifier: Notifier, launcher: (Context) -> Boolean) {
    if (!launcher(ctx)) notifier.show("No hay ninguna aplicación instalada para esto")
}

/** Parte del ancho de contenido que ocupa el coche en Inicio (pesos 1,6 : 1). */
private const val HERO_FRACTION = 1.6f / 2.6f
