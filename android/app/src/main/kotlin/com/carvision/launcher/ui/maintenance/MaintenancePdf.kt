package com.carvision.launcher.ui.maintenance

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Build
import android.text.TextUtils
import androidx.core.content.res.ResourcesCompat
import com.carvision.core.maintenance.DueState
import com.carvision.core.maintenance.MaintenanceBook
import com.carvision.core.maintenance.ServiceRecord
import com.carvision.core.maintenance.ServiceStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.OutputStream
import java.time.LocalDate

/**
 * Exporta el carnet de mantenimiento a PDF (A4) con el motor PDF de Android, sin librerías externas.
 * Contenido: vehículo, kilometraje y su origen, próximas intervenciones con su estado e historial completo.
 */
object MaintenancePdf {
    private const val W = 595 // A4 en puntos
    private const val H = 842
    private const val M = 42 // margen

    suspend fun writeTo(ctx: Context, uri: Uri, vehicle: String, book: MaintenanceBook, statuses: List<ServiceStatus>, odo: CurrentOdometer): Boolean =
        withContext(Dispatchers.IO) {
            runCatching {
                ctx.contentResolver.openOutputStream(uri)?.use { render(ctx, it, vehicle, book, statuses, odo) } ?: error("sin salida")
            }.isSuccess
        }

    /** Para radios sin selector de archivos: Android/data/<app>/files/export/. */
    suspend fun writeToExportFolder(ctx: Context, vehicle: String, book: MaintenanceBook, statuses: List<ServiceStatus>, odo: CurrentOdometer): File? =
        withContext(Dispatchers.IO) {
            val dir = ctx.getExternalFilesDir("export") ?: return@withContext null
            val f = File(dir, "carnet-mantenimiento-${LocalDate.now()}.pdf")
            runCatching { f.outputStream().use { render(ctx, it, vehicle, book, statuses, odo) }; f }.getOrNull()
        }

    private class Painter(ctx: Context) {
        private val base: Typeface = runCatching {
            ResourcesCompat.getFont(ctx, com.carvision.core.designsystem.R.font.manrope_variable)
        }.getOrNull() ?: Typeface.SANS_SERIF
        /** Peso de la fuente variable mediante el eje "wght" (Android 8+); funciona también en API 26–27. */
        fun paint(size: Float, w: Int = 400, color: Int = Color.rgb(20, 22, 26)) = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = size
            typeface = base
            if (!setFontVariationSettings("'wght' $w")) {
                typeface = if (Build.VERSION.SDK_INT >= 28) Typeface.create(base, w, false) else Typeface.create(base, if (w >= 600) Typeface.BOLD else Typeface.NORMAL)
            }
            this.color = color
        }

        val title = paint(22f, 700)
        val h2 = paint(13f, 700)
        val body = paint(9.5f)
        val bodyBold = paint(9.5f, 600)
        val muted = paint(8.5f, 400, Color.rgb(110, 114, 122))
        val line = Paint().apply { color = Color.rgb(222, 225, 230); strokeWidth = 0.7f }
        val band = Paint().apply { color = Color.rgb(12, 14, 18) }
        val white = paint(9f, 500, Color.WHITE)
    }

    private fun ellipsize(text: String, p: Paint, width: Float): String =
        TextUtils.ellipsize(text, android.text.TextPaint(p), width, TextUtils.TruncateAt.END).toString()

    private fun render(ctx: Context, out: OutputStream, vehicle: String, book: MaintenanceBook, statuses: List<ServiceStatus>, odo: CurrentOdometer) {
        val doc = PdfDocument()
        val p = Painter(ctx)
        var pageNo = 0
        lateinit var page: PdfDocument.Page
        lateinit var c: Canvas
        var y = 0f

        fun footer() {
            c.drawText("Generado con CarVision el ${Fmt.date(LocalDate.now())}", M.toFloat(), H - 24f, p.muted)
            val t = "Página $pageNo"
            c.drawText(t, W - M - p.muted.measureText(t), H - 24f, p.muted)
        }

        fun newPage() {
            if (pageNo > 0) { footer(); doc.finishPage(page) }
            pageNo++
            page = doc.startPage(PdfDocument.PageInfo.Builder(W, H, pageNo).create())
            c = page.canvas
            y = M.toFloat()
        }

        fun ensure(space: Float) { if (y + space > H - 50) newPage() }

        // ---- portada / cabecera
        newPage()
        c.drawRect(0f, 0f, W.toFloat(), 96f, p.band)
        c.drawText("Carnet de mantenimiento", M.toFloat(), 50f, p.paint(22f, 700, Color.WHITE))
        c.drawText(vehicle, M.toFloat(), 72f, p.white)
        y = 124f
        c.drawText("Kilometraje: ${odo.label}", M.toFloat(), y, p.body); y += 14f
        c.drawText("Intervalos según la configuración del usuario. Consulta siempre el manual del fabricante.", M.toFloat(), y, p.muted)
        y += 26f

        // ---- próximas
        c.drawText("Próximas intervenciones", M.toFloat(), y, p.h2); y += 16f
        val cols1 = floatArrayOf(M.toFloat(), 230f, 360f, 480f)
        c.drawText("Intervención", cols1[0], y, p.muted); c.drawText("Último", cols1[1], y, p.muted)
        c.drawText("Próximo", cols1[2], y, p.muted); c.drawText("Estado", cols1[3], y, p.muted)
        y += 6f; c.drawLine(M.toFloat(), y, (W - M).toFloat(), y, p.line); y += 13f
        statuses.forEach { s ->
            ensure(18f)
            c.drawText(ellipsize(s.title, p.bodyBold, 180f), cols1[0], y, p.bodyBold)
            val last = s.last?.let { r -> listOfNotNull(Fmt.date(LocalDate.ofEpochDay(r.dateEpochDay)), r.odometerKm?.let { Fmt.km(it) }).joinToString(" · ") } ?: "—"
            c.drawText(ellipsize(last, p.body, 125f), cols1[1], y, p.body)
            val next = listOfNotNull(s.dueDate?.let { Fmt.date(it) }, s.dueKm?.let { Fmt.km(it) }).joinToString(" · ").ifEmpty { "—" }
            c.drawText(ellipsize(next, p.body, 115f), cols1[2], y, p.body)
            val (label, col) = when (s.state) {
                DueState.OVERDUE -> "Vencido" to Color.rgb(200, 50, 50)
                DueState.SOON -> "Próximo" to Color.rgb(200, 128, 28)
                DueState.OK -> "Al día" to Color.rgb(30, 150, 100)
                DueState.NO_RECORD -> "Sin datos" to Color.rgb(130, 134, 140)
            }
            c.drawText(label, cols1[3], y, p.paint(9.5f, 600, col))
            y += 6f; c.drawLine(M.toFloat(), y, (W - M).toFloat(), y, p.line); y += 13f
        }

        // ---- historial
        y += 14f
        ensure(60f)
        c.drawText("Historial", M.toFloat(), y, p.h2); y += 16f
        val cols2 = floatArrayOf(M.toFloat(), 112f, 182f, 380f, 490f)
        c.drawText("Fecha", cols2[0], y, p.muted); c.drawText("Km", cols2[1], y, p.muted)
        c.drawText("Intervención", cols2[2], y, p.muted); c.drawText("Taller", cols2[3], y, p.muted)
        c.drawText("Coste", cols2[4], y, p.muted)
        y += 6f; c.drawLine(M.toFloat(), y, (W - M).toFloat(), y, p.line); y += 13f
        val records = book.records.sortedWith(compareByDescending<ServiceRecord> { it.dateEpochDay })
        if (records.isEmpty()) { c.drawText("Sin registros.", M.toFloat(), y, p.muted); y += 14f }
        var total = 0.0
        records.forEach { r ->
            val extra = listOf(r.description, r.notes).filter { it.isNotBlank() }.joinToString(" · ")
            ensure(if (extra.isNotEmpty()) 30f else 18f)
            c.drawText(Fmt.date(LocalDate.ofEpochDay(r.dateEpochDay)), cols2[0], y, p.body)
            c.drawText(r.odometerKm?.let { Fmt.group(it) } ?: "—", cols2[1], y, p.body)
            c.drawText(ellipsize(r.title, p.bodyBold, 190f), cols2[2], y, p.bodyBold)
            c.drawText(ellipsize(r.workshop.ifBlank { "—" }, p.body, 105f), cols2[3], y, p.body)
            r.cost?.let { total += it; c.drawText(Fmt.money(it), cols2[4], y, p.body) }
            if (extra.isNotEmpty()) {
                y += 12f
                c.drawText(ellipsize(extra, p.muted, (W - M) - cols2[2]), cols2[2], y, p.muted)
            }
            y += 6f; c.drawLine(M.toFloat(), y, (W - M).toFloat(), y, p.line); y += 13f
        }
        if (total > 0) {
            ensure(20f)
            val t = "Total registrado: ${Fmt.money(total)}"
            c.drawText(t, W - M - p.bodyBold.measureText(t), y + 4f, p.bodyBold)
        }

        footer()
        doc.finishPage(page)
        doc.writeTo(out)
        doc.close()
    }
}
