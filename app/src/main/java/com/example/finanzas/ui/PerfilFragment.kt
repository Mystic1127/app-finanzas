package com.example.finanzas.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.finanzas.R
import com.example.finanzas.data.api.SettingsService
import com.example.finanzas.data.api.UserService
import com.example.finanzas.ui.compose.SpendlyComposeTheme
import com.example.finanzas.util.CurrencyConverter
import com.example.finanzas.util.PerfLogger
import com.example.finanzas.util.Prefs
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

class PerfilFragment : Fragment() {

    private var profileName by mutableStateOf("—")
    private var profileEmail by mutableStateOf("—")
    private var currencyLabel by mutableStateOf(CURRENCY_PEN)
    private var manualRateText by mutableStateOf("")
    private var initialCurrency by mutableStateOf("PEN")
    private var initialCashText by mutableStateOf("0")
    private var initialCardText by mutableStateOf("0")
    private var hasPin by mutableStateOf(false)

    private var currencyError by mutableStateOf<String?>(null)
    private var manualRateError by mutableStateOf<String?>(null)
    private var initialCashError by mutableStateOf<String?>(null)
    private var initialCardError by mutableStateOf<String?>(null)

    private var perfStartMs = 0L
    private var loadStartMs = 0L
    private var firstRenderLogged = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = ComposeView(requireContext()).apply {
        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        setContent {
            SpendlyComposeTheme {
                ProfileScreen(
                    name = profileName,
                    email = profileEmail,
                    currencyOptions = CURRENCY_OPTIONS,
                    currencyLabel = currencyLabel,
                    onCurrencyChange = {
                        currencyLabel = it
                        currencyError = null
                    },
                    manualRateText = manualRateText,
                    onManualRateChange = {
                        manualRateText = it
                        manualRateError = null
                    },
                    currencyError = currencyError,
                    manualRateError = manualRateError,
                    onSaveCurrency = { saveCurrency() },
                    initialCurrencyOptions = CurrencyConverter.supportedCurrencies(),
                    initialCurrency = initialCurrency,
                    onInitialCurrencyChange = { initialCurrency = CurrencyConverter.normalize(it) },
                    initialCashText = initialCashText,
                    onInitialCashChange = {
                        initialCashText = it
                        initialCashError = null
                    },
                    initialCardText = initialCardText,
                    onInitialCardChange = {
                        initialCardText = it
                        initialCardError = null
                    },
                    initialCashError = initialCashError,
                    initialCardError = initialCardError,
                    onSaveInitialBalances = { saveInitialBalances() },
                    hasPin = hasPin,
                    onChangePassword = { findNavController().navigate(R.id.nav_change_password) },
                    onConfigurePin = { findNavController().navigate(R.id.nav_pin_setup) },
                    onRemovePin = { confirmRemovePin() }
                )
            }
        }
    }

    override fun onViewCreated(v: View, savedInstanceState: Bundle?) {
        super.onViewCreated(v, savedInstanceState)
        perfStartMs = PerfLogger.now()
        loadStartMs = PerfLogger.now()
        firstRenderLogged = false

        profileName = Prefs.getCurrentUserName(requireContext()).takeUnless { it.isNullOrEmpty() } ?: "—"
        profileEmail = Prefs.getCurrentUserEmail(requireContext()).takeUnless { it.isNullOrEmpty() } ?: "—"
        hasPin = Prefs.hasPin(requireContext())

        v.post {
            if (!isAdded) return@post
            logFirstRender()
            loadProfileDetails()
            loadSettings()
        }
    }

    override fun onResume() {
        super.onResume()
        if (isAdded) hasPin = Prefs.hasPin(requireContext())
    }

    private fun loadProfileDetails() {
        val emailForLookup = profileEmail
        if (emailForLookup == "—") return

        val appContext = requireContext().applicationContext
        val scope = viewLifecycleOwner.lifecycleScope
        scope.launch(Dispatchers.IO) {
            UserService.getMe(appContext, emailForLookup, object : UserService.MeCb {
                override fun onOk(id: Int, nombre: String, email: String) {
                    scope.launch(Dispatchers.Main) {
                        if (!isAdded) return@launch
                        profileName = nombre
                        profileEmail = email
                        Prefs.setUserSession(requireContext(), id.toLong(), email, nombre)
                    }
                }

                override fun onFail() = Unit
            })
        }
    }

    private fun loadSettings() {
        val appContext = requireContext().applicationContext
        PerfLogger.log("PerfilFragment", "loadStart")
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            val code = SettingsService.getCurrencyCode(appContext)
            val rate = SettingsService.getManualRate(appContext)
            val balancesCurrency = SettingsService.getInitialBalancesCurrency(appContext)
            val cash = SettingsService.getInitialCashBalance(appContext)
            val card = SettingsService.getInitialCardBalance(appContext)

            withContext(Dispatchers.Main) {
                if (!isAdded) return@withContext
                currencyLabel = labelForCurrency(code)
                manualRateText = if (rate > 0) String.format(Locale.US, "%.4f", rate) else ""
                initialCurrency = CurrencyConverter.normalize(balancesCurrency)
                initialCashText = if (cash > 0) String.format(Locale.US, "%.2f", cash) else "0"
                initialCardText = if (card > 0) String.format(Locale.US, "%.2f", card) else "0"
                PerfLogger.logSince("PerfilFragment", "loadComplete", loadStartMs)
            }
        }
    }

    private fun saveCurrency() {
        currencyError = null
        manualRateError = null

        val code = codeFromLabel(currencyLabel)
        if (code.isEmpty()) {
            currencyError = getString(R.string.perfil_currency_hint)
            return
        }

        var rate = 0.0
        val rawRate = manualRateText.trim()
        if (rawRate.isNotEmpty()) {
            try {
                rate = rawRate.replace(",", ".").toDouble()
            } catch (_: NumberFormatException) {
                manualRateError = getString(R.string.error_monto_invalido)
                return
            }
        }

        SettingsService.saveCurrency(requireContext(), code, rate, object : SettingsService.SaveCb {
            override fun onSuccess() {
                if (isAdded) Toast.makeText(requireContext(), R.string.perfil_currency_saved, Toast.LENGTH_SHORT).show()
            }

            override fun onFail() {
                if (isAdded) Toast.makeText(requireContext(), R.string.perfil_currency_error, Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun saveInitialBalances() {
        initialCashError = null
        initialCardError = null

        val cash = parseAmount(initialCashText)
        val card = parseAmount(initialCardText)
        if (cash == null) {
            initialCashError = getString(R.string.error_monto_invalido)
            return
        }
        if (card == null) {
            initialCardError = getString(R.string.error_monto_invalido)
            return
        }

        val currency = CurrencyConverter.normalize(initialCurrency)
        SettingsService.saveInitialBalances(requireContext(), cash, card, currency, object : SettingsService.SaveCb {
            override fun onSuccess() {
                if (isAdded) Toast.makeText(requireContext(), R.string.perfil_initial_balances_saved, Toast.LENGTH_SHORT).show()
            }

            override fun onFail() {
                if (isAdded) Toast.makeText(requireContext(), R.string.perfil_initial_balances_error, Toast.LENGTH_SHORT).show()
            }
        })
        PerfLogger.logSince("PerfilFragment", "onViewCreated", perfStartMs)
    }

    private fun confirmRemovePin() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.perfil_remove_pin)
            .setMessage(R.string.pin_setup_remove_confirm)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                if (!isAdded) return@setPositiveButton
                Prefs.clearPin(requireContext())
                hasPin = Prefs.hasPin(requireContext())
                Toast.makeText(requireContext(), R.string.pin_removed_success, Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun logFirstRender() {
        if (!firstRenderLogged) {
            firstRenderLogged = true
            PerfLogger.logSince("PerfilFragment", "firstRender", perfStartMs)
        }
    }

    private fun labelForCurrency(code: String?): String = when (code) {
        "USD" -> CURRENCY_USD
        "EUR" -> CURRENCY_EUR
        else -> CURRENCY_PEN
    }

    private fun codeFromLabel(label: String?): String {
        val normalized = label?.trim()?.uppercase(Locale.ROOT) ?: return ""
        return when {
            normalized.startsWith("USD") -> "USD"
            normalized.startsWith("EUR") -> "EUR"
            normalized.startsWith("PEN") -> "PEN"
            else -> ""
        }
    }

    private fun parseAmount(raw: String?): Double? {
        if (raw == null) return 0.0
        var clean = raw.trim()
        if (clean.isEmpty()) return null
        clean = clean.replace("[^0-9,.-]".toRegex(), "")
        if (clean.isEmpty()) return null
        val lastComma = clean.lastIndexOf(',')
        val lastDot = clean.lastIndexOf('.')
        clean = if (lastComma >= 0 && lastDot >= 0) {
            if (lastComma > lastDot) clean.replace(".", "").replace(',', '.') else clean.replace(",", "")
        } else if (lastComma >= 0) {
            clean.replace(',', '.')
        } else {
            clean
        }
        return try {
            val value = clean.toDouble()
            if (value.isNaN() || value.isInfinite() || value < 0) null else value
        } catch (_: NumberFormatException) {
            null
        }
    }

    companion object {
        private const val CURRENCY_PEN = "PEN (S/)"
        private const val CURRENCY_USD = "USD ($)"
        private const val CURRENCY_EUR = "EUR (€)"
        private val CURRENCY_OPTIONS = listOf(CURRENCY_PEN, CURRENCY_USD, CURRENCY_EUR)
    }
}

@Composable
private fun ProfileScreen(
    name: String,
    email: String,
    currencyOptions: List<String>,
    currencyLabel: String,
    onCurrencyChange: (String) -> Unit,
    manualRateText: String,
    onManualRateChange: (String) -> Unit,
    currencyError: String?,
    manualRateError: String?,
    onSaveCurrency: () -> Unit,
    initialCurrencyOptions: List<String>,
    initialCurrency: String,
    onInitialCurrencyChange: (String) -> Unit,
    initialCashText: String,
    onInitialCashChange: (String) -> Unit,
    initialCardText: String,
    onInitialCardChange: (String) -> Unit,
    initialCashError: String?,
    initialCardError: String?,
    onSaveInitialBalances: () -> Unit,
    hasPin: Boolean,
    onChangePassword: () -> Unit,
    onConfigurePin: () -> Unit,
    onRemovePin: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .navigationBarsPadding()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            ProfileSection(title = "Cuenta") {
                Text(
                    text = name,
                    color = MaterialTheme.colorScheme.onSurface,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = email,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            ProfileSection(title = stringResource(R.string.perfil_preferences)) {
                Text(
                    text = stringResource(R.string.perfil_currency_title),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(12.dp))
                DropdownField(
                    label = stringResource(R.string.perfil_currency_hint),
                    value = currencyLabel,
                    options = currencyOptions,
                    onValueChange = onCurrencyChange,
                    error = currencyError
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = manualRateText,
                    onValueChange = onManualRateChange,
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text(stringResource(R.string.perfil_currency_rate_hint)) },
                    isError = manualRateError != null,
                    supportingText = manualRateError?.let { { Text(it) } },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )
                Button(
                    onClick = onSaveCurrency,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp)
                        .height(48.dp)
                ) {
                    Text(stringResource(R.string.perfil_currency_save))
                }
            }

            ProfileSection(title = stringResource(R.string.perfil_initial_balances_title)) {
                Text(
                    text = stringResource(R.string.perfil_initial_balances_subtitle),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(12.dp))
                DropdownField(
                    label = stringResource(R.string.transaction_currency),
                    value = initialCurrency,
                    options = initialCurrencyOptions,
                    onValueChange = onInitialCurrencyChange
                )
                Spacer(modifier = Modifier.height(12.dp))
                val symbol = SettingsService.getCurrencySymbol(initialCurrency)
                OutlinedTextField(
                    value = initialCashText,
                    onValueChange = onInitialCashChange,
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text("${stringResource(R.string.perfil_initial_cash_hint)} ($symbol)") },
                    isError = initialCashError != null,
                    supportingText = initialCashError?.let { { Text(it) } },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = initialCardText,
                    onValueChange = onInitialCardChange,
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text("${stringResource(R.string.perfil_initial_card_hint)} ($symbol)") },
                    isError = initialCardError != null,
                    supportingText = initialCardError?.let { { Text(it) } },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )
                Button(
                    onClick = onSaveInitialBalances,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp)
                        .height(48.dp)
                ) {
                    Text(stringResource(R.string.perfil_initial_balances_save))
                }
            }

            ProfileSection(title = stringResource(R.string.perfil_security)) {
                OutlinedButton(
                    onClick = onChangePassword,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    Text(stringResource(R.string.perfil_change_password))
                }
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedButton(
                    onClick = onConfigurePin,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    Text(stringResource(R.string.perfil_config_pin))
                }
                if (hasPin) {
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedButton(
                        onClick = onRemovePin,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                    ) {
                        Text(stringResource(R.string.perfil_remove_pin))
                    }
                }
            }
        }
    }
}

@Composable
private fun ProfileSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = title,
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(12.dp))
            content()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DropdownField(
    label: String,
    value: String,
    options: List<String>,
    onValueChange: (String) -> Unit,
    error: String? = null
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded }
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = {},
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(),
            readOnly = true,
            singleLine = true,
            label = { Text(label) },
            isError = error != null,
            supportingText = error?.let { { Text(it) } },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) }
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option) },
                    onClick = {
                        onValueChange(option)
                        expanded = false
                    }
                )
            }
        }
    }
}
