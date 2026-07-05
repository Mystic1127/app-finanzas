package com.example.finanzas.util

import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import android.content.Context
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.finanzas.R

object DeviceAuthHelper {
    private const val AUTHENTICATORS =
        BiometricManager.Authenticators.BIOMETRIC_STRONG or
            BiometricManager.Authenticators.DEVICE_CREDENTIAL

    fun canAuthenticate(context: Context): Boolean {
        return BiometricManager.from(context).canAuthenticate(AUTHENTICATORS) == BiometricManager.BIOMETRIC_SUCCESS
    }

    fun authenticate(
        fragment: Fragment,
        onSuccess: () -> Unit,
        onCancel: () -> Unit,
        onFailure: () -> Unit,
        onNoDeviceLock: () -> Unit
    ) {
        val context = fragment.requireContext()
        val manager = BiometricManager.from(context)
        when (manager.canAuthenticate(AUTHENTICATORS)) {
            BiometricManager.BIOMETRIC_SUCCESS -> Unit
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> {
                onNoDeviceLock()
                return
            }
            else -> {
                onFailure()
                return
            }
        }

        val prompt = BiometricPrompt(
            fragment,
            ContextCompat.getMainExecutor(context),
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    onSuccess()
                }

                override fun onAuthenticationFailed() {
                    onFailure()
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    when (errorCode) {
                        BiometricPrompt.ERROR_CANCELED,
                        BiometricPrompt.ERROR_USER_CANCELED,
                        BiometricPrompt.ERROR_NEGATIVE_BUTTON -> onCancel()
                        BiometricPrompt.ERROR_NO_DEVICE_CREDENTIAL,
                        BiometricPrompt.ERROR_NO_BIOMETRICS -> onNoDeviceLock()
                        else -> onFailure()
                    }
                }
            }
        )

        prompt.authenticate(
            BiometricPrompt.PromptInfo.Builder()
                .setTitle(context.getString(R.string.device_auth_title))
                .setSubtitle(context.getString(R.string.device_auth_subtitle))
                .setDescription(context.getString(R.string.device_auth_description))
                .setAllowedAuthenticators(AUTHENTICATORS)
                .build()
        )
    }
}
