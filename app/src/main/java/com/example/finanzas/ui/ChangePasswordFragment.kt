package com.example.finanzas.ui

import android.os.Bundle
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.finanzas.R
import com.example.finanzas.data.api.UserService
import com.example.finanzas.ui.compose.SpendlyAuthCard
import com.example.finanzas.ui.compose.SpendlyAuthHeader
import com.example.finanzas.ui.compose.SpendlyAuthScreenContainer
import com.example.finanzas.ui.compose.SpendlyComposeTheme
import com.example.finanzas.ui.compose.SpendlyPasswordField
import com.example.finanzas.ui.compose.SpendlyPrimaryButton
import com.example.finanzas.util.Prefs

class ChangePasswordFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = ComposeView(requireContext()).apply {
        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        setContent {
            SpendlyComposeTheme {
                ChangePasswordScreen(onSave = ::changePassword)
            }
        }
    }

    private fun changePassword(oldPassword: String, newPassword: String, confirm: String) {
        val oldP = oldPassword.trim()
        val newP = newPassword.trim()
        val conf = confirm.trim()

        if (TextUtils.isEmpty(oldP) || TextUtils.isEmpty(newP) || TextUtils.isEmpty(conf)) {
            Toast.makeText(requireContext(), "Completa todos los campos", Toast.LENGTH_SHORT).show()
            return
        }
        if (newP != conf) {
            Toast.makeText(requireContext(), "La confirmación no coincide", Toast.LENGTH_SHORT).show()
            return
        }

        val userId = Prefs.getCurrentUserId(requireContext())
        if (userId <= 0) {
            Toast.makeText(requireContext(), "Inicia sesión nuevamente", Toast.LENGTH_SHORT).show()
            return
        }

        val email = Prefs.getCurrentUserEmail(requireContext())
        UserService.changePassword(requireContext(), email, oldP, newP, object : UserService.SimpleCb {
            override fun onOk() {
                if (!isAdded) return
                Toast.makeText(requireContext(), "Contraseña actualizada correctamente.", Toast.LENGTH_SHORT).show()
                Prefs.clearAuth(requireContext())
                findNavController().navigate(R.id.nav_login)
            }

            override fun onFail() {
                if (!isAdded) return
                Toast.makeText(
                    requireContext(),
                    "No se pudo actualizar en el servidor. Verifica tu contraseña o conexión.",
                    Toast.LENGTH_LONG
                ).show()
            }
        })
    }
}

@Composable
private fun ChangePasswordScreen(
    onSave: (String, String, String) -> Unit
) {
    var oldPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    var oldVisible by rememberSaveable { mutableStateOf(false) }
    var newVisible by rememberSaveable { mutableStateOf(false) }
    var confirmVisible by rememberSaveable { mutableStateOf(false) }

    SpendlyAuthScreenContainer {
        SpendlyAuthCard {
            SpendlyAuthHeader(
                appName = "Spendly",
                title = "Cambiar contraseña",
                subtitle = "Actualiza tu acceso de forma segura."
            )

            Spacer(modifier = Modifier.height(22.dp))

            SpendlyPasswordField(
                value = oldPassword,
                onValueChange = { oldPassword = it },
                label = "Contraseña actual",
                visible = oldVisible,
                onToggleVisible = { oldVisible = !oldVisible },
                enabled = true
            )

            Spacer(modifier = Modifier.height(12.dp))

            SpendlyPasswordField(
                value = newPassword,
                onValueChange = { newPassword = it },
                label = "Nueva contraseña",
                visible = newVisible,
                onToggleVisible = { newVisible = !newVisible },
                enabled = true
            )

            Spacer(modifier = Modifier.height(12.dp))

            SpendlyPasswordField(
                value = confirm,
                onValueChange = { confirm = it },
                label = "Confirmar nueva contraseña",
                visible = confirmVisible,
                onToggleVisible = { confirmVisible = !confirmVisible },
                enabled = true
            )

            SpendlyPrimaryButton(
                text = "Guardar",
                loading = false,
                enabled = true,
                onClick = { onSave(oldPassword, newPassword, confirm) }
            )
        }
    }
}
