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
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
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
import androidx.navigation.fragment.findNavController
import com.example.finanzas.R
import com.example.finanzas.data.cloud.CloudSyncService
import com.example.finanzas.data.api.SettingsService
import com.example.finanzas.data.local.LocalRepository
import com.example.finanzas.data.local.room.RecurringTransactionEntity
import com.example.finanzas.data.model.Categoria
import com.example.finanzas.ui.view.SpendlyDecorBackgroundDrawable
import com.example.finanzas.util.Format
import com.example.finanzas.util.RecurringTransactionStore
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class RecurringSettingsFragment : Fragment() {
    private var items by mutableStateOf<List<RecurringUi>>(emptyList())

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val host = ComposeView(requireContext()).apply {
            background = SpendlyDecorBackgroundDrawable(requireContext())
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                com.example.finanzas.ui.compose.SpendlyComposeTheme {
                    RecurringScreen(
                        items = items,
                        onBack = { findNavController().popBackStack() },
                        onAdd = { findNavController().navigate(R.id.nav_recurring_rule) },
                        onEdit = { editRecurring(it) },
                        onDelete = { confirmDelete(it) },
                        onToggle = { item, active -> toggleRecurring(item, active) }
                    )
                }
            }
        }
        return host
    }

    override fun onResume() {
        super.onResume()
        load()
    }

    private fun load() {
        val appContext = requireContext().applicationContext
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            val categories = LocalRepository.getInstance(appContext).listCategorias().associateBy { it.id }
            val mapped = RecurringTransactionStore.listTemplates(appContext).map { it.toUi(categories) }
            withContext(Dispatchers.Main) {
                if (isAdded) items = mapped
            }
        }
    }

    private fun editRecurring(item: RecurringUi) {
        findNavController().navigate(
            R.id.nav_recurring_rule,
            Bundle().apply { putInt(RecurringRuleFragment.ARG_TEMPLATE_ID, item.id) }
        )
    }

    private fun toggleRecurring(item: RecurringUi, active: Boolean) {
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            val ok = RecurringTransactionStore.setTemplateActive(requireContext(), item.id, active)
            withContext(Dispatchers.Main) {
                if (!isAdded) return@withContext
                if (!ok) Toast.makeText(requireContext(), "No se pudo actualizar el recurrente", Toast.LENGTH_SHORT).show()
                if (ok) CloudSyncService.scheduleUpload(requireContext())
                load()
            }
        }
    }

    private fun confirmDelete(item: RecurringUi) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Eliminar recurrente")
            .setMessage("Se eliminara esta regla recurrente. Las transacciones ya creadas no se borraran.")
            .setNegativeButton(android.R.string.cancel, null)
            .setPositiveButton("Eliminar") { _, _ ->
                viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
                    val ok = RecurringTransactionStore.deleteTemplate(requireContext(), item.id)
                    withContext(Dispatchers.Main) {
                        if (!isAdded) return@withContext
                        Toast.makeText(requireContext(), if (ok) "Recurrente eliminado" else "No se pudo eliminar", Toast.LENGTH_SHORT).show()
                        if (ok) CloudSyncService.scheduleUpload(requireContext())
                        load()
                    }
                }
            }
            .show()
    }

    private fun RecurringTransactionEntity.toUi(categories: Map<Int, Categoria>): RecurringUi {
        val next = RecurringTransactionStore.nextTriggerAtMillis(this)
        return RecurringUi(
            id = id,
            sourceTransactionId = sourceTransactionId,
            title = note?.takeIf { it.isNotBlank() } ?: categories[categoryId]?.nombre ?: if (isTransfer == 1) "Transferencia" else "Sin nota",
            amount = Format.money(if (isIncome == 1 || isTransfer == 1) amount else -amount, currency),
            category = if (isTransfer == 1) "Transferencia" else categories[categoryId]?.nombre ?: "Sin categoria",
            account = SettingsService.getFinancialAccountName(requireContext(), accountType),
            frequency = frequencyLabel(frequency),
            nextRun = next?.let { SimpleDateFormat("dd/MM/yyyy HH:mm", Locale("es", "PE")).format(Date(it)) } ?: "Sin proxima fecha",
            active = isActive == 1
        )
    }

    private fun frequencyLabel(value: String): String = when (value) {
        RecurringTransactionStore.FREQUENCY_WEEKDAYS -> "Lunes a viernes"
        RecurringTransactionStore.FREQUENCY_EVERYDAY -> "Diario"
        RecurringTransactionStore.FREQUENCY_WEEKLY -> "Semanal"
        RecurringTransactionStore.FREQUENCY_MONTHLY -> "Mensual"
        RecurringTransactionStore.FREQUENCY_CUSTOM -> "Personalizado"
        else -> value
    }
}

private data class RecurringUi(
    val id: Int,
    val sourceTransactionId: Int,
    val title: String,
    val amount: String,
    val category: String,
    val account: String,
    val frequency: String,
    val nextRun: String,
    val active: Boolean
)

@Composable
private fun RecurringScreen(
    items: List<RecurringUi>,
    onBack: () -> Unit,
    onAdd: () -> Unit,
    onEdit: (RecurringUi) -> Unit,
    onDelete: (RecurringUi) -> Unit,
    onToggle: (RecurringUi, Boolean) -> Unit
) {
    Surface(modifier = Modifier.fillMaxSize(), color = Color.Transparent) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding()
                .padding(horizontal = 22.dp, vertical = 24.dp)
        ) {
            Header(title = "Gastos recurrentes", onBack = onBack)
            Text(
                text = "Administra reglas que crean transacciones automaticamente.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 6.dp, bottom = 18.dp)
            )
            RecurringSummaryCard(count = items.size, activeCount = items.count { it.active }, onAdd = onAdd)
            Spacer(Modifier.height(18.dp))
            if (items.isEmpty()) {
                EmptyRecurring(onAdd)
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(items, key = { it.id }) { item ->
                        RecurringCard(item, onEdit, onDelete, onToggle)
                    }
                }
            }
        }
    }
}

@Composable
private fun RecurringSummaryCard(count: Int, activeCount: Int, onAdd: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(22.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Row(
            modifier = Modifier.padding(18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(painterResource(R.drawable.ic_repeat), contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(28.dp))
            }
            Column(Modifier.weight(1f)) {
                Text("$activeCount activos", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                Text("$count reglas configuradas", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp, modifier = Modifier.padding(top = 3.dp))
            }
            Button(
                onClick = onAdd,
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Icon(painterResource(R.drawable.ic_add), contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.size(6.dp))
                Text("Agregar")
            }
        }
    }
}

@Composable
private fun RecurringCard(
    item: RecurringUi,
    onEdit: (RecurringUi) -> Unit,
    onDelete: (RecurringUi) -> Unit,
    onToggle: (RecurringUi, Boolean) -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(if (item.active) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(painterResource(R.drawable.ic_repeat), contentDescription = null, tint = if (item.active) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(23.dp))
                }
                Column(Modifier.weight(1f)) {
                    Text(item.title, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Text(item.amount, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 3.dp))
                }
                Switch(checked = item.active, onCheckedChange = { onToggle(item, it) })
            }
            Text("${item.category} • ${item.account}", color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 8.dp))
            Text("${item.frequency} • Proxima: ${item.nextRun}", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp, modifier = Modifier.padding(top = 4.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.padding(top = 14.dp)) {
                OutlinedButton(onClick = { onEdit(item) }, shape = RoundedCornerShape(14.dp), modifier = Modifier.weight(1f)) {
                    Text("Editar")
                }
                OutlinedButton(onClick = { onDelete(item) }, shape = RoundedCornerShape(14.dp), modifier = Modifier.weight(1f)) {
                    Text("Eliminar")
                }
            }
        }
    }
}

@Composable
private fun EmptyRecurring(onAdd: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
    ) {
        Column(
            modifier = Modifier.padding(22.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(painterResource(R.drawable.ic_schedule), contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(32.dp))
            }
            Text("Sin reglas recurrentes", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold, fontSize = 19.sp, modifier = Modifier.padding(top = 14.dp))
            Text(
                text = "Crea una regla para automatizar gastos, ingresos o transferencias frecuentes.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp, bottom = 16.dp)
            )
            Button(onClick = onAdd, shape = RoundedCornerShape(16.dp)) {
                Icon(painterResource(R.drawable.ic_add), contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.size(8.dp))
                Text("Agregar recurrente")
            }
        }
    }
}

@Composable
private fun Header(title: String, onBack: () -> Unit) {
    Box(modifier = Modifier.fillMaxWidth().height(44.dp)) {
        IconButton(onClick = onBack, modifier = Modifier.align(Alignment.CenterStart).size(44.dp)) {
            Icon(painterResource(R.drawable.ic_arrow_back), contentDescription = null, tint = MaterialTheme.colorScheme.onSurface)
        }
        Text(title, modifier = Modifier.align(Alignment.Center), color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold, fontSize = 22.sp)
    }
}
