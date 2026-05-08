package com.example.finanzas.macrobenchmark

import android.content.ComponentName
import android.content.Intent
import androidx.benchmark.macro.MacrobenchmarkScope
import androidx.test.uiautomator.By
import androidx.test.uiautomator.BySelector
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiObject2
import androidx.test.uiautomator.Until

internal const val TARGET_PACKAGE = "com.example.finanzas"

private const val EXTRA_BENCHMARK_SEED_SESSION =
    "com.example.finanzas.extra.BENCHMARK_SEED_SESSION"
private const val DEFAULT_TIMEOUT_MS = 12_000L

internal fun MacrobenchmarkScope.prepareLoggedInState() {
    device.executeShellCommand("am force-stop $TARGET_PACKAGE")
    grantOptionalPermissions()
    pressHome()
    startActivityAndWait(launchIntent(seedSession = true))
    device.dismissPermissionDialogIfPresent()
    check(device.waitForHome(DEFAULT_TIMEOUT_MS)) {
        "No se pudo llegar a Inicio. Revisa si el flujo de sesión cambió."
    }
    pressHome()
}

internal fun MacrobenchmarkScope.startAndWaitForHome() {
    startActivityAndWait(launchIntent(seedSession = false))
    device.dismissPermissionDialogIfPresent()
    device.waitForHomeOrThrow()
}

internal fun MacrobenchmarkScope.navigateMainSections() {
    startAndWaitForHome()
    device.openTransactionsFromHome()
    device.scrollTransactionsIfAvailable()
    device.pressBack()
    device.waitForHomeOrThrow()
    device.clickRes("bottomNavNew")
    device.waitForTextOrThrow("Nueva transacción")
    device.clickRes("bottomNavAnalysis")
    device.waitForTextOrThrow("Análisis")
    device.clickRes("bottomNavBudget")
    device.waitForTextOrThrow("Presupuesto")
    device.clickRes("bottomNavHome")
    device.waitForHomeOrThrow()
}

internal fun MacrobenchmarkScope.openTransactionsAndScroll() {
    startAndWaitForHome()
    device.openTransactionsFromHome()
    device.scrollTransactionsIfAvailable()
}

private fun MacrobenchmarkScope.grantOptionalPermissions() {
    try {
        device.executeShellCommand("pm grant $TARGET_PACKAGE android.permission.POST_NOTIFICATIONS")
    } catch (_: Exception) {
        // Permission may not exist on older API levels or may already be decided.
    }
}

private fun launchIntent(seedSession: Boolean): Intent {
    return Intent(Intent.ACTION_MAIN)
        .setComponent(ComponentName(TARGET_PACKAGE, "$TARGET_PACKAGE.ui.MainActivity"))
        .addCategory(Intent.CATEGORY_LAUNCHER)
        .apply {
            if (seedSession) putExtra(EXTRA_BENCHMARK_SEED_SESSION, true)
        }
}

internal fun UiDevice.openTransactionsFromHome() {
    waitForHomeOrThrow()
    val shortcut = waitForObject(By.res(TARGET_PACKAGE, "btnHomeSeeTransactions"), DEFAULT_TIMEOUT_MS)
    shortcut.click()
    waitForObject(By.res(TARGET_PACKAGE, "listView"), DEFAULT_TIMEOUT_MS)
}

internal fun UiDevice.scrollTransactionsIfAvailable() {
    val list = waitForObject(By.res(TARGET_PACKAGE, "listView"), DEFAULT_TIMEOUT_MS)
    list.setGestureMargin(displayWidth / 8)
    repeat(4) {
        list.swipe(androidx.test.uiautomator.Direction.UP, 0.72f)
        waitForIdle()
    }
    repeat(2) {
        list.swipe(androidx.test.uiautomator.Direction.DOWN, 0.72f)
        waitForIdle()
    }
}

internal fun UiDevice.waitForHome(timeoutMs: Long = DEFAULT_TIMEOUT_MS): Boolean {
    return wait(Until.hasObject(By.text("Inicio")), timeoutMs)
}

internal fun UiDevice.waitForHomeOrThrow() {
    check(waitForHome()) { "No apareció la pantalla Inicio dentro del tiempo esperado." }
}

internal fun UiDevice.clickRes(resourceId: String) {
    waitForObject(By.res(TARGET_PACKAGE, resourceId), DEFAULT_TIMEOUT_MS).click()
    waitForIdle()
}

internal fun UiDevice.clickText(text: String) {
    waitForTextOrThrow(text).click()
    waitForIdle()
}

internal fun UiDevice.hasText(text: String): Boolean {
    return wait(Until.hasObject(By.text(text)), 3_000L)
}

internal fun UiDevice.waitForTextOrThrow(text: String): UiObject2 {
    return waitForObject(By.text(text), DEFAULT_TIMEOUT_MS)
}

private fun UiDevice.waitForObject(selector: BySelector, timeoutMs: Long): UiObject2 {
    wait(Until.hasObject(selector), timeoutMs)
    return findObject(selector) ?: error("No se encontró el selector: $selector")
}

private fun UiDevice.dismissPermissionDialogIfPresent() {
    val allowSelector = By.res("com.android.permissioncontroller", "permission_allow_button")
    wait(Until.hasObject(allowSelector), 3_000L)
    val allow = findObject(allowSelector)
    if (allow != null) {
        allow.click()
        waitForIdle()
        return
    }

    val deny = findObject(By.res("com.android.permissioncontroller", "permission_deny_button"))
    if (deny != null) {
        deny.click()
        waitForIdle()
    }
}
