package com.example.finanzas.ui;

import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;

import com.example.finanzas.R;
import com.example.finanzas.data.api.CategoryStore;
import com.example.finanzas.data.api.SettingsService;
import com.example.finanzas.data.model.Categoria;
import com.example.finanzas.data.model.ReceiptDraft;
import com.example.finanzas.ui.viewmodel.ReceiptScanViewModel;
import com.example.finanzas.util.CurrencyConverter;
import com.example.finanzas.util.UiFormUtils;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class ReceiptScanFragment extends Fragment {

    private TextView tvReceiptPro;
    private TextView tvReceiptImageStatus;
    private TextInputLayout tilAmount;
    private TextInputLayout tilDate;
    private TextInputLayout tilCategory;
    private TextInputLayout tilCurrency;
    private TextInputEditText etAmount;
    private TextInputEditText etDate;
    private TextInputEditText etMerchant;
    private MaterialAutoCompleteTextView actCategory;
    private MaterialAutoCompleteTextView actCurrency;
    private MaterialButton btnSelectImage;
    private MaterialButton btnSimulate;
    private MaterialButton btnCreate;
    private ReceiptScanViewModel viewModel;
    private final List<Categoria> expenseCategories = new ArrayList<>();

    private final ActivityResultLauncher<String> imagePicker =
            registerForActivityResult(new ActivityResultContracts.GetContent(), this::onReceiptImageSelected);

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_receipt_scan, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View v, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(v, savedInstanceState);

        tvReceiptPro = v.findViewById(R.id.tvReceiptPro);
        tvReceiptImageStatus = v.findViewById(R.id.tvReceiptImageStatus);
        tilAmount = v.findViewById(R.id.tilReceiptAmount);
        tilDate = v.findViewById(R.id.tilReceiptDate);
        tilCategory = v.findViewById(R.id.tilReceiptCategory);
        tilCurrency = v.findViewById(R.id.tilReceiptCurrency);
        etAmount = v.findViewById(R.id.etReceiptAmount);
        etDate = v.findViewById(R.id.etReceiptDate);
        etMerchant = v.findViewById(R.id.etReceiptMerchant);
        actCategory = v.findViewById(R.id.actReceiptCategory);
        actCurrency = v.findViewById(R.id.actReceiptCurrency);
        btnSelectImage = v.findViewById(R.id.btnSelectReceiptImage);
        btnSimulate = v.findViewById(R.id.btnSimulateReceipt);
        btnCreate = v.findViewById(R.id.btnCreateReceiptTransaction);

        viewModel = new ViewModelProvider(this).get(ReceiptScanViewModel.class);

        setupCurrencySelector();
        setupCategorySelector();
        if (etDate != null) {
            UiFormUtils.bindDatePicker(requireContext(), etDate);
            etDate.setText(UiFormUtils.formatUiDate(new Date()));
        }
        UiFormUtils.clearErrorOnTextChange(etAmount, etDate, actCategory, actCurrency);

        btnSelectImage.setOnClickListener(view -> imagePicker.launch("image/*"));
        btnSimulate.setOnClickListener(view -> viewModel.simulateDetectedReceipt());
        btnCreate.setOnClickListener(this::createTransaction);

        observeViewModel();
        loadCategories();
    }

    @Override
    public void onResume() {
        super.onResume();
        viewModel.refreshProState();
    }

    private void setupCurrencySelector() {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_list_item_1,
                CurrencyConverter.supportedCurrencies()
        );
        actCurrency.setAdapter(adapter);
        actCurrency.setText(SettingsService.getCurrencyCode(requireContext()), false);
        updateAmountPrefix();
        actCurrency.setOnFocusChangeListener((view, hasFocus) -> { if (hasFocus) actCurrency.showDropDown(); });
        actCurrency.setOnClickListener(view -> actCurrency.showDropDown());
        actCurrency.setOnItemClickListener((parent, view, position, id) -> updateAmountPrefix());
    }

    private void setupCategorySelector() {
        actCategory.setOnFocusChangeListener((view, hasFocus) -> { if (hasFocus) actCategory.showDropDown(); });
        actCategory.setOnClickListener(view -> actCategory.showDropDown());
    }

    private void loadCategories() {
        CategoryStore.loadOnce(requireContext(), new CategoryStore.Callback() {
            @Override
            public void onReady(List<? extends Categoria> cats) {
                expenseCategories.clear();
                List<String> names = new ArrayList<>();
                for (Categoria category : cats) {
                    if (category != null && !category.esIngreso) {
                        expenseCategories.add(category);
                        names.add(category.nombre);
                    }
                }
                actCategory.setAdapter(new ArrayAdapter<>(requireContext(), android.R.layout.simple_list_item_1, names));
            }

            @Override
            public void onError() {
                if (isAdded()) UiFormUtils.showMessage(requireView(), R.string.error_cargar_categorias);
            }
        });
    }

    private void observeViewModel() {
        viewModel.isProUser().observe(getViewLifecycleOwner(), this::renderProState);
        viewModel.getDraft().observe(getViewLifecycleOwner(), this::renderDraft);
        viewModel.getSaved().observe(getViewLifecycleOwner(), saved -> {
            if (Boolean.TRUE.equals(saved)) {
                UiFormUtils.showMessage(requireView(), R.string.receipt_transaction_created);
                NavHostFragment.findNavController(this).popBackStack();
            }
        });
        viewModel.getError().observe(getViewLifecycleOwner(), message -> {
            if (message != null && !message.trim().isEmpty()) {
                UiFormUtils.showMessage(requireView(), message);
            }
        });
    }

    private void renderProState(Boolean isPro) {
        boolean enabled = Boolean.TRUE.equals(isPro);
        tvReceiptPro.setText(enabled ? R.string.receipt_scan_ready : R.string.receipt_scan_pro_locked);
        btnSelectImage.setEnabled(enabled);
        btnSimulate.setEnabled(enabled);
        btnCreate.setEnabled(enabled);
        etAmount.setEnabled(enabled);
        etDate.setEnabled(enabled);
        etMerchant.setEnabled(enabled);
        actCategory.setEnabled(enabled);
        actCurrency.setEnabled(enabled);
    }

    private void renderDraft(ReceiptDraft draft) {
        if (draft == null) return;
        etAmount.setText(String.valueOf(draft.getMonto()));
        etMerchant.setText(draft.getComercio() == null ? "" : draft.getComercio());
        if (draft.getFecha() != null) etDate.setText(UiFormUtils.formatUiDate(draft.getFecha()));
        actCurrency.setText(CurrencyConverter.normalize(draft.getMoneda()), false);
        updateAmountPrefix();
    }

    private void onReceiptImageSelected(@Nullable Uri uri) {
        if (uri == null) return;
        tvReceiptImageStatus.setText(R.string.receipt_image_selected);
        // TODO: Connect ML Kit Text Recognition here and map detected text into ReceiptDraft.
    }

    private void createTransaction(View view) {
        UiFormUtils.clearErrors(etAmount, etDate, actCategory, actCurrency);
        double amount = parseAmount(etAmount.getText() == null ? "" : etAmount.getText().toString());
        if (amount <= 0.0) {
            tilAmount.setError(getString(R.string.error_monto_invalido));
            return;
        }

        Date date = UiFormUtils.parseUiDate(etDate.getText() == null ? "" : etDate.getText().toString().trim());
        if (date == null) {
            tilDate.setError(getString(R.string.error_formato_fecha));
            return;
        }

        Categoria category = selectedCategory();
        if (category == null) {
            tilCategory.setError(getString(R.string.error_selecciona_categoria));
            return;
        }

        String currency = CurrencyConverter.normalize(actCurrency.getText() == null ? "" : actCurrency.getText().toString());
        if (!CurrencyConverter.supportedCurrencies().contains(currency)) {
            tilCurrency.setError(getString(R.string.transaction_currency_error));
            return;
        }

        String merchant = etMerchant.getText() == null ? "" : etMerchant.getText().toString().trim();
        viewModel.createTransactionFromReceipt(category.id, amount, date.getTime(), merchant, currency);
    }

    @Nullable
    private Categoria selectedCategory() {
        String name = actCategory.getText() == null ? "" : actCategory.getText().toString().trim();
        if (TextUtils.isEmpty(name)) return null;
        for (Categoria category : expenseCategories) {
            if (category != null && name.equals(category.nombre)) return category;
        }
        return null;
    }

    private void updateAmountPrefix() {
        if (tilAmount != null) {
            String currency = actCurrency == null || actCurrency.getText() == null
                    ? SettingsService.getCurrencyCode(requireContext())
                    : actCurrency.getText().toString();
            tilAmount.setPrefixText(SettingsService.getCurrencySymbol(currency) + " ");
        }
    }

    private double parseAmount(String raw) {
        if (raw == null) return 0.0;
        String clean = raw.trim().replaceAll("[^0-9,.-]", "");
        if (clean.isEmpty()) return 0.0;
        int lastComma = clean.lastIndexOf(',');
        int lastDot = clean.lastIndexOf('.');
        if (lastComma >= 0 && lastDot >= 0 && lastComma > lastDot) {
            clean = clean.replace(".", "").replace(',', '.');
        } else if (lastComma >= 0 && lastDot >= 0) {
            clean = clean.replace(",", "");
        } else if (lastComma >= 0) {
            clean = clean.replace(',', '.');
        }
        try {
            return Math.abs(Double.parseDouble(clean));
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }
}
