package com.carvision.launcher.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.carvision.core.designsystem.Ambient
import com.carvision.core.designsystem.CardShape
import com.carvision.core.designsystem.CvIcons
import com.carvision.core.designsystem.CvIconsCustom
import com.carvision.core.designsystem.CvText
import com.carvision.core.designsystem.CvTheme
import com.carvision.core.designsystem.RoundIconButton
import com.carvision.core.designsystem.Tag
import com.carvision.core.designsystem.palette
import com.carvision.core.designsystem.pressable
import com.carvision.launcher.data.AppContainer
import com.carvision.launcher.data.PackState
import com.carvision.vehicle.packs.PackImportResult
import kotlinx.coroutines.launch

/** Paneles que se abren sobre toda la interfaz. */
enum class Sheet { GARAGE, AMBIENT, USB }

/** Muestra el panel abierto (si lo hay) con un velo oscuro detrás. Tocar fuera lo cierra. */
@Composable
fun SheetHost(container: AppContainer) {
    val c = CvTheme.colors
    val sheet = container.sheet
    AnimatedVisibility(sheet != null, enter = fadeIn(), exit = fadeOut()) {
        Box(
            Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = if (c.isDark) 0.55f else 0.3f))
                .pointerInput(Unit) { detectTapGestures { container.sheet = null } },
        )
    }
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
        AnimatedVisibility(
            sheet != null,
            enter = fadeIn() + slideInVertically { it / 4 },
            exit = fadeOut() + slideOutVertically { it / 4 },
        ) {
            Column(
                Modifier
                    .padding(bottom = 18.dp)
                    .widthIn(max = 860.dp)
                    .fillMaxWidth(0.82f)
                    .heightIn(max = 470.dp)
                    .clip(CardShape)
                    .background(if (c.isDark) c.backgroundTop.copy(alpha = 0.97f) else c.surfaceStrong.copy(alpha = 0.98f))
                    .border(1.dp, c.surfaceBorder, CardShape)
                    // Consume los toques para que no cierren el panel.
                    .pointerInput(Unit) { detectTapGestures { } }
                    .padding(22.dp),
            ) {
                when (container.sheet) {
                    Sheet.GARAGE -> GarageSheet(container)
                    Sheet.AMBIENT -> AmbientSheet(container)
                    Sheet.USB -> UsbSheet(container)
                    null -> Unit
                }
            }
        }
    }
}

@Composable
private fun SheetHeader(title: String, subtitle: String, onClose: () -> Unit) {
    val c = CvTheme.colors
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, style = CvText.title, color = c.textPrimary)
            Text(subtitle, style = CvText.label, color = c.textTertiary)
        }
        RoundIconButton(CvIcons.Close, "Cerrar", onClose, size = 44.dp)
    }
}

// ------------------------------------------------------------------ garaje

/** Resultado de importar un pack: avisa y lo deja elegido. */
internal fun reportImport(container: AppContainer, notifier: Notifier, scope: kotlinx.coroutines.CoroutineScope, result: PackImportResult) {
    when (result) {
        is PackImportResult.Success -> {
            // La licencia del modelo manda: si no permite redistribución comercial, se dice al importar.
            val personalOnly = !result.manifest.license.commercialRedistribution
            notifier.show("${result.manifest.name} importado" + if (personalOnly) " · licencia del modelo: solo uso personal" else "")
            val id = result.manifest.id
            // Si se reimporta el coche que ya está elegido (p. ej. una versión mejorada), la elección no cambia y
            // no se recargaría: se recarga a mano para que se vea el modelo nuevo.
            val alreadyShown = (container.packs.current.value as? PackState.Ready)?.pack?.manifest?.id == id
            scope.launch {
                container.settings.setSelectedPack(id)
                if (alreadyShown) container.packs.load(id)
            }
        }
        is PackImportResult.Failure -> notifier.show(result.errors.first())
    }
}

// ------------------------------------------------------------------ pendrive

@Composable
private fun ColumnScope.UsbSheet(container: AppContainer) {
    val c = CvTheme.colors
    val scope = rememberCoroutineScope()
    val notifier = LocalNotifier.current
    val volumes by container.usb.volumes.collectAsStateWithLifecycle()
    val volume = volumes.firstOrNull()
    var tree by remember(volume) { mutableStateOf(volume?.let { container.usb.grantedTree(it) }) }
    var found by remember(tree) { mutableStateOf<List<com.carvision.launcher.data.UsbPack>?>(null) }
    var busy by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(tree) { tree?.let { found = container.usb.scan(it) } }
    val access = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { r ->
        r.data?.data?.let { uri -> container.usb.remember(uri); tree = uri }
    }

    SheetHeader("Pendrive", volume?.getDescription(androidx.compose.ui.platform.LocalContext.current) ?: "No hay ningún pendrive conectado") { container.sheet = null }
    Spacer(Modifier.height(16.dp))
    when {
        volume == null -> Text("Conecta el pendrive con el archivo .zip de tu coche.", style = CvText.body, color = c.textSecondary)
        tree == null -> {
            Text(
                "Para buscar tu coche en el pendrive, Android pide permiso una sola vez. En la pantalla que se abre, toca «Permitir» (o «Usar esta carpeta»).",
                style = CvText.body, color = c.textSecondary,
            )
            Spacer(Modifier.height(16.dp))
            ActionButton("Buscar mi coche en el pendrive", primary = true) {
                val i = container.usb.accessIntent(volume)
                if (i == null) notifier.show("Esta radio no deja abrir el pendrive: usa «Importar .zip» en el Garaje")
                else runCatching { access.launch(i) }.onFailure { notifier.show("Esta radio no deja abrir el pendrive: usa «Importar .zip» en el Garaje") }
            }
        }
        found == null -> Text("Buscando coches en el pendrive…", style = CvText.body, color = c.textSecondary)
        found!!.isEmpty() -> Text(
            "No hay ningún coche (.zip de CarVision) en el pendrive. Cópialo a la raíz del pendrive o a una carpeta, sin descomprimir.",
            style = CvText.body, color = c.textSecondary,
        )
        else -> LazyVerticalGrid(GridCells.Adaptive(260.dp), Modifier.weight(1f, fill = false), verticalArrangement = Arrangement.spacedBy(10.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            items(found!!, key = { it.uri.toString() }) { p ->
                Row(
                    Modifier.clip(RoundedCornerShape(18.dp)).background(c.surface).border(1.dp, c.surfaceBorder, RoundedCornerShape(18.dp)).padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(p.title, style = CvText.heading, color = c.textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(p.fileName, style = CvText.label, color = c.textTertiary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    ActionButton(if (busy == p.fileName) "Instalando…" else "Instalar", primary = true) {
                        if (busy != null) return@ActionButton
                        busy = p.fileName
                        scope.launch {
                            // appScope: el panel se cierra enseguida y su propio ámbito se cancelaría antes de elegir el coche.
                            reportImport(container, notifier, container.appScope, container.packs.importFromUri(p.uri))
                            busy = null
                            container.sheet = null
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ColumnScope.GarageSheet(container: AppContainer) {
    val c = CvTheme.colors
    val scope = rememberCoroutineScope()
    val notifier = LocalNotifier.current
    val packs by container.packs.available.collectAsStateWithLifecycle()
    val current by container.packs.current.collectAsStateWithLifecycle()
    val currentId = (current as? PackState.Ready)?.pack?.manifest?.id
    LaunchedEffect(Unit) { container.packs.refresh() }

    fun report(result: PackImportResult) = reportImport(container, notifier, scope, result)

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) scope.launch { report(container.packs.importFromUri(uri)) }
    }
    // Logotipo: se recuerda para qué coche se está eligiendo.
    var logoFor by remember { mutableStateOf<String?>(null) }
    val logoPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        val id = logoFor
        if (uri != null && id != null) scope.launch {
            if (container.brandLogos.setFromUri(id, uri)) notifier.show("Logotipo actualizado") else notifier.show("No se pudo leer la imagen")
        }
    }
    val logoVersion by container.brandLogos.changes.collectAsStateWithLifecycle()

    SheetHeader("Garaje", "Elige el coche que se muestra o importa uno nuevo (.zip)") { container.sheet = null }
    Spacer(Modifier.height(16.dp))
    LazyVerticalGrid(GridCells.Adaptive(220.dp), Modifier.weight(1f, fill = false), verticalArrangement = Arrangement.spacedBy(10.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        items(packs, key = { it.id }) { p ->
            val selected = p.id == currentId
            Row(
                Modifier
                    .clip(RoundedCornerShape(18.dp))
                    .background(if (selected) c.selection else c.surface)
                    .border(1.dp, if (selected) c.accent.copy(alpha = 0.6f) else c.surfaceBorder, RoundedCornerShape(18.dp))
                    .pressable { scope.launch { container.settings.setSelectedPack(p.id) } }
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                // Logotipo del coche (o la marca en texto si no tiene).
                val logo by produceState<android.graphics.Bitmap?>(null, p.id, logoVersion) { value = container.packs.logoFor(p, container.brandLogos) }
                Box(Modifier.width(64.dp).height(36.dp), contentAlignment = Alignment.Center) {
                    val l = logo
                    if (l != null) CrispImage(l, 56.dp, contentDescription = p.make)
                    else BrandWordmark(p.make, c.textSecondary)
                }
                Column(Modifier.weight(1f)) {
                    Text(listOf(p.make, p.model).filter { it.isNotBlank() }.joinToString(" "), style = CvText.heading, color = c.textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(p.name, style = CvText.label, color = c.textTertiary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Spacer(Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Tag(if (p.imported) "Importado" else "Incluido", if (p.imported) c.accent else c.textTertiary)
                        Text(
                            "Logotipo", style = CvText.label, color = c.textSecondary,
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(c.surfaceStrong)
                                .pressable {
                                    logoFor = p.id
                                    runCatching { logoPicker.launch(arrayOf("image/*")) }
                                        .onFailure { notifier.show("Esta radio no tiene selector de imágenes") }
                                }
                                .padding(horizontal = 10.dp, vertical = 4.dp),
                        )
                        // Si el usuario eligió un logotipo para este coche, se puede quitar: vuelve el del pack
                        // (o la marca en texto si el pack no trae ninguno).
                        val custom = remember(p.id, logoVersion) { container.brandLogos.overrideFile(p.id).isFile }
                        if (custom) {
                            Text(
                                "Quitar", style = CvText.label, color = c.textSecondary,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(c.surfaceStrong)
                                    .pressable {
                                        scope.launch {
                                            container.brandLogos.clear(p.id)
                                            notifier.show("Logotipo quitado")
                                        }
                                    }
                                    .padding(horizontal = 10.dp, vertical = 4.dp),
                            )
                        }
                    }
                }
                if (p.imported && !selected) {
                    RoundIconButton(CvIcons.Close, "Eliminar", { scope.launch { container.packs.deleteImported(p.id) } }, size = 36.dp)
                }
                if (selected) Icon(CvIcons.Check, null, tint = c.accent, modifier = Modifier.size(22.dp))
            }
        }
    }
    if (currentId != null) {
        Spacer(Modifier.height(14.dp))
        PaintPicker(container, currentId)
    }
    Spacer(Modifier.height(16.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
        ActionButton("Importar .zip", primary = true) {
            runCatching { picker.launch(arrayOf("application/zip", "application/x-zip-compressed", "application/octet-stream")) }
                .onFailure { notifier.show("Esta radio no tiene selector de archivos: usa la carpeta de importación") }
        }
        ActionButton("Buscar en la carpeta") {
            scope.launch {
                val results = container.packs.importFromFolder()
                if (results.isEmpty()) notifier.show("No hay ningún .zip en la carpeta de importación")
                results.forEach { (_, r) -> report(r) }
            }
        }
        Text(
            "Carpeta: Android/data/${container.packageName}/files/import",
            style = CvText.label, color = c.textTertiary, modifier = Modifier.weight(1f), maxLines = 2,
        )
    }
}

/** Colores de carrocería habituales (sRGB). null = el color original del modelo. */
internal val PAINT_COLORS: List<Pair<String, Int?>> = listOf(
    "Original" to null,
    "Blanco" to 0xF2F2F0, "Gris plata" to 0xB8BCC2, "Gris oscuro" to 0x4A4E54, "Negro" to 0x0E0F11,
    "Rojo" to 0xB0141C, "Azul" to 0x1E4FA8, "Azul oscuro" to 0x14213D, "Verde" to 0x2F5D3A,
    "Amarillo" to 0xE8B923, "Naranja" to 0xD8601E, "Marrón" to 0x5A3B2A, "Beige" to 0xCBB894,
)

/** Color del coche elegido: se aplica al instante en el 3D (no hace falta otro pack). */
@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
internal fun PaintPicker(container: AppContainer, packId: String) {
    val c = CvTheme.colors
    val scope = rememberCoroutineScope()
    val chosen by remember(packId) { container.settings.paintColor(packId) }.collectAsStateWithLifecycle(null)
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("Color", style = CvText.heading, color = c.textPrimary, modifier = Modifier.width(64.dp))
      androidx.compose.foundation.layout.FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        PAINT_COLORS.forEach { (_, rgb) ->
            val sel = chosen == rgb
            Box(
                Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(
                        if (rgb == null) Brush.sweepGradient(listOf(Color(0xFFB0141C), Color(0xFF1E4FA8), Color(0xFFE8B923), Color(0xFFB0141C)))
                        else androidx.compose.ui.graphics.SolidColor(Color(0xFF000000 or rgb.toLong())),
                    )
                    .border(if (sel) 3.dp else 1.dp, if (sel) c.accent else c.surfaceBorder, CircleShape)
                    .pressable { scope.launch { container.settings.setPaintColor(packId, rgb) } },
            )
        }
      }
    }
    val name = PAINT_COLORS.firstOrNull { it.second == chosen }?.first ?: "Personalizado"
    Text("Color de la carrocería: $name", style = CvText.label, color = c.textTertiary, modifier = Modifier.padding(start = 74.dp, top = 4.dp))
}

@Composable
private fun ActionButton(text: String, primary: Boolean = false, onClick: () -> Unit) {
    val c = CvTheme.colors
    Text(
        text,
        style = CvText.heading,
        color = if (primary) c.onPrimaryButton else c.textPrimary,
        modifier = Modifier
            .clip(RoundedCornerShape(22.dp))
            .background(if (primary) c.primaryButton else c.surfaceStrong)
            .pressable(onClick = onClick)
            .padding(horizontal = 22.dp, vertical = 12.dp),
    )
}

// ------------------------------------------------------------------ ambiente

@Composable
private fun ColumnScope.AmbientSheet(container: AppContainer) {
    val c = CvTheme.colors
    val scope = rememberCoroutineScope()
    val selected by container.settings.ambient.collectAsStateWithLifecycle(Ambient.GRAPHITE)
    SheetHeader("Ambiente", "Fondo de la pantalla. Se adapta al tema claro y oscuro") { container.sheet = null }
    Spacer(Modifier.height(16.dp))
    LazyVerticalGrid(GridCells.Fixed(3), verticalArrangement = Arrangement.spacedBy(12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), contentPadding = PaddingValues(bottom = 4.dp)) {
        items(Ambient.entries.toList(), key = { it.name }) { a ->
            val p = a.palette(c.isDark)
            val isSel = a == selected
            Column(
                Modifier.pressable { scope.launch { container.settings.setAmbient(a) } },
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .aspectRatio(16f / 9f)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Brush.verticalGradient(listOf(p.top, p.bottom)))
                        .drawBehind {
                            fun glow(col: Color, at: Offset, rel: Float) {
                                val center = Offset(size.width * at.x, size.height * at.y)
                                val r = size.width * rel
                                drawCircle(Brush.radialGradient(listOf(col.copy(alpha = (col.alpha * 1.6f).coerceAtMost(1f)), Color.Transparent), center = center, radius = r), radius = r, center = center)
                            }
                            glow(p.glow1, p.glow1At, p.glow1Radius)
                            glow(p.glow2, p.glow2At, p.glow2Radius)
                        }
                        .border(if (isSel) 2.dp else 1.dp, if (isSel) p.accent else c.surfaceBorder, RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.BottomEnd,
                ) {
                    Box(Modifier.padding(10.dp).size(10.dp).clip(CircleShape).background(p.accent))
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(a.label, style = CvText.heading, color = if (isSel) c.textPrimary else c.textSecondary)
                    if (isSel) Icon(CvIcons.Check, null, tint = p.accent, modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}
