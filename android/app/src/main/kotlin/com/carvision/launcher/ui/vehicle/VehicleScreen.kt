package com.carvision.launcher.ui.vehicle

import androidx.compose.foundation.background
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import com.carvision.core.designsystem.CardShape
import com.carvision.vehicle.data.TripRecord
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.carvision.core.designsystem.CvButton
import com.carvision.core.designsystem.CvIcons
import com.carvision.core.designsystem.CvText
import com.carvision.core.designsystem.CvTheme
import com.carvision.core.designsystem.GlassCard
import com.carvision.core.designsystem.Hairline
import com.carvision.core.designsystem.Tag
import com.carvision.core.model.ConnectionKind
import com.carvision.core.model.Opening
import com.carvision.core.model.SignalSource
import com.carvision.core.model.SignalStatus
import com.carvision.core.model.VehicleSignal
import com.carvision.launcher.data.AppContainer
import com.carvision.launcher.data.PackState
import com.carvision.launcher.ui.label
import com.carvision.launcher.ui.compactLabel
import com.carvision.launcher.ui.maintenance.Fmt
import com.carvision.launcher.ui.maintenance.currentOdometer
import com.carvision.render3d.CarSchematic
import com.carvision.vehicle.data.TripStats
import kotlinx.coroutines.delay
import java.util.Locale

/**
 * Ordenador de a bordo: viaje actual (calculado), datos del vehículo con su origen y antigüedad, y aperturas.
 * Regla: ningún valor se muestra sin decir de dónde sale; lo que el coche no da se indica como no disponible.
 */
@Composable
fun VehicleScreen(container: AppContainer) {
    val c = CvTheme.colors
    val vehicle by container.vehicle.collectAsStateWithLifecycle()
    val trip by container.trip.stats.collectAsStateWithLifecycle()
    val pack by container.packs.current.collectAsStateWithLifecycle()
    val book by container.maintenance.book.collectAsStateWithLifecycle()
    val history by container.trips.history.collectAsStateWithLifecycle()
    var showHistory by remember { mutableStateOf(false) }
    // Reloj para la antigüedad de los datos ("hace 3 s").
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) { while (true) { now = System.currentTimeMillis(); delay(1000) } }

    val name = (pack as? PackState.Ready)?.pack?.manifest?.vehicle?.displayName ?: "Mi coche"
    val source = when (vehicle.connection.kind) {
        ConnectionKind.DEMO -> "Datos simulados (modo demostración)"
        ConnectionKind.CONNECTED -> "Datos del coche · ${vehicle.connection.adapterName ?: "CAN"}"
        ConnectionKind.CONNECTING -> "Conectando con el coche…"
        ConnectionKind.ERROR -> "Error de conexión: ${vehicle.connection.message ?: "sin detalles"}"
        ConnectionKind.NO_ADAPTER -> "Sin conexión CAN · los datos del coche no están disponibles"
    }

    Box(Modifier.fillMaxSize()) {
    Column(Modifier.fillMaxSize().padding(horizontal = 6.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Column(Modifier.padding(start = 8.dp, top = 6.dp)) {
            Text("Vehículo", style = CvText.display, color = c.textPrimary)
            Text("$name · $source", style = CvText.label, color = c.textTertiary, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            TripCard(trip, onReset = { container.trips.newTrip() }, onHistory = { showHistory = true }, modifier = Modifier.weight(1.1f).fillMaxHeight())
            DataCard(vehicle, book, now, Modifier.weight(1.2f).fillMaxHeight())
            OpeningsCard(vehicle, Modifier.weight(0.8f).fillMaxHeight())
        }
    }
    AnimatedVisibility(showHistory, enter = fadeIn(), exit = fadeOut()) {
        TripHistoryOverlay(history.trips, onClear = { container.trips.clear() }, onClose = { showHistory = false })
    }
    }
}

// ------------------------------------------------------------------ historial de viajes

@Composable
private fun TripHistoryOverlay(trips: List<TripRecord>, onClear: () -> Unit, onClose: () -> Unit) {
    val c = CvTheme.colors
    val loc = com.carvision.launcher.ui.UiLocale
    val dayFmt = remember { DateTimeFormatter.ofPattern("EEE d MMM · HH:mm", Locale("es", "ES")) }
    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = if (c.isDark) 0.55f else 0.3f))
            .pointerInput(Unit) { detectTapGestures { onClose() } },
        contentAlignment = Alignment.Center,
    ) {
        Column(
            Modifier
                .fillMaxWidth(0.94f)
                .fillMaxHeight(0.92f)
                .clip(CardShape)
                .background(c.backgroundTop)
                .background(c.surface)
                .border(1.dp, c.surfaceBorder, CardShape)
                .pointerInput(Unit) { detectTapGestures { } }
                .padding(22.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Historial de viajes", style = CvText.title, color = c.textPrimary)
                    val total = trips.filter { !it.simulated }.sumOf { it.distanceKm }
                    Text(
                        "${trips.size} viaje${if (trips.size == 1) "" else "s"} · ${String.format(loc, "%.1f", total)} km reales",
                        style = CvText.label, color = c.textTertiary,
                    )
                }
                if (trips.isNotEmpty()) CvButton("Borrar historial", onClear, destructive = true)
                Spacer(Modifier.width(10.dp))
                CvButton("Cerrar", onClose)
            }
            Spacer(Modifier.height(12.dp))
            if (trips.isEmpty()) {
                Text(
                    "Todavía no hay viajes. Cada trayecto se guarda al empezar uno nuevo o al volver a arrancar la radio.",
                    style = CvText.body, color = c.textTertiary,
                )
            }
            Row(Modifier.padding(vertical = 6.dp)) {
                listOf("Inicio" to 1.6f, "Distancia" to 1f, "Duración" to 1f, "Media" to 0.9f, "Máxima" to 0.9f).forEach { (t, w) ->
                    Text(t.uppercase(), style = CvText.caption, color = c.textTertiary, modifier = Modifier.weight(w))
                }
            }
            Hairline(Modifier.fillMaxWidth())
            LazyColumn(Modifier.fillMaxWidth().weight(1f)) {
                items(trips, key = { it.startedAtMs }) { t ->
                    Row(Modifier.fillMaxWidth().padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Row(Modifier.weight(1.6f), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            val start = Instant.ofEpochMilli(t.startedAtMs).atZone(ZoneId.systemDefault())
                            Text(dayFmt.format(start).replaceFirstChar { it.titlecase(loc) }, style = CvText.body, color = c.textPrimary)
                            if (t.simulated) Tag("Demo", c.demo)
                        }
                        Text(String.format(loc, "%.1f km", t.distanceKm), style = CvText.body, color = c.textPrimary, modifier = Modifier.weight(1f))
                        Text(Fmt2.hms(t.elapsedSeconds), style = CvText.body, color = c.textSecondary, modifier = Modifier.weight(1f))
                        Text(t.avgSpeedKmh?.let { String.format(loc, "%.0f km/h", it) } ?: "—", style = CvText.body, color = c.textSecondary, modifier = Modifier.weight(0.9f))
                        Text(t.maxSpeedKmh?.let { String.format(loc, "%.0f km/h", it) } ?: "—", style = CvText.body, color = c.textSecondary, modifier = Modifier.weight(0.9f))
                    }
                    Hairline(Modifier.fillMaxWidth())
                }
            }
        }
    }
}

// ------------------------------------------------------------------ viaje

@Composable
private fun TripCard(trip: TripStats, onReset: () -> Unit, onHistory: () -> Unit, modifier: Modifier) {
    val c = CvTheme.colors
    val loc = com.carvision.launcher.ui.UiLocale
    GlassCard(modifier, padding = PaddingValues(20.dp)) {
        Column(Modifier.fillMaxSize()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Viaje actual", style = CvText.title, color = c.textPrimary)
                    Text(
                        when {
                            !trip.hasData -> "Necesita la velocidad del coche"
                            trip.fromOdometer -> "Distancia del cuentakilómetros"
                            else -> "Calculado a partir de la velocidad"
                        },
                        style = CvText.label, color = c.textTertiary,
                    )
                }
                if (trip.simulated) Tag("Demo", c.demo)
            }
            Spacer(Modifier.height(14.dp))
            // Distancia: cifra protagonista.
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    if (trip.hasData) String.format(loc, "%.1f", trip.distanceKm) else "—",
                    style = CvText.hero, color = if (trip.hasData) c.textPrimary else c.textTertiary,
                )
                Spacer(Modifier.width(8.dp))
                Text("km", style = CvText.unit, color = c.textSecondary, modifier = Modifier.padding(bottom = 12.dp))
            }
            Spacer(Modifier.height(10.dp))
            Hairline(Modifier.fillMaxWidth())
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.SpaceEvenly) {
                TripRow(CvIcons.Rpm, "Duración", if (trip.hasData) Fmt2.hms(trip.elapsedSeconds) else "—", "${Fmt2.short(trip.movingSeconds)} en marcha".takeIf { trip.hasData })
                TripRow(CvIcons.Speed, "Velocidad media", trip.avgSpeedKmh?.let { String.format(loc, "%.0f km/h", it) } ?: "—", "en marcha")
                TripRow(CvIcons.Speed, "Velocidad máxima", trip.maxSpeedKmh?.let { String.format(loc, "%.0f km/h", it) } ?: "—", null)
                TripRow(CvIcons.Fuel, "Consumo medio", "No disponible", "sin dato del coche")
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                CvButton("Nuevo viaje", onReset, Modifier.weight(1.15f), horizontalPadding = 10.dp)
                CvButton("Historial", onHistory, Modifier.weight(1f), horizontalPadding = 10.dp)
            }
        }
    }
}

@Composable
private fun TripRow(icon: ImageVector, label: String, value: String, note: String?) {
    val c = CvTheme.colors
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Icon(icon, null, tint = c.textTertiary, modifier = Modifier.size(20.dp))
        Column(Modifier.weight(1f)) {
            Text(label, style = CvText.label, color = c.textSecondary)
            if (note != null) Text(note, style = CvText.label, color = c.textTertiary, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        val unavailable = value == "—" || value == "No disponible"
        Text(value, style = if (value == "No disponible") CvText.body else CvText.value, color = if (unavailable) c.textTertiary else c.textPrimary, maxLines = 1)
    }
}

// ------------------------------------------------------------------ datos

private data class DataItem(val icon: ImageVector, val label: String, val signal: VehicleSignal<*>?, val value: String?, val fallback: String? = null)

@Composable
private fun DataCard(vehicle: com.carvision.core.model.VehicleSnapshot, book: com.carvision.core.maintenance.MaintenanceBook, now: Long, modifier: Modifier) {
    val c = CvTheme.colors
    val loc = com.carvision.launcher.ui.UiLocale
    fun <T> v(s: VehicleSignal<T>, f: (T) -> String): String? = s.value?.takeIf { s.isDisplayable }?.let(f)
    val odo = currentOdometer(book, vehicle)
    val items = listOf(
        DataItem(CvIcons.Speed, "Velocidad", vehicle.speedKmh, v(vehicle.speedKmh) { String.format(loc, "%.0f km/h", it) }),
        DataItem(CvIcons.Rpm, "Revoluciones", vehicle.rpm, v(vehicle.rpm) { "${Fmt.group(it.toLong())} rpm" }),
        DataItem(CvIcons.Fuel, "Combustible", vehicle.fuelLiters, v(vehicle.fuelLiters) { String.format(loc, "%.0f L", it) }),
        DataItem(CvIcons.Temperature, "Temperatura exterior", vehicle.outsideTempC, v(vehicle.outsideTempC) { String.format(loc, "%.1f °C", it) }),
        DataItem(CvIcons.Battery, "Batería", vehicle.batteryVolts, v(vehicle.batteryVolts) { String.format(loc, "%.1f V", it) }),
        DataItem(
            CvIcons.Odometer, "Kilometraje", vehicle.odometerKm, v(vehicle.odometerKm) { Fmt.km(it) },
            // Sin dato del coche: el introducido a mano en Mantenimiento, indicado como tal.
            fallback = odo.km?.takeIf { !odo.fromCar }?.let { "${Fmt.km(it)} · introducido" },
        ),
        DataItem(CvIcons.Lights, "Luces", vehicle.lightsOn, v(vehicle.lightsOn) { if (it) "Encendidas" else "Apagadas" }),
    )
    GlassCard(modifier, padding = PaddingValues(horizontal = 20.dp, vertical = 16.dp)) {
        Column(Modifier.fillMaxSize()) {
            Text("Datos del coche", style = CvText.title, color = c.textPrimary)
            Text("Cada valor indica su origen", style = CvText.label, color = c.textTertiary)
            Spacer(Modifier.height(6.dp))
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                items.forEach { item ->
                    DataRow(item, now)
                    Hairline(Modifier.fillMaxWidth())
                }
            }
        }
    }
}

@Composable
private fun DataRow(item: DataItem, now: Long) {
    val c = CvTheme.colors
    val s = item.signal
    Row(Modifier.fillMaxWidth().padding(vertical = 11.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Icon(item.icon, null, tint = c.textTertiary, modifier = Modifier.size(20.dp))
        Column(Modifier.weight(1f)) {
            Text(item.label, style = CvText.body, color = c.textPrimary)
            val origin = when {
                s == null -> null
                s.status == SignalStatus.SIMULATED -> "Simulado"
                s.status == SignalStatus.STALE -> "Dato antiguo · ${Fmt2.age(now - s.timestampMs)}"
                s.status == SignalStatus.VALID && s.source == SignalSource.CAN -> "Del coche · ${Fmt2.age(now - s.timestampMs)}"
                item.fallback != null -> "Introducido a mano en Mantenimiento"
                else -> "No disponible sin conexión con el coche"
            }
            if (origin != null) Text(origin, style = CvText.label, color = c.textTertiary, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        val shown = item.value ?: item.fallback
        Text(shown ?: "—", style = CvText.value, color = if (shown != null) c.textPrimary else c.textTertiary, maxLines = 1)
        if (s?.status == SignalStatus.SIMULATED) Tag("Demo", c.demo)
    }
}

// ------------------------------------------------------------------ aperturas

@Composable
private fun OpeningsCard(vehicle: com.carvision.core.model.VehicleSnapshot, modifier: Modifier) {
    val c = CvTheme.colors
    GlassCard(modifier, padding = PaddingValues(horizontal = 20.dp, vertical = 16.dp)) {
        Column(Modifier.fillMaxSize()) {
            Text("Aperturas", style = CvText.title, color = c.textPrimary)
            CarSchematic(
                openings = vehicle.openings.mapKeys { it.key.signalKey }.mapValues { it.value.isDisplayable && it.value.value == true },
                bodyColor = c.surfaceStrong, lineColor = c.textSecondary, openColor = c.warning,
                modifier = Modifier.fillMaxWidth().weight(1f).padding(vertical = 8.dp),
            )
            Opening.entries.forEach { o ->
                val sig = vehicle.openings[o]
                val masc = o == Opening.TAILGATE || o == Opening.HOOD   // el maletero, el capó
                val (text, col) = when {
                    sig == null || !sig.isDisplayable -> "Sin datos" to c.textTertiary
                    sig.value == true -> (if (masc) "Abierto" else "Abierta") to c.warning
                    else -> (if (masc) "Cerrado" else "Cerrada") to c.ok
                }
                Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Box(Modifier.size(7.dp).clip(CircleShape).background(col))
                    Text(o.compactLabel(), style = CvText.label, color = c.textSecondary, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(text, style = CvText.label, color = col)
                }
            }
        }
    }
}

/** Formatos de tiempo del ordenador de a bordo. */
private object Fmt2 {
    /** Forma corta para notas: "58 s", "12 min", "1 h 05 min". */
    fun short(seconds: Long): String {
        val h = seconds / 3600
        val m = (seconds % 3600) / 60
        return when {
            h > 0 -> String.format(Locale.ROOT, "%d h %02d min", h, m)
            m > 0 -> "$m min"
            else -> "$seconds s"
        }
    }

    fun hms(seconds: Long): String {
        val h = seconds / 3600
        val m = (seconds % 3600) / 60
        val s = seconds % 60
        return if (h > 0) String.format(Locale.ROOT, "%d h %02d min", h, m) else String.format(Locale.ROOT, "%d min %02d s", m, s)
    }

    fun age(ms: Long): String = when {
        ms < 2_000 -> "ahora"
        ms < 60_000 -> "hace ${ms / 1000} s"
        else -> "hace ${ms / 60_000} min"
    }
}
