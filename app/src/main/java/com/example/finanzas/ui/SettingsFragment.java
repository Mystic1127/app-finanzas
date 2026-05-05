package com.example.finanzas.ui;

import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavOptions;
import androidx.navigation.fragment.NavHostFragment;

import com.example.finanzas.R;
import com.example.finanzas.data.api.SettingsService;
import com.example.finanzas.data.local.LocalRepository;
import com.example.finanzas.ui.viewmodel.BudgetViewModel;
import com.example.finanzas.ui.viewmodel.HomeViewModel;
import com.example.finanzas.ui.viewmodel.ReportsViewModel;
import com.example.finanzas.ui.viewmodel.TransactionsViewModel;
import com.example.finanzas.util.CurrencyConverter;
import com.example.finanzas.util.Prefs;
import com.example.finanzas.util.PinSession;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

public class SettingsFragment extends Fragment {
    private LinearLayout accountSection;
    private LinearLayout prefsSection;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_settings, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        accountSection = view.findViewById(R.id.settingsAccountSection);
        prefsSection = view.findViewById(R.id.settingsPrefsSection);
        view.findViewById(R.id.btnSettingsBack).setOnClickListener(v -> NavHostFragment.findNavController(this).popBackStack());
        renderRows();
    }

    private void renderRows() {
        accountSection.removeAllViews();
        prefsSection.removeAllViews();
        addRow(accountSection, R.drawable.ic_reports, "Reportes financieros", null, () -> navigate(R.id.nav_reports));
        addRow(accountSection, R.drawable.ic_upload, "Importaciones", null, () -> navigate(R.id.nav_imports));
        addRow(accountSection, R.drawable.ic_profile, "Perfil", null, () -> navigate(R.id.nav_perfil));
        addRow(accountSection, R.drawable.ic_logout, "Cerrar sesión", null, this::logout);

        addRow(prefsSection, R.drawable.ic_initial_balance, "Saldo inicial", "Establece tu saldo de inicio", () -> navigate(R.id.nav_perfil));
        addRow(prefsSection, R.drawable.ic_theme, "Tema", themeLabel(), this::showThemeDialog);
        addRow(prefsSection, R.drawable.ic_currency, "Moneda", SettingsService.getCurrencyCode(requireContext()), this::showCurrencyDialog);
    }

    private void addRow(@NonNull LinearLayout parent, @DrawableRes int iconRes, @NonNull String title, @Nullable String subtitle, @NonNull Runnable action) {
        if (parent.getChildCount() > 0) {
            View divider = new View(requireContext());
            divider.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.md_theme_outlineVariant));
            LinearLayout.LayoutParams dividerParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(1));
            dividerParams.leftMargin = dp(56);
            parent.addView(divider, dividerParams);
        }

        LinearLayout row = new LinearLayout(requireContext());
        row.setGravity(android.view.Gravity.CENTER_VERTICAL);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(0, dp(16), 0, dp(16));
        row.setClickable(true);
        row.setFocusable(true);
        row.setOnClickListener(v -> action.run());

        ImageView icon = new ImageView(requireContext());
        icon.setImageResource(iconRes);
        icon.setColorFilter(ContextCompat.getColor(requireContext(), R.color.md_theme_primary));
        GradientDrawable iconBg = new GradientDrawable();
        iconBg.setShape(GradientDrawable.OVAL);
        iconBg.setColor(ContextCompat.getColor(requireContext(), R.color.md_theme_surfaceContainer));
        iconBg.setStroke(dp(1), ContextCompat.getColor(requireContext(), R.color.md_theme_outlineVariant));
        icon.setBackground(iconBg);
        icon.setPadding(dp(12), dp(12), dp(12), dp(12));
        row.addView(icon, new LinearLayout.LayoutParams(dp(56), dp(56)));

        LinearLayout texts = new LinearLayout(requireContext());
        texts.setOrientation(LinearLayout.VERTICAL);
        TextView titleView = new TextView(requireContext());
        titleView.setText(title);
        titleView.setTextColor(ContextCompat.getColor(requireContext(), R.color.md_theme_onSurface));
        titleView.setTextSize(18f);
        titleView.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        texts.addView(titleView);
        if (subtitle != null && !subtitle.trim().isEmpty()) {
            TextView subtitleView = new TextView(requireContext());
            subtitleView.setText(subtitle);
            subtitleView.setTextColor(ContextCompat.getColor(requireContext(), R.color.md_theme_onSurfaceVariant));
            subtitleView.setTextSize(15f);
            texts.addView(subtitleView);
        }
        LinearLayout.LayoutParams textParams = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        textParams.leftMargin = dp(18);
        row.addView(texts, textParams);

        ImageView chevron = new ImageView(requireContext());
        chevron.setImageResource(R.drawable.ic_chevron_right);
        chevron.setColorFilter(ContextCompat.getColor(requireContext(), R.color.md_theme_onSurface));
        row.addView(chevron, new LinearLayout.LayoutParams(dp(28), dp(28)));

        parent.addView(row, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
    }

    private void showThemeDialog() {
        String[] labels = new String[] {
                getString(R.string.perfil_theme_system),
                getString(R.string.perfil_theme_light),
                getString(R.string.perfil_theme_dark)
        };
        String current = SettingsService.getThemeMode(requireContext());
        int checked = SettingsService.THEME_LIGHT.equals(current) ? 1 : SettingsService.THEME_DARK.equals(current) ? 2 : 0;
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.perfil_theme_title)
                .setSingleChoiceItems(labels, checked, (dialog, which) -> {
                    String mode = which == 1 ? SettingsService.THEME_LIGHT : which == 2 ? SettingsService.THEME_DARK : SettingsService.THEME_SYSTEM;
                    SettingsService.saveThemeMode(requireContext(), mode);
                    SettingsService.applyThemeMode(requireContext());
                    dialog.dismiss();
                    renderRows();
                })
                .show();
    }

    private void showCurrencyDialog() {
        java.util.List<String> currencies = CurrencyConverter.supportedCurrencies();
        String[] labels = currencies.toArray(new String[0]);
        String current = SettingsService.getCurrencyCode(requireContext());
        int checked = Math.max(0, currencies.indexOf(current));
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.perfil_currency_title)
                .setSingleChoiceItems(labels, checked, (dialog, which) -> {
                    SettingsService.saveCurrency(requireContext(), labels[which], SettingsService.getManualRate(requireContext()), new SettingsService.SaveCb() {
                        @Override
                        public void onSuccess() {
                            clearScopedViewModelCaches();
                            renderRows();
                            Toast.makeText(requireContext(), R.string.perfil_currency_saved, Toast.LENGTH_SHORT).show();
                        }

                        @Override
                        public void onFail() {
                            Toast.makeText(requireContext(), R.string.perfil_currency_error, Toast.LENGTH_SHORT).show();
                        }
                    });
                    dialog.dismiss();
                })
                .show();
    }

    private String themeLabel() {
        String mode = SettingsService.getThemeMode(requireContext());
        if (SettingsService.THEME_LIGHT.equals(mode)) return getString(R.string.perfil_theme_light);
        if (SettingsService.THEME_DARK.equals(mode)) return getString(R.string.perfil_theme_dark);
        return getString(R.string.perfil_theme_system);
    }

    private void navigate(int destination) {
        NavHostFragment.findNavController(this).navigate(destination);
    }

    private void logout() {
        Prefs.clearAuth(requireContext());
        clearScopedViewModelCaches();
        LocalRepository.invalidateDataVersion();
        PinSession.lock();
        Toast.makeText(requireContext(), "Sesión cerrada", Toast.LENGTH_SHORT).show();
        NavOptions out = new NavOptions.Builder()
                .setPopUpTo(R.id.nav_graph, true)
                .build();
        NavHostFragment.findNavController(this).navigate(R.id.nav_welcome, null, out);
    }

    private void clearScopedViewModelCaches() {
        ViewModelProvider provider = new ViewModelProvider(requireActivity());
        provider.get(HomeViewModel.class).clearCache();
        provider.get(TransactionsViewModel.class).clearCache();
        provider.get(ReportsViewModel.class).clearCache();
        provider.get(BudgetViewModel.class).clearCache();
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
