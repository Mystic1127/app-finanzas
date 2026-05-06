package com.example.finanzas.util

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.os.Environment
import com.example.finanzas.data.api.SettingsService
import com.example.finanzas.data.model.FinancialReport
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MonthlyReportPdfExporter(private val context: Context) {
    private val pageWidth = 595
    private val pageHeight = 842
    private val margin = 42f
    private val lineHeight = 18f

    private object PdfPalette {
        val page = Color.rgb(255, 255, 255)
        val title = Color.rgb(23, 33, 27)
        val primary = Color.rgb(35, 107, 78)
        val muted = Color.rgb(81, 97, 89)
        val expense = Color.rgb(195, 59, 74)
        val barTrack = Color.rgb(221, 233, 226)
    }

    fun export(report: FinancialReport): File {
        val dir = File(context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), "reports")
        if (!dir.exists() && !dir.mkdirs()) {
            throw IllegalStateException("No se pudo crear la carpeta de reportes")
        }

        val safeMonth = report.monthLabel.lowercase(Locale.ROOT)
            .replace(Regex("[^a-z0-9]+"), "-")
            .trim('-')
            .ifBlank { "mensual" }
        val file = File(dir, "reporte-financiero-$safeMonth.pdf")
        val document = PdfDocument()

        try {
            val writer = PdfWriter(document)
            writer.startPage()
            drawReport(writer, report)
            writer.finish()
            FileOutputStream(file).use { document.writeTo(it) }
        } finally {
            document.close()
        }

        return file
    }

    private fun drawReport(writer: PdfWriter, report: FinancialReport) {
        val title = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = PdfPalette.title
            textSize = 22f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val section = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = PdfPalette.primary
            textSize = 14f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val body = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = PdfPalette.title
            textSize = 11f
        }
        val muted = Paint(body).apply {
            color = PdfPalette.muted
        }
        val incomeBar = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = PdfPalette.primary
            style = Paint.Style.FILL
        }
        val expenseBar = Paint(incomeBar).apply {
            color = PdfPalette.expense
        }
        val barBg = Paint(incomeBar).apply {
            color = PdfPalette.barTrack
        }

        writer.text("Spendly", section)
        writer.move(10f)
        writer.text("Reporte financiero mensual", title)
        writer.text("${report.monthLabel} - Moneda base: ${report.currencyCode}", muted)
        writer.move(10f)

        if (!report.hasData) {
            drawSection(writer, "Estado del reporte", section)
            drawWrapped(writer, "No hay datos suficientes para este mes. Registra ingresos y gastos para generar metricas completas.", body)
            writer.move(10f)
        }

        val summary = report.summary
        drawSection(writer, "Resumen", section)
        listOf(
            "Ingresos totales" to Format.money(summary.ingresos, report.currencyCode),
            "Gastos totales" to Format.money(summary.gastos, report.currencyCode),
            "Balance mensual" to Format.money(summary.saldo, report.currencyCode),
            "Saldo inicial efectivo" to Format.money(summary.initialCashBalance, report.currencyCode),
            "Saldo inicial tarjeta/cuenta" to Format.money(summary.initialCardBalance, report.currencyCode),
            "Saldo actual efectivo" to Format.money(summary.efectivo, report.currencyCode),
            "Saldo actual tarjeta/cuenta" to Format.money(summary.tarjetaCuenta, report.currencyCode),
            "Saldo total actual" to Format.money(summary.saldoActualTotal, report.currencyCode),
            "Gasto proyectado" to Format.money(summary.gastoProyectado, report.currencyCode),
            "Saldo estimado fin de mes" to Format.money(summary.proyeccionFinMes, report.currencyCode),
            "Confianza de proyeccion" to (summary.confianzaProyeccion ?: "Sin datos"),
            "Ahorro estimado" to Format.money(summary.ahorroSugerido, report.currencyCode),
            "Score financiero" to "${summary.scoreFinanciero}/100 - ${summary.scoreEstado ?: "Sin estado"}",
            "Estado general" to report.status
        ).forEach { (label, value) ->
            writer.ensure(lineHeight)
            writer.canvas.drawText(label, margin, writer.y, muted)
            writer.canvas.drawText(value, 320f, writer.y, body)
            writer.move(lineHeight)
        }
        writer.move(12f)

        drawSection(writer, "Ingresos vs gastos", section)
        drawWrapped(writer, "El balance mensual es ingresos menos gastos del mes; Saldo inicial aparece como ingreso especial cuando corresponde.", muted)
        val maxSummary = maxOf(summary.ingresos, summary.gastos)
        drawHorizontalBar(writer, "Ingresos", summary.ingresos, maxSummary, report.currencyCode, incomeBar, barBg, body)
        drawHorizontalBar(writer, "Gastos", summary.gastos, maxSummary, report.currencyCode, expenseBar, barBg, body)
        writer.move(12f)

        drawSection(writer, "Top categorias de gasto", section)
        if (report.topCategories.isEmpty()) {
            drawWrapped(writer, "Sin gastos por categoria para mostrar.", body)
        } else {
            val maxCategory = report.topCategories.maxOfOrNull { it.gastado } ?: 0.0
            report.topCategories.forEachIndexed { index, item ->
                val name = item.categoriaNombre?.takeIf { it.isNotBlank() } ?: "Sin categoria"
                drawHorizontalBar(writer, "${index + 1}. $name", item.gastado, maxCategory, report.currencyCode, expenseBar, barBg, body)
            }
        }
        writer.move(12f)

        drawSection(writer, "Alertas e insights", section)
        val insights = (listOfNotNull(summary.insightPrincipal, summary.alertaPrincipal) + summary.alertas + summary.notasInformativas)
            .filter { it.isNotBlank() }
            .distinct()
            .take(4)
        if (insights.isEmpty()) {
            drawWrapped(writer, "Sin alertas relevantes por ahora.", body)
        } else {
            insights.forEach { drawWrapped(writer, "- $it", body) }
        }
        writer.move(12f)

        drawSection(writer, "Transacciones recientes", section)
        if (report.recentTransactions.isEmpty()) {
            drawWrapped(writer, "Sin transacciones recientes en este periodo.", body)
        } else {
            report.recentTransactions.forEach { tx ->
                val type = if (tx.isTransfer) "Transferencia" else if (tx.isEsIngreso) "Ingreso" else "Gasto"
                val name = tx.categoriaNombre?.takeIf { it.isNotBlank() } ?: "Sin categoria"
                val note = tx.displayNote?.takeIf { it.isNotBlank() }?.let { " - $it" } ?: ""
                val account = SettingsService.getFinancialAccountName(context, tx.accountType)
                val signed = if (tx.isTransfer) tx.monto else if (tx.isEsIngreso) tx.monto else -tx.monto
                val amount = Format.money(signed, report.currencyCode)
                drawWrapped(writer, "${Format.date(tx.fecha)} - $type - $name - $account$note - $amount", body)
            }
        }

        val generated = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale("es", "PE")).format(Date(report.generatedAt))
        writer.footer("Generado el $generated", muted)
    }

    private fun drawSection(writer: PdfWriter, label: String, paint: Paint) {
        writer.ensure(24f)
        writer.canvas.drawText(label, margin, writer.y, paint)
        writer.move(20f)
    }

    private fun drawWrapped(writer: PdfWriter, text: String, paint: Paint) {
        val maxWidth = pageWidth - margin * 2
        val words = text.split(" ")
        val line = StringBuilder()
        words.forEach { word ->
            val candidate = if (line.isEmpty()) word else "$line $word"
            if (paint.measureText(candidate) > maxWidth && line.isNotEmpty()) {
                writer.ensure(lineHeight)
                writer.canvas.drawText(line.toString(), margin, writer.y, paint)
                writer.move(lineHeight)
                line.clear()
                line.append(word)
            } else {
                line.clear()
                line.append(candidate)
            }
        }
        if (line.isNotEmpty()) {
            writer.ensure(lineHeight)
            writer.canvas.drawText(line.toString(), margin, writer.y, paint)
            writer.move(lineHeight)
        }
    }

    private fun drawHorizontalBar(
        writer: PdfWriter,
        label: String,
        value: Double,
        maxValue: Double,
        currencyCode: String,
        fill: Paint,
        background: Paint,
        text: Paint
    ) {
        writer.ensure(36f)
        writer.canvas.drawText(label.take(34), margin, writer.y, text)
        writer.canvas.drawText(Format.money(value, currencyCode), 380f, writer.y, text)
        val top = writer.y + 6f
        val left = margin
        val width = pageWidth - margin * 2
        writer.canvas.drawRect(left, top, left + width, top + 8f, background)
        val ratio = if (maxValue > 0.0) (value / maxValue).coerceIn(0.0, 1.0).toFloat() else 0f
        writer.canvas.drawRect(left, top, left + width * ratio, top + 8f, fill)
        writer.move(32f)
    }

    private inner class PdfWriter(private val document: PdfDocument) {
        private var pageNumber = 0
        private var currentPage: PdfDocument.Page? = null
        lateinit var canvas: Canvas
            private set
        var y: Float = margin
            private set

        fun startPage() {
            pageNumber++
            val page = document.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create())
            currentPage = page
            canvas = page.canvas
            canvas.drawColor(PdfPalette.page)
            y = margin
        }

        fun ensure(required: Float) {
            if (y + required <= pageHeight - 58f) return
            finishCurrent()
            startPage()
        }

        fun text(text: String, paint: Paint) {
            ensure(lineHeight)
            canvas.drawText(text, margin, y, paint)
            move(lineHeight)
        }

        fun move(delta: Float) {
            y += delta
        }

        fun footer(text: String, paint: Paint) {
            canvas.drawText(text, margin, pageHeight - 36f, paint)
        }

        fun finish() {
            finishCurrent()
        }

        private fun finishCurrent() {
            currentPage?.let {
                document.finishPage(it)
                currentPage = null
            }
        }
    }
}
