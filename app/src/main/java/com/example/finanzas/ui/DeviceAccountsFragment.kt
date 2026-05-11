package com.example.finanzas.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import com.example.finanzas.R
import com.example.finanzas.data.local.LocalRepository
import com.example.finanzas.ui.view.SpendlyDecorBackgroundDrawable
import com.example.finanzas.util.Prefs
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class DeviceAccountsFragment : Fragment() {
    private var accounts by mutableStateOf<List<DeviceAccountUi>>(emptyList())

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
        ComposeView(requireContext()).apply {
            background = SpendlyDecorBackgroundDrawable(requireContext())
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                com.example.finanzas.ui.compose.SpendlyComposeTheme {
                    DeviceAccountsScreen(
                        accounts = accounts,
                        onBack = { findNavController().popBackStack() },
                        onRemove = ::confirmRemove
                    )
                }
            }
        }

    override fun onResume() {
        super.onResume()
        load()
    }

    private fun load() {
        val appContext = requireContext().applicationContext
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            val remembered = Prefs.getRememberedUserIds(appContext)
            val current = Prefs.getCurrentUserId(appContext)
            val mapped = LocalRepository.getInstance(appContext).listUsers()
                .filter { remembered.contains(it.id.toLong()) }
                .map {
                    val id = it.id.toLong()
                    val uid = Prefs.getFirebaseUidForUser(appContext, id)
                    val linkedEmail = Prefs.getFirebaseEmailForUser(appContext, id)
                    DeviceAccountUi(
                        id = id,
                        name = it.nombre.orEmpty().ifBlank { linkedEmail.substringBefore('@').ifBlank { "Cuenta" } },
                        email = linkedEmail.ifBlank { it.email.orEmpty() },
                        linked = !uid.isNullOrBlank(),
                        active = id == current
                    )
                }
            withContext(Dispatchers.Main) {
                if (isAdded) accounts = mapped
            }
        }
    }

    private fun confirmRemove(account: DeviceAccountUi) {
        val message = if (account.linked) {
            "Esta cuenta se quitara de este dispositivo. Tus datos en la nube no se eliminaran."
        } else {
            "Esta cuenta solo existe en este dispositivo. Si la quitas, podrias perder el acceso a sus datos locales desde el selector."
        }
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Quitar cuenta")
            .setMessage(message)
            .setNegativeButton(android.R.string.cancel, null)
            .setPositiveButton("Quitar") { _, _ -> removeAccount(account) }
            .show()
    }

    private fun removeAccount(account: DeviceAccountUi) {
        Prefs.forgetRememberedUser(requireContext(), account.id)
        if (account.active) {
            Prefs.clearAuth(requireContext())
            FirebaseAuth.getInstance().signOut()
            clearGoogleCache()
            Toast.makeText(requireContext(), "Cuenta quitada de este dispositivo", Toast.LENGTH_SHORT).show()
            val opts = NavOptions.Builder().setPopUpTo(R.id.nav_graph, true).build()
            findNavController().navigate(R.id.nav_welcome, null, opts)
        } else {
            Toast.makeText(requireContext(), "Cuenta quitada del selector del dispositivo", Toast.LENGTH_SHORT).show()
            load()
        }
    }

    private fun clearGoogleCache() {
        val options = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        GoogleSignIn.getClient(requireActivity(), options).signOut()
    }
}

private data class DeviceAccountUi(
    val id: Long,
    val name: String,
    val email: String,
    val linked: Boolean,
    val active: Boolean
)

@Composable
private fun DeviceAccountsScreen(
    accounts: List<DeviceAccountUi>,
    onBack: () -> Unit,
    onRemove: (DeviceAccountUi) -> Unit
) {
    Surface(modifier = Modifier.fillMaxSize(), color = Color.Transparent) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding()
                .padding(horizontal = 22.dp, vertical = 24.dp)
        ) {
            DeviceHeader("Cuentas del dispositivo", onBack)
            Text(
                text = "Quita cuentas guardadas en este dispositivo sin borrar tus datos en la nube.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 6.dp, bottom = 18.dp)
            )
            DeviceAccountsIntro(count = accounts.size)
            Box(modifier = Modifier.height(16.dp))
            if (accounts.isEmpty()) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(18.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
                ) {
                    Text("No hay cuentas guardadas en este dispositivo.", modifier = Modifier.padding(18.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(accounts, key = { it.id }) { account ->
                        DeviceAccountCard(account, onRemove)
                    }
                }
            }
        }
    }
}

@Composable
private fun DeviceAccountCard(account: DeviceAccountUi, onRemove: (DeviceAccountUi) -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(18.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(if (account.linked) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(if (account.linked) R.drawable.ic_cloud_upload else R.drawable.ic_person),
                        contentDescription = null,
                        tint = if (account.linked) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(25.dp)
                    )
                }
                Column(Modifier.weight(1f)) {
                    Text(account.name, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Text(account.email, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp, modifier = Modifier.padding(top = 3.dp))
                }
            }
            Text(
                text = when {
                    account.active -> "Activa actualmente"
                    account.linked -> "Cuenta vinculada"
                    else -> "Solo local en este dispositivo"
                },
                color = if (account.active || account.linked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(top = 14.dp)
            )
            Text(
                text = if (account.linked) "Quitarla de este dispositivo no elimina tu cuenta ni tus datos en la nube." else "Quitarla la oculta del acceso rapido local.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp,
                modifier = Modifier.padding(top = 5.dp, bottom = 14.dp)
            )
            OutlinedButton(onClick = { onRemove(account) }, shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth().height(46.dp)) {
                Icon(painterResource(R.drawable.ic_delete), contentDescription = null, modifier = Modifier.size(17.dp), tint = MaterialTheme.colorScheme.error)
                Box(modifier = Modifier.size(8.dp))
                Text("Quitar de este dispositivo", color = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@Composable
private fun DeviceAccountsIntro(count: Int) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(22.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
    ) {
        Row(
            modifier = Modifier.padding(18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(painterResource(R.drawable.ic_group_24), contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(28.dp))
            }
            Column(Modifier.weight(1f)) {
                Text("$count cuentas guardadas", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold, fontSize = 19.sp)
                Text("Controla que cuentas aparecen en este dispositivo.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp, modifier = Modifier.padding(top = 3.dp))
            }
        }
    }
}

@Composable
private fun DeviceHeader(title: String, onBack: () -> Unit) {
    Box(modifier = Modifier.fillMaxWidth().height(44.dp)) {
        IconButton(onClick = onBack, modifier = Modifier.align(Alignment.CenterStart).size(44.dp)) {
            Icon(painterResource(R.drawable.ic_arrow_back), contentDescription = null, tint = MaterialTheme.colorScheme.onSurface)
        }
        Text(title, modifier = Modifier.align(Alignment.Center), color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold, fontSize = 22.sp)
    }
}
