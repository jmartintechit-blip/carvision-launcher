package com.carvision.launcher.ui.maintenance

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.carvision.core.designsystem.CardShape
import com.carvision.core.designsystem.CvButton
import com.carvision.core.designsystem.CvIcons
import com.carvision.core.designsystem.CvIconsCustom
import com.carvision.core.designsystem.CvText
import com.carvision.core.designsystem.CvTextField
import com.carvision.core.designsystem.CvTheme
import com.carvision.core.designsystem.GlassCard
import com.carvision.core.designsystem.Hairline
import com.carvision.core.designsystem.Segment
import com.carvision.core.designsystem.SegmentedRow
import com.carvision.core.designsystem.pressable
import com.carvision.core.maintenance.DueState
import com.carvision.core.maintenance.MaintenanceBook
import com.carvision.core.maintenance.MaintenanceCalculator
import com.carvision.core.maintenance.ServiceRecord
import com.carvision.core.maintenance.ServiceSchedule
import com.carvision.core.maintenance.ServiceStatus
import com.carvision.core.maintenance.ServiceType
import com.carvision.launcher.data.AppContainer
import com.carvision.launcher.data.PackState
import com.carvision.launcher.ui.LocalNotifier
import kotlinx.coroutines.launch
import java.time.LocalDate

/** Overlays de edición de la pantalla de mantenimiento. */
private sealed interface Editor {
    data class Record(val record: ServiceRecord?) : Editor
    data class Schedule(val schedule: ServiceSchedule) : Editor
    data object Odometer : Editor
}

@Composable
fun MaintenanceScreen(container: AppContainer) {
    val c = CvTheme.colors
    val ctx = LocalContext.current
    val notifier = LocalNotifier.current
    val scope = rememberCoroutineScope()
    val book by container.maintenance.book.collectAsStateWithLifecycle()
    val vehicle by container.vehicle.collectAsStateWithLifecycle()
    val pack by container.packs.current.collectAsStateWithLifecycle()
    val current = currentOdometer(book, vehicle)
    val statuses = MaintenanceCalculator.compute(book, current.km, LocalDate.now())
    var editor by remember { mutableStateOf<Editor?>(null) }
    val vehicleName = (pack as? PackState.Ready)?.pack?.manifest?.vehicle?.displayName ?: "Mi coche"

    val pdfSaver = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/pdf")) { uri ->
        if (uri != null) scope.launch {
            val ok = MaintenancePdf.writeTo(ctx, uri, vehicleName, book, statuses, current)
            notifier.show(if (ok) "Carnet exportado a PDF" else "No se pudo exportar el PDF")
        }
    }

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().padding(horizontal = 6.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            // Cabecera
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f).padding(start = 8.dp, top = 6.dp)) {
                    Text("Mantenimiento", style = CvText.display, color = c.textPrimary)
                    Text("$vehicleName · ${current.label}", style = CvText.label, color = c.textTertiary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    CvButton("Kilometraje", { editor = Editor.Odometer })
                    CvButton("PDF", {
                        runCatching { pdfSaver.launch("carnet-mantenimiento-${LocalDate.now()}.pdf") }.onFailure {
                            scope.launch {
                                val f = MaintenancePdf.writeToExportFolder(ctx, vehicleName, book, statuses, current)
                                notifier.show(if (f != null) "PDF guardado en Android/data/${container.packageName}/files/export" else "No se pudo exportar el PDF")
                            }
                        }
                    })
                    CvButton("Añadir", { editor = Editor.Record(null) }, primary = true)
                }
            }

            Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                // Próximas intervenciones
                GlassCard(Modifier.weight(1.25f).fillMaxHeight(), padding = PaddingValues(horizontal = 18.dp, vertical = 14.dp)) {
                    Column {
                        SectionTitle("Próximas", "Toca una intervención para ajustar su intervalo")
                        LazyColumn(Modifier.fillMaxSize()) {
                            items(statuses, key = { it.type.name }) { s ->
                                DueRow(s) { editor = Editor.Schedule(book.schedule(s.type)) }
                                Hairline(Modifier.fillMaxWidth())
                            }
                        }
                    }
                }
                // Historial
                GlassCard(Modifier.weight(1f).fillMaxHeight(), padding = PaddingValues(horizontal = 18.dp, vertical = 14.dp)) {
                    Column {
                        SectionTitle("Historial", "${book.records.size} registro${if (book.records.size == 1) "" else "s"}")
                        val records = book.records.sortedWith(compareByDescending<ServiceRecord> { it.dateEpochDay }.thenByDescending { it.odometerKm ?: 0 })
                        if (records.isEmpty()) {
                            Text(
                                "Aún no hay registros. Pulsa «Añadir» para guardar la primera intervención.",
                                style = CvText.body, color = c.textTertiary, modifier = Modifier.padding(top = 18.dp),
                            )
                        }
                        LazyColumn(Modifier.fillMaxSize()) {
                            items(records, key = { it.id }) { r ->
                                HistoryRow(r) { editor = Editor.Record(r) }
                                Hairline(Modifier.fillMaxWidth())
                            }
                        }
                    }
                }
            }
        }

        // Editores
        val e = editor
        AnimatedVisibility(e != null, enter = fadeIn(), exit = fadeOut()) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = if (c.isDark) 0.55f else 0.3f))
                    .pointerInput(Unit) { detectTapGestures { editor = null } }
                    // El editor se mantiene por encima del teclado y su contenido se desplaza al campo enfocado.
                    .imePadding(),
                contentAlignment = Alignment.Center,
            ) {
                Column(
                    Modifier
                        .widthIn(max = 820.dp)
                        .fillMaxWidth(0.94f)
                        .heightIn(max = 560.dp)
                        .clip(CardShape)
                        .background(if (c.isDark) c.backgroundTop.copy(alpha = 0.98f) else c.surfaceStrong.copy(alpha = 0.99f))
                        .border(1.dp, c.surfaceBorder, CardShape)
                        .pointerInput(Unit) { detectTapGestures { } }
                        .padding(22.dp),
                ) {
                    when (e) {
                        is Editor.Record -> RecordEditor(e.record, current.km, onSave = {
                            container.maintenance.upsert(it); editor = null; notifier.show("Registro guardado")
                        }, onDelete = {
                            container.maintenance.delete(it); editor = null; notifier.show("Registro eliminado")
                        }, onCancel = { editor = null }, newId = { container.maintenance.newId() })
                        is Editor.Schedule -> ScheduleEditor(e.schedule, onSave = { container.maintenance.updateSchedule(it); editor = null }, onCancel = { editor = null })
                        Editor.Odometer -> OdometerEditor(book.manualOdometerKm, current.fromCar, onSave = {
                            container.maintenance.setManualOdometer(it); editor = null
                        }, onCancel = { editor = null })
                        null -> Unit
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionTitle(title: String, subtitle: String) {
    val c = CvTheme.colors
    Column(Modifier.padding(bottom = 6.dp)) {
        Text(title, style = CvText.title, color = c.textPrimary)
        Text(subtitle, style = CvText.label, color = c.textTertiary)
    }
}

@Composable
fun stateColor(state: DueState): Color {
    val c = CvTheme.colors
    return when (state) {
        DueState.OVERDUE -> c.danger
        DueState.SOON -> c.warning
        DueState.OK -> c.ok
        DueState.NO_RECORD -> c.textTertiary
    }
}

@Composable
private fun DueRow(s: ServiceStatus, onClick: () -> Unit) {
    val c = CvTheme.colors
    val col = stateColor(s.state)
    Column(Modifier.fillMaxWidth().pressable(onClick = onClick).padding(vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(Modifier.size(8.dp).clip(CircleShape).background(col))
            Column(Modifier.weight(1f)) {
                Text(s.title, style = CvText.heading, color = c.textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(lastLine(s), style = CvText.label, color = c.textTertiary, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Text(dueLine(s), style = CvText.label, color = if (s.state == DueState.NO_RECORD) c.textTertiary else col, maxLines = 2)
        }
        s.progress?.let { p ->
            Box(Modifier.padding(start = 20.dp).fillMaxWidth().height(3.dp).clip(RoundedCornerShape(2.dp)).background(c.surfaceStrong)) {
                Box(Modifier.fillMaxWidth(p.coerceIn(0.02f, 1f)).height(3.dp).clip(RoundedCornerShape(2.dp)).background(col))
            }
        }
    }
}

@Composable
private fun HistoryRow(r: ServiceRecord, onClick: () -> Unit) {
    val c = CvTheme.colors
    Row(
        Modifier.fillMaxWidth().pressable(onClick = onClick).padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(CvIconsCustom.Service, null, tint = c.textTertiary, modifier = Modifier.size(20.dp))
        Column(Modifier.weight(1f)) {
            Text(r.title, style = CvText.heading, color = c.textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
            val parts = listOfNotNull(
                Fmt.date(LocalDate.ofEpochDay(r.dateEpochDay)),
                r.odometerKm?.let { Fmt.km(it) },
                r.workshop.takeIf { it.isNotBlank() },
            )
            Text(parts.joinToString(" · "), style = CvText.label, color = c.textTertiary, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        r.cost?.let { Text(Fmt.money(it), style = CvText.body, color = c.textSecondary) }
    }
}

private fun lastLine(s: ServiceStatus): String {
    val l = s.last ?: return "Sin registros todavía"
    val km = l.odometerKm?.let { " a ${Fmt.km(it)}" } ?: ""
    return "Hecho el ${Fmt.date(LocalDate.ofEpochDay(l.dateEpochDay))}$km"
}

private fun dueLine(s: ServiceStatus): String {
    if (s.state == DueState.NO_RECORD) return "Añade el último\npara calcularlo"
    val parts = mutableListOf<String>()
    s.remainingKm?.let { parts += if (it >= 0) "en ${Fmt.km(it)}" else "pasado ${Fmt.km(-it)}" }
    s.remainingDays?.let { parts += if (it >= 0) "en ${Fmt.duration(it)}" else "hace ${Fmt.duration(-it)}" }
    if (parts.isEmpty()) return "—"
    return (if (s.state == DueState.OVERDUE) "Vencido\n" else "") + parts.joinToString(" · ")
}

// ------------------------------------------------------------------ editores

@Composable
private fun EditorHeader(title: String, subtitle: String) {
    val c = CvTheme.colors
    Text(title, style = CvText.title, color = c.textPrimary)
    Text(subtitle, style = CvText.label, color = c.textTertiary)
    Spacer(Modifier.height(16.dp))
}

@Composable
private fun ColumnScope.RecordEditor(
    initial: ServiceRecord?,
    currentKm: Long?,
    onSave: (ServiceRecord) -> Unit,
    onDelete: (String) -> Unit,
    onCancel: () -> Unit,
    newId: () -> String,
) {
    var type by remember { mutableStateOf(initial?.type ?: ServiceType.OIL_CHANGE) }
    var name by remember { mutableStateOf(initial?.customName.orEmpty()) }
    var date by remember { mutableStateOf(Fmt.date(initial?.let { LocalDate.ofEpochDay(it.dateEpochDay) } ?: LocalDate.now())) }
    var km by remember { mutableStateOf(initial?.odometerKm?.toString() ?: currentKm?.toString().orEmpty()) }
    var cost by remember { mutableStateOf(initial?.cost?.let { Fmt.plainMoney(it) }.orEmpty()) }
    var workshop by remember { mutableStateOf(initial?.workshop.orEmpty()) }
    var description by remember { mutableStateOf(initial?.description.orEmpty()) }
    var notes by remember { mutableStateOf(initial?.notes.orEmpty()) }
    var nextDate by remember { mutableStateOf(initial?.nextDateEpochDay?.let { Fmt.date(LocalDate.ofEpochDay(it)) }.orEmpty()) }
    var nextKm by remember { mutableStateOf(initial?.nextKm?.toString().orEmpty()) }
    var showErrors by remember { mutableStateOf(false) }

    val parsedDate = Fmt.parseDate(date)
    val parsedNextDate = nextDate.takeIf { it.isNotBlank() }?.let { Fmt.parseDate(it) }
    val parsedKm = km.filter { it.isDigit() }.toLongOrNull()
    val parsedNextKm = nextKm.filter { it.isDigit() }.toLongOrNull()
    val parsedCost = cost.replace(",", ".").toDoubleOrNull()
    val dateError = if (parsedDate == null) "Formato dd/mm/aaaa" else null
    val nextDateError = if (nextDate.isNotBlank() && parsedNextDate == null) "Formato dd/mm/aaaa" else null
    val customError = if (type == ServiceType.CUSTOM && name.isBlank()) "Escribe qué se ha hecho" else null
    val valid = dateError == null && nextDateError == null && customError == null

    EditorHeader(if (initial == null) "Nueva intervención" else "Editar intervención", "Fecha y tipo son obligatorios; el resto, opcional")
    Column(Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(Modifier.horizontalScroll(rememberScrollState())) {
            SegmentedRow {
                ServiceType.entries.forEach { t -> Segment(t.label, t == type, { type = t }) }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            CvTextField(date, { date = it }, "Fecha", Modifier.weight(1f), "dd/mm/aaaa", KeyboardType.Number, error = if (showErrors) dateError else null)
            CvTextField(km, { km = it }, "Kilometraje", Modifier.weight(1f), "p. ej. 128450", KeyboardType.Number, suffix = "km")
            CvTextField(cost, { cost = it }, "Coste", Modifier.weight(1f), "Opcional", KeyboardType.Decimal, suffix = "€")
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            CvTextField(name, { name = it }, if (type == ServiceType.CUSTOM) "Qué se ha hecho" else "Nombre (opcional)", Modifier.weight(1f), type.label, error = if (showErrors) customError else null)
            CvTextField(workshop, { workshop = it }, "Taller", Modifier.weight(1f), "Opcional")
        }
        CvTextField(description, { description = it }, "Descripción", Modifier.fillMaxWidth(), "Piezas, marcas, referencias…", singleLine = false)
        CvTextField(notes, { notes = it }, "Observaciones", Modifier.fillMaxWidth(), "Opcional", singleLine = false)
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            CvTextField(nextDate, { nextDate = it }, "Próxima fecha (taller)", Modifier.weight(1f), "Si el taller la indicó", KeyboardType.Number, error = if (showErrors) nextDateError else null)
            CvTextField(nextKm, { nextKm = it }, "Próximo kilometraje (taller)", Modifier.weight(1f), "Si el taller lo indicó", KeyboardType.Number, suffix = "km")
        }
    }
    Spacer(Modifier.height(16.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
        if (initial != null) CvButton("Eliminar", { onDelete(initial.id) }, destructive = true)
        Spacer(Modifier.weight(1f))
        CvButton("Cancelar", onCancel)
        CvButton("Guardar", {
            if (!valid) {
                showErrors = true
            } else {
                onSave(
                    ServiceRecord(
                        id = initial?.id ?: newId(),
                        type = type,
                        customName = name.trim().takeIf { it.isNotEmpty() },
                        dateEpochDay = parsedDate!!.toEpochDay(),
                        odometerKm = parsedKm,
                        description = description.trim(),
                        cost = parsedCost,
                        workshop = workshop.trim(),
                        notes = notes.trim(),
                        nextDateEpochDay = parsedNextDate?.toEpochDay(),
                        nextKm = parsedNextKm,
                    ),
                )
            }
        }, primary = true)
    }
}

@Composable
private fun ColumnScope.ScheduleEditor(initial: ServiceSchedule, onSave: (ServiceSchedule) -> Unit, onCancel: () -> Unit) {
    var enabled by remember { mutableStateOf(initial.enabled) }
    var km by remember { mutableStateOf(initial.intervalKm?.toString().orEmpty()) }
    var months by remember { mutableStateOf(initial.intervalMonths?.toString().orEmpty()) }
    val c = CvTheme.colors
    EditorHeader(initial.type.label, "Intervalo de mantenimiento. Los valores por defecto son orientativos: usa los del manual de tu coche.")
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Text("Seguimiento", style = CvText.heading, color = c.textPrimary, modifier = Modifier.weight(1f))
            SegmentedRow {
                Segment("Activado", enabled, { enabled = true })
                Segment("Desactivado", !enabled, { enabled = false })
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            CvTextField(km, { km = it }, "Cada (kilómetros)", Modifier.weight(1f), "Vacío = no va por km", KeyboardType.Number, suffix = "km")
            CvTextField(months, { months = it }, "Cada (meses)", Modifier.weight(1f), "Vacío = no va por tiempo", KeyboardType.Number, suffix = "meses")
        }
        initial.type.let { t ->
            if (t.defaultKm != null || t.defaultMonths != null) {
                Text(
                    "Valor orientativo de CarVision: " + listOfNotNull(t.defaultKm?.let { Fmt.km(it.toLong()) }, t.defaultMonths?.let { "$it meses" }).joinToString(" o "),
                    style = CvText.label, color = c.textTertiary,
                )
            }
        }
    }
    Spacer(Modifier.height(18.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Spacer(Modifier.weight(1f))
        CvButton("Cancelar", onCancel)
        CvButton("Guardar", {
            onSave(initial.copy(enabled = enabled, intervalKm = km.filter { it.isDigit() }.toIntOrNull(), intervalMonths = months.filter { it.isDigit() }.toIntOrNull()))
        }, primary = true)
    }
}

@Composable
private fun ColumnScope.OdometerEditor(initialKm: Long?, fromCar: Boolean, onSave: (Long) -> Unit, onCancel: () -> Unit) {
    var km by remember { mutableStateOf(initialKm?.toString().orEmpty()) }
    val parsed = km.filter { it.isDigit() }.toLongOrNull()
    EditorHeader("Kilometraje actual", if (fromCar) "El coche ya informa del kilometraje; este valor solo se usa si se pierde la conexión." else "Sin conexión con el coche: introdúcelo a mano para calcular las próximas revisiones.")
    CvTextField(km, { km = it }, "Kilómetros del cuentakilómetros", Modifier.width(360.dp), "p. ej. 128450", KeyboardType.Number, suffix = "km")
    Spacer(Modifier.height(18.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Spacer(Modifier.weight(1f))
        CvButton("Cancelar", onCancel)
        CvButton("Guardar", { parsed?.let(onSave) }, primary = true, enabled = parsed != null)
    }
}

/** Kilometraje con el que se calculan los vencimientos y de dónde sale. */
data class CurrentOdometer(val km: Long?, val fromCar: Boolean, val label: String)

/**
 * Kilometraje actual: del coche si es un dato real y válido; si no, el introducido a mano.
 * Los datos simulados (modo demo) nunca se usan para el mantenimiento.
 */
fun currentOdometer(book: MaintenanceBook, vehicle: com.carvision.core.model.VehicleSnapshot): CurrentOdometer {
    val sig = vehicle.odometerKm
    if (sig.status == com.carvision.core.model.SignalStatus.VALID && sig.value != null) {
        return CurrentOdometer(sig.value, true, "${Fmt.km(sig.value!!)} (del coche)")
    }
    val manual = book.manualOdometerKm
    if (manual != null) {
        val d = book.manualOdometerEpochDay?.let { " el ${Fmt.date(LocalDate.ofEpochDay(it))}" } ?: ""
        return CurrentOdometer(manual, false, "${Fmt.km(manual)} (introducido$d)")
    }
    return CurrentOdometer(null, false, "kilometraje desconocido")
}
