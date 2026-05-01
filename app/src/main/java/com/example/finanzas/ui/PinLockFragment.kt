package com.example.finanzas.ui

import android.os.Bundle
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
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
import com.example.finanzas.ui.compose.SpendlySecondaryTextButton
import com.example.finanzas.util.Prefs
import com.example.finanzas.util.PinSession
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class PinLockFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = ComposeView(requireContext()).apply {
        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        setContent {
            SpendlyComposeTheme {
                PinLockScreen(
                    onUnlock = ::unlockWithPin,
                    onForgotPin = ::confirmClearPin
                )
            }
        }
    }

    override fun onViewCreated(v: View, savedInstanceState: Bundle?) {
        super.onViewCreated(v, savedInstanceState)
        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    // Evitar salir sin PIN.
                }
            }
        )
    }

    private fun unlockWithPin(pin: String): String? {
        if (pin.length != 4 || !TextUtils.isDigitsOnly(pin)) {
            return getString(R.string.pin_setup_invalid)
        }
        if (!Prefs.verifyPin(requireContext(), pin)) {
            return getString(R.string.pin_lock_wrong)
        }

        PinSession.unlock()
        findNavController().popBackStack()
        return null
    }

    private fun confirmClearPin() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.pin_lock_forgot)
            .setMessage(R.string.pin_lock_clear_confirm)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                Prefs.clearPin(requireContext())
                PinSession.lock()
                Toast.makeText(requireContext(), R.string.pin_lock_cleared, Toast.LENGTH_SHORT).show()
                findNavController().popBackStack()
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }
}

@Composable
private fun PinLockScreen(
    onUnlock: (String) -> String?,
    onForgotPin: () -> Unit
) {
    var pin by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var pinVisible by rememberSaveable { mutableStateOf(false) }

    SpendlyAuthScreenContainer {
        SpendlyAuthCard {
            SpendlyAuthHeader(
                appName = stringResource(R.string.app_name_spendly),
                title = stringResource(R.string.pin_lock_title),
                subtitle = stringResource(R.string.pin_lock_message)
            )

            Spacer(modifier = Modifier.height(20.dp))

            OutlinedTextField(
                value = pin,
                onValueChange = { value ->
                    pin = value.filter(Char::isDigit).take(4)
                    error = null
                },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                label = { Text(stringResource(R.string.pin_lock_hint)) },
                isError = error != null,
                supportingText = error?.let { { Text(it) } },
                textStyle = androidx.compose.ui.text.TextStyle(textAlign = TextAlign.Center),
                visualTransformation = if (pinVisible) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                trailingIcon = {
                    androidx.compose.material3.TextButton(onClick = { pinVisible = !pinVisible }) {
                        Text(if (pinVisible) "Ocultar" else "Ver")
                    }
                }
            )

            SpendlyPrimaryButton(
                text = stringResource(R.string.pin_lock_button),
                loading = false,
                enabled = true,
                onClick = { error = onUnlock(pin) }
            )

            SpendlySecondaryTextButton(
                text = stringResource(R.string.pin_lock_forgot),
                enabled = true,
                onClick = onForgotPin
            )
        }
    }
}
