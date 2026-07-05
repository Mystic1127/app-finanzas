package com.example.finanzas.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.finanzas.R
import com.example.finanzas.data.api.CategoryStore
import com.example.finanzas.data.api.SettingsService
import com.example.finanzas.data.local.LocalRepository
import com.example.finanzas.ui.view.SpendlyDecorBackgroundDrawable
import com.example.finanzas.ui.viewmodel.BudgetViewModel
import com.example.finanzas.ui.viewmodel.HomeViewModel
import com.example.finanzas.ui.viewmodel.ReportsViewModel
import com.example.finanzas.ui.viewmodel.TransactionsViewModel
import com.example.finanzas.util.CurrencyConverter
import com.example.finanzas.util.UiFormUtils
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class InitialBalanceFragment : Fragment() {
    private lateinit var cardForm: MaterialCardView
    private lateinit var tvStatus: TextView
    private lateinit var actCurrency: MaterialAutoCompleteTextView
    private lateinit var tilCash: TextInputLayout
    private lateinit var tilCard: TextInputLayout
    private lateinit var etCash: TextInputEditText
    private lateinit var etCard: TextInputEditText
    private lateinit var tvCashSymbol: TextView
    private lateinit var tvCardSymbol: TextView
    private lateinit var btnSave: MaterialButton

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.fragment_initial_balance, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        view.background = SpendlyDecorBackgroundDrawable(requireContext())
        cardForm = view.findViewById(R.id.cardInitialBalanceForm)
        tvStatus = view.findViewById(R.id.tvInitialBalanceStatus)
        actCurrency = view.findViewById(R.id.actInitialCurrency)
        tilCash = view.findViewById(R.id.tilInitialCash)
        tilCard = view.findViewById(R.id.tilInitialCard)
        etCash = view.findViewById(R.id.etInitialCash)
        etCard = view.findViewById(R.id.etInitialCard)
        tvCashSymbol = view.findViewById(R.id.tvInitialCashSymbol)
        tvCardSymbol = view.findViewById(R.id.tvInitialCardSymbol)
        btnSave = view.findViewById(R.id.btnSaveInitialBalance)
        view.findViewById<View>(R.id.btnInitialBalanceBack).setOnClickListener {
            findNavController().popBackStack()
        }

        setupCurrencySelector()
        UiFormUtils.clearErrorOnTextChange(etCash, etCard)
        btnSave.setOnClickListener { saveInitialBalances() }
        loadState()
    }

    private fun setupCurrencySelector() {
        val adapter = object : ArrayAdapter<String>(
            requireContext(),
            R.layout.item_currency_dropdown,
            CurrencyConverter.supportedCurrencies()
        ) {
            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                return bindCurrencyRow(position, convertView, parent)
            }

            override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
                return bindCurrencyRow(position, convertView, parent)
            }

            private fun bindCurrencyRow(position: Int, convertView: View?, parent: ViewGroup): View {
                val row = convertView ?: layoutInflater.inflate(R.layout.item_currency_dropdown, parent, false)
                val code = getItem(position).orEmpty()
                row.findViewById<TextView>(R.id.tvCurrencyCode).text = code
                row.findViewById<ImageView>(R.id.ivCurrencyFlag).setImageResource(flagForCurrency(code))
                return row
            }
        }
        actCurrency.setAdapter(adapter)
        actCurrency.setText(SettingsService.getCurrencyCode(requireContext()), false)
        actCurrency.setOnFocusChangeListener { _, hasFocus -> if (hasFocus) actCurrency.showDropDown() }
        actCurrency.setOnClickListener { actCurrency.showDropDown() }
        actCurrency.setOnItemClickListener { _, _, _, _ -> updateCurrencyPrefix() }
        updateCurrencyPrefix()
    }

    private fun updateCurrencyPrefix() {
        val symbol = SettingsService.getCurrencySymbol(CurrencyConverter.normalize(actCurrency.text?.toString()))
        tvCashSymbol.text = symbol
        tvCardSymbol.text = symbol
    }

    private fun flagForCurrency(code: String): Int = when (CurrencyConverter.normalize(code)) {
        "USD" -> R.drawable.ic_flag_us
        "EUR" -> R.drawable.ic_flag_eu
        "CLP" -> R.drawable.ic_flag_cl
        else -> R.drawable.ic_flag_pe
    }

    private fun loadState() {
        val appContext = requireContext().applicationContext
        viewLifecycleOwner.lifecycleScope.launch {
            val configured = withContext(Dispatchers.IO) {
                runCatching { LocalRepository.getInstance(appContext).hasInitialBalanceConfigured() }.getOrDefault(false)
            }
            if (!isAdded) return@launch
            cardForm.visibility = if (configured) View.GONE else View.VISIBLE
            tvStatus.visibility = if (configured) View.VISIBLE else View.GONE
            tvStatus.text = if (configured) {
                getString(R.string.perfil_initial_balances_configured) + "\n" + getString(R.string.perfil_initial_balances_locked_help)
            } else {
                ""
            }
        }
    }

    private fun saveInitialBalances() {
        tilCash.error = null
        tilCard.error = null
        val cash = parseAmount(etCash.text?.toString())
        val card = parseAmount(etCard.text?.toString())
        if (cash == null) {
            tilCash.error = getString(R.string.error_monto_invalido)
            return
        }
        if (card == null) {
            tilCard.error = getString(R.string.error_monto_invalido)
            return
        }
        if (cash <= 0.0 && card <= 0.0) {
            tilCash.error = getString(R.string.perfil_initial_balances_empty)
            tilCard.error = getString(R.string.perfil_initial_balances_empty)
            return
        }

        UiFormUtils.setActionLoading(btnSave, true)
        val currency = CurrencyConverter.normalize(actCurrency.text?.toString())
        val appContext = requireContext().applicationContext
        viewLifecycleOwner.lifecycleScope.launch {
            val saved = withContext(Dispatchers.IO) {
                runCatching { LocalRepository.getInstance(appContext).configureInitialBalances(cash, card, currency) }
                    .getOrDefault(false)
            }
            if (!isAdded) return@launch
            UiFormUtils.setActionLoading(btnSave, false)
            if (saved) {
                CategoryStore.clearCache()
                clearScopedViewModelCaches()
                etCash.setText("")
                etCard.setText("")
                Toast.makeText(requireContext(), R.string.perfil_initial_balances_saved, Toast.LENGTH_SHORT).show()
                loadState()
            } else {
                Toast.makeText(requireContext(), R.string.perfil_initial_balances_error, Toast.LENGTH_SHORT).show()
                loadState()
            }
        }
    }

    private fun parseAmount(raw: String?): Double? {
        if (raw == null) return 0.0
        var clean = raw.trim()
        if (clean.isEmpty()) return 0.0
        clean = clean.replace("[^0-9,.-]".toRegex(), "")
        if (clean.isEmpty()) return null
        val lastComma = clean.lastIndexOf(',')
        val lastDot = clean.lastIndexOf('.')
        clean = if (lastComma >= 0 && lastDot >= 0) {
            if (lastComma > lastDot) clean.replace(".", "").replace(',', '.') else clean.replace(",", "")
        } else if (lastComma >= 0) {
            clean.replace(',', '.')
        } else {
            clean
        }
        return runCatching { clean.toDouble() }
            .getOrNull()
            ?.takeIf { !it.isNaN() && !it.isInfinite() && it >= 0.0 }
    }

    private fun clearScopedViewModelCaches() {
        val provider = ViewModelProvider(requireActivity())
        provider.get(HomeViewModel::class.java).clearCache()
        provider.get(TransactionsViewModel::class.java).clearCache()
        provider.get(ReportsViewModel::class.java).clearCache()
        provider.get(BudgetViewModel::class.java).clearCache()
    }
}
