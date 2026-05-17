package com.example.finanzas.util

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.os.Environment
import com.example.finanzas.data.api.SettingsService
import com.example.finanzas.data.model.FinancialReport
import com.example.finanzas.data.model.Transaccion
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.max

class MonthlyReportPdfExporter(private val context: Context) {
    private val pageWidth = 595
    private val pageHeight = 842
    private val margin = 36f
    private val contentWidth = pageWidth - margin * 2

    private object PdfPalette {
        val page = Color.rgb(249, 251, 249)
        val ink = Color.rgb(22, 31, 27)
        val muted = Color.rgb(91, 105, 99)
        val subtle = Color.rgb(126, 141, 134)
        val primary = Color.rgb(47, 107, 72)
        val primaryDark = Color.rgb(34, 80, 54)
        val income = Color.rgb(28, 132, 83)
        val expense = Color.rgb(196, 69, 82)
        val transfer = Color.rgb(49, 120, 182)
        val card = Color.rgb(255, 255, 255)
        val line = Color.rgb(220, 230, 224)
        val track = Color.rgb(232, 239, 235)
        val softGreen = Color.rgb(233, 244, 237)
        val softRed = Color.rgb(251, 236, 238)
        val softBlue = Color.rgb(232, 241, 250)
        val softYellow = Color.rgb(250, 244, 226)
        val categoryColors = intArrayOf(
            Color.rgb(47, 107, 72),
            Color.rgb(210, 91, 79),
            Color.rgb(59, 130, 190),
            Color.rgb(148, 103, 189),
            Color.rgb(226, 151, 54),
            Color.rgb(42, 157, 143),
            Color.rgb(122, 128, 52),
            Color.rgb(198, 88, 150),
            Color.rgb(70, 92, 167),
            Color.rgb(93, 156, 89),
            Color.rgb(184, 110, 60),
            Color.rgb(92, 131, 142)
        )
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
        val stamp = SimpleDateFormat("yyyyMMdd-HHmmss-SSS", Locale.US).format(Date())
        val file = File(dir, "reporte-financiero-$safeMonth-$stamp.pdf")
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
        drawHeader(writer, report)
        if (!report.hasData) {
            drawEmptyState(writer)
        }
        drawSummary(writer, report)
        drawCategories(writer, report)
        drawInsights(writer, report)
        drawTransactions(writer, report)

        val generated = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale("es", "PE")).format(Date(report.generatedAt))
        writer.footer("Spendly • Generado el $generated", textPaint(9f, PdfPalette.subtle))
    }

    private fun drawHeader(writer: PdfWriter, report: FinancialReport) {
        val logo = textPaint(16f, Color.WHITE, Typeface.BOLD)
        val title = textPaint(24f, PdfPalette.ink, Typeface.BOLD)
        val subtitle = textPaint(10.5f, PdfPalette.muted)
        val badge = textPaint(10f, PdfPalette.primaryDark, Typeface.BOLD)

        writer.ensure(96f)
        writer.canvas.drawRoundRect(RectF(margin, writer.y, margin + 80f, writer.y + 34f), 17f, 17f, fill(PdfPalette.primary))
        writer.canvas.drawText("Spendly", margin + 15f, writer.y + 22f, logo)
        writer.canvas.drawText("Reporte financiero mensual", margin, writer.y + 58f, title)
        writer.canvas.drawText("${report.monthLabel} • Moneda base: ${report.currencyCode}", margin, writer.y + 78f, subtitle)
        drawPill(writer.canvas, pageWidth - margin - 128f, writer.y + 46f, 128f, 26f, report.status, PdfPalette.softGreen, badge)
        writer.move(104f)
    }

    private fun drawEmptyState(writer: PdfWriter) {
        writer.ensure(68f)
        val rect = RectF(margin, writer.y, pageWidth - margin, writer.y + 58f)
        writer.canvas.drawRoundRect(rect, 16f, 16f, fill(PdfPalette.softYellow))
        drawWrapped(writer.canvas, "No hay datos suficientes para este mes. Registra ingresos y gastos para generar metricas completas.", margin + 16f, writer.y + 24f, contentWidth - 32f, textPaint(10.5f, PdfPalette.muted), 15f)
        writer.move(74f)
    }

    private fun drawSummary(writer: PdfWriter, report: FinancialReport) {
        val summary = report.summary
        drawSectionTitle(writer, "Resumen esencial", "Solo los indicadores clave")

        val metrics = listOf(
            Metric("Ingresos", Format.money(summary.ingresos, report.currencyCode), PdfPalette.income),
            Metric("Gastos", Format.money(summary.gastos, report.currencyCode), PdfPalette.expense),
            Metric("Balance", Format.money(summary.saldo, report.currencyCode), if (summary.saldo >= 0) PdfPalette.income else PdfPalette.expense),
            Metric("Score", "${summary.scoreFinanciero}/100", PdfPalette.transfer)
        )

        val gap = 10f
        val cardW = (contentWidth - gap) / 2f
        val cardH = 64f
        metrics.chunked(2).forEach { row ->
            writer.ensure(cardH + 12f)
            row.forEachIndexed { index, metric ->
                drawMetricCard(writer.canvas, margin + index * (cardW + gap), writer.y, cardW, cardH, metric)
            }
            writer.move(cardH + 10f)
        }

        val compact = listOf(
            "Saldo actual" to Format.money(summary.saldoActualTotal, report.currencyCode),
            "Fin de mes" to Format.money(summary.proyeccionFinMes, report.currencyCode),
            "Ahorro estimado" to Format.money(summary.ahorroSugerido, report.currencyCode),
            "Estado financiero" to (summary.scoreEstado ?: "Sin estado")
        )
        drawCompactFacts(writer, compact)
        writer.move(8f)
    }

    private fun drawIncomeExpense(writer: PdfWriter, report: FinancialReport) {
        val summary = report.summary
        drawSectionTitle(writer, "Ingresos vs gastos", "Balance mensual y proyeccion")
        val maxValue = max(summary.ingresos, summary.gastos)
        drawHorizontalBar(writer, "Ingresos", summary.ingresos, maxValue, report.currencyCode, PdfPalette.income, true)
        drawHorizontalBar(writer, "Gastos", summary.gastos, maxValue, report.currencyCode, PdfPalette.expense, true)
        writer.move(10f)
    }

    private fun drawCategories(writer: PdfWriter, report: FinancialReport) {
        drawSectionTitle(writer, "Top categorias", "Mayores gastos del periodo")
        if (report.topCategories.isEmpty()) {
            drawMutedBlock(writer, "Sin gastos por categoria para mostrar.")
            return
        }

        val maxCategory = report.topCategories.maxOfOrNull { it.gastado } ?: 0.0
        report.topCategories.take(5).forEachIndexed { index, item ->
            val color = PdfPalette.categoryColors[index % PdfPalette.categoryColors.size]
            val name = item.categoriaNombre?.takeIf { it.isNotBlank() } ?: "Sin categoria"
            drawCategoryRow(writer, index + 1, name, item.gastado, maxCategory, report.currencyCode, color)
        }
        writer.move(8f)
    }

    private fun drawInsights(writer: PdfWriter, report: FinancialReport) {
        val summary = report.summary
        val insights = (listOfNotNull(summary.insightPrincipal, summary.alertaPrincipal) + summary.alertas + summary.notasInformativas)
            .filter { it.isNotBlank() }
            .distinct()
            .take(3)
        drawSectionTitle(writer, "Alertas clave", "Lectura rapida")
        if (insights.isEmpty()) {
            drawMutedBlock(writer, "Sin alertas relevantes por ahora.")
        } else {
            insights.forEach { drawInsight(writer, it) }
        }
        writer.move(8f)
    }

    private fun drawTransactions(writer: PdfWriter, report: FinancialReport) {
        drawSectionTitle(writer, "Ultimos movimientos", "Solo los mas recientes")
        if (report.recentTransactions.isEmpty()) {
            drawMutedBlock(writer, "Sin transacciones recientes en este periodo.")
            return
        }

        report.recentTransactions.take(6).forEach { tx ->
            drawTransactionRow(writer, tx, report.currencyCode)
        }
    }

    private fun drawMetricCard(canvas: Canvas, left: Float, top: Float, width: Float, height: Float, metric: Metric) {
        val rect = RectF(left, top, left + width, top + height)
        canvas.drawRoundRect(rect, 16f, 16f, fill(PdfPalette.card))
        canvas.drawRoundRect(rect, 16f, 16f, stroke(PdfPalette.line))
        canvas.drawCircle(left + 20f, top + 22f, 7f, fill(metric.color))
        canvas.drawText(metric.label, left + 34f, top + 25f, textPaint(10f, PdfPalette.muted, Typeface.BOLD))
        canvas.drawText(truncate(metric.value, 17), left + 16f, top + 50f, textPaint(16f, metric.color, Typeface.BOLD))
    }

    private fun drawCompactFacts(writer: PdfWriter, items: List<Pair<String, String>>) {
        writer.ensure(86f)
        val rect = RectF(margin, writer.y, pageWidth - margin, writer.y + 78f)
        writer.canvas.drawRoundRect(rect, 16f, 16f, fill(PdfPalette.card))
        writer.canvas.drawRoundRect(rect, 16f, 16f, stroke(PdfPalette.line))
        val labelPaint = textPaint(8.8f, PdfPalette.subtle)
        val valuePaint = textPaint(9.5f, PdfPalette.ink, Typeface.BOLD)
        val colW = contentWidth / 4f
        items.take(8).forEachIndexed { index, item ->
            val col = index % 4
            val row = index / 4
            val x = margin + 14f + col * colW
            val y = writer.y + 24f + row * 34f
            writer.canvas.drawText(item.first, x, y, labelPaint)
            writer.canvas.drawText(truncate(item.second, 18), x, y + 14f, valuePaint)
        }
        writer.move(92f)
    }

    private fun drawCategoryRow(
        writer: PdfWriter,
        rank: Int,
        name: String,
        amount: Double,
        maxValue: Double,
        currencyCode: String,
        color: Int
    ) {
        writer.ensure(46f)
        val top = writer.y
        val rankPaint = textPaint(9f, Color.WHITE, Typeface.BOLD).apply { textAlign = Paint.Align.CENTER }
        writer.canvas.drawCircle(margin + 13f, top + 15f, 12f, fill(color))
        writer.canvas.drawText(rank.toString(), margin + 13f, top + 18f, rankPaint)
        writer.canvas.drawText(truncate(name, 28), margin + 34f, top + 12f, textPaint(10.5f, PdfPalette.ink, Typeface.BOLD))
        writer.canvas.drawText(Format.money(amount, currencyCode), pageWidth - margin, top + 12f, textPaint(10f, PdfPalette.ink, Typeface.BOLD).right())
        val barLeft = margin + 34f
        val barTop = top + 24f
        val barW = contentWidth - 34f
        writer.canvas.drawRoundRect(RectF(barLeft, barTop, barLeft + barW, barTop + 8f), 4f, 4f, fill(PdfPalette.track))
        val ratio = if (maxValue > 0.0) (amount / maxValue).coerceIn(0.0, 1.0).toFloat() else 0f
        writer.canvas.drawRoundRect(RectF(barLeft, barTop, barLeft + barW * ratio, barTop + 8f), 4f, 4f, fill(color))
        writer.move(42f)
    }

    private fun drawTransactionRow(writer: PdfWriter, tx: Transaccion, currencyCode: String) {
        writer.ensure(66f)
        val top = writer.y
        val type = when {
            tx.isTransfer -> "Transferencia"
            tx.isEsIngreso -> "Ingreso"
            else -> "Gasto"
        }
        val color = when {
            tx.isTransfer -> PdfPalette.transfer
            tx.isEsIngreso -> PdfPalette.income
            else -> PdfPalette.expense
        }
        val soft = when {
            tx.isTransfer -> PdfPalette.softBlue
            tx.isEsIngreso -> PdfPalette.softGreen
            else -> PdfPalette.softRed
        }
        val signed = if (tx.isTransfer || tx.isEsIngreso) tx.monto else -tx.monto
        val amount = Format.money(signed, currencyCode)
        val category = tx.categoriaNombre?.takeIf { it.isNotBlank() } ?: "Sin categoria"
        val account = SettingsService.getFinancialAccountName(context, tx.accountType)
        val note = tx.displayNote?.takeIf { it.isNotBlank() }
        val date = tx.fecha ?: Date()
        val dateText = SimpleDateFormat("dd MMM yyyy", Locale("es", "PE")).format(date)
        val timeText = SimpleDateFormat("HH:mm", Locale("es", "PE")).format(date)

        val rect = RectF(margin, top, pageWidth - margin, top + 56f)
        writer.canvas.drawRoundRect(rect, 14f, 14f, fill(PdfPalette.card))
        writer.canvas.drawRoundRect(rect, 14f, 14f, stroke(PdfPalette.line))
        drawPill(writer.canvas, margin + 12f, top + 13f, 86f, 24f, type, soft, textPaint(8.8f, color, Typeface.BOLD))
        writer.canvas.drawText("$dateText • $timeText", margin + 112f, top + 18f, textPaint(9f, PdfPalette.subtle))
        writer.canvas.drawText(truncate(category, 30), margin + 112f, top + 36f, textPaint(11f, PdfPalette.ink, Typeface.BOLD))
        writer.canvas.drawText(account, margin + 290f, top + 36f, textPaint(9.2f, PdfPalette.muted))
        if (!note.isNullOrBlank()) {
            writer.canvas.drawText(truncate(note, 24), margin + 112f, top + 50f, textPaint(8.5f, PdfPalette.subtle))
        }
        writer.canvas.drawText(amount, pageWidth - margin - 12f, top + 32f, textPaint(11f, color, Typeface.BOLD).right())
        writer.move(64f)
    }

    private fun drawHorizontalBar(writer: PdfWriter, label: String, value: Double, maxValue: Double, currencyCode: String, color: Int, big: Boolean) {
        writer.ensure(if (big) 42f else 34f)
        val top = writer.y
        writer.canvas.drawText(label, margin, top + 11f, textPaint(10.5f, PdfPalette.ink, Typeface.BOLD))
        writer.canvas.drawText(Format.money(value, currencyCode), pageWidth - margin, top + 11f, textPaint(10f, PdfPalette.ink, Typeface.BOLD).right())
        val barTop = top + 22f
        writer.canvas.drawRoundRect(RectF(margin, barTop, pageWidth - margin, barTop + 10f), 5f, 5f, fill(PdfPalette.track))
        val ratio = if (maxValue > 0.0) (value / maxValue).coerceIn(0.0, 1.0).toFloat() else 0f
        writer.canvas.drawRoundRect(RectF(margin, barTop, margin + contentWidth * ratio, barTop + 10f), 5f, 5f, fill(color))
        writer.move(if (big) 40f else 32f)
    }

    private fun drawInsight(writer: PdfWriter, text: String) {
        writer.ensure(42f)
        val top = writer.y
        writer.canvas.drawCircle(margin + 8f, top + 12f, 4f, fill(PdfPalette.primary))
        drawWrapped(writer.canvas, text, margin + 22f, top + 6f, contentWidth - 22f, textPaint(10f, PdfPalette.ink), 15f)
        writer.move(36f)
    }

    private fun drawMutedBlock(writer: PdfWriter, text: String) {
        writer.ensure(46f)
        val rect = RectF(margin, writer.y, pageWidth - margin, writer.y + 40f)
        writer.canvas.drawRoundRect(rect, 14f, 14f, fill(PdfPalette.card))
        writer.canvas.drawRoundRect(rect, 14f, 14f, stroke(PdfPalette.line))
        writer.canvas.drawText(text, margin + 14f, writer.y + 24f, textPaint(10f, PdfPalette.muted))
        writer.move(52f)
    }

    private fun drawSectionTitle(writer: PdfWriter, title: String, subtitle: String) {
        writer.ensure(44f)
        writer.canvas.drawText(title, margin, writer.y, textPaint(15f, PdfPalette.primaryDark, Typeface.BOLD))
        writer.canvas.drawText(subtitle, margin, writer.y + 16f, textPaint(9.5f, PdfPalette.subtle))
        writer.move(30f)
    }

    private fun drawPill(canvas: Canvas, left: Float, top: Float, width: Float, height: Float, text: String, bg: Int, paint: Paint) {
        canvas.drawRoundRect(RectF(left, top, left + width, top + height), height / 2f, height / 2f, fill(bg))
        val centered = Paint(paint).apply { textAlign = Paint.Align.CENTER }
        canvas.drawText(truncate(text, 20), left + width / 2f, top + height / 2f + 3.5f, centered)
    }

    private fun drawWrapped(canvas: Canvas, text: String, left: Float, top: Float, maxWidth: Float, paint: Paint, lineHeight: Float) {
        val words = text.split(" ")
        var line = ""
        var y = top
        words.forEach { word ->
            val candidate = if (line.isBlank()) word else "$line $word"
            if (paint.measureText(candidate) > maxWidth && line.isNotBlank()) {
                canvas.drawText(line, left, y, paint)
                y += lineHeight
                line = word
            } else {
                line = candidate
            }
        }
        if (line.isNotBlank()) {
            canvas.drawText(line, left, y, paint)
        }
    }

    private fun textPaint(size: Float, color: Int, style: Int = Typeface.NORMAL): Paint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = color
            textSize = size
            typeface = Typeface.create(Typeface.DEFAULT, style)
        }

    private fun fill(color: Int): Paint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = color
            style = Paint.Style.FILL
        }

    private fun stroke(color: Int): Paint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = color
            style = Paint.Style.STROKE
            strokeWidth = 1f
        }

    private fun Paint.right(): Paint = Paint(this).apply { textAlign = Paint.Align.RIGHT }

    private fun truncate(value: String, maxChars: Int): String {
        if (value.length <= maxChars) return value
        return value.take(maxChars - 3).trimEnd() + "..."
    }

    private data class Metric(
        val label: String,
        val value: String,
        val color: Int
    )

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

        fun move(delta: Float) {
            y += delta
        }

        fun footer(text: String, paint: Paint) {
            canvas.drawText(text, margin, pageHeight - 34f, paint)
            canvas.drawText("Pag. $pageNumber", pageWidth - margin, pageHeight - 34f, Paint(paint).right())
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
