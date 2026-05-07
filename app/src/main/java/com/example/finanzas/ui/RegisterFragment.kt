package com.example.finanzas.ui

import android.os.Bundle
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.Text
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import com.example.finanzas.R
import com.example.finanzas.data.api.AuthService
import com.example.finanzas.data.api.SettingsService
import com.example.finanzas.ui.compose.HighlightedSentence
import com.example.finanzas.ui.compose.SpendlyAuthCard
import com.example.finanzas.ui.compose.SpendlyAuthField
import com.example.finanzas.ui.compose.SpendlyAuthScreenContainer
import com.example.finanzas.ui.compose.SpendlyBrandTitle
import com.example.finanzas.ui.compose.SpendlyComposeTheme
import com.example.finanzas.ui.compose.SpendlyLogoMark
import com.example.finanzas.ui.compose.SpendlyPasswordField
import com.example.finanzas.ui.compose.SpendlyPrimaryButton
import com.example.finanzas.ui.compose.SpendlyTopBar
import com.example.finanzas.ui.compose.spendlyAuthColors
import com.example.finanzas.util.Prefs
import com.example.finanzas.util.RecurringTransactionStore
import kotlinx.coroutines.launch

class RegisterFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = ComposeView(requireContext()).apply {
        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        setContent {
            SpendlyComposeTheme {
                RegisterScreen(
                    onBackClick = { findNavController().popBackStack() },
                    onRegister = { nombre, email, pass, conf, setLoading ->
                        register(nombre, email, pass, conf, setLoading)
                    },
                    onLoginClick = {
                        val opts = NavOptions.Builder().setPopUpTo(R.id.nav_register, true).build()
                        findNavController().navigate(R.id.nav_login, null, opts)
                    }
                )
            }
        }
    }

    private fun register(
        nombreRaw: String,
        emailRaw: String,
        passRaw: String,
        confRaw: String,
        setLoading: (Boolean) -> Unit
    ) {
        val nombre = nombreRaw.trim()
        val email = emailRaw.trim()
        val pass = passRaw.trim()
        val conf = confRaw.trim()

        if (TextUtils.isEmpty(nombre) || TextUtils.isEmpty(email) ||
            TextUtils.isEmpty(pass) || TextUtils.isEmpty(conf)
        ) {
            Toast.makeText(requireContext(), "Completa todos los campos", Toast.LENGTH_SHORT).show()
            setLoading(false)
            return
        }
        if (pass != conf) {
            Toast.makeText(requireContext(), "Las contraseñas no coinciden", Toast.LENGTH_SHORT).show()
            setLoading(false)
            return
        }

        setLoading(true)

        viewLifecycleOwner.lifecycleScope.launch {
            val result = AuthService.register(requireContext(), nombre, email, pass)
            if (!isAdded) return@launch

            if (result != null) {
                Prefs.setToken(requireContext(), result.token)
                Prefs.setUserSession(
                    requireContext(),
                    if (result.userId > 0) result.userId.toLong() else 1L,
                    result.email,
                    result.nombre
                )
                SettingsService.prepareCurrencySetupForNewUser(requireContext())
                RecurringTransactionStore.processDueAsync(requireContext())

                Toast.makeText(requireContext(), "Cuenta creada. ¡Bienvenido!", Toast.LENGTH_SHORT).show()
                val opts = NavOptions.Builder().setPopUpTo(R.id.nav_graph, true).build()
                findNavController().navigate(R.id.nav_home, null, opts)
            } else {
                setLoading(false)
                Toast.makeText(
                    requireContext(),
                    "No se pudo registrar (verifica conexión o email ya usado)",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }
}

@Composable
private fun RegisterScreen(
    onBackClick: () -> Unit,
    onRegister: (String, String, String, String, (Boolean) -> Unit) -> Unit,
    onLoginClick: () -> Unit
) {
    var nombre by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var passwordVisible by rememberSaveable { mutableStateOf(false) }
    var confirmVisible by rememberSaveable { mutableStateOf(false) }
    var loading by remember { mutableStateOf(false) }
    val colors = spendlyAuthColors()

    SpendlyAuthScreenContainer {
        SpendlyTopBar(
            title = stringResource(R.string.auth_titulo_registro),
            onBackClick = onBackClick
        )

        Spacer(modifier = Modifier.height(30.dp))

        SpendlyLogoMark(markSize = 62.dp)

        Spacer(modifier = Modifier.height(2.dp))

        SpendlyBrandTitle(fontSize = 36)
        HighlightedSentence(
            before = "Crea tu cuenta y ",
            highlighted = "controla tus metas",
            after = "",
            fontSize = 14,
            centered = true
        )

        Spacer(modifier = Modifier.height(22.dp))

        SpendlyAuthCard {
            SpendlyAuthField(
                value = nombre,
                onValueChange = { nombre = it },
                placeholder = stringResource(R.string.auth_nombre),
                iconRes = R.drawable.ic_person,
                enabled = !loading,
                keyboardType = KeyboardType.Text
            )

            Spacer(modifier = Modifier.height(12.dp))

            SpendlyAuthField(
                value = email,
                onValueChange = { email = it },
                placeholder = stringResource(R.string.auth_email),
                iconRes = R.drawable.ic_mail,
                enabled = !loading,
                keyboardType = KeyboardType.Email
            )

            Spacer(modifier = Modifier.height(12.dp))

            SpendlyPasswordField(
                value = password,
                onValueChange = { password = it },
                label = stringResource(R.string.auth_password),
                visible = passwordVisible,
                onToggleVisible = { passwordVisible = !passwordVisible },
                enabled = !loading
            )

            Spacer(modifier = Modifier.height(12.dp))

            SpendlyPasswordField(
                value = confirmPassword,
                onValueChange = { confirmPassword = it },
                label = stringResource(R.string.auth_confirm_password),
                visible = confirmVisible,
                onToggleVisible = { confirmVisible = !confirmVisible },
                enabled = !loading
            )

            Spacer(modifier = Modifier.height(18.dp))

            SpendlyPrimaryButton(
                text = stringResource(R.string.auth_btn_registrarse),
                loading = loading,
                enabled = !loading,
                onClick = { onRegister(nombre, email, password, confirmPassword) { loading = it } },
            )

            Text(
                text = buildAnnotatedString {
                    withStyle(SpanStyle(color = colors.accent.copy(alpha = 0.78f))) {
                        append("¿Ya tienes cuenta? ")
                    }
                    withStyle(SpanStyle(color = colors.accent, fontWeight = FontWeight.Bold)) {
                        append("Inicia sesión")
                    }
                },
                fontSize = 15.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 18.dp)
                    .clickable(enabled = !loading, onClick = onLoginClick)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))
    }
}
