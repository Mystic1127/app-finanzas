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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.finanzas.R
import com.example.finanzas.data.api.UserService
import com.example.finanzas.ui.compose.SpendlyAuthCard
import com.example.finanzas.ui.compose.SpendlyAuthScreenContainer
import com.example.finanzas.ui.compose.SpendlyBrandTitle
import com.example.finanzas.ui.compose.SpendlyComposeTheme
import com.example.finanzas.ui.compose.SpendlyDividerOr
import com.example.finanzas.ui.compose.SpendlyLogoMark
import com.example.finanzas.ui.compose.SpendlyPasswordField
import com.example.finanzas.ui.compose.SpendlyPrimaryButton
import com.example.finanzas.ui.compose.SpendlyRequirementCard
import com.example.finanzas.ui.compose.SpendlyTopBar
import com.example.finanzas.ui.compose.spendlyAuthColors
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
                ChangePasswordScreen(
                    onBackClick = { findNavController().popBackStack() },
                    onCancel = { findNavController().popBackStack() },
                    onSave = ::changePassword
                )
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
    onBackClick: () -> Unit,
    onCancel: () -> Unit,
    onSave: (String, String, String) -> Unit
) {
    var oldPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    var oldVisible by rememberSaveable { mutableStateOf(false) }
    var newVisible by rememberSaveable { mutableStateOf(false) }
    var confirmVisible by rememberSaveable { mutableStateOf(false) }
    val colors = spendlyAuthColors()

    SpendlyAuthScreenContainer {
        SpendlyTopBar(
            title = "Cambiar contraseña",
            onBackClick = onBackClick
        )

        Spacer(modifier = Modifier.height(30.dp))

        SpendlyLogoMark(markSize = 72.dp)

        Spacer(modifier = Modifier.height(2.dp))

        SpendlyBrandTitle(fontSize = 38)

        Text(
            text = "Actualiza tu acceso de forma segura",
            color = colors.muted,
            fontSize = 16.sp,
            lineHeight = 24.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 4.dp)
        )

        Spacer(modifier = Modifier.height(28.dp))

        SpendlyAuthCard {
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
                enabled = true,
                imeAction = ImeAction.Done,
                onImeAction = { onSave(oldPassword, newPassword, confirm) }
            )

            Spacer(modifier = Modifier.height(18.dp))

            SpendlyRequirementCard(
                title = "Tu nueva contraseña debe:",
                requirements = listOf(
                    "Tener al menos 8 caracteres",
                    "Combinar letras y números",
                    "Ser diferente a la anterior"
                )
            )

            Spacer(modifier = Modifier.height(22.dp))

            SpendlyPrimaryButton(
                text = "Guardar contraseña",
                loading = false,
                enabled = true,
                onClick = { onSave(oldPassword, newPassword, confirm) }
            )

            SpendlyDividerOr()

            Text(
                text = "Cancelar",
                color = colors.accent,
                fontSize = 18.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.CenterHorizontally)
                    .clickable(onClick = onCancel)
            )
        }

        Spacer(modifier = Modifier.height(18.dp))
    }
}
