package com.example.finanzas.data.repository

import android.content.Context
import com.example.finanzas.data.api.SettingsService
import com.example.finanzas.data.model.FinancialAssistantGeneratedBy
import com.example.finanzas.data.model.FinancialAssistantMonthlyAggregate
import com.example.finanzas.data.model.FinancialAssistantResult
import com.example.finanzas.data.model.FinancialAssistantRiskLevel
import com.example.finanzas.data.model.HomeSummary
import com.example.finanzas.util.PerfLogger
import com.example.finanzas.util.Prefs
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.functions.FirebaseFunctions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale
import kotlin.math.round

class FinancialAssistantRepository(context: Context) {
    private val appContext = context.applicationContext
    private val prefs = appContext.getSharedPreferences("financial_assistant_cache", Context.MODE_PRIVATE)
    private val remoteTimeoutMs = 8_000L

    suspend fun analyze(summary: HomeSummary, forceRemote: Boolean): FinancialAssistantResult =
        withContext(Dispatchers.IO) {
            val aggregate = buildAggregate(summary)
            if (!hasUsefulData(aggregate)) {
                return@withContext localFallback(
                    aggregate,
                    "Aun no hay suficiente informacion para analizar tu mes. Registra ingresos y gastos para recibir recomendaciones."
                )
            }

            cacheKey(aggregate)?.let { key ->
                if (!forceRemote) {
                    readCached(key)?.let { cached ->
                        PerfLogger.log("FinancialAssistant", "cacheHit year=${aggregate.year} month=${aggregate.month}")
                        return@withContext cached
                    }
                }
            }

            val firebaseUser = FirebaseAuth.getInstance().currentUser
            val linkedUid = Prefs.getFirebaseUidForCurrentUser(appContext)
            if (firebaseUser == null || linkedUid.isNullOrBlank() || firebaseUser.uid != linkedUid) {
                PerfLogger.log("FinancialAssistant", "fallback reason=noLinkedFirebaseUser")
                return@withContext localFallback(aggregate)
            }

            val start = PerfLogger.now()
            runCatching {
                val result = withTimeout(remoteTimeoutMs) {
                    FirebaseFunctions.getInstance()
                        .getHttpsCallable("financialAssistant")
                        .call(aggregate.toCallableMap())
                        .await()
                }
                parseRemoteResult(result.data)
            }.onSuccess { remote ->
                PerfLogger.logSince("FinancialAssistant", "remoteSuccess", start)
                cacheKey(aggregate)?.let { writeCached(it, remote) }
                return@withContext remote
            }.onFailure {
                PerfLogger.logSince("FinancialAssistant", "remoteFallback", start)
            }

            localFallback(aggregate)
        }

    private fun buildAggregate(summary: HomeSummary): FinancialAssistantMonthlyAggregate {
        val currency = SettingsService.getCurrencyCode(appContext).ifBlank { "PEN" }
        val activeGoals = summary.metas.filter { it.montoObjetivo > 0.0 && it.montoActual < it.montoObjetivo }
        val activeGoalsTarget = activeGoals.sumOf { it.montoObjetivo }
        val activeGoalsSaved = activeGoals.sumOf { it.montoActual }
        val topCategory = summary.chartCategorias.maxByOrNull { it.gastado }
        val hints = mutableListOf<String>()
        if (!summary.alertaPrincipal.isNullOrBlank()) hints.add(summary.alertaPrincipal)
        hints.addAll(summary.alertas)
        hints.addAll(summary.notasInformativas)

        return FinancialAssistantMonthlyAggregate(
            year = summary.anio,
            month = summary.mes,
            currency = currency,
            totalIncome = cleanMoney(summary.ingresosRecurrentes.takeIf { it > 0.0 } ?: summary.ingresos),
            totalExpense = cleanMoney(summary.gastos),
            currentBalance = cleanMoney(summary.saldoActualTotal),
            estimatedMonthEndBalance = cleanMoney(summary.proyeccionFinMes),
            projectedExpense = cleanMoney(summary.gastoProyectado),
            budgetTotal = cleanMoney(summary.presupuestoMonto),
            budgetUsed = cleanMoney(summary.gastos),
            budgetRemaining = cleanMoney(summary.presupuestoRestante),
            topExpenseCategoryName = topCategory?.categoriaNombre?.trim()?.take(40),
            topExpenseCategoryAmount = cleanMoney(topCategory?.gastado ?: summary.categoriaMayorGastoMonto),
            activeGoalsCount = activeGoals.size,
            activeGoalsTotalTarget = cleanMoney(activeGoalsTarget),
            activeGoalsSavedAmount = cleanMoney(activeGoalsSaved),
            recurringExpensesTotal = null,
            financialScore = summary.scoreFinanciero.takeIf { it > 0 },
            riskHints = hints.map { it.trim().take(120) }.filter { it.isNotBlank() }.distinct().take(4)
        )
    }

    private fun hasUsefulData(aggregate: FinancialAssistantMonthlyAggregate): Boolean {
        return aggregate.totalIncome > 0.0 || aggregate.totalExpense > 0.0
    }

    private fun localFallback(
        aggregate: FinancialAssistantMonthlyAggregate,
        neutralMessage: String? = null
    ): FinancialAssistantResult {
        if (neutralMessage != null) {
            return FinancialAssistantResult(
                summary = neutralMessage,
                suggestedSavingAmount = 0.0,
                riskLevel = FinancialAssistantRiskLevel.LOW,
                riskLabel = "Sin datos",
                recommendedAction = "Registra al menos un ingreso y un gasto para activar recomendaciones mas precisas.",
                alerts = emptyList(),
                positiveInsight = "Empezar con datos simples ya mejora tu control financiero.",
                mainConcern = "Faltan movimientos del mes para estimar tu situacion.",
                generatedBy = FinancialAssistantGeneratedBy.local_fallback
            )
        }

        val expenseRatio = if (aggregate.totalIncome > 0.0) aggregate.totalExpense / aggregate.totalIncome else 1.0
        val topRatio = if (aggregate.totalExpense > 0.0) aggregate.topExpenseCategoryAmount / aggregate.totalExpense else 0.0
        val risk = when {
            aggregate.totalExpense > aggregate.totalIncome && aggregate.totalIncome > 0.0 -> FinancialAssistantRiskLevel.HIGH
            aggregate.estimatedMonthEndBalance < 0.0 -> FinancialAssistantRiskLevel.HIGH
            aggregate.totalIncome <= 0.0 && aggregate.totalExpense > 0.0 -> FinancialAssistantRiskLevel.HIGH
            expenseRatio >= 0.9 -> FinancialAssistantRiskLevel.HIGH
            expenseRatio >= 0.8 -> FinancialAssistantRiskLevel.MEDIUM
            aggregate.budgetTotal > 0.0 && aggregate.budgetUsed >= aggregate.budgetTotal * 0.85 -> FinancialAssistantRiskLevel.MEDIUM
            else -> FinancialAssistantRiskLevel.LOW
        }
        val suggestedSaving = when {
            risk == FinancialAssistantRiskLevel.HIGH -> 0.0
            aggregate.currentBalance <= 0.0 -> 0.0
            aggregate.totalIncome > 0.0 && aggregate.activeGoalsCount > 0 ->
                minOf(aggregate.currentBalance * 0.08, aggregate.totalIncome * 0.1)
            aggregate.totalIncome > 0.0 -> minOf(aggregate.currentBalance * 0.06, aggregate.totalIncome * 0.08)
            else -> 0.0
        }.coerceAtLeast(0.0)

        val concern = when {
            aggregate.totalIncome <= 0.0 && aggregate.totalExpense > 0.0 -> "No hay ingresos registrados este mes."
            aggregate.estimatedMonthEndBalance < 0.0 -> "El saldo estimado de fin de mes podria quedar negativo."
            expenseRatio >= 0.8 -> "Tus gastos ya consumen una parte alta de tus ingresos."
            topRatio >= 0.45 && !aggregate.topExpenseCategoryName.isNullOrBlank() ->
                "Tu gasto esta concentrado en ${aggregate.topExpenseCategoryName}."
            aggregate.budgetTotal <= 0.0 -> "Aun no tienes un presupuesto mensual como referencia."
            else -> "Mantener el ritmo actual durante el resto del mes."
        }
        val action = when {
            risk == FinancialAssistantRiskLevel.HIGH -> "Pausa gastos no esenciales y revisa tus proximos pagos antes de ahorrar."
            topRatio >= 0.45 && !aggregate.topExpenseCategoryName.isNullOrBlank() ->
                "Revisa ${aggregate.topExpenseCategoryName} y define un limite simple para lo que queda del mes."
            suggestedSaving > 0.0 -> "Separa un ahorro moderado y conserva margen para gastos del mes."
            else -> "Sigue registrando movimientos y revisa tu presupuesto semanalmente."
        }
        val alerts = buildList {
            if (aggregate.estimatedMonthEndBalance < 0.0) add("Saldo estimado negativo al cierre del mes.")
            if (aggregate.totalIncome > 0.0 && aggregate.totalExpense > aggregate.totalIncome) add("Los gastos superan los ingresos del mes.")
            if (topRatio >= 0.45 && !aggregate.topExpenseCategoryName.isNullOrBlank()) add("Mayor concentracion de gasto en ${aggregate.topExpenseCategoryName}.")
        }.take(3)

        return FinancialAssistantResult(
            summary = when (risk) {
                FinancialAssistantRiskLevel.LOW -> "Tu mes se ve estable con los datos agregados actuales."
                FinancialAssistantRiskLevel.MEDIUM -> "Tu mes requiere atencion para conservar margen."
                FinancialAssistantRiskLevel.HIGH -> "Tu mes esta ajustado y conviene priorizar liquidez."
            },
            suggestedSavingAmount = cleanMoney(suggestedSaving),
            riskLevel = risk,
            riskLabel = riskLabel(risk),
            recommendedAction = action,
            alerts = alerts.ifEmpty { aggregate.riskHints.take(2) },
            positiveInsight = positiveInsight(aggregate, risk),
            mainConcern = concern,
            generatedBy = FinancialAssistantGeneratedBy.local_fallback
        )
    }

    private fun positiveInsight(
        aggregate: FinancialAssistantMonthlyAggregate,
        risk: FinancialAssistantRiskLevel
    ): String = when {
        aggregate.activeGoalsCount > 0 -> "Tienes metas activas, eso ayuda a dar direccion a tu ahorro."
        aggregate.budgetTotal > 0.0 && aggregate.budgetRemaining >= 0.0 -> "Estas midiendo tu gasto contra un presupuesto mensual."
        risk == FinancialAssistantRiskLevel.LOW -> "Tus gastos se mantienen por debajo de tus ingresos."
        aggregate.currentBalance > 0.0 -> "Aun mantienes saldo disponible para ordenar el cierre del mes."
        else -> "Ya tienes datos para detectar ajustes concretos."
    }

    private fun parseRemoteResult(data: Any?): FinancialAssistantResult {
        val map = data as? Map<*, *> ?: error("Invalid assistant response")
        val summary = map.stringValue("summary")
        val riskLevel = when (map.stringValue("riskLevel").uppercase(Locale.US)) {
            "LOW" -> FinancialAssistantRiskLevel.LOW
            "MEDIUM" -> FinancialAssistantRiskLevel.MEDIUM
            "HIGH" -> FinancialAssistantRiskLevel.HIGH
            else -> error("Invalid risk level")
        }
        val generatedBy = when (map.stringValue("generatedBy")) {
            "ai" -> FinancialAssistantGeneratedBy.ai
            "local_fallback" -> FinancialAssistantGeneratedBy.local_fallback
            else -> error("Invalid generator")
        }
        return FinancialAssistantResult(
            summary = summary,
            suggestedSavingAmount = cleanMoney(map.doubleValue("suggestedSavingAmount")),
            riskLevel = riskLevel,
            riskLabel = map.stringValue("riskLabel"),
            recommendedAction = map.stringValue("recommendedAction"),
            alerts = map.listValue("alerts").take(4),
            positiveInsight = map.stringValue("positiveInsight"),
            mainConcern = map.stringValue("mainConcern"),
            generatedBy = generatedBy
        ).also { validate(it) }
    }

    private fun validate(result: FinancialAssistantResult) {
        require(result.summary.isNotBlank())
        require(result.riskLabel.isNotBlank())
        require(result.recommendedAction.isNotBlank())
        require(result.positiveInsight.isNotBlank())
        require(result.mainConcern.isNotBlank())
        require(result.suggestedSavingAmount >= 0.0)
    }

    private fun FinancialAssistantMonthlyAggregate.toCallableMap(): Map<String, Any?> = mapOf(
        "year" to year,
        "month" to month,
        "currency" to currency,
        "totalIncome" to totalIncome,
        "totalExpense" to totalExpense,
        "currentBalance" to currentBalance,
        "estimatedMonthEndBalance" to estimatedMonthEndBalance,
        "projectedExpense" to projectedExpense,
        "budgetTotal" to budgetTotal,
        "budgetUsed" to budgetUsed,
        "budgetRemaining" to budgetRemaining,
        "topExpenseCategoryName" to topExpenseCategoryName,
        "topExpenseCategoryAmount" to topExpenseCategoryAmount,
        "activeGoalsCount" to activeGoalsCount,
        "activeGoalsTotalTarget" to activeGoalsTotalTarget,
        "activeGoalsSavedAmount" to activeGoalsSavedAmount,
        "recurringExpensesTotal" to recurringExpensesTotal,
        "financialScore" to financialScore,
        "riskHints" to riskHints
    )

    private fun cacheKey(aggregate: FinancialAssistantMonthlyAggregate): String? {
        if (aggregate.year <= 0 || aggregate.month !in 1..12) return null
        return listOf(
            aggregate.year,
            aggregate.month,
            aggregate.currency,
            (aggregate.totalIncome * 100).toLong(),
            (aggregate.totalExpense * 100).toLong(),
            (aggregate.currentBalance * 100).toLong(),
            (aggregate.estimatedMonthEndBalance * 100).toLong(),
            aggregate.financialScore ?: -1
        ).joinToString("-")
    }

    private fun readCached(key: String): FinancialAssistantResult? {
        val raw = prefs.getString(key, null) ?: return null
        return runCatching { parseJsonResult(JSONObject(raw)) }.getOrNull()
    }

    private fun writeCached(key: String, result: FinancialAssistantResult) {
        prefs.edit().putString(key, result.toJson().toString()).apply()
    }

    private fun parseJsonResult(json: JSONObject): FinancialAssistantResult {
        val risk = FinancialAssistantRiskLevel.valueOf(json.optString("riskLevel", "LOW"))
        val generatedBy = FinancialAssistantGeneratedBy.valueOf(json.optString("generatedBy", "local_fallback"))
        val alertsJson = json.optJSONArray("alerts") ?: JSONArray()
        val alerts = (0 until alertsJson.length()).mapNotNull { alertsJson.optString(it).takeIf { value -> value.isNotBlank() } }
        return FinancialAssistantResult(
            summary = json.optString("summary"),
            suggestedSavingAmount = json.optDouble("suggestedSavingAmount", 0.0),
            riskLevel = risk,
            riskLabel = json.optString("riskLabel"),
            recommendedAction = json.optString("recommendedAction"),
            alerts = alerts,
            positiveInsight = json.optString("positiveInsight"),
            mainConcern = json.optString("mainConcern"),
            generatedBy = generatedBy
        ).also { validate(it) }
    }

    private fun FinancialAssistantResult.toJson(): JSONObject = JSONObject()
        .put("summary", summary)
        .put("suggestedSavingAmount", suggestedSavingAmount)
        .put("riskLevel", riskLevel.name)
        .put("riskLabel", riskLabel)
        .put("recommendedAction", recommendedAction)
        .put("alerts", JSONArray(alerts))
        .put("positiveInsight", positiveInsight)
        .put("mainConcern", mainConcern)
        .put("generatedBy", generatedBy.name)

    private fun Map<*, *>.stringValue(key: String): String =
        (this[key] as? String)?.trim()?.take(280)?.takeIf { it.isNotBlank() } ?: error("Missing $key")

    private fun Map<*, *>.doubleValue(key: String): Double =
        (this[key] as? Number)?.toDouble() ?: error("Missing $key")

    private fun Map<*, *>.listValue(key: String): List<String> =
        (this[key] as? List<*>)?.mapNotNull { (it as? String)?.trim()?.take(180) }?.filter { it.isNotBlank() }.orEmpty()

    private fun riskLabel(risk: FinancialAssistantRiskLevel): String = when (risk) {
        FinancialAssistantRiskLevel.LOW -> "Estable"
        FinancialAssistantRiskLevel.MEDIUM -> "Atento"
        FinancialAssistantRiskLevel.HIGH -> "Riesgo alto"
    }

    private fun cleanMoney(value: Double): Double {
        if (!value.isFinite()) return 0.0
        return round(value * 100.0) / 100.0
    }
}
