package com.example.finanzas.ui

import android.os.Bundle
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.finanzas.R
import com.example.finanzas.ui.compose.SpendlyAuthCard
import com.example.finanzas.ui.compose.SpendlyAuthHeader
import com.example.finanzas.ui.compose.SpendlyAuthScreenContainer
import com.example.finanzas.ui.compose.SpendlyComposeTheme
import com.example.finanzas.ui.compose.SpendlyPrimaryButton
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
    onSave: (String, String) -> Pair<String?, String?>,
    onRemove: () -> Unit
) {
    var pin by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    var pinError by remember { mutableStateOf<String?>(null) }
    var confirmError by remember { mutableStateOf<String?>(null) }
    var pinVisible by rememberSaveable { mutableStateOf(false) }
    var confirmVisible by rememberSaveable { mutableStateOf(false) }

    SpendlyAuthScreenContainer {
        SpendlyAuthCard {
            SpendlyAuthHeader(
                appName = stringResource(R.string.app_name_spendly),
                title = stringResource(R.string.pin_setup_title),
                subtitle = stringResource(R.string.pin_setup_message)
            )

            Spacer(modifier = Modifier.height(24.dp))

            PinTextField(
                value = pin,
                onValueChange = {
                    pin = it
                    pinError = null
                },
                label = stringResource(R.string.pin_setup_new_hint),
                visible = pinVisible,
                onToggleVisible = { pinVisible = !pinVisible },
                error = pinError
            )

            Spacer(modifier = Modifier.height(16.dp))

            PinTextField(
                value = confirm,
                onValueChange = {
                    confirm = it
                    confirmError = null
                },
                label = stringResource(R.string.pin_setup_confirm_hint),
                visible = confirmVisible,
                onToggleVisible = { confirmVisible = !confirmVisible },
                error = confirmError
            )

            SpendlyPrimaryButton(
                text = stringResource(R.string.pin_setup_save),
                loading = false,
                enabled = true,
                onClick = {
                    val errors = onSave(pin, confirm)
                    pinError = errors.first
                    confirmError = errors.second
                }
            )

            if (hasPin) {
                OutlinedButton(
                    onClick = onRemove,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    Text(stringResource(R.string.pin_setup_remove))
                }
            }
        }
    }
}

@Composable
private fun PinTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    visible: Boolean,
    onToggleVisible: () -> Unit,
    error: String?
) {
    OutlinedTextField(
        value = value,
        onValueChange = { onValueChange(it.filter(Char::isDigit).take(4)) },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        label = { Text(label) },
        isError = error != null,
        supportingText = error?.let { { Text(it) } },
        textStyle = androidx.compose.ui.text.TextStyle(textAlign = TextAlign.Center),
        visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
        trailingIcon = {
            TextButton(onClick = onToggleVisible) {
                Text(if (visible) "Ocultar" else "Ver")
            }
        }
    )
}
