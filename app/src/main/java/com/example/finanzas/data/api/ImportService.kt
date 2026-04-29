package com.example.finanzas.data.api

import android.content.Context
import com.example.finanzas.data.local.LocalRepository
import com.example.finanzas.data.model.ImportJob
import com.example.finanzas.data.model.ImportRule
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

object ImportService {

    interface ListCallback { fun onSuccess(items: List<ImportJob>); fun onError() }
    interface RulesCallback { fun onSuccess(rules: List<ImportRule>); fun onError() }
    interface CreateCallback { fun onSuccess(importId: Int); fun onError() }
    interface SimpleCallback { fun onSuccess(); fun onError() }
    interface ProcessCallback { fun onSuccess(response: JSONObject); fun onError() }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    suspend fun list(ctx: Context): List<ImportJob> = withContext(Dispatchers.IO) {
        LocalRepository.getInstance(ctx).listImports()
    }

    suspend fun listRules(ctx: Context): List<ImportRule> = withContext(Dispatchers.IO) {
        LocalRepository.getInstance(ctx).listImportRules()
    }

    suspend fun create(ctx: Context, tipo: String, nombre: String, lineas: JSONArray?): Int = withContext(Dispatchers.IO) {
        LocalRepository.getInstance(ctx).createImport(nombre, tipo, lineas?.toString() ?: "[]")
    }

    suspend fun saveRule(ctx: Context, rule: ImportRule) = withContext(Dispatchers.IO) {
        LocalRepository.getInstance(ctx).saveImportRule(rule)
    }

    suspend fun deleteRule(ctx: Context, ruleId: Int) = withContext(Dispatchers.IO) {
        LocalRepository.getInstance(ctx).deleteImportRule(ruleId)
    }

    suspend fun process(ctx: Context, importId: Int): JSONObject = withContext(Dispatchers.IO) {
        LocalRepository.getInstance(ctx).processImport(importId)
    }

    @JvmStatic
    fun list(ctx: Context, cb: ListCallback) {
        scope.launch { runCatching { list(ctx) }.onSuccess(cb::onSuccess).onFailure { cb.onError() } }
    }

    @JvmStatic
    fun listRules(ctx: Context, cb: RulesCallback) {
        scope.launch { runCatching { listRules(ctx) }.onSuccess(cb::onSuccess).onFailure { cb.onError() } }
    }

    @JvmStatic
    fun create(ctx: Context, tipo: String, nombre: String, lineas: JSONArray?, cb: CreateCallback) {
        scope.launch { runCatching { create(ctx, tipo, nombre, lineas) }.onSuccess(cb::onSuccess).onFailure { cb.onError() } }
    }

    @JvmStatic
    fun saveRule(ctx: Context, rule: ImportRule, cb: SimpleCallback) {
        scope.launch { runCatching { saveRule(ctx, rule) }.onSuccess { cb.onSuccess() }.onFailure { cb.onError() } }
    }

    @JvmStatic
    fun deleteRule(ctx: Context, ruleId: Int, cb: SimpleCallback) {
        scope.launch { runCatching { deleteRule(ctx, ruleId) }.onSuccess { cb.onSuccess() }.onFailure { cb.onError() } }
    }

    @JvmStatic
    fun process(ctx: Context, importId: Int, cb: ProcessCallback) {
        scope.launch { runCatching { process(ctx, importId) }.onSuccess(cb::onSuccess).onFailure { cb.onError() } }
    }
}
