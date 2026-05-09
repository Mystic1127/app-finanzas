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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.finanzas.R
import com.example.finanzas.ui.compose.SpendlyAuthCard
import com.example.finanzas.ui.compose.SpendlyAuthScreenContainer
import com.example.finanzas.ui.compose.SpendlyBrandTitle
import com.example.finanzas.ui.compose.SpendlyComposeTheme
import com.example.finanzas.ui.compose.SpendlyDividerOr
import com.example.finanzas.ui.compose.SpendlyLogoMark
import com.example.finanzas.ui.compose.SpendlyPinField
import com.example.finanzas.ui.compose.SpendlyPrimaryButton
import com.example.finanzas.ui.compose.SpendlyRequirementCard
import com.example.finanzas.ui.compose.SpendlyTopBar
import com.example.finanzas.ui.compose.spendlyAuthColors
import com.example.finanzas.util.Prefs
import com.example.finanzas.util.PinSession
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class PinSetupFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = ComposeView(requireContext()).apply {
        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        setContent {
            SpendlyComposeTheme {
                PinSetupScreen(
                    hasPin = Prefs.hasPin(requireContext()),
                    onBackClick = { findNavController().popBackStack() },
                    onCancel = { findNavController().popBackStack() },
                    onSave = ::savePin,
                    onRemove = ::confirmRemovePin
                )
            }
        }
    }

    private fun savePin(pin: String, confirm: String): Pair<String?, String?> {
        if (pin.length != 4 || !TextUtils.isDigitsOnly(pin)) {
            return getString(R.string.pin_setup_invalid) to null
        }
        if (pin != confirm) {
            return null to getString(R.string.pin_setup_mismatch)
        }

        Prefs.savePin(requireContext(), pin)
        PinSession.unlock()
        Toast.makeText(requireContext(), R.string.pin_setup_success, Toast.LENGTH_SHORT).show()
        findNavController().popBackStack()
        return null to null
    }

    private fun confirmRemovePin() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.pin_setup_remove)
            .setMessage(R.string.pin_setup_remove_confirm)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                Prefs.clearPin(requireContext())
                PinSession.lock()
                Toast.makeText(requireContext(), R.string.pin_removed_success, Toast.LENGTH_SHORT).show()
                findNavController().popBackStack()
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }
}

@Composable
private fun PinSetupScreen(
    hasPin: Boolean,
    onBackClick: () -> Unit,
    onCancel: () -> Unit,
    onSave: (String, String) -> Pair<String?, String?>,
    onRemove: () -> Unit
) {
    var pin by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    var pinError by remember { mutableStateOf<String?>(null) }
    var confirmError by remember { mutableStateOf<String?>(null) }
    var pinVisible by rememberSaveable { mutableStateOf(false) }
    var confirmVisible by rememberSaveable { mutableStateOf(false) }
    val colors = spendlyAuthColors()

    fun save() {
        val errors = onSave(pin, confirm)
        pinError = errors.first
        confirmError = errors.second
    }

    SpendlyAuthScreenContainer {
        SpendlyTopBar(
            title = stringResource(R.string.pin_setup_title),
            onBackClick = onBackClick
        )

        Spacer(modifier = Modifier.height(30.dp))

        SpendlyLogoMark(markSize = 72.dp)

        Spacer(modifier = Modifier.height(2.dp))

        SpendlyBrandTitle(fontSize = 38)

        Text(
            text = "Protege tu acceso con un PIN seguro",
            color = colors.muted,
            fontSize = 16.sp,
            lineHeight = 24.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 4.dp)
        )

        Spacer(modifier = Modifier.height(28.dp))

        SpendlyAuthCard {
            SpendlyPinField(
                value = pin,
                onValueChange = {
                    pin = it
                    pinError = null
                },
                label = stringResource(R.string.pin_setup_new_hint),
                visible = pinVisible,
                onToggleVisible = { pinVisible = !pinVisible },
                enabled = true,
                error = pinError
            )

            Spacer(modifier = Modifier.height(12.dp))

            SpendlyPinField(
                value = confirm,
                onValueChange = {
                    confirm = it
                    confirmError = null
                },
                label = stringResource(R.string.pin_setup_confirm_hint),
                visible = confirmVisible,
                onToggleVisible = { confirmVisible = !confirmVisible },
                enabled = true,
                error = confirmError,
                imeAction = ImeAction.Done,
                onImeAction = ::save
            )

            Spacer(modifier = Modifier.height(18.dp))

            SpendlyRequirementCard(
                title = "Tu PIN debe:",
                requirements = listOf(
                    "Tener 4 dígitos",
                    "Ser fácil de recordar para ti",
                    "Ser difícil de adivinar"
                )
            )

            Spacer(modifier = Modifier.height(22.dp))

            SpendlyPrimaryButton(
                text = stringResource(R.string.pin_setup_save),
                loading = false,
                enabled = true,
                onClick = ::save
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

            if (hasPin) {
                Text(
                    text = stringResource(R.string.pin_setup_remove),
                    color = colors.accent.copy(alpha = 0.78f),
                    fontSize = 15.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 14.dp)
                        .align(Alignment.CenterHorizontally)
                        .clickable(onClick = onRemove)
                )
            }
        }

        Spacer(modifier = Modifier.height(18.dp))
    }
}
