package com.example.finanzas.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import com.example.finanzas.R
import com.example.finanzas.data.api.AuthService
import com.example.finanzas.data.cloud.CloudSyncService
import com.example.finanzas.ui.compose.SpendlyAuthCard
import com.example.finanzas.ui.compose.SpendlyAuthScreenContainer
import com.example.finanzas.ui.compose.SpendlyBrandTitle
import com.example.finanzas.ui.compose.SpendlyComposeTheme
import com.example.finanzas.ui.compose.SpendlyLogoMark
import com.example.finanzas.ui.compose.SpendlyOutlinedButton
import com.example.finanzas.ui.compose.SpendlyPrimaryButton
import com.example.finanzas.ui.compose.SpendlyTopBar
import com.example.finanzas.ui.compose.spendlyAuthColors
import com.example.finanzas.util.Prefs
import com.example.finanzas.util.RecurringTransactionStore
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

class EmailVerificationFragment : Fragment() {
    companion object {
        const val ARG_EMAIL = "email"
        const val ARG_NAME = "name"
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
        ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                SpendlyComposeTheme {
                    EmailVerificationScreen(
                        email = arguments?.getString(ARG_EMAIL).orEmpty(),
                        onVerified = ::checkVerification,
                        onResend = ::resendVerification,
                        onChangeEmail = ::changeEmail
                    )
                }
            }
        }

    private fun checkVerification(setLoading: (Boolean) -> Unit) {
        setLoading(true)
        viewLifecycleOwner.lifecycleScope.launch {
            val result = AuthService.completeVerifiedEmailSession(
                requireContext(),
                arguments?.getString(ARG_NAME)
            )
            if (!isAdded) return@launch
            if (result?.emailVerified == true && result.userId > 0) {
                Prefs.setToken(requireContext(), result.token)
                Prefs.setUserSession(requireContext(), result.userId.toLong(), result.email, result.nombre)
                RecurringTransactionStore.processDueAsync(requireContext())
                CloudSyncService.scheduleSync(requireContext())
                Toast.makeText(requireContext(), "Correo verificado. Bienvenido a Spendly.", Toast.LENGTH_SHORT).show()
                val opts = NavOptions.Builder().setPopUpTo(R.id.nav_graph, true).build()
                findNavController().navigate(R.id.nav_home, null, opts)
            } else {
                setLoading(false)
                Toast.makeText(requireContext(), "Tu correo aun no aparece verificado.", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun resendVerification() {
        viewLifecycleOwner.lifecycleScope.launch {
            val result = AuthService.resendEmailVerification()
            if (isAdded) Toast.makeText(requireContext(), result.message, Toast.LENGTH_LONG).show()
        }
    }

    private fun changeEmail() {
        FirebaseAuth.getInstance().signOut()
        Prefs.clearAuth(requireContext())
        val opts = NavOptions.Builder().setPopUpTo(R.id.nav_graph, true).build()
        findNavController().navigate(R.id.nav_login, null, opts)
    }
}

@Composable
private fun EmailVerificationScreen(
    email: String,
    onVerified: ((Boolean) -> Unit) -> Unit,
    onResend: () -> Unit,
    onChangeEmail: () -> Unit
) {
    var loading by remember { mutableStateOf(false) }
    val colors = spendlyAuthColors()
    SpendlyAuthScreenContainer(scrollEnabled = false, scrollWhenImeVisible = false) {
        SpendlyTopBar(title = "Verifica tu correo", onBackClick = onChangeEmail)
        Spacer(modifier = Modifier.height(22.dp))
        SpendlyLogoMark(markSize = 64.dp)
        SpendlyBrandTitle(fontSize = 34)
        Spacer(modifier = Modifier.height(20.dp))
        SpendlyAuthCard(horizontalPadding = 20.dp, verticalPadding = 22.dp, cornerRadius = 24.dp) {
            Text(
                text = "Te enviamos un correo de verificacion. Revisa tu bandeja y confirma tu correo para continuar.",
                color = colors.text,
                fontSize = 18.sp,
                lineHeight = 25.sp,
                textAlign = TextAlign.Center
            )
            if (email.isNotBlank()) {
                Text(
                    text = email,
                    color = colors.accent,
                    fontSize = 15.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 10.dp)
                )
            }
            Text(
                text = "Spendly no sincronizara ni subira datos hasta que este correo este verificado.",
                color = colors.muted,
                fontSize = 14.sp,
                lineHeight = 20.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 14.dp, bottom = 18.dp)
            )
            SpendlyPrimaryButton(
                text = "Ya verifique",
                loading = loading,
                enabled = !loading,
                onClick = { onVerified { loading = it } }
            )
            Spacer(modifier = Modifier.height(10.dp))
            SpendlyOutlinedButton(text = "Reenviar correo", onClick = onResend)
            Spacer(modifier = Modifier.height(10.dp))
            SpendlyOutlinedButton(text = "Cambiar correo", onClick = onChangeEmail)
        }
    }
}
