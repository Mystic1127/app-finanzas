package com.example.finanzas.ui;

import android.content.res.ColorStateList;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputFilter;
import android.text.InputType;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.FrameLayout;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.finanzas.R;
import com.example.finanzas.data.api.CategoryStore;
import com.example.finanzas.data.model.Categoria;
import com.example.finanzas.ui.view.SpendlyDecorBackgroundDrawable;
import com.example.finanzas.util.CategoryPrefs;
import com.example.finanzas.util.CategoryVisuals;
import com.example.finanzas.util.TransactionLabelStore;
import com.example.finanzas.util.UiFormUtils;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class CategoriesFragment extends androidx.fragment.app.Fragment {
    private RecyclerView recycler;
    private TextInputEditText search;
    private MaterialButton btnAll;
    private MaterialButton btnDeleted;
    private final CategoryAdapter adapter = new CategoryAdapter();
    private final List<Categoria> allCategories = new ArrayList<>();
    private boolean showingDeleted = false;

    private static final String[] ICON_KEYS = new String[] {
            CategoryPrefs.ICON_GROCERIES, CategoryPrefs.ICON_RESTAURANT, CategoryPrefs.ICON_RESTAURANT_MENU,
            CategoryPrefs.ICON_DELIVERY, CategoryPrefs.ICON_BUS, CategoryPrefs.ICON_HOME,
            CategoryPrefs.ICON_HOME_REPAIR, CategoryPrefs.ICON_DEBT,
            CategoryPrefs.ICON_ENTERTAINMENT, CategoryPrefs.ICON_FAMILY, CategoryPrefs.ICON_WORK_HISTORY,
            CategoryPrefs.ICON_ACCOUNT_BALANCE, CategoryPrefs.ICON_TRENDING_UP, CategoryPrefs.ICON_STORE,
            CategoryPrefs.ICON_ADD_CARD, CategoryPrefs.ICON_RETURN, CategoryPrefs.ICON_REDEEM,
            CategoryPrefs.ICON_CHECKROOM, CategoryPrefs.ICON_PAYMENTS, CategoryPrefs.ICON_DEVICES,
            CategoryPrefs.ICON_SELL, CategoryPrefs.ICON_PHARMACY, CategoryPrefs.ICON_SUBSCRIPTIONS,
            CategoryPrefs.ICON_WIFI_CALLING, CategoryPrefs.ICON_BOLT, CategoryPrefs.ICON_WATER,
            CategoryPrefs.ICON_FIRE, CategoryPrefs.ICON_BUILD, CategoryPrefs.ICON_SPA,
            CategoryPrefs.ICON_HEALTH, CategoryPrefs.ICON_EDUCATION, CategoryPrefs.ICON_SHOPPING,
            CategoryPrefs.ICON_PETS, CategoryPrefs.ICON_SAVINGS, CategoryPrefs.ICON_GIFT,
            CategoryPrefs.ICON_WORK, CategoryPrefs.ICON_OTHER
    };

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_categories, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        view.setBackground(new SpendlyDecorBackgroundDrawable(requireContext()));
        recycler = view.findViewById(R.id.rvCategories);
        search = view.findViewById(R.id.etCategorySearch);
        btnAll = view.findViewById(R.id.btnCategoriesAll);
        btnDeleted = view.findViewById(R.id.btnCategoriesDeleted);
        recycler.setLayoutManager(new LinearLayoutManager(requireContext()));
        recycler.setAdapter(adapter);
        adapter.listener = new CategoryAdapter.Listener() {
            @Override public void onClick(@NonNull Categoria categoria) {
                if (showingDeleted) restoreCategory(categoria);
                else showCategoryActions(categoria);
            }

            @Override public void onLongClick(@NonNull Categoria categoria) {
                showCategoryActions(categoria);
            }
        };
        view.findViewById(R.id.btnCategoriesBack).setOnClickListener(v -> NavHostFragment.findNavController(this).popBackStack());
        view.findViewById(R.id.btnCategoryAdd).setOnClickListener(v -> showCategoryForm(null));
        btnAll.setOnClickListener(v -> {
            showingDeleted = false;
            render();
        });
        btnDeleted.setOnClickListener(v -> {
            showingDeleted = true;
            render();
        });
        search.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { render(); }
            @Override public void afterTextChanged(Editable s) { }
        });
        loadCategories();
    }

    private void loadCategories() {
        CategoryStore.clearCache();
        CategoryStore.loadOnce(requireContext(), new CategoryStore.Callback() {
            @Override public void onReady(List<? extends Categoria> cats) {
                allCategories.clear();
                if (cats != null) allCategories.addAll(cats);
                render();
            }

            @Override public void onError() {
                if (isAdded()) UiFormUtils.showMessage(requireView(), R.string.error_cargar_categorias);
            }
        });
    }

    private void render() {
        if (!isAdded()) return;
        styleTabs();
        String q = search == null || search.getText() == null ? "" : CategoryVisuals.normalize(search.getText().toString());
        List<Categoria> rows = new ArrayList<>();
        for (Categoria c : allCategories) {
            if (c == null || TextUtils.isEmpty(c.nombre)) continue;
            if (isSpecial(c)) continue;
            boolean deleted = CategoryPrefs.isDeleted(requireContext(), c);
            if (deleted != showingDeleted) continue;
            if (!q.isEmpty() && !CategoryVisuals.normalize(c.nombre).contains(q)) continue;
            rows.add(c);
        }
        rows.sort((a, b) -> {
            boolean aCustom = CategoryPrefs.hasMeta(requireContext(), a.id);
            boolean bCustom = CategoryPrefs.hasMeta(requireContext(), b.id);
            if (aCustom != bCustom) return aCustom ? -1 : 1;
            return String.valueOf(a.nombre).compareToIgnoreCase(String.valueOf(b.nombre));
        });
        adapter.setItems(rows, showingDeleted);
    }

    private void styleTabs() {
        styleTab(btnAll, !showingDeleted);
        styleTab(btnDeleted, showingDeleted);
    }

    private void styleTab(@NonNull MaterialButton button, boolean selected) {
        int primary = ContextCompat.getColor(requireContext(), R.color.planning_dialog_button);
        int surface = ContextCompat.getColor(requireContext(), R.color.md_theme_surface);
        int text = ContextCompat.getColor(requireContext(), selected ? android.R.color.white : R.color.md_theme_onSurface);
        button.setBackgroundTintList(ColorStateList.valueOf(selected ? primary : surface));
        button.setTextColor(text);
        button.setStrokeColor(ColorStateList.valueOf(selected ? primary : ContextCompat.getColor(requireContext(), R.color.md_theme_outlineVariant)));
        button.setStrokeWidth(dp(1));
    }

    private void showCategoryActions(@NonNull Categoria categoria) {
        BottomSheetDialog dialog = new BottomSheetDialog(requireContext());
        LinearLayout root = new LinearLayout(requireContext());
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(18), dp(12), dp(18), dp(12));
        root.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.dialog_surface));
        MaterialButton edit = sheetAction(R.drawable.ic_edit, getString(R.string.categories_edit), ContextCompat.getColor(requireContext(), R.color.md_theme_onSurface));
        edit.setOnClickListener(v -> {
            dialog.dismiss();
            showCategoryForm(categoria);
        });
        root.addView(edit, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(54)));
        MaterialButton delete = sheetAction(R.drawable.ic_visibilityoff, getString(R.string.categories_hide), ContextCompat.getColor(requireContext(), R.color.md_theme_onSurface));
        delete.setOnClickListener(v -> {
            dialog.dismiss();
            confirmDelete(categoria);
        });
        LinearLayout.LayoutParams deleteParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(54));
        deleteParams.topMargin = dp(2);
        root.addView(delete, deleteParams);
        MaterialButton cancel = sheetAction(0, getString(R.string.import_sheet_cancel), ContextCompat.getColor(requireContext(), R.color.md_theme_onSurface));
        cancel.setOnClickListener(v -> dialog.dismiss());
        LinearLayout.LayoutParams cancelParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(54));
        cancelParams.topMargin = dp(2);
        root.addView(cancel, cancelParams);
        dialog.setContentView(root);
        dialog.show();
    }

    private MaterialButton sheetAction(int icon, @NonNull String text, @ColorInt int color) {
        MaterialButton button = new MaterialButton(requireContext());
        button.setText(text);
        button.setAllCaps(false);
        button.setGravity(android.view.Gravity.CENTER_VERTICAL);
        button.setTextColor(color);
        button.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.md_theme_surfaceContainer)));
        button.setMinHeight(0);
        button.setInsetTop(0);
        button.setInsetBottom(0);
        if (icon != 0) {
            button.setIconResource(icon);
            button.setIconTint(ColorStateList.valueOf(color));
        }
        button.setCornerRadius(dp(8));
        return button;
    }

    private void confirmDelete(@NonNull Categoria categoria) {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.categories_delete_confirm_title)
                .setMessage(R.string.categories_delete_confirm_message)
                .setNegativeButton(R.string.import_sheet_cancel, null)
                .setPositiveButton(R.string.categories_hide, (d, w) -> {
                    CategoryPrefs.setDeleted(requireContext(), categoria, true);
                    render();
                    Toast.makeText(requireContext(), R.string.categories_deleted_toast, Toast.LENGTH_SHORT).show();
                })
                .show();
    }

    private void restoreCategory(@NonNull Categoria categoria) {
        CategoryPrefs.setDeleted(requireContext(), categoria, false);
        render();
        Toast.makeText(requireContext(), R.string.categories_restored_toast, Toast.LENGTH_SHORT).show();
    }

    private void showCategoryForm(@Nullable Categoria editing) {
        BottomSheetDialog dialog = new BottomSheetDialog(requireContext());
        LinearLayout root = new LinearLayout(requireContext());
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(22), dp(12), dp(22), dp(18));
        root.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.dialog_surface));

        final String[] iconKey = { editing == null ? CategoryPrefs.ICON_GROCERIES : CategoryPrefs.meta(requireContext(), editing).iconKey };
        final String[] selectedHex = { editing == null ? TransactionLabelStore.paletteColors()[0] : hexFor(CategoryPrefs.meta(requireContext(), editing).color) };
        final int[] color = { Color.parseColor(selectedHex[0]) };

        LinearLayout header = new LinearLayout(requireContext());
        header.setGravity(android.view.Gravity.CENTER_VERTICAL);
        header.setOrientation(LinearLayout.HORIZONTAL);
        ImageView close = new ImageView(requireContext());
        close.setImageResource(R.drawable.ic_close);
        close.setColorFilter(ContextCompat.getColor(requireContext(), R.color.md_theme_onSurfaceVariant));
        close.setPadding(dp(8), dp(8), dp(8), dp(8));
        close.setOnClickListener(v -> dialog.dismiss());
        header.addView(close, new LinearLayout.LayoutParams(dp(44), dp(44)));
        TextView title = new TextView(requireContext());
        title.setText(editing == null ? R.string.categories_new_title : R.string.categories_edit_title);
        title.setTextColor(ContextCompat.getColor(requireContext(), R.color.md_theme_onSurface));
        title.setTextSize(22f);
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        title.setGravity(android.view.Gravity.CENTER);
        header.addView(title, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        ImageView save = new ImageView(requireContext());
        save.setImageResource(R.drawable.ic_check_circle);
        save.setColorFilter(ContextCompat.getColor(requireContext(), R.color.planning_dialog_button));
        save.setPadding(dp(8), dp(8), dp(8), dp(8));
        header.addView(save, new LinearLayout.LayoutParams(dp(44), dp(44)));
        root.addView(header);

        FrameLayout iconPreview = new FrameLayout(requireContext());
        LinearLayout.LayoutParams previewParams = new LinearLayout.LayoutParams(dp(78), dp(78));
        previewParams.gravity = android.view.Gravity.CENTER_HORIZONTAL;
        previewParams.topMargin = dp(14);
        root.addView(iconPreview, previewParams);
        ImageView previewIcon = new ImageView(requireContext());
        previewIcon.setColorFilter(ContextCompat.getColor(requireContext(), android.R.color.white));
        iconPreview.addView(previewIcon, new FrameLayout.LayoutParams(dp(40), dp(40), android.view.Gravity.CENTER));
        Runnable updatePreview = () -> {
            GradientDrawable bg = new GradientDrawable();
            bg.setShape(GradientDrawable.OVAL);
            bg.setColor(color[0]);
            iconPreview.setBackground(bg);
            previewIcon.setImageResource(CategoryPrefs.iconFor(iconKey[0]));
        };
        updatePreview.run();

        TextView editIcon = new TextView(requireContext());
        editIcon.setText(R.string.categories_edit_icon);
        editIcon.setTextColor(ContextCompat.getColor(requireContext(), R.color.planning_dialog_button));
        editIcon.setTextSize(14f);
        editIcon.setGravity(android.view.Gravity.CENTER);
        editIcon.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        editIcon.setClickable(true);
        editIcon.setFocusable(true);
        editIcon.setOnClickListener(v -> showIconPickerDialog(iconKey[0], selected -> {
            iconKey[0] = selected;
            updatePreview.run();
        }));
        LinearLayout.LayoutParams editIconParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        editIconParams.topMargin = dp(6);
        root.addView(editIcon, editIconParams);

        TextInputLayout tilName = new TextInputLayout(requireContext());
        tilName.setHint(getString(R.string.categories_name_label));
        tilName.setBoxBackgroundColor(ContextCompat.getColor(requireContext(), R.color.md_theme_surface));
        TextInputEditText etName = new TextInputEditText(requireContext());
        etName.setSingleLine(true);
        etName.setFilters(new InputFilter[]{ new InputFilter.LengthFilter(30) });
        etName.setText(editing == null ? "" : editing.nombre);
        etName.setTextColor(ContextCompat.getColor(requireContext(), R.color.md_theme_onSurface));
        tilName.addView(etName);
        LinearLayout.LayoutParams nameParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        nameParams.topMargin = dp(14);
        root.addView(tilName, nameParams);

        TextInputLayout tilType = new TextInputLayout(requireContext());
        tilType.setHint(getString(R.string.categories_type_label));
        MaterialAutoCompleteTextView type = new MaterialAutoCompleteTextView(requireContext());
        type.setInputType(0);
        type.setAdapter(new ArrayAdapter<>(requireContext(), R.layout.item_dropdown, new String[]{ getString(R.string.tipo_gasto), getString(R.string.tipo_ingreso) }));
        type.setText(editing != null && editing.esIngreso ? getString(R.string.tipo_ingreso) : getString(R.string.tipo_gasto), false);
        type.setOnClickListener(v -> type.showDropDown());
        tilType.addView(type);
        LinearLayout.LayoutParams typeParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        typeParams.topMargin = dp(8);
        root.addView(tilType, typeParams);

        TextView colorTitle = new TextView(requireContext());
        colorTitle.setText(R.string.categories_color_label);
        colorTitle.setTextColor(ContextCompat.getColor(requireContext(), R.color.md_theme_onSurfaceVariant));
        colorTitle.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        LinearLayout.LayoutParams colorTitleParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        colorTitleParams.topMargin = dp(12);
        root.addView(colorTitle, colorTitleParams);

        TextInputLayout tilHex = new TextInputLayout(requireContext());
        tilHex.setHintEnabled(false);
        tilHex.setBoxBackgroundColor(ContextCompat.getColor(requireContext(), R.color.md_theme_surface));
        tilHex.setEndIconMode(TextInputLayout.END_ICON_CUSTOM);
        tilHex.setEndIconDrawable(R.drawable.ic_edit);
        tilHex.setEndIconTintList(ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.md_theme_onSurfaceVariant)));
        TextInputEditText etHex = new TextInputEditText(requireContext());
        etHex.setSingleLine(true);
        etHex.setHint(R.string.transaction_label_hex);
        etHex.setTextColor(ContextCompat.getColor(requireContext(), R.color.md_theme_onSurface));
        etHex.setHintTextColor(ContextCompat.getColor(requireContext(), R.color.md_theme_onSurfaceVariant));
        etHex.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        etHex.setFilters(new InputFilter[]{ new InputFilter.LengthFilter(7) });
        etHex.setText(selectedHex[0]);
        tilHex.addView(etHex);
        LinearLayout.LayoutParams hexParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        hexParams.topMargin = dp(6);
        root.addView(tilHex, hexParams);

        GridLayout colorGrid = new GridLayout(requireContext());
        colorGrid.setColumnCount(6);
        LinearLayout.LayoutParams colorGridParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        colorGridParams.topMargin = dp(8);
        root.addView(colorGrid, colorGridParams);
        Runnable[] renderColors = new Runnable[1];
        renderColors[0] = () -> {
            colorGrid.removeAllViews();
            String[] palette = compactCategoryPalette();
            for (int i = 0; i < palette.length; i++) {
                String value = palette[i];
                CategoryColorSwatchView swatch = new CategoryColorSwatchView(requireContext());
                swatch.setColor(Color.parseColor(value));
                swatch.setSelectedColor(value.equals(selectedHex[0]));
                swatch.setOnClickListener(v -> {
                    selectedHex[0] = value;
                    color[0] = Color.parseColor(value);
                    etHex.setText(value);
                    etHex.setSelection(etHex.length());
                    tilHex.setError(null);
                    updatePreview.run();
                    renderColors[0].run();
                });
                GridLayout.LayoutParams params = new GridLayout.LayoutParams(
                        GridLayout.spec(i / 6),
                        GridLayout.spec(i % 6, 1f)
                );
                params.width = 0;
                params.height = dp(34);
                params.setMargins(0, 0, i % 6 == 5 ? 0 : dp(8), dp(6));
                colorGrid.addView(swatch, params);
            }
        };
        renderColors[0].run();

        tilHex.setEndIconOnClickListener(v -> {
            etHex.requestFocus();
            etHex.setSelection(etHex.length());
        });
        etHex.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { }
            @Override public void afterTextChanged(Editable s) {
                String raw = s == null ? "" : s.toString().trim();
                if (TransactionLabelStore.isValidHex(raw)) {
                    String normalized = TransactionLabelStore.normalizeHex(raw);
                    selectedHex[0] = normalized;
                    color[0] = Color.parseColor(normalized);
                    tilHex.setError(null);
                    updatePreview.run();
                    renderColors[0].run();
                } else if (!raw.isEmpty()) {
                    tilHex.setError(getString(R.string.transaction_label_hex_error));
                    renderColors[0].run();
                } else {
                    tilHex.setError(null);
                    renderColors[0].run();
                }
            }
        });

        save.setOnClickListener(v -> {
            String name = etName.getText() == null ? "" : etName.getText().toString().trim();
            if (name.isEmpty()) {
                tilName.setError(getString(R.string.category_label_name_error));
                return;
            }
            String rawHex = etHex.getText() == null ? "" : etHex.getText().toString().trim();
            if (!TransactionLabelStore.isValidHex(rawHex)) {
                tilHex.setError(getString(R.string.transaction_label_hex_error));
                return;
            }
            selectedHex[0] = TransactionLabelStore.normalizeHex(rawHex);
            color[0] = Color.parseColor(selectedHex[0]);
            boolean income = getString(R.string.tipo_ingreso).contentEquals(type.getText());
            if (editing == null) {
                CategoryStore.createCategoria(requireContext(), name, income, new CategoryStore.CreateCallback() {
                    @Override public void onReady(Categoria categoria) {
                        CategoryPrefs.saveMeta(requireContext(), categoria.id, iconKey[0], color[0], false);
                        dialog.dismiss();
                        Toast.makeText(requireContext(), R.string.categories_created_toast, Toast.LENGTH_SHORT).show();
                        loadCategories();
                    }
                    @Override public void onError() { Toast.makeText(requireContext(), R.string.pres_category_create_error, Toast.LENGTH_SHORT).show(); }
                });
            } else {
                editing.nombre = name;
                editing.esIngreso = income;
                CategoryStore.updateCategoria(requireContext(), editing, new CategoryStore.SimpleCallback() {
                    @Override public void onSuccess() {
                        CategoryPrefs.saveMeta(requireContext(), editing.id, iconKey[0], color[0], false);
                        dialog.dismiss();
                        loadCategories();
                    }
                    @Override public void onError() { Toast.makeText(requireContext(), R.string.category_update_error, Toast.LENGTH_SHORT).show(); }
                });
            }
        });

        dialog.setContentView(root);
        dialog.setOnShowListener(d -> {
            View bottomSheet = dialog.findViewById(com.google.android.material.R.id.design_bottom_sheet);
            if (bottomSheet == null) return;
            BottomSheetBehavior<View> behavior = BottomSheetBehavior.from(bottomSheet);
            behavior.setSkipCollapsed(true);
            behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
        });
        dialog.show();
    }

    @NonNull
    private String[] compactCategoryPalette() {
        String[] source = TransactionLabelStore.paletteColors();
        int count = Math.min(24, source.length);
        String[] compact = new String[count];
        System.arraycopy(source, 0, compact, 0, count);
        return compact;
    }

    private void showIconPickerDialog(@Nullable String selectedIconKey, @NonNull IconPickCallback callback) {
        LinearLayout root = new LinearLayout(requireContext());
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(18), dp(8), dp(18), dp(2));

        TextView hint = new TextView(requireContext());
        hint.setText(R.string.categories_icon_picker_hint);
        hint.setTextColor(ContextCompat.getColor(requireContext(), R.color.md_theme_onSurfaceVariant));
        hint.setTextSize(14f);
        root.addView(hint, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        ScrollView scroll = new ScrollView(requireContext());
        scroll.setFillViewport(false);
        GridLayout iconGrid = new GridLayout(requireContext());
        iconGrid.setColumnCount(5);
        iconGrid.setPadding(0, dp(14), 0, dp(4));
        scroll.addView(iconGrid, new ScrollView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));
        root.addView(scroll, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(320)));

        androidx.appcompat.app.AlertDialog dialog = new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.categories_icon_picker_title)
                .setView(root)
                .setNegativeButton(R.string.import_sheet_cancel, null)
                .create();

        for (String key : ICON_KEYS) {
            FrameLayout iconButton = new FrameLayout(requireContext());
            GradientDrawable bg = new GradientDrawable();
            bg.setShape(GradientDrawable.OVAL);
            boolean selected = key.equals(selectedIconKey);
            bg.setColor(ContextCompat.getColor(requireContext(), selected ? R.color.category_icon_picker_bg : R.color.md_theme_surfaceContainer));
            bg.setStroke(dp(selected ? 2 : 1), ContextCompat.getColor(requireContext(), selected ? R.color.planning_dialog_button : R.color.md_theme_outlineVariant));
            iconButton.setBackground(bg);
            iconButton.setClickable(true);
            iconButton.setFocusable(true);

            ImageView icon = new ImageView(requireContext());
            icon.setImageResource(CategoryPrefs.iconFor(key));
            icon.setColorFilter(ContextCompat.getColor(requireContext(), selected ? android.R.color.white : R.color.md_theme_onSurface));
            iconButton.addView(icon, new FrameLayout.LayoutParams(dp(26), dp(26), android.view.Gravity.CENTER));
            iconButton.setOnClickListener(v -> {
                callback.onPick(key);
                dialog.dismiss();
            });

            GridLayout.LayoutParams params = new GridLayout.LayoutParams();
            params.width = dp(52);
            params.height = dp(52);
            params.setMargins(0, 0, dp(12), dp(12));
            iconGrid.addView(iconButton, params);
        }
        dialog.show();
    }

    private boolean isSpecial(@NonNull Categoria categoria) {
        String clean = CategoryVisuals.normalize(categoria.nombre);
        return clean.contains("saldo inicial") || clean.contains("transferencia");
    }

    @NonNull
    private String hexFor(@ColorInt int value) {
        return String.format(Locale.US, "#%06X", 0xFFFFFF & value);
    }

    private interface IconPickCallback {
        void onPick(@NonNull String iconKey);
    }

    private final class CategoryColorSwatchView extends View {
        private final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint border = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint check = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint checkShadow = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final RectF rect = new RectF();
        private boolean selected;

        CategoryColorSwatchView(@NonNull android.content.Context context) {
            super(context);
            setLayerType(View.LAYER_TYPE_SOFTWARE, null);
            border.setStyle(Paint.Style.STROKE);
            check.setColor(Color.WHITE);
            check.setStyle(Paint.Style.STROKE);
            check.setStrokeCap(Paint.Cap.ROUND);
            check.setStrokeJoin(Paint.Join.ROUND);
            checkShadow.setColor(Color.argb(105, 0, 0, 0));
            checkShadow.setStyle(Paint.Style.STROKE);
            checkShadow.setStrokeCap(Paint.Cap.ROUND);
            checkShadow.setStrokeJoin(Paint.Join.ROUND);
            setClickable(true);
            setFocusable(true);
        }

        void setColor(@ColorInt int value) {
            fill.setColor(value);
            invalidate();
        }

        void setSelectedColor(boolean selected) {
            this.selected = selected;
            invalidate();
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            float inset = selected ? dp(3) : dp(5);
            rect.set(inset, inset, getWidth() - inset, getHeight() - inset);
            canvas.drawRoundRect(rect, dp(10), dp(10), fill);

            border.setStrokeWidth(dp(selected ? 3 : 1));
            border.setColor(ContextCompat.getColor(
                    getContext(),
                    selected ? R.color.md_theme_primary : R.color.md_theme_outlineVariant
            ));
            canvas.drawRoundRect(rect, dp(10), dp(10), border);

            if (selected) {
                float stroke = dp(3);
                check.setStrokeWidth(stroke);
                checkShadow.setStrokeWidth(stroke + dp(1));
                float startX = getWidth() * 0.34f;
                float startY = getHeight() * 0.53f;
                float midX = getWidth() * 0.45f;
                float midY = getHeight() * 0.64f;
                float endX = getWidth() * 0.68f;
                float endY = getHeight() * 0.38f;
                canvas.drawLine(startX, startY, midX, midY, checkShadow);
                canvas.drawLine(midX, midY, endX, endY, checkShadow);
                canvas.drawLine(startX, startY, midX, midY, check);
                canvas.drawLine(midX, midY, endX, endY, check);
            }
        }
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    static class CategoryAdapter extends RecyclerView.Adapter<CategoryAdapter.VH> {
        interface Listener {
            void onClick(@NonNull Categoria categoria);
            void onLongClick(@NonNull Categoria categoria);
        }
        private final List<Categoria> items = new ArrayList<>();
        private boolean deleted;
        Listener listener;

        void setItems(@NonNull List<Categoria> rows, boolean deleted) {
            this.deleted = deleted;
            items.clear();
            items.addAll(rows);
            notifyDataSetChanged();
        }

        @NonNull @Override public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            LinearLayout row = new LinearLayout(parent.getContext());
            row.setLayoutParams(new RecyclerView.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            ));
            row.setGravity(android.view.Gravity.CENTER_VERTICAL);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setPadding(0, 12, 0, 12);
            ImageView icon = new ImageView(parent.getContext());
            row.addView(icon, new LinearLayout.LayoutParams(dp(parent, 54), dp(parent, 54)));
            LinearLayout texts = new LinearLayout(parent.getContext());
            texts.setOrientation(LinearLayout.VERTICAL);
            TextView title = new TextView(parent.getContext());
            title.setTextColor(ContextCompat.getColor(parent.getContext(), R.color.md_theme_onSurface));
            title.setTextSize(17f);
            title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
            texts.addView(title);
            TextView sub = new TextView(parent.getContext());
            sub.setTextColor(ContextCompat.getColor(parent.getContext(), R.color.md_theme_onSurfaceVariant));
            sub.setTextSize(14f);
            texts.addView(sub);
            LinearLayout.LayoutParams textParams = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
            textParams.leftMargin = dp(parent, 14);
            row.addView(texts, textParams);
            TextView action = new TextView(parent.getContext());
            action.setTextColor(ContextCompat.getColor(parent.getContext(), R.color.planning_dialog_button));
            action.setTextSize(13f);
            row.addView(action, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            ImageView chevron = new ImageView(parent.getContext());
            chevron.setImageResource(R.drawable.ic_chevron_right);
            chevron.setColorFilter(ContextCompat.getColor(parent.getContext(), R.color.md_theme_onSurfaceVariant));
            LinearLayout.LayoutParams chevronParams = new LinearLayout.LayoutParams(dp(parent, 28), dp(parent, 28));
            chevronParams.leftMargin = dp(parent, 8);
            row.addView(chevron, chevronParams);
            return new VH(row, icon, title, sub, action, chevron);
        }

        @Override public void onBindViewHolder(@NonNull VH h, int pos) {
            Categoria c = items.get(pos);
            CategoryPrefs.Meta meta = CategoryPrefs.meta(h.itemView.getContext(), c);
            h.title.setText(c.nombre);
            h.sub.setText(c.esIngreso ? R.string.tipo_ingreso : R.string.tipo_gasto);
            h.icon.setImageResource(CategoryPrefs.iconFor(meta.iconKey));
            h.icon.setColorFilter(Color.WHITE);
            GradientDrawable bg = new GradientDrawable();
            bg.setShape(GradientDrawable.OVAL);
            bg.setColor(meta.color);
            h.icon.setBackground(bg);
            h.icon.setPadding(dp(h.itemView, 12), dp(h.itemView, 12), dp(h.itemView, 12), dp(h.itemView, 12));
            h.action.setVisibility(deleted ? View.VISIBLE : View.GONE);
            h.action.setText(R.string.categories_restore);
            h.chevron.setVisibility(deleted ? View.GONE : View.VISIBLE);
            h.itemView.setOnClickListener(v -> { if (listener != null) listener.onClick(c); });
            h.itemView.setOnLongClickListener(v -> {
                if (listener != null) listener.onLongClick(c);
                return true;
            });
        }
        @Override public int getItemCount() { return items.size(); }
        static int dp(@NonNull View view, int value) { return Math.round(value * view.getResources().getDisplayMetrics().density); }
        static class VH extends RecyclerView.ViewHolder {
            final ImageView icon, chevron;
            final TextView title, sub, action;
            VH(@NonNull View row, ImageView icon, TextView title, TextView sub, TextView action, ImageView chevron) {
                super(row);
                this.icon = icon;
                this.title = title;
                this.sub = sub;
                this.action = action;
                this.chevron = chevron;
            }
        }
    }
}
