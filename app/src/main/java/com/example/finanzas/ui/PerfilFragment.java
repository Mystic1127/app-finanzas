package com.example.finanzas.ui;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.example.finanzas.R;
import com.example.finanzas.data.api.SettingsService;
import com.example.finanzas.data.api.UserService;
import com.example.finanzas.util.CurrencyConverter;
import com.example.finanzas.util.PerfLogger;
import com.example.finanzas.util.Prefs;
import com.example.finanzas.util.UiFormUtils;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class PerfilFragment extends Fragment {

    private static final String CURRENCY_PEN = "PEN (S/)";
    private static final String CURRENCY_USD = "USD ($)";
    private static final String CURRENCY_EUR = "EUR (€)";

    private MaterialButton btnRemovePin;
    private MaterialAutoCompleteTextView actCurrency;
    private TextInputEditText etManualRate;
    private TextInputEditText etInitialCash;
    private TextInputEditText etInitialCard;
    private MaterialAutoCompleteTextView actInitialBalancesCurrency;
    private TextInputLayout tilCurrency;
    private TextInputLayout tilManualRate;
    private TextInputLayout tilInitialCash;
    private TextInputLayout tilInitialCard;
    private long perfStartMs;
    private long loadStartMs;
    private boolean firstRenderLogged;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_perfil, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View v, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(v, savedInstanceState);
        perfStartMs = PerfLogger.now();
        loadStartMs = PerfLogger.now();
        firstRenderLogged = false;

        TextView tvNombre = v.findViewById(R.id.tvPerfilNombre);
        TextView tvEmail  = v.findViewById(R.id.tvPerfilEmail);
        MaterialButton btnCambiarPass = v.findViewById(R.id.btnCambiarPass);
        MaterialButton btnConfigPin = v.findViewById(R.id.btnConfigPin);
        MaterialButton btnSaveCurrency = v.findViewById(R.id.btnSaveCurrency);
        MaterialButton btnSaveInitialBalances = v.findViewById(R.id.btnSaveInitialBalances);
        btnRemovePin = v.findViewById(R.id.btnRemovePin);
        actCurrency = v.findViewById(R.id.actCurrency);
        etManualRate = v.findViewById(R.id.etManualRate);
        actInitialBalancesCurrency = v.findViewById(R.id.actInitialBalancesCurrency);
        etInitialCash = v.findViewById(R.id.etInitialCash);
        etInitialCard = v.findViewById(R.id.etInitialCard);
        tilCurrency = v.findViewById(R.id.tilCurrency);
        tilManualRate = v.findViewById(R.id.tilManualRate);
        tilInitialCash = v.findViewById(R.id.tilInitialCash);
        tilInitialCard = v.findViewById(R.id.tilInitialCard);

        String cachedNombre = Prefs.getCurrentUserName(requireContext());
        String cachedEmail  = Prefs.getCurrentUserEmail(requireContext());
        if (cachedNombre == null || cachedNombre.isEmpty()) cachedNombre = "—";
        if (cachedEmail == null || cachedEmail.isEmpty()) cachedEmail = "—";
        tvNombre.setText(cachedNombre);
        tvEmail.setText(cachedEmail);
        logFirstRender();

        if (getContext() != null && cachedEmail != null && !"—".equals(cachedEmail)) {
            Context appContext = requireContext().getApplicationContext();
            String emailForLookup = cachedEmail;
            ExecutorService executor = Executors.newSingleThreadExecutor();
            executor.execute(() -> UserService.getMe(appContext, emailForLookup, new UserService.MeCb() {
                @Override public void onOk(int id, String nom, String em) {
                    if (!isAdded()) {
                        executor.shutdown();
                        return;
                    }
                    requireActivity().runOnUiThread(() -> {
                        if (!isAdded()) return;
                        tvNombre.setText(nom);
                        tvEmail.setText(em);
                        Prefs.setUserSession(requireContext(), id, em, nom);
                    });
                    executor.shutdown();
                }
                @Override public void onFail() {
                    executor.shutdown();
                }
            }));
        }

        setupCurrencyControls();
        setupInitialBalanceControls();
        v.post(() -> {
            if (!isAdded()) return;
            PerfLogger.log("PerfilFragment", "loadStart");
            loadCurrency();
            loadInitialBalances();
            PerfLogger.logSince("PerfilFragment", "loadComplete", loadStartMs);
        });
        btnSaveCurrency.setOnClickListener(view -> saveCurrency());
        btnSaveInitialBalances.setOnClickListener(view -> saveInitialBalances());

        btnCambiarPass.setOnClickListener(view ->
                Navigation.findNavController(view).navigate(R.id.nav_change_password));

        btnConfigPin.setOnClickListener(view ->
                Navigation.findNavController(view).navigate(R.id.nav_pin_setup));

        updatePinButtons();

        btnRemovePin.setOnClickListener(view -> {
            if (getContext() == null) return;
            new MaterialAlertDialogBuilder(requireContext())
                    .setTitle(R.string.perfil_remove_pin)
                    .setMessage(R.string.pin_setup_remove_confirm)
                    .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                        if (getContext() == null) return;
                        Prefs.clearPin(requireContext());
                        Toast.makeText(requireContext(), R.string.pin_removed_success, Toast.LENGTH_SHORT).show();
                        updatePinButtons();
                    })
                    .setNegativeButton(android.R.string.cancel, null)
                    .show();
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        updatePinButtons();
    }

    private void setupCurrencyControls() {
        List<String> currencies = Arrays.asList(CURRENCY_PEN, CURRENCY_USD, CURRENCY_EUR);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_list_item_1, currencies);
        actCurrency.setAdapter(adapter);
        actCurrency.setOnClickListener(v -> actCurrency.showDropDown());
        actCurrency.setOnFocusChangeListener((v, hasFocus) -> { if (hasFocus) actCurrency.showDropDown(); });
    }

    private void loadCurrency() {
        if (actCurrency == null || getContext() == null) return;
        String code = SettingsService.getCurrencyCode(requireContext());
        actCurrency.setText(labelForCurrency(code), false);
        double rate = SettingsService.getManualRate(requireContext());
        etManualRate.setText(rate > 0 ? String.format(Locale.US, "%.4f", rate) : "");
    }

    private void setupInitialBalanceControls() {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_list_item_1,
                CurrencyConverter.supportedCurrencies()
        );
        actInitialBalancesCurrency.setAdapter(adapter);
        actInitialBalancesCurrency.setOnClickListener(v -> actInitialBalancesCurrency.showDropDown());
        actInitialBalancesCurrency.setOnFocusChangeListener((v, hasFocus) -> { if (hasFocus) actInitialBalancesCurrency.showDropDown(); });
        actInitialBalancesCurrency.setOnItemClickListener((parent, view, position, id) -> updateInitialBalancePrefixes());
        UiFormUtils.clearErrorOnTextChange(etInitialCash, etInitialCard);
    }

    private void loadInitialBalances() {
        if (actInitialBalancesCurrency == null || getContext() == null) return;
        String currency = SettingsService.getInitialBalancesCurrency(requireContext());
        actInitialBalancesCurrency.setText(CurrencyConverter.normalize(currency), false);
        double cash = SettingsService.getInitialCashBalance(requireContext());
        double card = SettingsService.getInitialCardBalance(requireContext());
        etInitialCash.setText(cash > 0 ? String.format(Locale.US, "%.2f", cash) : "0");
        etInitialCard.setText(card > 0 ? String.format(Locale.US, "%.2f", card) : "0");
        updateInitialBalancePrefixes();
    }

    private void saveInitialBalances() {
        tilInitialCash.setError(null);
        tilInitialCard.setError(null);

        Double cash = parseAmount(etInitialCash.getText() == null ? "" : etInitialCash.getText().toString());
        Double card = parseAmount(etInitialCard.getText() == null ? "" : etInitialCard.getText().toString());
        if (cash == null) {
            tilInitialCash.setError(getString(R.string.error_monto_invalido));
            return;
        }
        if (card == null) {
            tilInitialCard.setError(getString(R.string.error_monto_invalido));
            return;
        }

        String currency = CurrencyConverter.normalize(
                actInitialBalancesCurrency.getText() == null ? "" : actInitialBalancesCurrency.getText().toString()
        );
        SettingsService.saveInitialBalances(requireContext(), cash, card, currency, new SettingsService.SaveCb() {
            @Override
            public void onSuccess() {
                if (isAdded()) Toast.makeText(requireContext(), R.string.perfil_initial_balances_saved, Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onFail() {
                if (isAdded()) Toast.makeText(requireContext(), R.string.perfil_initial_balances_error, Toast.LENGTH_SHORT).show();
            }
        });
        PerfLogger.logSince("PerfilFragment", "onViewCreated", perfStartMs);
    }

    private void logFirstRender() {
        if (!firstRenderLogged) {
            firstRenderLogged = true;
            PerfLogger.logSince("PerfilFragment", "firstRender", perfStartMs);
        }
    }

    private void saveCurrency() {
        tilCurrency.setError(null);
        tilManualRate.setError(null);

        String code = codeFromLabel(actCurrency.getText() == null ? "" : actCurrency.getText().toString());
        if (code.isEmpty()) {
            tilCurrency.setError(getString(R.string.perfil_currency_hint));
            return;
        }

        double rate = 0.0;
        String rawRate = etManualRate.getText() == null ? "" : etManualRate.getText().toString().trim();
        if (!rawRate.isEmpty()) {
            try {
                rate = Double.parseDouble(rawRate.replace(",", "."));
            } catch (NumberFormatException e) {
                tilManualRate.setError(getString(R.string.error_monto_invalido));
                return;
            }
        }

        SettingsService.saveCurrency(requireContext(), code, rate, new SettingsService.SaveCb() {
            @Override
            public void onSuccess() {
                if (isAdded()) Toast.makeText(requireContext(), R.string.perfil_currency_saved, Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onFail() {
                if (isAdded()) Toast.makeText(requireContext(), R.string.perfil_currency_error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private String labelForCurrency(String code) {
        if ("USD".equals(code)) return CURRENCY_USD;
        if ("EUR".equals(code)) return CURRENCY_EUR;
        return CURRENCY_PEN;
    }

    private String codeFromLabel(String label) {
        if (label == null) return "";
        String normalized = label.trim().toUpperCase(Locale.ROOT);
        if (normalized.startsWith("USD")) return "USD";
        if (normalized.startsWith("EUR")) return "EUR";
        if (normalized.startsWith("PEN")) return "PEN";
        return "";
    }

    private void updatePinButtons() {
        if (btnRemovePin == null || getContext() == null) return;
        btnRemovePin.setVisibility(Prefs.hasPin(requireContext()) ? View.VISIBLE : View.GONE);
    }

    @Nullable
    private Double parseAmount(@Nullable String raw) {
        if (raw == null) return 0.0;
        String clean = raw.trim();
        if (clean.isEmpty()) return null;
        clean = clean.replaceAll("[^0-9,.-]", "");
        if (clean.isEmpty()) return null;
        int lastComma = clean.lastIndexOf(',');
        int lastDot = clean.lastIndexOf('.');
        if (lastComma >= 0 && lastDot >= 0) {
            if (lastComma > lastDot) {
                clean = clean.replace(".", "").replace(',', '.');
            } else {
                clean = clean.replace(",", "");
            }
        } else if (lastComma >= 0) {
            clean = clean.replace(',', '.');
        }
        try {
            double value = Double.parseDouble(clean);
            return Double.isNaN(value) || Double.isInfinite(value) || value < 0 ? null : value;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private void updateInitialBalancePrefixes() {
        String currency = CurrencyConverter.normalize(
                actInitialBalancesCurrency.getText() == null ? "" : actInitialBalancesCurrency.getText().toString()
        );
        String prefix = SettingsService.getCurrencySymbol(currency) + " ";
        tilInitialCash.setPrefixText(prefix);
        tilInitialCard.setPrefixText(prefix);
    }
}
