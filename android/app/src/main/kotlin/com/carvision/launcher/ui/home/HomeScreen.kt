package com.carvision.launcher.ui.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.draw.drawBehind
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.carvision.core.designsystem.CardShape
import com.carvision.core.designsystem.CvIcons
import com.carvision.core.designsystem.CvIconsCustom
import com.carvision.core.designsystem.CvIconsFilled
import com.carvision.launcher.ui.Sheet
import com.carvision.core.designsystem.CvText
import com.carvision.core.designsystem.CvTheme
import com.carvision.core.designsystem.GlassCard
import com.carvision.core.designsystem.Hairline
import com.carvision.core.designsystem.RoundIconButton
import com.carvision.core.designsystem.Segment
import com.carvision.core.designsystem.SquircleShape
import com.carvision.core.designsystem.StatusChip
import com.carvision.core.designsystem.Tag
import com.carvision.core.designsystem.pressable
import com.carvision.core.model.ConnectionKind
import com.carvision.core.model.Opening
import com.carvision.core.model.SignalSource
import com.carvision.core.model.SignalStatus
import com.carvision.core.model.VehicleSignal
import com.carvision.core.model.VehicleSnapshot
import com.carvision.launcher.data.AppContainer
import com.carvision.launcher.data.PackState
import com.carvision.launcher.media.MediaState
import com.carvision.launcher.media.NowPlayingRepository
import com.carvision.launcher.ui.DateFormat
import com.carvision.launcher.ui.Destination
import com.carvision.launcher.ui.ExternalApps
import com.carvision.launcher.ui.LocalNotifier
import com.carvision.launcher.ui.TimeFormat
import com.carvision.launcher.ui.launchOrNotify
import com.carvision.launcher.ui.openLabel
import com.carvision.launcher.ui.shortLabel
import com.carvision.render3d.CarViewMode
import com.carvision.render3d.CarViewport
import com.carvision.render3d.SchematicColors
import java.time.LocalDateTime
import java.util.Locale

@Composable
fun HomeScreen(container: AppContainer, now: LocalDateTime, onNavigate: (Destination) -> Unit, active: Boolean = true) {
    val vehicle by container.vehicle.collectAsStateWithLifecycle()
    val packState by container.packs.current.collectAsStateWithLifecycle()
    val maintBook by container.maintenance.book.collectAsStateWithLifecycle()
    val packId = (packState as? PackState.Ready)?.pack?.manifest?.id
    val paintColor by remember(packId) { packId?.let { container.settings.paintColor(it) } ?: kotlinx.coroutines.flow.flowOf(null) }
        .collectAsStateWithLifecycle(null)

    Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        HeroCar(
            vehicle = vehicle,
            packState = packState,
            onToggleOpening = { container.demoProvider.toggleOpening(it) },
            onToggleLights = { container.demoProvider.toggleLights() },
            onCloseAll = { container.demoProvider.closeAll() },
            onNavigate = onNavigate,
            onOpenSheet = { container.sheet = it },
            maintenanceSummary = com.carvision.launcher.ui.maintenance.maintenanceSummary(maintBook, vehicle),
            active = active,
            introArmed = container.introPending,
            paintColor = paintColor,
            onCarShown = { container.carShown = true },
            modifier = Modifier.weight(1.6f).fillMaxHeight(),
        )
        Column(Modifier.weight(1f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            ClockCard(now, vehicle, Modifier.fillMaxWidth())
            MediaCard(container.media, Modifier.fillMaxWidth())
            DriveCard(vehicle, Modifier.fillMaxWidth().weight(1f))
        }
    }
}

// ------------------------------------------------------------------ coche

@Composable
private fun HeroCar(
    vehicle: VehicleSnapshot,
    packState: PackState,
    onToggleOpening: (Opening) -> Unit,
    onToggleLights: () -> Unit,
    onCloseAll: () -> Unit,
    onNavigate: (Destination) -> Unit,
    onOpenSheet: (Sheet) -> Unit,
    maintenanceSummary: Pair<String, com.carvision.core.maintenance.DueState?>,
    active: Boolean,
    introArmed: Boolean,
    paintColor: Int?,
    onCarShown: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = CvTheme.colors
    var viewMode by remember { mutableStateOf(CarViewMode.PERSPECTIVE) }
    var resetToken by remember { mutableIntStateOf(0) }
    var has3d by remember { mutableStateOf(true) }
    var demoPanel by remember { mutableStateOf(false) }
    val isDemo = vehicle.connection.kind == ConnectionKind.DEMO
    val open = vehicle.openOpenings
    if (!isDemo) demoPanel = false

    // Sin marco: el coche "flota" sobre el fondo, como en las presentaciones de los fabricantes.
    Box(modifier) {
        when (packState) {
            is PackState.Ready -> CarViewport(
                pack = packState.pack,
                openings = vehicle.openings.mapKeys { it.key.signalKey }
                    .mapValues { it.value.isDisplayable && it.value.value == true },
                viewMode = viewMode,
                resetToken = resetToken,
                schematicColors = SchematicColors(c.surfaceStrong, c.textSecondary, c.warning),
                speedKmh = vehicle.speedKmh.takeIf { it.isDisplayable }?.value,
                active = active,
                darkTheme = c.isDark,
                introArmed = introArmed,
                paintColor = paintColor,
                modifier = Modifier.fillMaxSize().padding(top = 36.dp, bottom = 40.dp),
                onDoubleTap = { resetToken++ },
                onRendererSupport = { has3d = it },
                onCarShown = onCarShown,
            )
            is PackState.Failed -> Text(
                "No se pudo cargar el vehículo 3D:\n${packState.message}",
                style = CvText.body, color = c.danger, modifier = Modifier.align(Alignment.Center).padding(24.dp),
            )
            PackState.Loading -> Unit
        }

        // Cabecera
        Row(Modifier.fillMaxWidth().padding(start = 8.dp, top = 6.dp, end = 4.dp), verticalAlignment = Alignment.Top) {
            Column(Modifier.weight(1f)) {
                val m = (packState as? PackState.Ready)?.pack?.manifest?.vehicle
                Text(
                    m?.displayName ?: "Mi coche",
                    style = CvText.display, color = c.textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val (dot, text) = when {
                        open.isNotEmpty() -> c.warning to (if (open.size == 1) open.first().openLabel() else "${open.size} aperturas abiertas")
                        vehicle.allKnownClosed -> c.ok to "Todo cerrado"
                        else -> c.textTertiary to "Estado de puertas no disponible"
                    }
                    Box(Modifier.size(6.dp).clip(CircleShape).background(dot))
                    Text(text, style = CvText.label, color = c.textSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
            RoundIconButton(CvIconsCustom.Garage, "Garaje", { onOpenSheet(Sheet.GARAGE) }, size = 46.dp)
            RoundIconButton(CvIconsCustom.Ambient, "Ambiente", { onOpenSheet(Sheet.AMBIENT) }, size = 46.dp)
            if (has3d) {
                RoundIconButton(
                    if (viewMode == CarViewMode.TOP) CvIcons.View3d else CvIcons.TopView,
                    if (viewMode == CarViewMode.TOP) "Vista 3D" else "Vista en planta",
                    {
                        viewMode = if (viewMode == CarViewMode.TOP) CarViewMode.PERSPECTIVE else CarViewMode.TOP
                        if (viewMode == CarViewMode.PERSPECTIVE) resetToken++
                    },
                    size = 46.dp,
                )
                RoundIconButton(CvIcons.Recenter, "Vista inicial", { viewMode = CarViewMode.PERSPECTIVE; resetToken++ }, size = 46.dp)
            } else {
                Tag("Vista 2D", c.textTertiary)
            }
        }

        // Pie: estado + panel de simulación
        Column(Modifier.align(Alignment.BottomStart).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            AnimatedVisibility(
                visible = demoPanel,
                enter = fadeIn() + slideInVertically { it / 3 },
                exit = fadeOut() + slideOutVertically { it / 3 },
            ) {
                DemoPanel(vehicle, onToggleOpening, onToggleLights, onCloseAll)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatusChip(
                    if (open.isNotEmpty()) CvIcons.DoorOpen else CvIcons.DoorClosed, "Aperturas",
                    when {
                        open.isNotEmpty() -> "${open.size} abierta${if (open.size > 1) "s" else ""}"
                        vehicle.allKnownClosed -> "Cerradas"
                        else -> "Sin datos"
                    },
                    tint = c.warning,
                    highlighted = open.isNotEmpty(),
                )
                val (mText, mState) = maintenanceSummary
                val mColor = when (mState) {
                    com.carvision.core.maintenance.DueState.OVERDUE -> c.danger
                    com.carvision.core.maintenance.DueState.SOON -> c.warning
                    else -> c.accent
                }
                StatusChip(
                    CvIcons.Service, "Mantenimiento", mText, mColor,
                    highlighted = mState == com.carvision.core.maintenance.DueState.OVERDUE || mState == com.carvision.core.maintenance.DueState.SOON,
                    onClick = { onNavigate(Destination.MAINTENANCE) },
                )
                StatusChip(
                    if (isDemo) CvIcons.Demo else CvIcons.NoConnection,
                    "Datos del coche",
                    when (vehicle.connection.kind) {
                        ConnectionKind.DEMO -> "Simulación · tocar"
                        ConnectionKind.CONNECTED -> vehicle.connection.adapterName ?: "Conectado"
                        else -> "Sin conexión CAN"
                    },
                    tint = c.demo,
                    highlighted = isDemo && demoPanel,
                    onClick = { if (isDemo) demoPanel = !demoPanel else onNavigate(Destination.SETTINGS) },
                )
            }
        }
    }
}

@Composable
private fun DemoPanel(vehicle: VehicleSnapshot, onToggle: (Opening) -> Unit, onToggleLights: () -> Unit, onCloseAll: () -> Unit) {
    val c = CvTheme.colors
    Column(
        Modifier
            .clip(CardShape)
            .background(if (c.isDark) c.backgroundTop.copy(alpha = 0.92f) else c.surfaceStrong)
            .border(1.dp, c.surfaceBorder, CardShape)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Tag("Simulación", c.demo)
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            listOf(Opening.FRONT_LEFT_DOOR, Opening.FRONT_RIGHT_DOOR, Opening.REAR_LEFT_DOOR, Opening.REAR_RIGHT_DOOR, Opening.TAILGATE, Opening.HOOD)
                .forEach { o -> Segment(o.shortLabel(), vehicle.openings[o]?.value == true, { onToggle(o) }) }
            Segment("Luces", vehicle.lightsOn.value == true, onToggleLights)
            Segment("Cerrar", false, onCloseAll)
        }
    }
}

// ------------------------------------------------------------------ reloj

@Composable
private fun ClockCard(now: LocalDateTime, vehicle: VehicleSnapshot, modifier: Modifier = Modifier) {
    val c = CvTheme.colors
    GlassCard(modifier, padding = PaddingValues(horizontal = 22.dp, vertical = 16.dp)) {
        Row(verticalAlignment = Alignment.Bottom) {
            Column(Modifier.weight(1f)) {
                Text(now.format(TimeFormat), style = CvText.clock, color = c.textPrimary)
                Spacer(Modifier.height(6.dp))
                Text(
                    now.format(DateFormat).replaceFirstChar { it.titlecase(com.carvision.launcher.ui.UiLocale) },
                    style = CvText.label, color = c.textSecondary,
                )
            }
            val temp = vehicle.outsideTempC
            if (temp.isDisplayable) {
                Column(horizontalAlignment = Alignment.End) {
                    Text(String.format(com.carvision.launcher.ui.UiLocale, "%.1f°", temp.value), style = CvText.display, color = c.textPrimary)
                    SourceTag(temp)
                }
            }
        }
    }
}

// ------------------------------------------------------------------ música

@Composable
private fun MediaCard(media: NowPlayingRepository, modifier: Modifier = Modifier) {
    val c = CvTheme.colors
    val ctx = LocalContext.current
    val notifier = LocalNotifier.current
    val state by media.current.collectAsStateWithLifecycle()
    val np = (state as? MediaState.Active)?.nowPlaying
    val onCardClick: () -> Unit = when (state) {
        MediaState.NoPermission -> { { runCatching { ctx.startActivity(media.permissionIntent()) } } }
        is MediaState.Active -> { { if (!media.openPlayer()) launchOrNotify(ctx, notifier, ExternalApps::music) } }
        MediaState.Idle -> { { launchOrNotify(ctx, notifier, ExternalApps::music) } }
    }
    GlassCard(modifier, padding = PaddingValues(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Box(Modifier.size(90.dp).pressable(onClick = onCardClick)) {
                Box(
                    Modifier
                        .size(84.dp)
                        .clip(SquircleShape)
                        .background(Brush.linearGradient(listOf(c.surfaceStrong, c.surface))),
                    contentAlignment = Alignment.Center,
                ) {
                    val art = np?.art
                    if (art != null) {
                        Image(art.asImageBitmap(), contentDescription = "Carátula", contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                    } else {
                        Icon(CvIcons.Music, null, tint = c.textSecondary, modifier = Modifier.size(32.dp))
                    }
                }
                // Sin el icono de la app que suena en la esquina: el cliente lo pidió quitar (2026-11-13). El nombre
                // de la app sigue en el subtítulo cuando no hay artista.
            }
            Column(Modifier.weight(1f)) {
                Column(Modifier.fillMaxWidth().pressable(onClick = onCardClick)) {
                    val (title, subtitle) = when (val st = state) {
                        MediaState.NoPermission -> "Música" to "Tocar para mostrar lo que suena"
                        MediaState.Idle -> "Sin reproducción" to "Tocar para abrir la música"
                        is MediaState.Active -> st.nowPlaying.title.ifBlank { "Reproduciendo" } to
                            (st.nowPlaying.artist ?: st.nowPlaying.appLabel ?: "")
                    }
                    Text(title, style = CvText.heading, color = c.textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Spacer(Modifier.height(2.dp))
                    Text(subtitle, style = CvText.label, color = c.textTertiary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Spacer(Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    val enabled = np != null
                    RoundIconButton(CvIconsFilled.Previous, "Anterior", { media.previous() }, size = 38.dp, enabled = enabled, iconScale = 0.55f)
                    RoundIconButton(
                        if (np?.isPlaying == true) CvIconsFilled.Pause else CvIconsFilled.Play,
                        if (np?.isPlaying == true) "Pausa" else "Reproducir",
                        { media.playPause() }, size = 44.dp, filled = true, enabled = enabled, iconScale = 0.46f,
                    )
                    RoundIconButton(CvIconsFilled.Next, "Siguiente", { media.next() }, size = 38.dp, enabled = enabled, iconScale = 0.55f)
                }
            }
        }
    }
}

// ------------------------------------------------------------------ conducción

private data class Stat(val icon: ImageVector, val label: String, val signal: VehicleSignal<*>, val format: (Any) -> String)

@Composable
private fun DriveCard(vehicle: VehicleSnapshot, modifier: Modifier = Modifier) {
    val c = CvTheme.colors
    val loc = com.carvision.launcher.ui.UiLocale
    GlassCard(modifier, padding = PaddingValues(horizontal = 20.dp, vertical = 14.dp)) {
        Column(Modifier.fillMaxSize()) {
            // Velocidad: cifra protagonista.
            Row(verticalAlignment = Alignment.Bottom) {
                val sp = vehicle.speedKmh
                Text(
                    if (sp.isDisplayable) String.format(loc, "%.0f", sp.value) else "—",
                    style = CvText.hero, color = if (sp.isDisplayable) c.textPrimary else c.textTertiary,
                )
                Spacer(Modifier.width(8.dp))
                Column(Modifier.padding(bottom = 10.dp)) {
                    Text("km/h", style = CvText.unit, color = c.textSecondary)
                    SourceTag(sp)
                }
                Spacer(Modifier.weight(1f))
                val rpm = vehicle.rpm
                Column(horizontalAlignment = Alignment.End, modifier = Modifier.padding(bottom = 10.dp)) {
                    Text(if (rpm.isDisplayable) "${rpm.value}" else "—", style = CvText.value, color = if (rpm.isDisplayable) c.textPrimary else c.textTertiary)
                    Text("rpm", style = CvText.unit, color = c.textSecondary)
                }
            }
            Spacer(Modifier.height(8.dp))
            Hairline(Modifier.fillMaxWidth())
            Spacer(Modifier.height(4.dp))
            val stats = listOf(
                Stat(CvIcons.Fuel, "Combustible", vehicle.fuelLiters) { String.format(loc, "%.0f L", it as Double) },
                Stat(CvIcons.Temperature, "Exterior", vehicle.outsideTempC) { String.format(loc, "%.1f °C", it as Double) },
                Stat(CvIcons.Battery, "Batería", vehicle.batteryVolts) { String.format(loc, "%.1f V", it as Double) },
                Stat(CvIcons.Odometer, "Kilómetros", vehicle.odometerKm) { String.format(loc, "%,d", it as Long) },
            )
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.SpaceEvenly) {
                stats.chunked(2).forEach { row ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        row.forEach { s -> StatRow(s, Modifier.weight(1f)) }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatRow(stat: Stat, modifier: Modifier = Modifier) {
    val c = CvTheme.colors
    Row(modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Icon(stat.icon, null, tint = c.textTertiary, modifier = Modifier.size(20.dp))
        Column(Modifier.weight(1f)) {
            val v = stat.signal.value
            Text(
                if (stat.signal.isDisplayable && v != null) stat.format(v) else "—",
                style = CvText.value.copy(fontSize = CvText.value.fontSize * 0.86f),
                color = if (stat.signal.isDisplayable) c.textPrimary else c.textTertiary, maxLines = 1,
            )
            Text(stat.label, style = CvText.label, color = c.textTertiary, maxLines = 1)
        }
    }
}

/** Origen del dato: nunca se muestra un valor sin decir de dónde sale. */
@Composable
private fun SourceTag(signal: VehicleSignal<*>) {
    val c = CvTheme.colors
    when {
        signal.status == SignalStatus.SIMULATED -> Tag("Demo", c.demo)
        signal.status == SignalStatus.STALE -> Tag("Antiguo", c.warning)
        signal.source == SignalSource.CAN && signal.status == SignalStatus.VALID -> Tag("Coche", c.ok)
        else -> Unit
    }
}
