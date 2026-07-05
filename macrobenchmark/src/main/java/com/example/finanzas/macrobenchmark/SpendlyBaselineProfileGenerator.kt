package com.example.finanzas.macrobenchmark

import androidx.benchmark.macro.junit4.BaselineProfileRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SpendlyBaselineProfileGenerator {
    @get:Rule
    val baselineProfileRule = BaselineProfileRule()

    @Test
    fun generateBaselineProfile() = baselineProfileRule.collect(
        packageName = TARGET_PACKAGE
    ) {
        prepareLoggedInState()
        startAndWaitForHome()
        device.openTransactionsFromHome()
        device.scrollTransactionsIfAvailable()
        device.pressBack()
        device.waitForHomeOrThrow()
        device.clickRes("bottomNavNew")
        device.waitForResOrThrow("etMonto")
        device.clickRes("bottomNavAnalysis")
        device.waitForResOrThrow("swipeAnalysis")
        device.clickRes("bottomNavBudget")
        device.waitForResOrThrow("etPresupuesto")
        device.clickRes("bottomNavHome")
        device.waitForHomeOrThrow()
    }
}
