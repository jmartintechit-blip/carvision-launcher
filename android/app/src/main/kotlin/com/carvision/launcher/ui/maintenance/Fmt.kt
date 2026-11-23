package com.carvision.launcher.ui.maintenance

import java.text.NumberFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.ResolverStyle
import java.util.Locale

/** Formatos en español de España (separador de miles con punto, fechas dd/mm/aaaa, euros). */
object Fmt {
    private val es = Locale("es", "ES")
    private val dateFmt = DateTimeFormatter.ofPattern("dd/MM/uuuu").withResolverStyle(ResolverStyle.STRICT)
    private val intFmt = NumberFormat.getIntegerInstance(es).apply { isGroupingUsed = true }

    fun date(d: LocalDate): String = d.format(dateFmt)

    /** Admite d/m/aaaa, dd-mm-aaaa y dd.mm.aaaa. */
    fun parseDate(text: String): LocalDate? {
        val parts = text.trim().split('/', '-', '.').filter { it.isNotBlank() }
        if (parts.size != 3) return null
        val (d, m, y) = parts.map { it.toIntOrNull() ?: return null }
        if (y < 1950 || y > 2100) return null
        return runCatching { LocalDate.of(y, m, d) }.getOrNull()
    }

    /** "128.450 km" (el agrupado de es-ES no separa números de 4 cifras: se fuerza). */
    fun km(v: Long): String = "${group(v)} km"

    fun group(v: Long): String {
        val s = intFmt.format(v)
        return if (v in 1000..9999 && !s.contains('.')) s.substring(0, s.length - 3) + "." + s.takeLast(3) else s
    }

    fun money(v: Double): String = String.format(es, "%,.2f €", v)

    fun plainMoney(v: Double): String = String.format(es, "%.2f", v)

    /** "6 meses", "3 semanas", "12 días", "1 año y 2 meses". */
    fun duration(days: Long): String = when {
        days >= 365 -> {
            val y = days / 365
            val m = (days % 365) / 30
            "$y año${if (y > 1) "s" else ""}" + if (m > 0) " y $m mes${if (m > 1) "es" else ""}" else ""
        }
        days >= 60 -> "${days / 30} meses"
        days >= 14 -> "${days / 7} semanas"
        days == 1L -> "1 día"
        else -> "$days días"
    }
}

/** Resumen del carnet para Inicio y avisos: texto corto y el estado más urgente (null = nada que avisar). */
fun maintenanceSummary(
    book: com.carvision.core.maintenance.MaintenanceBook,
    vehicle: com.carvision.core.model.VehicleSnapshot,
): Pair<String, com.carvision.core.maintenance.DueState?> {
    if (book.records.isEmpty()) return "Sin registros" to null
    val st = com.carvision.core.maintenance.MaintenanceCalculator.compute(book, currentOdometer(book, vehicle).km, LocalDate.now())
    val overdue = st.count { it.state == com.carvision.core.maintenance.DueState.OVERDUE }
    val soon = st.count { it.state == com.carvision.core.maintenance.DueState.SOON }
    return when {
        overdue > 0 -> "$overdue vencido${if (overdue > 1) "s" else ""}" to com.carvision.core.maintenance.DueState.OVERDUE
        soon > 0 -> "$soon próximo${if (soon > 1) "s" else ""}" to com.carvision.core.maintenance.DueState.SOON
        else -> "Todo al día" to com.carvision.core.maintenance.DueState.OK
    }
}
