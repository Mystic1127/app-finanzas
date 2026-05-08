package com.example.finanzas.ui

import android.os.Bundle
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
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import com.example.finanzas.R
import com.example.finanzas.data.api.AuthService
import com.example.finanzas.ui.compose.SpendlyAuthCard
import com.example.finanzas.ui.compose.SpendlyAuthField
import com.example.finanzas.ui.compose.SpendlyAuthScreenContainer
import com.example.finanzas.ui.compose.SpendlyBrandTitle
import com.example.finanzas.ui.compose.SpendlyComposeTheme
import com.example.finanzas.ui.compose.SpendlyDividerDot
import com.example.finanzas.ui.compose.SpendlyLogoMark
import com.example.finanzas.ui.compose.SpendlyPasswordField
import com.example.finanzas.ui.compose.SpendlyPrimaryButton
import com.example.finanzas.ui.compose.SpendlyTopBar
import com.example.finanzas.ui.compose.spendlyAuthColors
import com.example.finanzas.util.Prefs
import com.example.finanzas.util.RecurringTransactionStore
import kotlinx.coroutines.launch

class LoginFragment : Fragment() {

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
                    onForgotPasswordClick = {
                        Toast.makeText(requireContext(), "La recuperación estará disponible pronto.", Toast.LENGTH_SHORT).show()
                    },
                    onLogin = { email, pass, setLoading ->
                        login(email, pass, setLoading)
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

                Toast.makeText(requireContext(), "¡Bienvenido, ${result.nombre}!", Toast.LENGTH_SHORT).show()
                val opts = NavOptions.Builder().setPopUpTo(R.id.nav_graph, true).build()
                findNavController().navigate(R.id.nav_home, null, opts)
            } else {
                setLoading(false)
                Toast.makeText(
                    requireContext(),
                    "Credenciales inválidas o servidor no disponible",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }
}

@Composable
private fun LoginScreen(
    onBackClick: () -> Unit,
    onForgotPasswordClick: () -> Unit,
    onLogin: (String, String, (Boolean) -> Unit) -> Unit,
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
                    .clickable(enabled = !loading, onClick = onForgotPasswordClick)
            )

            Spacer(modifier = Modifier.height(18.dp))

            SpendlyPrimaryButton(
                text = stringResource(R.string.auth_btn_login),
                loading = loading,
                enabled = !loading,
                onClick = { onLogin(email, password) { loading = it } },
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
