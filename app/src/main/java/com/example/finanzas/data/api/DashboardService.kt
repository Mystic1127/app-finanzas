package com.example.finanzas.data.api

import android.content.Context
import com.example.finanzas.data.local.LocalRepository
import com.example.finanzas.data.model.HomeSummary

object DashboardService {

    interface SummaryCb {
        fun onOk(summary: HomeSummary)
        fun onError()
    }

    @JvmStatic
    fun getSummary(ctx: Context, anio: Int, mes: Int, cb: SummaryCb) {
        DbCoroutine.io(
            block = { LocalRepository.getInstance(ctx).buildHomeSummary(anio, mes) },
            onSuccess = cb::onOk,
            onError = cb::onError
        )
    }
}
