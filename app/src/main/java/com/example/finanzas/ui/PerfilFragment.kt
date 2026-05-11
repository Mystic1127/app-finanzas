package com.example.finanzas.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.finanzas.R
import com.example.finanzas.data.api.AuthService
import com.example.finanzas.data.api.CategoryStore
import com.example.finanzas.data.api.SettingsService
import com.example.finanzas.data.api.UserService
import com.example.finanzas.data.cloud.CloudSyncService
import com.example.finanzas.data.local.LocalRepository
import com.example.finanzas.ui.compose.SpendlyComposeTheme
import com.example.finanzas.ui.view.SpendlyDecorBackgroundDrawable
import com.example.finanzas.ui.viewmodel.BudgetViewModel
import com.example.finanzas.ui.viewmodel.HomeViewModel
import com.example.finanzas.ui.viewmodel.ReportsViewModel
import com.example.finanzas.ui.viewmodel.TransactionsViewModel
import com.example.finanzas.util.CurrencyConverter
import com.example.finanzas.util.NavigationAnimations
import com.example.finanzas.util.PerfLogger
import com.example.finanzas.util.Prefs
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.Locale

class PerfilFragment : Fragment() {

    private var profileName by mutableStateOf("—")
    private var profileEmail by mutableStateOf("—")
    private var currencyLabel by mutableStateOf(CURRENCY_PEN)
    private var manualRateText by mutableStateOf("")
    private var initialCurrency by mutableStateOf("PEN")
    private var initialCashText by mutableStateOf("")
    private var initialCardText by mutableStateOf("")
    private var initialBalancesConfigured by mutableStateOf(false)
    private var themeMode by mutableStateOf(SettingsService.THEME_SYSTEM)
    private var hasPin by mutableStateOf(false)
    private var currentUserId by mutableStateOf(-1L)
    private var accounts by mutableStateOf<List<AccountUi>>(emptyList())
    private var googleLinked by mutableStateOf(false)
    private var googleLinkEmail by mutableStateOf("")
    private var googleLinking by mutableStateOf(false)
    private var googleLinkMessage by mutableStateOf<String?>(null)

    private var currencyError by mutableStateOf<String?>(null)
    private var manualRateError by mutableStateOf<String?>(null)
    private var initialCashError by mutableStateOf<String?>(null)
    private var initialCardError by mutableStateOf<String?>(null)

    private var perfStartMs = 0L
    private var loadStartMs = 0L
    private var firstRenderLogged = false

    private val googleLinkLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        viewLifecycleOwner.lifecycleScope.launch {
            runCatching {
                GoogleSignIn.getSignedInAccountFromIntent(result.data).await()
            }.onSuccess { account ->
                googleLinking = false
                showGoogleLinkConfirmation(account)
            }.onFailure {
                googleLinking = false
                val messageRes = if (result.data == null) {
                    R.string.perfil_google_link_cancelled
                } else {
                    R.string.perfil_google_link_error
                }
                googleLinkMessage = getString(messageRes)
                if (isAdded) Toast.makeText(requireContext(), messageRes, Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val host = FrameLayout(requireContext()).apply {
            background = SpendlyDecorBackgroundDrawable(requireContext())
        }
        val composeView = ComposeView(requireContext()).apply {
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
                        initialBalancesConfigured = initialBalancesConfigured,
                        initialCashError = initialCashError,
                        initialCardError = initialCardError,
                        onSaveInitialBalances = { saveInitialBalances() },
                        themeMode = themeMode,
                        onThemeModeChange = { saveThemeMode(it) },
                        hasPin = hasPin,
                        currentUserId = currentUserId,
                        accounts = accounts,
                        googleLinked = googleLinked,
                        googleLinkEmail = googleLinkEmail,
                        googleLinking = googleLinking,
                        googleLinkMessage = googleLinkMessage,
                        onLinkGoogle = {
                            if (googleLinked) syncLinkedGoogleNow() else startGoogleLink()
                        },
                        onSwitchAccount = { switchAccount(it) },
                        onBack = { findNavController().popBackStack() },
                        onChangePassword = {
                            findNavController().navigate(
                                R.id.nav_change_password,
                                null,
                                NavigationAnimations.detailSlide()
                            )
                        },
                        onConfigurePin = {
                            findNavController().navigate(
                                R.id.nav_pin_setup,
                                null,
                                NavigationAnimations.detailSlide()
                            )
                        },
                        onRemovePin = { confirmRemovePin() },
                        onDeleteFinancialData = { confirmDeleteFinancialData() }
                    )
                }
            }
        }
        host.addView(
            composeView,
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        )
        return host
    }

    override fun onViewCreated(v: View, savedInstanceState: Bundle?) {
        super.onViewCreated(v, savedInstanceState)
        perfStartMs = PerfLogger.now()
        loadStartMs = PerfLogger.now()
        firstRenderLogged = false

        profileName = Prefs.getCurrentUserName(requireContext()).takeUnless { it.isNullOrEmpty() } ?: "—"
        profileEmail = Prefs.getCurrentUserEmail(requireContext()).takeUnless { it.isNullOrEmpty() } ?: "—"
        currentUserId = Prefs.getCurrentUserId(requireContext())
        hasPin = Prefs.hasPin(requireContext())
        refreshGoogleLinkState()

        v.post {
            if (!isAdded) return@post
            logFirstRender()
            loadProfileDetails()
            loadSettings()
            loadAccounts()
        }
    }

    override fun onResume() {
        super.onResume()
        if (isAdded) {
            hasPin = Prefs.hasPin(requireContext())
            refreshGoogleLinkState()
        }
    }

    private fun loadAccounts() {
        val appContext = requireContext().applicationContext
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            val remembered = Prefs.getRememberedUserIds(appContext)
            val users = LocalRepository.getInstance(appContext).listUsers()
            val mapped = users.filter { remembered.contains(it.id.toLong()) }.map {
                val firebaseEmail = Prefs.getFirebaseEmailForUser(appContext, it.id.toLong())
                AccountUi(
                    id = it.id.toLong(),
                    name = it.nombre ?: "",
                    email = firebaseEmail.ifBlank { it.email ?: "" }
                )
            }
            withContext(Dispatchers.Main) {
                if (!isAdded) return@withContext
                currentUserId = Prefs.getCurrentUserId(requireContext())
                refreshGoogleLinkState()
                accounts = mapped
            }
        }
    }

    private fun switchAccount(account: AccountUi) {
        if (account.id <= 0 || account.id == Prefs.getCurrentUserId(requireContext())) return
        (activity as? MainActivity)?.switchToUser(account.id, account.email, account.name)
    }

    private fun refreshGoogleLinkState() {
        if (!isAdded) return
        val ctx = requireContext().applicationContext
        val userId = Prefs.getCurrentUserId(ctx)
        val uid = Prefs.getFirebaseUidForUser(ctx, userId)
        googleLinked = !uid.isNullOrBlank()
        googleLinkEmail = Prefs.getFirebaseEmailForUser(ctx, userId)
    }

    private fun startGoogleLink() {
        if (googleLinking) return
        googleLinking = true
        googleLinkMessage = null

        val options = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        val googleClient = GoogleSignIn.getClient(requireActivity(), options)
        viewLifecycleOwner.lifecycleScope.launch {
            runCatching { googleClient.signOut().await() }
                .onSuccess {
                    if (!isAdded) {
                        googleLinking = false
                        return@onSuccess
                    }
                    googleLinkLauncher.launch(googleClient.signInIntent)
                }
                .onFailure {
                    googleLinking = false
                    googleLinkMessage = getString(R.string.perfil_google_link_error)
                    if (isAdded) Toast.makeText(requireContext(), R.string.perfil_google_link_error, Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun syncLinkedGoogleNow() {
        if (googleLinking) return
        val appContext = requireContext().applicationContext
        val userId = Prefs.getCurrentUserId(appContext)
        val linkedUid = Prefs.getFirebaseUidForUser(appContext, userId)
        val currentFirebaseUid = FirebaseAuth.getInstance().currentUser?.uid
        if (linkedUid.isNullOrBlank() || linkedUid != currentFirebaseUid) {
            googleLinkMessage = getString(R.string.perfil_google_sync_login_required)
            Toast.makeText(requireContext(), R.string.perfil_google_sync_login_required, Toast.LENGTH_LONG).show()
            return
        }

        googleLinking = true
        googleLinkMessage = null
        Prefs.setToken(appContext, "firebase:$linkedUid")
        viewLifecycleOwner.lifecycleScope.launch {
            val result = runCatching { CloudSyncService.uploadNow(appContext) }
                .getOrElse { CloudSyncService.Result(false, message = it.message.orEmpty()) }
            if (!isAdded) return@launch
            googleLinking = false
            googleLinkMessage = if (result.ok) {
                getString(R.string.perfil_google_sync_success)
            } else {
                result.message.ifBlank { getString(R.string.perfil_google_link_error) }
            }
            Toast.makeText(requireContext(), googleLinkMessage, if (result.ok) Toast.LENGTH_SHORT else Toast.LENGTH_LONG).show()
        }
    }

    private fun showGoogleLinkConfirmation(account: GoogleSignInAccount) {
        val email = account.email.orEmpty()
        if (email.isBlank() || account.idToken.isNullOrBlank()) {
            googleLinkMessage = getString(R.string.perfil_google_link_error)
            Toast.makeText(requireContext(), R.string.perfil_google_link_error, Toast.LENGTH_SHORT).show()
            clearGoogleLinkCache()
            return
        }
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.perfil_google_link_confirm_title)
            .setMessage(getString(R.string.perfil_google_link_confirm_message, email))
            .setNegativeButton(android.R.string.cancel) { _, _ ->
                googleLinkMessage = getString(R.string.perfil_google_link_cancelled)
                clearGoogleLinkCache()
            }
            .setPositiveButton(R.string.perfil_google_link_confirm_action) { _, _ ->
                authenticateConfirmedGoogleAccount(account)
            }
            .setOnCancelListener {
                googleLinkMessage = getString(R.string.perfil_google_link_cancelled)
                clearGoogleLinkCache()
            }
            .show()
    }

    private fun authenticateConfirmedGoogleAccount(account: GoogleSignInAccount) {
        val idToken = account.idToken
        if (idToken.isNullOrBlank()) {
            googleLinkMessage = getString(R.string.perfil_google_link_error)
            Toast.makeText(requireContext(), R.string.perfil_google_link_error, Toast.LENGTH_SHORT).show()
            clearGoogleLinkCache()
            return
        }
        googleLinking = true
        viewLifecycleOwner.lifecycleScope.launch {
            runCatching {
                val credential = GoogleAuthProvider.getCredential(idToken, null)
                FirebaseAuth.getInstance().signInWithCredential(credential).await().user
                    ?: error("Firebase no devolvio usuario")
            }.onSuccess { firebaseUser ->
                completeGoogleLink(firebaseUser)
            }.onFailure {
                googleLinking = false
                googleLinkMessage = getString(R.string.perfil_google_link_error)
                if (isAdded) Toast.makeText(requireContext(), R.string.perfil_google_link_error, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun clearGoogleLinkCache() {
        val options = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        GoogleSignIn.getClient(requireActivity(), options).signOut()
    }

    private fun completeGoogleLink(firebaseUser: FirebaseUser) {
        viewLifecycleOwner.lifecycleScope.launch {
            val result = runCatching {
                AuthService.linkCurrentLocalUserWithGoogle(requireContext(), firebaseUser)
            }.getOrElse {
                CloudSyncService.Result(false, message = getString(R.string.perfil_google_link_error))
            }
            if (!isAdded) return@launch
            googleLinking = false
            if (result.ok) {
                refreshGoogleLinkState()
                googleLinkMessage = getString(R.string.perfil_google_link_success)
                Toast.makeText(requireContext(), R.string.perfil_google_link_success, Toast.LENGTH_SHORT).show()
                return@launch
            }

            if (result.message == CloudSyncService.MESSAGE_REMOTE_HAS_DATA) {
                googleLinkMessage = getString(R.string.perfil_google_link_cloud_conflict_short)
                showGoogleCloudConflict()
            } else {
                googleLinkMessage = result.message.ifBlank { getString(R.string.perfil_google_link_error) }
                Toast.makeText(requireContext(), googleLinkMessage, Toast.LENGTH_LONG).show()
            }
            refreshGoogleLinkState()
        }
    }

    private fun showGoogleCloudConflict() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.perfil_google_link_cloud_conflict_title)
            .setMessage(R.string.perfil_google_link_cloud_conflict_message)
            .setPositiveButton(android.R.string.ok, null)
            .show()
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
            val repository = LocalRepository.getInstance(appContext)
            val configured = repository.hasInitialBalanceConfigured()
            val code = SettingsService.getCurrencyCode(appContext)
            val rate = SettingsService.getManualRate(appContext)
            val balancesCurrency = SettingsService.getInitialBalancesCurrency(appContext)
            val cash = SettingsService.getInitialCashBalance(appContext)
            val card = SettingsService.getInitialCardBalance(appContext)
            val mode = SettingsService.getThemeMode(appContext)

            withContext(Dispatchers.Main) {
                if (!isAdded) return@withContext
                currencyLabel = labelForCurrency(code)
                manualRateText = if (rate > 0) String.format(Locale.US, "%.4f", rate) else ""
                initialCurrency = CurrencyConverter.normalize(balancesCurrency)
                initialBalancesConfigured = configured
                initialCashText = if (!configured && cash > 0) String.format(Locale.US, "%.2f", cash) else ""
                initialCardText = if (!configured && card > 0) String.format(Locale.US, "%.2f", card) else ""
                themeMode = mode
                currentUserId = Prefs.getCurrentUserId(appContext)
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
        if (initialBalancesConfigured) {
            Toast.makeText(requireContext(), R.string.perfil_initial_balances_locked_help, Toast.LENGTH_SHORT).show()
            return
        }

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
        if (cash <= 0.0 && card <= 0.0) {
            initialCashError = getString(R.string.perfil_initial_balances_empty)
            initialCardError = getString(R.string.perfil_initial_balances_empty)
            return
        }

        val currency = CurrencyConverter.normalize(initialCurrency)
        val appContext = requireContext().applicationContext
        viewLifecycleOwner.lifecycleScope.launch {
            val saved = withContext(Dispatchers.IO) {
                runCatching {
                    LocalRepository.getInstance(appContext).configureInitialBalances(cash, card, currency)
                }.getOrDefault(false)
            }
            if (!isAdded) return@launch
            if (saved) {
                CategoryStore.clearCache()
                initialBalancesConfigured = true
                initialCashText = ""
                initialCardText = ""
                clearScopedViewModelCaches()
                Toast.makeText(requireContext(), R.string.perfil_initial_balances_saved, Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(requireContext(), R.string.perfil_initial_balances_error, Toast.LENGTH_SHORT).show()
                loadSettings()
            }
        }
        PerfLogger.logSince("PerfilFragment", "onViewCreated", perfStartMs)
    }

    private fun saveThemeMode(mode: String) {
        val normalized = when (mode) {
            SettingsService.THEME_LIGHT -> SettingsService.THEME_LIGHT
            SettingsService.THEME_DARK -> SettingsService.THEME_DARK
            else -> SettingsService.THEME_SYSTEM
        }
        themeMode = normalized
        SettingsService.saveThemeMode(requireContext(), normalized)
        SettingsService.applyThemeMode(requireContext())
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

    private fun confirmDeleteFinancialData() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.perfil_delete_financial_data_title)
            .setMessage(R.string.perfil_delete_financial_data_confirm)
            .setNegativeButton(android.R.string.cancel, null)
            .setPositiveButton(R.string.perfil_delete_financial_data_action) { _, _ -> deleteFinancialData() }
            .show()
    }

    private fun deleteFinancialData() {
        val appContext = requireContext().applicationContext
        viewLifecycleOwner.lifecycleScope.launch {
            val deleted = withContext(Dispatchers.IO) {
                runCatching { LocalRepository.getInstance(appContext).deleteCurrentUserFinancialData() }
                    .getOrDefault(false)
            }
            if (!isAdded) return@launch
            if (deleted) {
                CategoryStore.clearCache()
                clearScopedViewModelCaches()
                initialBalancesConfigured = false
                initialCashText = ""
                initialCardText = ""
                initialCashError = null
                initialCardError = null
                Toast.makeText(requireContext(), R.string.perfil_financial_data_deleted, Toast.LENGTH_SHORT).show()
                loadSettings()
            } else {
                Toast.makeText(requireContext(), R.string.perfil_financial_data_delete_error, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun clearScopedViewModelCaches() {
        val provider = ViewModelProvider(requireActivity())
        provider.get(HomeViewModel::class.java).clearCache()
        provider.get(TransactionsViewModel::class.java).clearCache()
        provider.get(ReportsViewModel::class.java).clearCache()
        provider.get(BudgetViewModel::class.java).clearCache()
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
        "CLP" -> CURRENCY_CLP
        else -> CURRENCY_PEN
    }

    private fun codeFromLabel(label: String?): String {
        val normalized = label?.trim()?.uppercase(Locale.ROOT) ?: return ""
        return when {
            normalized.startsWith("USD") -> "USD"
            normalized.startsWith("EUR") -> "EUR"
            normalized.startsWith("CLP") -> "CLP"
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
        private const val CURRENCY_CLP = "CLP (CLP$)"
        private val CURRENCY_OPTIONS = listOf(CURRENCY_PEN, CURRENCY_USD, CURRENCY_EUR, CURRENCY_CLP)
    }
}

private data class AccountUi(
    val id: Long,
    val name: String,
    val email: String
)

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
    initialBalancesConfigured: Boolean,
    initialCashError: String?,
    initialCardError: String?,
    onSaveInitialBalances: () -> Unit,
    themeMode: String,
    onThemeModeChange: (String) -> Unit,
    hasPin: Boolean,
    currentUserId: Long,
    accounts: List<AccountUi>,
    googleLinked: Boolean,
    googleLinkEmail: String,
    googleLinking: Boolean,
    googleLinkMessage: String?,
    onLinkGoogle: () -> Unit,
    onSwitchAccount: (AccountUi) -> Unit,
    onBack: () -> Unit,
    onChangePassword: () -> Unit,
    onConfigurePin: () -> Unit,
    onRemovePin: () -> Unit,
    onDeleteFinancialData: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color.Transparent
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(start = 22.dp, top = 24.dp, end = 22.dp, bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            ProfileHeader(onBack = onBack)

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
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 14.dp),
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
                )
                Text(
                    text = stringResource(R.string.perfil_google_link_title),
                    color = MaterialTheme.colorScheme.onSurface,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = if (googleLinked) {
                        googleLinkEmail.takeIf { it.isNotBlank() } ?: stringResource(R.string.perfil_google_linked)
                    } else {
                        stringResource(R.string.perfil_google_link_subtitle)
                    },
                    color = if (googleLinked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 4.dp)
                )
                googleLinkMessage?.takeIf { it.isNotBlank() }?.let {
                    Text(
                        text = it,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 6.dp)
                    )
                }
                OutlinedButton(
                    onClick = onLinkGoogle,
                    enabled = !googleLinking,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp)
                        .height(48.dp),
                    shape = RoundedCornerShape(16.dp),
                    border = profileControlBorder()
                ) {
                    Text(
                        when {
                            googleLinking && googleLinked -> stringResource(R.string.perfil_google_syncing)
                            googleLinking -> stringResource(R.string.perfil_google_linking)
                            googleLinked -> stringResource(R.string.perfil_google_link_retry)
                            else -> stringResource(R.string.perfil_google_link_button)
                        }
                    )
                }
            }

            ProfileSection(title = stringResource(R.string.profile_accounts_title)) {
                val otherAccounts = accounts.filter { it.id != currentUserId }
                if (otherAccounts.isEmpty()) {
                    Text(
                        text = stringResource(R.string.profile_accounts_empty),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                accounts.forEachIndexed { index, account ->
                    if (index > 0) {
                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 12.dp),
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = account.name.ifBlank { stringResource(R.string.profile_account_without_name) },
                                color = MaterialTheme.colorScheme.onSurface,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = account.email,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.bodySmall
                            )
                            if (account.id == currentUserId) {
                                Text(
                                    text = stringResource(R.string.profile_account_current),
                                    color = MaterialTheme.colorScheme.primary,
                                    style = MaterialTheme.typography.labelMedium,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }
                        }
                        if (account.id != currentUserId) {
                            OutlinedButton(
                                onClick = { onSwitchAccount(account) },
                                shape = RoundedCornerShape(16.dp),
                                border = profileControlBorder()
                            ) {
                                Text(stringResource(R.string.profile_account_switch))
                            }
                        }
                    }
                }
            }

            ProfileSection(title = stringResource(R.string.perfil_financial_data_title)) {
                Text(
                    text = stringResource(R.string.perfil_financial_data_subtitle),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium
                )
                OutlinedButton(
                    onClick = onDeleteFinancialData,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp)
                        .height(48.dp),
                    shape = RoundedCornerShape(16.dp),
                    border = profileControlBorder()
                ) {
                    Text(stringResource(R.string.perfil_delete_financial_data_title))
                }
            }

            ProfileSection(title = stringResource(R.string.perfil_security)) {
                OutlinedButton(
                    onClick = onChangePassword,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(16.dp),
                    border = profileControlBorder()
                ) {
                    Text(stringResource(R.string.perfil_change_password))
                }
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedButton(
                    onClick = onConfigurePin,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(16.dp),
                    border = profileControlBorder()
                ) {
                    Text(stringResource(R.string.perfil_config_pin))
                }
                if (hasPin) {
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedButton(
                        onClick = onRemovePin,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = RoundedCornerShape(16.dp),
                        border = profileControlBorder()
                    ) {
                        Text(stringResource(R.string.perfil_remove_pin))
                    }
                }
            }
        }
    }
}

@Composable
private fun ProfileHeader(onBack: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp)
    ) {
        IconButton(
            onClick = onBack,
            modifier = Modifier
                .align(Alignment.CenterStart)
                .size(44.dp)
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_arrow_back),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(24.dp)
            )
        }
        Text(
            text = stringResource(R.string.nav_profile_title),
            color = MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.titleLarge,
            fontSize = 22.sp,
            lineHeight = 26.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.align(Alignment.Center)
        )
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
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
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
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            shape = RoundedCornerShape(16.dp),
            colors = profileFieldColors()
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            shape = RoundedCornerShape(18.dp),
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option, color = MaterialTheme.colorScheme.onSurface) },
                    onClick = {
                        onValueChange(option)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun profilePrimaryButtonColors() = ButtonDefaults.buttonColors(
    containerColor = MaterialTheme.colorScheme.primary,
    contentColor = MaterialTheme.colorScheme.onPrimary
)

@Composable
private fun profileControlBorder() = BorderStroke(
    width = 1.dp,
    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.65f)
)

@Composable
private fun profileFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = MaterialTheme.colorScheme.primary,
    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.65f),
    focusedLabelColor = MaterialTheme.colorScheme.primary,
    unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
    cursorColor = MaterialTheme.colorScheme.primary,
    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.38f),
    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.38f),
    errorContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.38f),
    focusedTextColor = MaterialTheme.colorScheme.onSurface,
    unfocusedTextColor = MaterialTheme.colorScheme.onSurface
)
