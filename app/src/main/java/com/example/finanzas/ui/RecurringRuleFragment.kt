package com.example.finanzas.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.finanzas.R
import com.example.finanzas.data.cloud.CloudSyncService
import com.example.finanzas.data.local.LocalRepository
import com.example.finanzas.data.local.room.RecurringTransactionEntity
import com.example.finanzas.data.model.Categoria
import com.example.finanzas.ui.compose.SpendlyComposeTheme
import com.example.finanzas.ui.compose.SpendlyOutlinedButton
import com.example.finanzas.ui.compose.SpendlyPrimaryButton
import com.example.finanzas.ui.view.SpendlyDecorBackgroundDrawable
import com.example.finanzas.util.Format
import com.example.finanzas.util.RecurringTransactionStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class RecurringRuleFragment : Fragment() {
    private var categories by mutableStateOf<List<Categoria>>(emptyList())
    private var initial by mutableStateOf<RecurringRuleInitial?>(null)

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
        ComposeView(requireContext()).apply {
            background = SpendlyDecorBackgroundDrawable(requireContext())
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                SpendlyComposeTheme {
                    RecurringRuleScreen(
                        categories = categories,
                        initial = initial,
                        onBack = { findNavController().popBackStack() },
                        onSave = ::saveRule
                    )
                }
            }
        }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        loadInitial()
    }

    private fun loadInitial() {
        val templateId = arguments?.getInt(ARG_TEMPLATE_ID, 0) ?: 0
        val appContext = requireContext().applicationContext
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            val loadedCategories = LocalRepository.getInstance(appContext).listCategorias()
            val template = if (templateId > 0) RecurringTransactionStore.findTemplate(appContext, templateId) else null
            val mapped = template?.toInitial()
            withContext(Dispatchers.Main) {
                if (!isAdded) return@withContext
                categories = loadedCategories
                initial = mapped ?: RecurringRuleInitial()
            }
        }
    }

    private fun saveRule(input: RecurringRuleInput) {
        val appContext = requireContext().applicationContext
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            val ok = RecurringTransactionStore.saveRule(
                context = appContext,
                templateId = input.templateId,
                sourceTransactionId = input.sourceTransactionId,
                frequency = input.frequency,
                daysMask = 0,
                isActive = input.active,
                isTransfer = input.type == RuleType.TRANSFER,
                categoryId = input.categoryId,
                isIncome = input.type == RuleType.INCOME,
                amount = input.amount,
                note = input.note,
                firstDate = input.firstDate,
                currency = input.currency,
                accountType = input.accountType,
                destinationAccountType = input.destinationAccountType
            )
            withContext(Dispatchers.Main) {
                if (!isAdded) return@withContext
                if (ok) {
                    CloudSyncService.scheduleUpload(requireContext())
                    Toast.makeText(requireContext(), "Recurrente guardado", Toast.LENGTH_SHORT).show()
                    findNavController().popBackStack()
                } else {
                    Toast.makeText(requireContext(), "Revisa los datos del recurrente", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun RecurringTransactionEntity.toInitial(): RecurringRuleInitial =
        RecurringRuleInitial(
            templateId = id,
            sourceTransactionId = sourceTransactionId,
            type = when {
                isTransfer == 1 -> RuleType.TRANSFER
                isIncome == 1 -> RuleType.INCOME
                else -> RuleType.EXPENSE
            },
            amount = amount.toString(),
            categoryId = categoryId,
            frequency = frequency,
            firstDate = firstDate,
            note = note.orEmpty(),
            active = isActive == 1,
            accountType = accountType,
            destinationAccountType = destinationAccountType ?: "CASH",
            currency = currency
        )

    companion object {
        const val ARG_TEMPLATE_ID = "template_id"
    }
}

private enum class RuleType(val label: String) {
    EXPENSE("Gasto"),
    INCOME("Ingreso"),
    TRANSFER("Transferencia")
}

private data class RecurringRuleInitial(
    val templateId: Int? = null,
    val sourceTransactionId: Int? = null,
    val type: RuleType = RuleType.EXPENSE,
    val amount: String = "",
    val categoryId: Int = 0,
    val frequency: String = RecurringTransactionStore.FREQUENCY_MONTHLY,
    val firstDate: Long = System.currentTimeMillis(),
    val note: String = "",
    val active: Boolean = true,
    val accountType: String = "CARD",
    val destinationAccountType: String = "CASH",
    val currency: String = "PEN"
)

private data class RecurringRuleInput(
    val templateId: Int?,
    val sourceTransactionId: Int?,
    val type: RuleType,
    val amount: Double,
    val categoryId: Int,
    val frequency: String,
    val firstDate: Long,
    val note: String,
    val active: Boolean,
    val accountType: String,
    val destinationAccountType: String,
    val currency: String
)

@Composable
private fun RecurringRuleScreen(
    categories: List<Categoria>,
    initial: RecurringRuleInitial?,
    onBack: () -> Unit,
    onSave: (RecurringRuleInput) -> Unit
) {
    if (initial == null) return
    var type by remember(initial) { mutableStateOf(initial.type) }
    var amount by remember(initial) { mutableStateOf(initial.amount) }
    var categoryId by remember(initial) { mutableStateOf(initial.categoryId) }
    var frequency by remember(initial) { mutableStateOf(initial.frequency) }
    var firstDateText by remember(initial) { mutableStateOf(formatDate(initial.firstDate)) }
    var note by remember(initial) { mutableStateOf(initial.note) }
    var active by remember(initial) { mutableStateOf(initial.active) }
    var accountType by remember(initial) { mutableStateOf(initial.accountType) }
    var destinationType by remember(initial) { mutableStateOf(initial.destinationAccountType) }
    var error by remember(initial) { mutableStateOf<String?>(null) }

    val filteredCategories = categories.filter {
        when (type) {
            RuleType.INCOME -> it.esIngreso
            RuleType.EXPENSE -> !it.esIngreso
            RuleType.TRANSFER -> false
        }
    }
    if (type != RuleType.TRANSFER && categoryId <= 0 && filteredCategories.isNotEmpty()) {
        categoryId = filteredCategories.first().id
    }

    val firstDate = parseDate(firstDateText)
    val amountValue = amount.replace(',', '.').toDoubleOrNull()
    val nextRun = if (firstDate != null) previewNextRun(firstDate, frequency, type, amountValue ?: 0.0, categoryId, accountType, destinationType) else null

    Surface(modifier = Modifier.fillMaxSize(), color = Color.Transparent) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 22.dp, vertical = 24.dp)
        ) {
            RuleHeader(if (initial.templateId == null) "Nuevo recurrente" else "Editar recurrente", onBack)
            Text(
                "Configura la regla. Spendly creara transacciones despues segun esta frecuencia.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 6.dp, bottom = 18.dp)
            )

            RuleSection(title = "Tipo y monto", icon = R.drawable.ic_repeat) {
                SegmentedOptions(RuleType.entries, type, { it.label }) { type = it }
                Spacer(Modifier.height(12.dp))
                RuleTextField("Monto", amount, { amount = it }, KeyboardType.Decimal)
                if (type != RuleType.TRANSFER) {
                    Spacer(Modifier.height(12.dp))
                    SimpleChoice(
                        label = "Categoria",
                        value = filteredCategories.firstOrNull { it.id == categoryId }?.nombre ?: "Selecciona categoria",
                        options = filteredCategories.map { it.nombre },
                        onPick = { label -> categoryId = filteredCategories.firstOrNull { it.nombre == label }?.id ?: categoryId }
                    )
                }
            }

            RuleSection(title = "Cuenta", icon = R.drawable.ic_account_balance_wallet) {
                SimpleChoice(
                    label = "Origen",
                    value = accountLabel(accountType),
                    options = listOf("Tarjeta/cuenta", "Efectivo"),
                    onPick = { accountType = if (it == "Efectivo") "CASH" else "CARD" }
                )
                if (type == RuleType.TRANSFER) {
                    Spacer(Modifier.height(12.dp))
                    SimpleChoice(
                        label = "Destino",
                        value = accountLabel(destinationType),
                        options = listOf("Efectivo", "Tarjeta/cuenta"),
                        onPick = { destinationType = if (it == "Efectivo") "CASH" else "CARD" }
                    )
                }
            }

            RuleSection(title = "Programacion", icon = R.drawable.ic_calendar_month) {
                SimpleChoice(
                    label = "Frecuencia",
                    value = frequencyLabel(frequency),
                    options = frequencyOptions.map { frequencyLabel(it) },
                    onPick = { picked -> frequency = frequencyOptions.first { frequencyLabel(it) == picked } }
                )
                Spacer(Modifier.height(12.dp))
                RuleTextField("Fecha de inicio (dd/MM/yyyy)", firstDateText, { firstDateText = it }, KeyboardType.Number)
                Spacer(Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Switch(checked = active, onCheckedChange = { active = it })
                    Text(if (active) "Activo" else "Pausado", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.SemiBold)
                }
                Text(
                    "Proxima ejecucion: ${nextRun ?: "pendiente"}",
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(top = 10.dp)
                )
            }

            RuleSection(title = "Descripcion", icon = R.drawable.ic_description) {
                RuleTextField("Nota o descripcion", note, { note = it }, KeyboardType.Text)
            }

            error?.let {
                Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(bottom = 12.dp))
            }
            SpendlyPrimaryButton(
                text = "Guardar recurrente",
                onClick = {
                    val parsedDate = parseDate(firstDateText)
                    val parsedAmount = amount.replace(',', '.').toDoubleOrNull()
                    val validation = when {
                        parsedAmount == null || parsedAmount <= 0.0 -> "Ingresa un monto valido."
                        parsedDate == null -> "Ingresa una fecha valida."
                        type != RuleType.TRANSFER && categoryId <= 0 -> "Selecciona una categoria."
                        else -> null
                    }
                    if (validation != null) {
                        error = validation
                    } else {
                        error = null
                        onSave(
                            RecurringRuleInput(
                                templateId = initial.templateId,
                                sourceTransactionId = initial.sourceTransactionId,
                                type = type,
                                amount = parsedAmount!!,
                                categoryId = categoryId,
                                frequency = frequency,
                                firstDate = parsedDate!!,
                                note = note,
                                active = active,
                                accountType = accountType,
                                destinationAccountType = destinationType,
                                currency = initial.currency.ifBlank { "PEN" }
                            )
                        )
                    }
                }
            )
            Spacer(Modifier.height(14.dp))
            SpendlyOutlinedButton(text = "Cancelar", onClick = onBack)
        }
    }
}

@Composable
private fun RuleHeader(title: String, onBack: () -> Unit) {
    Box(modifier = Modifier.fillMaxWidth().height(44.dp)) {
        IconButton(onClick = onBack, modifier = Modifier.align(Alignment.CenterStart).size(44.dp)) {
            Icon(painterResource(R.drawable.ic_arrow_back), contentDescription = null, tint = MaterialTheme.colorScheme.onSurface)
        }
        Text(title, modifier = Modifier.align(Alignment.Center), color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold, fontSize = 22.sp)
    }
}

@Composable
private fun RuleSection(title: String, icon: Int, content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(bottom = 14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(
                    modifier = Modifier.size(34.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(painterResource(icon), contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(19.dp))
                }
                Text(title, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold, fontSize = 17.sp)
            }
            Spacer(Modifier.height(14.dp))
            content()
        }
    }
}

@Composable
private fun <T> SegmentedOptions(options: List<T>, selected: T, label: (T) -> String, onPick: (T) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        options.forEach { option ->
            val active = option == selected
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(42.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
                    .clickable { onPick(option) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    label(option),
                    color = if (active) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp
                )
            }
        }
    }
}

@Composable
private fun RuleTextField(label: String, value: String, onChange: (String) -> Unit, keyboardType: KeyboardType) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        modifier = Modifier.fillMaxWidth(),
        label = { Text(label) },
        singleLine = true,
        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = keyboardType),
        shape = RoundedCornerShape(14.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            focusedLabelColor = MaterialTheme.colorScheme.primary,
            cursorColor = MaterialTheme.colorScheme.primary
        )
    )
}

@Composable
private fun SimpleChoice(label: String, value: String, options: List<String>, onPick: (String) -> Unit) {
    Column {
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp, modifier = Modifier.padding(bottom = 8.dp))
        options.chunked(2).forEachIndexed { rowIndex, rowOptions ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = if (rowIndex == 0) 0.dp else 8.dp)
            ) {
                rowOptions.forEach { option ->
                    val active = value == option
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(42.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (active) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
                            .clickable { onPick(option) },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            option,
                            color = if (active) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 12.sp,
                            maxLines = 1
                        )
                    }
                }
                if (rowOptions.size == 1) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

private val frequencyOptions = listOf(
    RecurringTransactionStore.FREQUENCY_WEEKDAYS,
    RecurringTransactionStore.FREQUENCY_EVERYDAY,
    RecurringTransactionStore.FREQUENCY_WEEKLY,
    RecurringTransactionStore.FREQUENCY_MONTHLY
)

private fun frequencyLabel(value: String): String = when (value) {
    RecurringTransactionStore.FREQUENCY_WEEKDAYS -> "Lun-vie"
    RecurringTransactionStore.FREQUENCY_EVERYDAY -> "Diario"
    RecurringTransactionStore.FREQUENCY_WEEKLY -> "Semanal"
    RecurringTransactionStore.FREQUENCY_MONTHLY -> "Mensual"
    else -> "Mensual"
}

private fun accountLabel(value: String): String = if (value.equals("CASH", true)) "Efectivo" else "Tarjeta/cuenta"

private fun formatDate(value: Long): String = SimpleDateFormat("dd/MM/yyyy", Locale("es", "PE")).format(Date(value))

private fun parseDate(value: String): Long? = runCatching {
    val date = SimpleDateFormat("dd/MM/yyyy", Locale("es", "PE")).apply { isLenient = false }.parse(value.trim()) ?: return null
    Calendar.getInstance().apply {
        time = date
        set(Calendar.HOUR_OF_DAY, 9)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis
}.getOrNull()

private fun previewNextRun(
    firstDate: Long,
    frequency: String,
    type: RuleType,
    amount: Double,
    categoryId: Int,
    accountType: String,
    destinationType: String
): String? {
    val entity = RecurringTransactionEntity(
        id = 1,
        userId = 1,
        sourceTransactionId = -1,
        frequency = frequency,
        daysMask = 0,
        isActive = 1,
        isTransfer = if (type == RuleType.TRANSFER) 1 else 0,
        categoryId = categoryId,
        isIncome = if (type == RuleType.INCOME) 1 else 0,
        amount = amount,
        currency = "PEN",
        accountType = accountType,
        destinationAccountType = destinationType,
        note = "",
        labelId = null,
        firstDate = firstDate,
        lastGeneratedDay = null
    )
    val next = RecurringTransactionStore.nextTriggerAtMillis(entity) ?: return null
    return "${SimpleDateFormat("dd/MM/yyyy HH:mm", Locale("es", "PE")).format(Date(next))} • ${Format.money(amount, "PEN")}"
}
