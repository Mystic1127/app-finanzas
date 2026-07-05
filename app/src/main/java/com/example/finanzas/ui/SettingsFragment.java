package com.example.finanzas.ui;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavOptions;
import androidx.navigation.fragment.NavHostFragment;

import com.example.finanzas.R;
import com.example.finanzas.data.api.SettingsService;
import com.example.finanzas.data.local.LocalRepository;
import com.example.finanzas.ui.view.SpendlyDecorBackgroundDrawable;
import com.example.finanzas.ui.viewmodel.BudgetViewModel;
import com.example.finanzas.ui.viewmodel.HomeViewModel;
import com.example.finanzas.ui.viewmodel.ReportsViewModel;
import com.example.finanzas.ui.viewmodel.TransactionsViewModel;
import com.example.finanzas.util.CurrencyConverter;
import com.example.finanzas.util.NavigationAnimations;
import com.example.finanzas.util.Prefs;
import com.example.finanzas.util.PinSession;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;

import java.util.ArrayList;
import java.util.List;

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
        view.setBackground(new SpendlyDecorBackgroundDrawable(requireContext()));
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
        addRow(accountSection, R.drawable.ic_repeat, "Gastos recurrentes", "Gestiona reglas automaticas", () -> navigate(R.id.nav_recurring_settings));
        addRow(accountSection, R.drawable.ic_group_24, "Cuentas del dispositivo", "Quita cuentas guardadas de forma segura", () -> navigate(R.id.nav_device_accounts));
        addRow(accountSection, R.drawable.ic_logout, "Cerrar sesión", null, this::confirmLogout);

        addRow(prefsSection, R.drawable.ic_category, getString(R.string.categories_title), getString(R.string.categories_subtitle), () -> navigate(R.id.nav_categories));
        addRow(prefsSection, R.drawable.ic_initial_balance, "Saldo inicial", "Establece tu saldo de inicio", () -> navigate(R.id.nav_initial_balance));
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
        String[] labels = new String[]{
                getString(R.string.perfil_theme_system),
                getString(R.string.perfil_theme_light),
                getString(R.string.perfil_theme_dark)
        };
        int[] icons = new int[]{
                R.drawable.ic_brightness_6,
                R.drawable.ic_light_mode,
                R.drawable.ic_dark_mode
        };
        String currentMode = SettingsService.getThemeMode(requireContext());
        final int[] selected = {
                SettingsService.THEME_DARK.equals(currentMode)
                        ? 2
                        : SettingsService.THEME_LIGHT.equals(currentMode) ? 1 : 0
        };
        showChoiceDialog(
                R.drawable.ic_palette,
                "Tema",
                "Elige la apariencia de la app",
                labels,
                icons,
                null,
                selected,
                which -> {
                    String mode = which == 1
                            ? SettingsService.THEME_LIGHT
                            : which == 2 ? SettingsService.THEME_DARK : SettingsService.THEME_SYSTEM;
                    SettingsService.saveThemeMode(requireContext(), mode);
                    SettingsService.applyThemeMode(requireContext());
                    renderRows();
                }
        );
    }

    private void showCurrencyDialog() {
        String[] labels = new String[]{"PEN", "USD", "EUR", "CLP"};
        int[] flags = new int[]{
                R.drawable.ic_flag_pe,
                R.drawable.ic_flag_us,
                R.drawable.ic_flag_eu,
                R.drawable.ic_flag_cl
        };
        String currentCurrency = SettingsService.getCurrencyCode(requireContext());
        int currentIndex = 0;
        for (int i = 0; i < labels.length; i++) {
            if (labels[i].equalsIgnoreCase(currentCurrency)) {
                currentIndex = i;
                break;
            }
        }
        final int[] selected = {currentIndex};
        showChoiceDialog(
                R.drawable.ic_currency,
                "Moneda principal",
                "Selecciona la moneda que usarás por defecto",
                labels,
                null,
                flags,
                selected,
                which -> {
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
                }
        );
    }

    private interface ChoiceDone {
        void onDone(int selected);
    }

    private void showChoiceDialog(
            @DrawableRes int headerIcon,
            @NonNull String title,
            @NonNull String subtitle,
            @NonNull String[] labels,
            @Nullable int[] optionIcons,
            @Nullable int[] trailingIcons,
            @NonNull int[] selected,
            @NonNull ChoiceDone done
    ) {
        LinearLayout root = new LinearLayout(requireContext());
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(20), dp(20), dp(20), dp(20));
        root.setBackground(roundedDrawable(color(R.color.dialog_surface), dp(26)));

        LinearLayout header = new LinearLayout(requireContext());
        header.setGravity(android.view.Gravity.CENTER_VERTICAL);
        header.setOrientation(LinearLayout.HORIZONTAL);

        ImageView icon = new ImageView(requireContext());
        icon.setImageResource(headerIcon);
        icon.setColorFilter(color(R.color.md_theme_onPrimaryContainer));
        icon.setPadding(dp(11), dp(11), dp(11), dp(11));
        icon.setBackground(ovalDrawable(color(R.color.md_theme_primaryContainer)));
        header.addView(icon, new LinearLayout.LayoutParams(dp(50), dp(50)));

        LinearLayout headerTexts = new LinearLayout(requireContext());
        headerTexts.setOrientation(LinearLayout.VERTICAL);
        TextView titleView = new TextView(requireContext());
        titleView.setText(title);
        titleView.setTextColor(color(R.color.md_theme_onSurface));
        titleView.setTextSize(22f);
        titleView.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        headerTexts.addView(titleView);
        TextView subtitleView = new TextView(requireContext());
        subtitleView.setText(subtitle);
        subtitleView.setTextColor(color(R.color.md_theme_onSurfaceVariant));
        subtitleView.setTextSize(14f);
        LinearLayout.LayoutParams subtitleParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        subtitleParams.topMargin = dp(2);
        headerTexts.addView(subtitleView, subtitleParams);
        LinearLayout.LayoutParams headerTextParams = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        headerTextParams.leftMargin = dp(14);
        header.addView(headerTexts, headerTextParams);
        root.addView(header);

        LinearLayout list = new LinearLayout(requireContext());
        list.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams listParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        listParams.topMargin = dp(18);
        root.addView(list, listParams);

        List<View> rows = new ArrayList<>();
        List<RadioButton> radios = new ArrayList<>();
        List<ImageView> checks = new ArrayList<>();
        List<ImageView> optionIconViews = new ArrayList<>();
        List<TextView> labelViews = new ArrayList<>();
        for (int i = 0; i < labels.length; i++) {
            View row = dialogChoiceRow(labels[i],
                    optionIcons == null ? 0 : optionIcons[i],
                    trailingIcons == null ? 0 : trailingIcons[i]);
            final int index = i;
            row.setOnClickListener(v -> {
                selected[0] = index;
                updateChoiceRows(rows, radios, checks, optionIconViews, labelViews, selected[0]);
            });
            rows.add(row);
            radios.add(row.findViewWithTag("radio"));
            checks.add(row.findViewWithTag("check"));
            optionIconViews.add(row.findViewWithTag("optionIcon"));
            labelViews.add(row.findViewWithTag("label"));
            LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    dp(54)
            );
            if (i > 0) rowParams.topMargin = dp(8);
            list.addView(row, rowParams);
        }
        updateChoiceRows(rows, radios, checks, optionIconViews, labelViews, selected[0]);

        MaterialButton doneButton = new MaterialButton(requireContext());
        doneButton.setText("Listo");
        doneButton.setAllCaps(false);
        doneButton.setTextColor(color(R.color.md_theme_onPrimary));
        doneButton.setBackgroundTintList(ColorStateList.valueOf(color(R.color.md_theme_primary)));
        doneButton.setCornerRadius(dp(18));
        LinearLayout.LayoutParams doneParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(52)
        );
        doneParams.topMargin = dp(18);
        root.addView(doneButton, doneParams);

        AlertDialog dialog = new MaterialAlertDialogBuilder(requireContext())
                .setView(root)
                .create();
        doneButton.setOnClickListener(v -> {
            done.onDone(selected[0]);
            dialog.dismiss();
        });
        dialog.show();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }
    }

    private View dialogChoiceRow(@NonNull String label, @DrawableRes int optionIcon, @DrawableRes int trailingIcon) {
        LinearLayout row = new LinearLayout(requireContext());
        row.setGravity(android.view.Gravity.CENTER_VERTICAL);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(dp(10), 0, dp(12), 0);
        row.setClickable(true);
        row.setFocusable(true);

        RadioButton radio = new RadioButton(requireContext());
        radio.setTag("radio");
        radio.setClickable(false);
        radio.setButtonTintList(ColorStateList.valueOf(color(R.color.md_theme_primary)));
        row.addView(radio, new LinearLayout.LayoutParams(dp(38), dp(38)));

        if (optionIcon != 0) {
            ImageView option = new ImageView(requireContext());
            option.setTag("optionIcon");
            option.setImageResource(optionIcon);
            option.setColorFilter(color(R.color.md_theme_primary));
            LinearLayout.LayoutParams optionParams = new LinearLayout.LayoutParams(dp(26), dp(26));
            optionParams.leftMargin = dp(4);
            row.addView(option, optionParams);
        }

        TextView labelView = new TextView(requireContext());
        labelView.setTag("label");
        labelView.setText(label);
        labelView.setTextColor(color(R.color.md_theme_onSurface));
        labelView.setTextSize(16f);
        labelView.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        LinearLayout.LayoutParams labelParams = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        labelParams.leftMargin = optionIcon == 0 ? dp(10) : dp(12);
        row.addView(labelView, labelParams);

        if (trailingIcon != 0) {
            ImageView trailing = new ImageView(requireContext());
            trailing.setImageResource(trailingIcon);
            row.addView(trailing, new LinearLayout.LayoutParams(dp(30), dp(22)));
        }

        if (trailingIcon == 0) {
            ImageView check = new ImageView(requireContext());
            check.setTag("check");
            check.setImageResource(R.drawable.ic_check_circle);
            LinearLayout.LayoutParams checkParams = new LinearLayout.LayoutParams(dp(25), dp(25));
            checkParams.leftMargin = dp(8);
            row.addView(check, checkParams);
        }
        return row;
    }

    private void updateChoiceRows(
            @NonNull List<View> rows,
            @NonNull List<RadioButton> radios,
            @NonNull List<ImageView> checks,
            @NonNull List<ImageView> optionIcons,
            @NonNull List<TextView> labels,
            int selected
    ) {
        for (int i = 0; i < rows.size(); i++) {
            boolean checked = i == selected;
            int background = checked ? color(R.color.md_theme_primaryContainer) : color(R.color.md_theme_surface);
            int content = checked ? color(R.color.md_theme_onPrimaryContainer) : color(R.color.md_theme_onSurface);
            int accent = checked ? color(R.color.md_theme_onPrimaryContainer) : color(R.color.md_theme_primary);
            rows.get(i).setBackground(roundedStrokeDrawable(
                    background,
                    color(R.color.md_theme_outlineVariant),
                    dp(16)
            ));
            radios.get(i).setChecked(checked);
            radios.get(i).setButtonTintList(ColorStateList.valueOf(accent));
            ImageView option = optionIcons.get(i);
            if (option != null) {
                option.setColorFilter(accent);
            }
            TextView label = labels.get(i);
            if (label != null) {
                label.setTextColor(content);
            }
            ImageView check = checks.get(i);
            if (check != null) {
                check.setColorFilter(accent);
                check.setVisibility(checked ? View.VISIBLE : View.INVISIBLE);
            }
        }
    }

    private String themeLabel() {
        String mode = SettingsService.getThemeMode(requireContext());
        if (SettingsService.THEME_LIGHT.equals(mode)) return getString(R.string.perfil_theme_light);
        if (SettingsService.THEME_DARK.equals(mode)) return getString(R.string.perfil_theme_dark);
        return getString(R.string.perfil_theme_system);
    }

    private void navigate(int destination) {
        NavHostFragment.findNavController(this).navigate(destination, null, NavigationAnimations.detailSlide());
    }

    private void confirmLogout() {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.nav_logout_confirm_title)
                .setMessage(R.string.nav_logout_confirm_message)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(R.string.nav_logout_title, (dialog, which) -> logout())
                .show();
    }

    private void logout() {
        Prefs.clearAuth(requireContext());
        FirebaseAuth.getInstance().signOut();
        clearGoogleSignInCache();
        clearScopedViewModelCaches();
        LocalRepository.invalidateDataVersion();
        PinSession.lock();
        Toast.makeText(requireContext(), R.string.nav_logout_done, Toast.LENGTH_SHORT).show();
        NavOptions out = new NavOptions.Builder()
                .setPopUpTo(R.id.nav_graph, true)
                .build();
        NavHostFragment.findNavController(this).navigate(R.id.nav_welcome, null, out);
    }

    private void clearGoogleSignInCache() {
        GoogleSignInOptions options = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        GoogleSignIn.getClient(requireActivity(), options).signOut();
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

    private int color(int resId) {
        return ContextCompat.getColor(requireContext(), resId);
    }

    private GradientDrawable roundedDrawable(int color, int radius) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(color);
        drawable.setCornerRadius(radius);
        return drawable;
    }

    private GradientDrawable roundedStrokeDrawable(int color, int stroke, int radius) {
        GradientDrawable drawable = roundedDrawable(color, radius);
        drawable.setStroke(dp(1), stroke);
        return drawable;
    }

    private GradientDrawable ovalDrawable(int color) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setShape(GradientDrawable.OVAL);
        drawable.setColor(color);
        return drawable;
    }
}
