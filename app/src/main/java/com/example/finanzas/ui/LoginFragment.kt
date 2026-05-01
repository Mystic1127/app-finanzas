package com.example.finanzas.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import com.example.finanzas.R
import com.example.finanzas.data.api.AuthService
import com.example.finanzas.ui.compose.SpendlyAuthCard
import com.example.finanzas.ui.compose.SpendlyAuthHeader
import com.example.finanzas.ui.compose.SpendlyAuthScreenContainer
import com.example.finanzas.ui.compose.SpendlyPasswordField
import com.example.finanzas.ui.compose.SpendlyPrimaryButton
import com.example.finanzas.ui.compose.SpendlySecondaryTextButton
import com.example.finanzas.ui.compose.SpendlyComposeTheme
import com.example.finanzas.util.Prefs
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
    onLogin: (String, String, (Boolean) -> Unit) -> Unit,
    onRegisterClick: () -> Unit
) {
    var email by rememberSaveable { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by rememberSaveable { mutableStateOf(false) }
    var loading by remember { mutableStateOf(false) }

    SpendlyAuthScreenContainer {
        SpendlyAuthCard {
            SpendlyAuthHeader(
                appName = stringResource(R.string.app_name_spendly),
                title = stringResource(R.string.auth_titulo_login),
                subtitle = "Bienvenido a tu espacio financiero"
            )

            Spacer(modifier = Modifier.height(22.dp))

            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                modifier = Modifier.fillMaxWidth(),
                enabled = !loading,
                singleLine = true,
                label = { Text(stringResource(R.string.auth_email)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
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

            SpendlyPrimaryButton(
                text = stringResource(R.string.auth_btn_login),
                loading = loading,
                enabled = !loading,
                onClick = { onLogin(email, password) { loading = it } },
            )

            SpendlySecondaryTextButton(
                text = stringResource(R.string.auth_link_registro),
                enabled = !loading,
                onClick = onRegisterClick
            )
        }
    }
}
