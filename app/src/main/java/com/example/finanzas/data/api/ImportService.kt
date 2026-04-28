package com.example.finanzas.data.api

import android.content.Context
import com.example.finanzas.data.local.LocalRepository
import com.example.finanzas.data.model.ImportJob
import com.example.finanzas.data.model.ImportRule
import org.json.JSONArray
import org.json.JSONObject

object ImportService {

    interface ListCallback {
        fun onSuccess(items: List<ImportJob>)
        fun onError()
    }

    interface RulesCallback {
        fun onSuccess(rules: List<ImportRule>)
        fun onError()
    }

    interface CreateCallback {
        fun onSuccess(importId: Int)
        fun onError()
    }

    interface SimpleCallback {
        fun onSuccess()
        fun onError()
    }

    interface ProcessCallback {
        fun onSuccess(response: JSONObject)
        fun onError()
    }

    @JvmStatic
    fun list(ctx: Context, cb: ListCallback) {
        DbCoroutine.io(
            block = { LocalRepository.getInstance(ctx).listImports() },
            onSuccess = cb::onSuccess,
            onError = cb::onError
        )
    }

    @JvmStatic
    fun listRules(ctx: Context, cb: RulesCallback) {
        DbCoroutine.io(
            block = { LocalRepository.getInstance(ctx).listImportRules() },
            onSuccess = cb::onSuccess,
            onError = cb::onError
        )
    }

    @JvmStatic
    fun create(ctx: Context, tipo: String, nombre: String, lineas: JSONArray?, cb: CreateCallback) {
        DbCoroutine.io(
            block = {
                LocalRepository.getInstance(ctx).createImport(
                    nombre,
                    tipo,
                    lineas?.toString() ?: "[]"
                )
            },
            onSuccess = cb::onSuccess,
            onError = cb::onError
        )
    }

    @JvmStatic
    fun saveRule(ctx: Context, rule: ImportRule, cb: SimpleCallback) {
        DbCoroutine.io(
            block = {
                LocalRepository.getInstance(ctx).saveImportRule(rule)
                Unit
            },
            onSuccess = { cb.onSuccess() },
            onError = cb::onError
        )
    }

    @JvmStatic
    fun deleteRule(ctx: Context, ruleId: Int, cb: SimpleCallback) {
        DbCoroutine.io(
            block = {
                LocalRepository.getInstance(ctx).deleteImportRule(ruleId)
                Unit
            },
            onSuccess = { cb.onSuccess() },
            onError = cb::onError
        )
    }

    @JvmStatic
    fun process(ctx: Context, importId: Int, cb: ProcessCallback) {
        DbCoroutine.io(
            block = { LocalRepository.getInstance(ctx).processImport(importId) },
            onSuccess = cb::onSuccess,
            onError = cb::onError
        )
    }
}
