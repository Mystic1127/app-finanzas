package com.example.finanzas.util

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.os.Environment
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
            val page = document.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create())
            val canvas = page.canvas
            drawReport(canvas, report)
            document.finishPage(page)
            FileOutputStream(file).use { document.writeTo(it) }
        } finally {
            document.close()
        }

        return file
    }

    private fun drawReport(canvas: Canvas, report: FinancialReport) {
        canvas.drawColor(Color.WHITE)
        var y = margin

        val title = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(31, 42, 36)
            textSize = 22f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val section = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(46, 125, 91)
            textSize = 14f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val body = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(31, 42, 36)
            textSize = 11f
        }
        val muted = Paint(body).apply {
            color = Color.rgb(76, 90, 83)
        }

        canvas.drawText("Finanzas", margin, y, section)
        y += 28f
        canvas.drawText("Reporte financiero mensual", margin, y, title)
        y += 22f
        canvas.drawText("${report.monthLabel} · Moneda base: ${report.currencyCode}", margin, y, muted)
        y += 28f

        if (!report.hasData) {
            y = drawSection(canvas, "Estado del reporte", y, section)
            y = drawWrapped(canvas, "No hay datos suficientes para este mes. Registra ingresos y gastos para generar métricas completas.", y, body)
            y += 10f
        }

        y = drawSection(canvas, "Resumen", y, section)
        val summary = report.summary
        val rows = listOf(
            "Ingresos totales" to Format.money(summary.ingresos, report.currencyCode),
            "Gastos totales" to Format.money(summary.gastos, report.currencyCode),
            "Saldo mensual" to Format.money(summary.saldo, report.currencyCode),
            "Ahorro estimado" to Format.money(summary.ahorroSugerido, report.currencyCode),
            "Score financiero" to "${summary.scoreFinanciero}/100 - ${summary.scoreEstado ?: "Sin estado"}",
            "Estado general" to report.status
        )
        rows.forEach { (label, value) ->
            canvas.drawText(label, margin, y, muted)
            canvas.drawText(value, 320f, y, body)
            y += lineHeight
        }
        y += 12f

        y = drawSection(canvas, "Top categorías de gasto", y, section)
        if (report.topCategories.isEmpty()) {
            y = drawWrapped(canvas, "Sin gastos por categoría para mostrar.", y, body)
        } else {
            report.topCategories.forEachIndexed { index, item ->
                val name = item.categoriaNombre?.takeIf { it.isNotBlank() } ?: "Sin categoría"
                canvas.drawText("${index + 1}. $name", margin, y, body)
                canvas.drawText(Format.money(item.gastado, report.currencyCode), 320f, y, body)
                y += lineHeight
            }
        }
        y += 12f

        y = drawSection(canvas, "Alertas e insights", y, section)
        val insights = (listOfNotNull(summary.insightPrincipal, summary.alertaPrincipal) + summary.alertas)
            .filter { it.isNotBlank() }
            .distinct()
            .take(4)
        if (insights.isEmpty()) {
            y = drawWrapped(canvas, "Sin alertas relevantes por ahora.", y, body)
        } else {
            insights.forEach { insight ->
                y = drawWrapped(canvas, "• $insight", y, body)
            }
        }
        y += 12f

        y = drawSection(canvas, "Transacciones recientes", y, section)
        if (report.recentTransactions.isEmpty()) {
            y = drawWrapped(canvas, "Sin transacciones recientes en este periodo.", y, body)
        } else {
            report.recentTransactions.take(8).forEach { tx ->
                val type = if (tx.isEsIngreso) "Ingreso" else "Gasto"
                val name = tx.categoriaNombre?.takeIf { it.isNotBlank() } ?: "Sin categoría"
                val note = tx.nota?.takeIf { it.isNotBlank() }?.let { " · $it" } ?: ""
                val amount = Format.money(if (tx.isEsIngreso) tx.monto else -tx.monto, report.currencyCode)
                y = drawWrapped(canvas, "${Format.date(tx.fecha)} · $type · $name$note · $amount", y, body)
            }
        }

        val generated = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale("es", "PE")).format(Date(report.generatedAt))
        canvas.drawText("Generado el $generated", margin, pageHeight - 36f, muted)
    }

    private fun drawSection(canvas: Canvas, label: String, y: Float, paint: Paint): Float {
        canvas.drawText(label, margin, y, paint)
        return y + 20f
    }

    private fun drawWrapped(canvas: Canvas, text: String, yStart: Float, paint: Paint): Float {
        val maxWidth = pageWidth - margin * 2
        val words = text.split(" ")
        val line = StringBuilder()
        var y = yStart
        words.forEach { word ->
            val candidate = if (line.isEmpty()) word else "$line $word"
            if (paint.measureText(candidate) > maxWidth && line.isNotEmpty()) {
                canvas.drawText(line.toString(), margin, y, paint)
                y += lineHeight
                line.clear()
                line.append(word)
            } else {
                line.clear()
                line.append(candidate)
            }
        }
        if (line.isNotEmpty()) {
            canvas.drawText(line.toString(), margin, y, paint)
            y += lineHeight
        }
        return y
    }
}
