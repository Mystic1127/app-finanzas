package com.example.finanzas.ui;

import android.content.res.ColorStateList;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.finanzas.R;
import com.example.finanzas.data.api.SettingsService;
import com.example.finanzas.data.model.Categoria;
import com.example.finanzas.data.model.CategoryBudgetInput;
import com.example.finanzas.ui.adapter.CategoryBudgetEditAdapter;
import com.example.finanzas.ui.view.SpendlyDecorBackgroundDrawable;
import com.example.finanzas.ui.viewmodel.BudgetViewModel;
import com.example.finanzas.util.CategoryVisuals;
import com.example.finanzas.util.PerfLogger;
import com.example.finanzas.util.UiFormUtils;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class PresupuestoFragment extends androidx.fragment.app.Fragment {

    private EditText etPresupuesto;
    private TextInputLayout tilPresupuesto;
    private CategoryBudgetEditAdapter categoryAdapter;
    private MaterialButton btnGuardarPresupuesto;
    private MaterialButton btnAgregarCategoria;
    private MaterialButton btnAgregarCategoriaEmpty;
    private SwipeRefreshLayout swipeRefreshLayout;
    private View cardEmptyCategories;
    private int anio;
    private int mes;
    private BudgetViewModel viewModel;
    private boolean manualRefresh;
    private long perfStartMs;
    private long loadStartMs;
    private boolean firstRenderLogged;
    private final Handler saveHandler = new Handler(Looper.getMainLooper());
    private final Runnable saveCategoryRunnable = () -> {
        if (viewModel != null && categoryAdapter != null) {
            viewModel.saveCategoryBudgetsQuiet(anio, mes, categoryAdapter.getItems());
        }
    };
    private List<Categoria> availableCategories = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_presupuesto, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View v, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(v, savedInstanceState);
        v.setBackground(new SpendlyDecorBackgroundDrawable(requireContext()));
        perfStartMs = PerfLogger.now();
        firstRenderLogged = false;

        etPresupuesto = v.findViewById(R.id.etPresupuesto);
        tilPresupuesto = v.findViewById(R.id.tilPresupuesto);
        btnGuardarPresupuesto = v.findViewById(R.id.btnGuardarPresupuesto);
        btnAgregarCategoria = v.findViewById(R.id.btnAgregarCategoria);
        btnAgregarCategoriaEmpty = v.findViewById(R.id.btnAgregarCategoriaEmpty);
        cardEmptyCategories = v.findViewById(R.id.cardEmptyCategories);
        androidx.recyclerview.widget.RecyclerView rvCategory = v.findViewById(R.id.rvCategoryBudgets);
        swipeRefreshLayout = v.findViewById(R.id.swipeBudget);

        rvCategory.setLayoutManager(new LinearLayoutManager(requireContext()));
        categoryAdapter = new CategoryBudgetEditAdapter();
        categoryAdapter.setListener(new CategoryBudgetEditAdapter.Listener() {
            @Override
            public void onBudgetChanged(@NonNull List<CategoryBudgetInput> items) {
                scheduleCategorySave();
            }

            @Override
            public void onDelete(@NonNull CategoryBudgetInput item) {
                removeCategoryBudget(item);
            }
        });
        rvCategory.setAdapter(categoryAdapter);
        viewModel = new androidx.lifecycle.ViewModelProvider(requireActivity()).get(BudgetViewModel.class);
        viewModel.clearCacheIfUserChanged();

        Calendar cal = Calendar.getInstance();
        anio = cal.get(Calendar.YEAR);
        mes = cal.get(Calendar.MONTH) + 1;

        btnGuardarPresupuesto.setOnClickListener(view -> guardarPresupuesto());
        btnAgregarCategoria.setOnClickListener(view -> showCategoryPickerSheet());
        if (btnAgregarCategoriaEmpty != null) btnAgregarCategoriaEmpty.setOnClickListener(view -> showCategoryPickerSheet());
        UiFormUtils.clearErrorOnTextChange(etPresupuesto);
        tilPresupuesto.setPrefixText(SettingsService.getCurrencySymbol(requireContext()) + " ");

        if (swipeRefreshLayout != null) {
            swipeRefreshLayout.setOnRefreshListener(() -> {
                manualRefresh = true;
                recargarDatos();
            });
        }
        observeViewModel();

        recargarDatos();
        PerfLogger.logSince("PresupuestoFragment", "onViewCreated", perfStartMs);
    }

    @Override
    public void onDestroyView() {
        saveHandler.removeCallbacks(saveCategoryRunnable);
        super.onDestroyView();
    }

    private void recargarDatos() {
        loadStartMs = PerfLogger.now();
        PerfLogger.log("PresupuestoFragment", "loadStart");
        viewModel.load(anio, mes, manualRefresh);
    }

    private void guardarPresupuesto() {
        String s = etPresupuesto.getText() == null ? "" : etPresupuesto.getText().toString().trim();
        double val;
        try {
            val = s.isEmpty() ? 0.0 : Math.max(0, parseMontoSeguro(s));
        } catch (NumberFormatException ex) {
            tilPresupuesto.setError(getString(R.string.error_monto_invalido));
            return;
        }

        tilPresupuesto.setError(null);
        viewModel.saveBudget(anio, mes, val, SettingsService.getCurrencyCode(requireContext()));
    }

    private void scheduleCategorySave() {
        saveHandler.removeCallbacks(saveCategoryRunnable);
        saveHandler.postDelayed(saveCategoryRunnable, 550L);
    }

    private void addCategoryBudget(@NonNull Categoria categoria) {
        CategoryBudgetInput input = new CategoryBudgetInput();
        input.setCategoriaId(categoria.id);
        input.setCategoriaNombre(categoria.nombre);
        input.setMonto(0.0);
        input.setMoneda(SettingsService.getCurrencyCode(requireContext()));
        input.setGastado(0.0);
        input.setDisponible(0.0);
        input.setPorcentaje(0.0);

        List<CategoryBudgetInput> next = categoryAdapter.getItems();
        next.add(input);
        next.sort((a, b) -> String.valueOf(a.getCategoriaNombre()).compareToIgnoreCase(String.valueOf(b.getCategoriaNombre())));
        categoryAdapter.setItems(next);
        availableCategories.removeIf(existing -> existing != null && existing.id == categoria.id);
        updateEmptyState(next);
        viewModel.setLocalCategoryBudgets(next);
        viewModel.saveCategoryBudgetsQuiet(anio, mes, next);
        UiFormUtils.showMessage(requireView(), R.string.pres_category_created);
    }

    private void removeCategoryBudget(@NonNull CategoryBudgetInput item) {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.pres_category_delete_confirm_title)
                .setMessage(R.string.pres_category_delete_confirm_message)
                .setNegativeButton(R.string.import_sheet_cancel, null)
                .setPositiveButton(R.string.categories_delete, (dialog, which) -> {
                    List<CategoryBudgetInput> next = categoryAdapter.getItems();
                    next.removeIf(existing -> existing.getCategoriaId() == item.getCategoriaId());
                    categoryAdapter.setItems(next);
                    updateEmptyState(next);
                    viewModel.setLocalCategoryBudgets(next);
                    Categoria restored = new Categoria(item.getCategoriaId(), item.getCategoriaNombre(), false);
                    availableCategories.add(restored);
                    availableCategories.sort((a, b) -> String.valueOf(a.nombre).compareToIgnoreCase(String.valueOf(b.nombre)));
                    viewModel.saveCategoryBudgetsQuiet(anio, mes, next);
                    Snackbar snackbar = Snackbar.make(requireView(), getString(R.string.pres_category_deleted) + "\n" + getString(R.string.pres_category_deleted_detail), Snackbar.LENGTH_SHORT)
                            .setTextMaxLines(2);
                    View bottomNav = requireActivity().findViewById(R.id.bottom_nav_container);
                    if (bottomNav != null) {
                        snackbar.setAnchorView(bottomNav);
                    } else {
                        View snackView = snackbar.getView();
                        ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) snackView.getLayoutParams();
                        params.setMargins(params.leftMargin, params.topMargin, params.rightMargin, dp(96));
                        snackView.setLayoutParams(params);
                    }
                    snackbar.show();
                })
                .show();
    }

    private void stopRefreshing() {
        if (swipeRefreshLayout != null) {
            swipeRefreshLayout.setRefreshing(false);
        }
    }

    private void observeViewModel() {
        viewModel.getLoading().observe(getViewLifecycleOwner(), loading -> {
            boolean isLoading = Boolean.TRUE.equals(loading);
            if (swipeRefreshLayout != null) swipeRefreshLayout.setRefreshing(isLoading && manualRefresh);
            UiFormUtils.setActionLoading(btnGuardarPresupuesto, isLoading);
            UiFormUtils.setActionLoading(btnAgregarCategoria, isLoading);
            if (btnAgregarCategoriaEmpty != null) UiFormUtils.setActionLoading(btnAgregarCategoriaEmpty, isLoading);
            if (!isLoading) {
                PerfLogger.logSince("PresupuestoFragment", "loadComplete", loadStartMs);
                manualRefresh = false;
            }
        });
        viewModel.getBudget().observe(getViewLifecycleOwner(), monto -> {
            if (monto != null) etPresupuesto.setText(monto > 0.0 ? moneyNumber(monto) : "");
        });
        viewModel.getCategoryBudgets().observe(getViewLifecycleOwner(), items -> {
            if (!firstRenderLogged) {
                firstRenderLogged = true;
                PerfLogger.logSince("PresupuestoFragment", "firstRender", perfStartMs);
            }
            List<CategoryBudgetInput> safe = items == null ? new ArrayList<>() : items;
            categoryAdapter.setItems(safe);
            updateEmptyState(safe);
        });
        viewModel.getAvailableCategories().observe(getViewLifecycleOwner(), items -> {
            availableCategories = items == null ? new ArrayList<>() : new ArrayList<>(items);
        });
        viewModel.getMessage().observe(getViewLifecycleOwner(), msgRes -> {
            if (msgRes != null) {
                UiFormUtils.showMessage(requireView(), msgRes);
                viewModel.consumeMessage();
            }
        });
    }

    private void updateEmptyState(@NonNull List<CategoryBudgetInput> items) {
        boolean empty = items.isEmpty();
        if (cardEmptyCategories != null) cardEmptyCategories.setVisibility(empty ? View.VISIBLE : View.GONE);
        if (btnAgregarCategoria != null) btnAgregarCategoria.setVisibility(empty ? View.GONE : View.VISIBLE);
    }

    private void showCategoryPickerSheet() {
        BottomSheetDialog dialog = new BottomSheetDialog(requireContext());
        LinearLayout root = new LinearLayout(requireContext());
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(18), dp(10), dp(18), dp(22));
        root.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.dialog_surface));

        View handle = new View(requireContext());
        GradientDrawable handleBg = new GradientDrawable();
        handleBg.setColor(ContextCompat.getColor(requireContext(), R.color.md_theme_outlineVariant));
        handleBg.setCornerRadius(dp(3));
        handle.setBackground(handleBg);
        LinearLayout.LayoutParams handleParams = new LinearLayout.LayoutParams(dp(64), dp(5));
        handleParams.gravity = android.view.Gravity.CENTER_HORIZONTAL;
        handleParams.bottomMargin = dp(18);
        root.addView(handle, handleParams);
        bindDragHandle(dialog, handle, root);

        LinearLayout titleRow = new LinearLayout(requireContext());
        titleRow.setGravity(android.view.Gravity.CENTER_VERTICAL);
        titleRow.setOrientation(LinearLayout.HORIZONTAL);
        root.addView(titleRow, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));

        TextView title = new TextView(requireContext());
        title.setText(R.string.pres_select_category_title);
        title.setTextColor(ContextCompat.getColor(requireContext(), R.color.md_theme_onSurface));
        title.setTextSize(18f);
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        titleRow.addView(title, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        ImageView close = new ImageView(requireContext());
        close.setImageResource(R.drawable.ic_close);
        close.setColorFilter(ContextCompat.getColor(requireContext(), R.color.md_theme_onSurfaceVariant));
        close.setPadding(dp(10), dp(10), dp(10), dp(10));
        close.setOnClickListener(v -> dialog.dismiss());
        titleRow.addView(close, new LinearLayout.LayoutParams(dp(44), dp(44)));

        TextInputLayout searchLayout = new TextInputLayout(requireContext());
        searchLayout.setHint(getString(R.string.pres_search_category));
        searchLayout.setStartIconDrawable(R.drawable.ic_search);
        searchLayout.setBoxBackgroundColor(ContextCompat.getColor(requireContext(), R.color.md_theme_surface));
        searchLayout.setBoxStrokeColor(ContextCompat.getColor(requireContext(), R.color.md_theme_outlineVariant));
        searchLayout.setBoxCornerRadii(dp(8), dp(8), dp(8), dp(8));
        TextInputEditText search = new TextInputEditText(requireContext());
        search.setSingleLine(true);
        search.setTextColor(ContextCompat.getColor(requireContext(), R.color.md_theme_onSurface));
        search.setHintTextColor(ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.md_theme_onSurfaceVariant)));
        searchLayout.addView(search, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        LinearLayout.LayoutParams searchParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        searchParams.topMargin = dp(14);
        root.addView(searchLayout, searchParams);

        LinearLayout list = new LinearLayout(requireContext());
        list.setOrientation(LinearLayout.VERTICAL);
        androidx.core.widget.NestedScrollView scroll = new androidx.core.widget.NestedScrollView(requireContext());
        scroll.setFillViewport(false);
        scroll.addView(list, new androidx.core.widget.NestedScrollView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));
        LinearLayout.LayoutParams listParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(360));
        listParams.topMargin = dp(12);
        root.addView(scroll, listParams);

        Runnable[] render = new Runnable[1];
        render[0] = () -> renderAvailableCategories(dialog, list, search.getText() == null ? "" : search.getText().toString());
        render[0].run();
        search.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { render[0].run(); }
            @Override public void afterTextChanged(Editable s) { }
        });

        dialog.setContentView(root);
        configureFixedScrollableSheet(dialog, dp(520));
        dialog.show();
    }

    private void configureFixedScrollableSheet(@NonNull BottomSheetDialog dialog, int peekHeight) {
        dialog.setOnShowListener(d -> {
            View bottomSheet = dialog.findViewById(com.google.android.material.R.id.design_bottom_sheet);
            if (bottomSheet == null) return;
            BottomSheetBehavior<View> behavior = BottomSheetBehavior.from(bottomSheet);
            behavior.setHideable(true);
            behavior.setSkipCollapsed(true);
            behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
            behavior.setPeekHeight(peekHeight, true);
            behavior.setDraggable(true);
        });
    }

    private void bindDragHandle(@NonNull BottomSheetDialog dialog, @NonNull View handle, @NonNull View sheetContent) {
        final float[] startY = new float[1];
        final float[] lastDelta = new float[1];
        handle.setOnTouchListener((view, event) -> {
            switch (event.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    startY[0] = event.getRawY();
                    lastDelta[0] = 0f;
                    sheetContent.animate().cancel();
                    view.getParent().requestDisallowInterceptTouchEvent(true);
                    return true;
                case MotionEvent.ACTION_MOVE:
                    lastDelta[0] = Math.max(0f, event.getRawY() - startY[0]);
                    sheetContent.setTranslationY(lastDelta[0]);
                    return true;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    view.getParent().requestDisallowInterceptTouchEvent(false);
                    if (lastDelta[0] > dp(72)) {
                        dialog.dismiss();
                    } else {
                        sheetContent.animate().translationY(0f).setDuration(160L).start();
                    }
                    return true;
                default:
                    return false;
            }
        });
    }

    private void renderAvailableCategories(@NonNull BottomSheetDialog dialog, @NonNull LinearLayout list, @NonNull String query) {
        list.removeAllViews();
        String clean = CategoryVisuals.normalize(query);
        int count = 0;
        for (Categoria categoria : availableCategories) {
            if (categoria == null || TextUtils.isEmpty(categoria.nombre)) continue;
            if (!clean.isEmpty() && !CategoryVisuals.normalize(categoria.nombre).contains(clean)) continue;
            list.addView(categoryChoiceRow(dialog, categoria));
            count++;
        }
        if (count == 0) {
            TextView empty = new TextView(requireContext());
            empty.setText(R.string.pres_no_categories_available);
            empty.setTextColor(ContextCompat.getColor(requireContext(), R.color.md_theme_onSurfaceVariant));
            empty.setTextSize(14f);
            empty.setPadding(0, dp(18), 0, dp(18));
            list.addView(empty);
        }
    }

    private View categoryChoiceRow(@NonNull BottomSheetDialog dialog, @NonNull Categoria categoria) {
        LinearLayout row = new LinearLayout(requireContext());
        row.setGravity(android.view.Gravity.CENTER_VERTICAL);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(0, dp(9), 0, dp(9));

        int accent = CategoryVisuals.colorFor(requireContext(), categoria);
        FrameLayout iconBg = new FrameLayout(requireContext());
        GradientDrawable bg = new GradientDrawable();
        bg.setShape(GradientDrawable.OVAL);
        bg.setColor(accent);
        iconBg.setBackground(bg);
        ImageView icon = new ImageView(requireContext());
        icon.setImageResource(CategoryVisuals.iconFor(requireContext(), categoria));
        icon.setColorFilter(ContextCompat.getColor(requireContext(), android.R.color.white));
        iconBg.addView(icon, new FrameLayout.LayoutParams(dp(22), dp(22), android.view.Gravity.CENTER));
        row.addView(iconBg, new LinearLayout.LayoutParams(dp(38), dp(38)));

        TextView name = new TextView(requireContext());
        name.setText(categoria.nombre);
        name.setTextColor(ContextCompat.getColor(requireContext(), R.color.md_theme_onSurface));
        name.setTextSize(14f);
        name.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        LinearLayout.LayoutParams nameParams = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        nameParams.leftMargin = dp(12);
        row.addView(name, nameParams);

        ImageView radio = new ImageView(requireContext());
        radio.setImageResource(R.drawable.ic_radio_unchecked);
        radio.setColorFilter(ContextCompat.getColor(requireContext(), R.color.md_theme_onSurfaceVariant));
        row.addView(radio, new LinearLayout.LayoutParams(dp(24), dp(24)));

        row.setOnClickListener(v -> {
            dialog.dismiss();
            addCategoryBudget(categoria);
        });
        return row;
    }

    private double parseMontoSeguro(String raw) {
        if (raw == null) return 0;
        String limpio = raw.trim();
        if (limpio.isEmpty()) return 0;
        limpio = limpio.replaceAll("[^0-9,.-]", "");
        if (limpio.isEmpty()) return 0;
        int lastComma = limpio.lastIndexOf(',');
        int lastDot = limpio.lastIndexOf('.');
        if (lastComma >= 0 && lastDot >= 0) {
            if (lastComma > lastDot) {
                limpio = limpio.replace(".", "");
                limpio = limpio.replace(',', '.');
            } else {
                limpio = limpio.replace(",", "");
            }
        } else if (lastComma >= 0) {
            limpio = limpio.replace(',', '.');
        }
        if ("-".equals(limpio) || ".".equals(limpio) || "-.".equals(limpio) || ",".equals(limpio)) {
            return 0;
        }
        return Double.parseDouble(limpio);
    }

    private String moneyNumber(double amount) {
        if (Math.abs(amount - Math.rint(amount)) < 0.005) {
            return String.format(Locale.US, "%.0f", amount);
        }
        return String.format(Locale.US, "%.2f", amount);
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
