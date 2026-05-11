package com.example.finanzas.ui

import android.os.Bundle
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.Fragment
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import com.example.finanzas.R
import com.example.finanzas.data.api.AuthService
import com.example.finanzas.data.cloud.CloudSyncService
import com.example.finanzas.data.local.LocalRepository
import com.example.finanzas.ui.compose.SpendlyAuthCard
import com.example.finanzas.ui.compose.SpendlyAuthField
import com.example.finanzas.ui.compose.SpendlyAuthScreenContainer
import com.example.finanzas.ui.compose.SpendlyBrandTitle
import com.example.finanzas.ui.compose.SpendlyComposeTheme
import com.example.finanzas.ui.compose.SpendlyDividerDot
import com.example.finanzas.ui.compose.SpendlyGoogleButton
import com.example.finanzas.ui.compose.SpendlyLogoMark
import com.example.finanzas.ui.compose.SpendlyPasswordField
import com.example.finanzas.ui.compose.SpendlyPrimaryButton
import com.example.finanzas.ui.compose.SpendlyTopBar
import com.example.finanzas.ui.compose.spendlyAuthColors
import com.example.finanzas.util.DeviceAuthHelper
import com.example.finanzas.util.Prefs
import com.example.finanzas.util.RecurringTransactionStore
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class LoginFragment : Fragment() {
    private var googleLoading: ((Boolean) -> Unit)? = null

    private val googleSignInLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val setLoading = googleLoading
        viewLifecycleOwner.lifecycleScope.launch {
            runCatching {
                val account = GoogleSignIn.getSignedInAccountFromIntent(result.data).await()
                val credential = GoogleAuthProvider.getCredential(account.idToken, null)
                FirebaseAuth.getInstance().signInWithCredential(credential).await().user
                    ?: error("Firebase no devolvió usuario")
            }.onSuccess { user ->
                completeGoogleLogin(user, setLoading)
            }.onFailure { error ->
                setLoading?.invoke(false)
                if (isAdded) {
                    val message = if (error is FirebaseAuthUserCollisionException) {
                        "Este correo ya esta registrado. Inicia sesion con correo y contrasena."
                    } else {
                        "No se pudo iniciar sesion con Google"
                    }
                    Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = ComposeView(requireContext()).apply {
        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        setContent {
            SpendlyComposeTheme {
                LoginScreen(
                    onBackClick = { findNavController().popBackStack() },
                    onForgotPasswordClick = ::recoverPassword,
                    onLogin = { email, pass, setLoading ->
                        login(email, pass, setLoading)
                    },
                    onGoogleLogin = { setLoading ->
                        loginWithGoogle(setLoading)
                    },
                    onRegisterClick = {
                        findNavController().navigate(R.id.nav_register)
                    }
                )
            }
        }
    }

    override fun onViewCreated(v: View, savedInstanceState: Bundle?) {
        super.onViewCreated(v, savedInstanceState)

        if (Prefs.isLoggedIn(requireContext())) {
            val opts = NavOptions.Builder().setPopUpTo(R.id.nav_graph, true).build()
            findNavController().navigate(R.id.nav_home, null, opts)
        }
    }

    private fun login(
        emailRaw: String,
        passRaw: String,
        setLoading: (Boolean) -> Unit
    ) {
        val email = emailRaw.trim()
        val pass = passRaw.trim()

        if (!hasInternet()) {
            Toast.makeText(requireContext(), R.string.auth_internet_required, Toast.LENGTH_SHORT).show()
            setLoading(false)
            return
        }
        if (email.isEmpty() || pass.isEmpty()) {
            Toast.makeText(requireContext(), "Completa email y contraseña", Toast.LENGTH_SHORT).show()
            setLoading(false)
            return
        }

        setLoading(true)

        viewLifecycleOwner.lifecycleScope.launch {
            val result = AuthService.login(requireContext(), email, pass)
            if (!isAdded) return@launch

            if (result != null) {
                Prefs.setToken(requireContext(), result.token)
                Prefs.setUserSession(
                    requireContext(),
                    if (result.userId > 0) result.userId.toLong() else 1L,
                    result.email,
                    result.nombre
                )
                RecurringTransactionStore.processDueAsync(requireContext())
                if (result.token.startsWith("firebase:")) {
                    CloudSyncService.scheduleSync(requireContext())
                }
                if (!result.emailVerified) {
                    Toast.makeText(requireContext(), R.string.auth_email_verification_pending, Toast.LENGTH_LONG).show()
                }

                Toast.makeText(requireContext(), "¡Bienvenido, ${result.nombre}!", Toast.LENGTH_SHORT).show()
                val opts = NavOptions.Builder().setPopUpTo(R.id.nav_graph, true).build()
                findNavController().navigate(R.id.nav_home, null, opts)
            } else {
                setLoading(false)
                Toast.makeText(
                    requireContext(),
                    "Credenciales inválidas",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun loginWithGoogle(setLoading: (Boolean) -> Unit) {
        if (!hasInternet()) {
            Toast.makeText(requireContext(), R.string.auth_internet_required, Toast.LENGTH_SHORT).show()
            setLoading(false)
            return
        }
        setLoading(true)
        googleLoading = setLoading
        val options = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        googleSignInLauncher.launch(GoogleSignIn.getClient(requireActivity(), options).signInIntent)
    }

    private suspend fun completeGoogleLogin(
        firebaseUser: com.google.firebase.auth.FirebaseUser,
        setLoading: ((Boolean) -> Unit)?
    ) {
        val result = AuthService.loginWithGoogle(requireContext(), firebaseUser)
        if (!isAdded) return
        if (result == null) {
            setLoading?.invoke(false)
            Toast.makeText(requireContext(), "No se pudo vincular la cuenta de Google", Toast.LENGTH_SHORT).show()
            return
        }
        Prefs.setToken(requireContext(), result.token)
        Prefs.setUserSession(requireContext(), result.userId.toLong(), result.email, result.nombre)
        RecurringTransactionStore.processDueAsync(requireContext())
        CloudSyncService.scheduleSync(requireContext())
        Toast.makeText(
            requireContext(),
            "Sesión de Google conectada",
            Toast.LENGTH_SHORT
        ).show()
        val opts = NavOptions.Builder().setPopUpTo(R.id.nav_graph, true).build()
        findNavController().navigate(R.id.nav_home, null, opts)
    }

    private fun recoverPassword(emailRaw: String) {
        val email = emailRaw.trim()
        if (email.isEmpty()) {
            Toast.makeText(requireContext(), R.string.auth_recovery_email_required, Toast.LENGTH_SHORT).show()
            return
        }

        val appContext = requireContext().applicationContext
        viewLifecycleOwner.lifecycleScope.launch {
            val user = withContext(Dispatchers.IO) {
                LocalRepository.getInstance(appContext).getUserByEmail(email)
            }
            if (!isAdded) return@launch
            if (user == null) {
                sendPasswordResetEmail(email)
                return@launch
            }

            MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.auth_recovery_options_title)
                .setMessage(R.string.auth_recovery_options_message)
                .setPositiveButton(R.string.auth_recovery_device_action) { _, _ ->
                    recoverLocalPasswordWithDevice(email)
                }
                .setNegativeButton(R.string.auth_recovery_email_action) { _, _ ->
                    sendPasswordResetEmail(email)
                }
                .setNeutralButton(android.R.string.cancel, null)
                .show()
        }
    }

    private fun recoverLocalPasswordWithDevice(email: String) {
        DeviceAuthHelper.authenticate(
            fragment = this@LoginFragment,
            onSuccess = {
                Toast.makeText(requireContext(), R.string.device_auth_verified, Toast.LENGTH_SHORT).show()
                findNavController().navigate(
                    R.id.nav_change_password,
                    Bundle().apply {
                        putBoolean(ChangePasswordFragment.ARG_RECOVERY_MODE, true)
                        putString(ChangePasswordFragment.ARG_RECOVERY_EMAIL, email)
                    }
                )
            },
            onCancel = {
                Toast.makeText(requireContext(), R.string.device_auth_cancelled, Toast.LENGTH_SHORT).show()
            },
            onFailure = {
                Toast.makeText(requireContext(), R.string.device_auth_failed, Toast.LENGTH_SHORT).show()
            },
            onNoDeviceLock = {
                Toast.makeText(requireContext(), R.string.device_auth_no_lock, Toast.LENGTH_LONG).show()
            }
        )
    }

    private fun sendPasswordResetEmail(email: String) {
        if (!hasInternet()) {
            Toast.makeText(requireContext(), R.string.auth_internet_required, Toast.LENGTH_SHORT).show()
            return
        }
        viewLifecycleOwner.lifecycleScope.launch {
            val result = AuthService.sendPasswordResetEmail(email)
            if (!isAdded) return@launch
            Toast.makeText(requireContext(), result.message, if (result.ok) Toast.LENGTH_LONG else Toast.LENGTH_SHORT).show()
        }
    }

    private fun hasInternet(): Boolean {
        val manager = requireContext().getSystemService(ConnectivityManager::class.java) ?: return false
        val network = manager.activeNetwork ?: return false
        val capabilities = manager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }
}

@Composable
private fun LoginScreen(
    onBackClick: () -> Unit,
    onForgotPasswordClick: (String) -> Unit,
    onLogin: (String, String, (Boolean) -> Unit) -> Unit,
    onGoogleLogin: ((Boolean) -> Unit) -> Unit,
    onRegisterClick: () -> Unit
) {
    var email by rememberSaveable { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by rememberSaveable { mutableStateOf(false) }
    var loading by remember { mutableStateOf(false) }
    val colors = spendlyAuthColors()

    SpendlyAuthScreenContainer {
        SpendlyTopBar(
            title = stringResource(R.string.auth_titulo_login),
            onBackClick = onBackClick
        )

        Spacer(modifier = Modifier.height(30.dp))

        SpendlyLogoMark(markSize = 72.dp)

        Spacer(modifier = Modifier.height(2.dp))

        SpendlyBrandTitle(fontSize = 38)

        Text(
            text = "Bienvenido a tu espacio financiero",
            color = colors.muted,
            fontSize = 16.sp,
            lineHeight = 24.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 4.dp)
        )

        Spacer(modifier = Modifier.height(28.dp))

        SpendlyAuthCard {
            SpendlyAuthField(
                value = email,
                onValueChange = { email = it },
                placeholder = stringResource(R.string.auth_email),
                iconRes = R.drawable.ic_mail,
                enabled = !loading,
                keyboardType = KeyboardType.Email
            )

            Spacer(modifier = Modifier.height(16.dp))

            SpendlyPasswordField(
                value = password,
                onValueChange = { password = it },
                label = stringResource(R.string.auth_password),
                visible = passwordVisible,
                onToggleVisible = { passwordVisible = !passwordVisible },
                enabled = !loading,
                imeAction = ImeAction.Done,
                onImeAction = { onLogin(email, password) { loading = it } }
            )

            Text(
                text = stringResource(R.string.auth_forgot_password),
                color = colors.accent,
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .padding(top = 18.dp)
                    .align(Alignment.CenterHorizontally)
                    .clickable(enabled = !loading) { onForgotPasswordClick(email) }
            )

            Spacer(modifier = Modifier.height(18.dp))

            SpendlyPrimaryButton(
                text = stringResource(R.string.auth_btn_login),
                loading = loading,
                enabled = !loading,
                onClick = { onLogin(email, password) { loading = it } },
            )

            Spacer(modifier = Modifier.height(10.dp))

            SpendlyGoogleButton(
                text = "Continuar con Google",
                loading = false,
                enabled = !loading,
                onClick = { onGoogleLogin { loading = it } },
            )

            SpendlyDividerDot()

            Text(
                text = buildAnnotatedString {
                    withStyle(SpanStyle(color = colors.accent.copy(alpha = 0.78f))) {
                        append("¿No tienes cuenta? ")
                    }
                    withStyle(SpanStyle(color = colors.accent, fontWeight = FontWeight.Bold)) {
                        append("Crear una")
                    }
                },
                fontSize = 15.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(enabled = !loading, onClick = onRegisterClick)
            )
        }

        Spacer(modifier = Modifier.height(14.dp))
    }
}
