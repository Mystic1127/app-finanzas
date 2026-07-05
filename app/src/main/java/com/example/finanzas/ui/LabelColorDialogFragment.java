package com.example.finanzas.ui;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.FrameLayout;
import android.widget.GridLayout;
import android.widget.TextView;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.DialogFragment;

import com.example.finanzas.R;
import com.example.finanzas.util.TransactionLabelStore;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.util.Locale;

public class LabelColorDialogFragment extends DialogFragment {
    public static final String REQUEST_KEY = "label_color_dialog_result";
    public static final String RESULT_LABEL_ID = "label_id";
    public static final String RESULT_LABEL_NAME = "label_name";
    public static final String RESULT_COLOR_HEX = "color_hex";

    private static final String ARG_LABEL_ID = "arg_label_id";
    private static final String ARG_LABEL_NAME = "arg_label_name";
    private static final String ARG_COLOR_HEX = "arg_color_hex";

    private TextInputLayout tilName;
    private TextInputLayout tilHex;
    private TextInputEditText etName;
    private TextInputEditText etHex;
    private ColorPreviewView preview;
    private GridLayout paletteGrid;
    private boolean internalHexChange;
    private String selectedHex = "#4FA37A";

    @NonNull
    public static LabelColorDialogFragment newInstance(
            @Nullable String labelId,
            @Nullable String labelName,
            @Nullable String colorHex
    ) {
        Bundle args = new Bundle();
        args.putString(ARG_LABEL_ID, labelId);
        args.putString(ARG_LABEL_NAME, labelName);
        args.putString(ARG_COLOR_HEX, colorHex);
        LabelColorDialogFragment fragment = new LabelColorDialogFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        Dialog dialog = super.onCreateDialog(savedInstanceState);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        return dialog;
    }

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState
    ) {
        return inflater.inflate(R.layout.dialog_label_color, container, false);
    }

    @Override
    public void onStart() {
        super.onStart();
        Dialog dialog = getDialog();
        if (dialog == null) return;
        Window window = dialog.getWindow();
        if (window == null) return;
        window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        window.setDimAmount(0.46f);
        window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);

        int screenWidth = getResources().getDisplayMetrics().widthPixels;
        int width = Math.min(screenWidth - dp(28), dp(520));
        window.setLayout(Math.max(width, dp(320)), WindowManager.LayoutParams.WRAP_CONTENT);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        tilName = view.findViewById(R.id.tilLabelName);
        tilHex = view.findViewById(R.id.tilLabelHex);
        etName = view.findViewById(R.id.etLabelName);
        etHex = view.findViewById(R.id.etLabelHex);
        paletteGrid = view.findViewById(R.id.gridLabelPalette);
        MaterialButton cancel = view.findViewById(R.id.btnLabelCancel);
        MaterialButton save = view.findViewById(R.id.btnLabelSave);
        TextView title = view.findViewById(R.id.tvLabelColorTitle);
        FrameLayout previewHost = view.findViewById(R.id.colorPreview);

        Bundle args = getArguments() == null ? Bundle.EMPTY : getArguments();
        String labelId = args.getString(ARG_LABEL_ID);
        String name = args.getString(ARG_LABEL_NAME, "");
        selectedHex = validOrDefault(args.getString(ARG_COLOR_HEX));

        title.setText(labelId == null ? R.string.transaction_label_create : R.string.transaction_label_edit);
        etName.setText(name);
        setHexText(selectedHex);

        preview = new ColorPreviewView(requireContext());
        previewHost.addView(preview, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        ));
        preview.setColor(Color.parseColor(selectedHex));

        buildPalette();

        cancel.setOnClickListener(v -> dismiss());
        save.setOnClickListener(v -> submit(labelId));
        tilHex.setEndIconOnClickListener(v -> {
            etHex.requestFocus();
            etHex.setSelection(etHex.length());
            InputMethodManager imm = (InputMethodManager) requireContext().getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) imm.showSoftInput(etHex, InputMethodManager.SHOW_IMPLICIT);
        });

        etName.addTextChangedListener(new SimpleWatcher() {
            @Override public void afterTextChanged(Editable s) {
                if (tilName != null && s != null && s.toString().trim().length() > 0) {
                    tilName.setError(null);
                }
            }
        });
        etHex.addTextChangedListener(new SimpleWatcher() {
            @Override public void afterTextChanged(Editable s) {
                if (internalHexChange) return;
                String raw = s == null ? "" : s.toString().trim();
                if (TransactionLabelStore.isValidHex(raw)) {
                    String normalized = TransactionLabelStore.normalizeHex(raw);
                    if (!normalized.equals(raw)) {
                        int cursor = Math.min(etHex.getSelectionStart(), normalized.length());
                        setHexText(normalized);
                        etHex.setSelection(Math.max(0, cursor));
                    }
                    updateSelectedHex(normalized, true);
                    tilHex.setError(null);
                } else if (!raw.isEmpty()) {
                    tilHex.setError(getString(R.string.transaction_label_hex_error));
                    refreshPaletteSelection("");
                } else {
                    tilHex.setError(null);
                    refreshPaletteSelection("");
                }
            }
        });
    }

    private void submit(@Nullable String labelId) {
        String name = etName.getText() == null ? "" : etName.getText().toString().trim();
        String rawHex = etHex.getText() == null ? "" : etHex.getText().toString().trim();
        boolean valid = true;

        if (name.isEmpty()) {
            tilName.setError(getString(R.string.transaction_label_name_error));
            valid = false;
        } else {
            tilName.setError(null);
        }

        if (!TransactionLabelStore.isValidHex(rawHex)) {
            tilHex.setError(getString(R.string.transaction_label_hex_error));
            valid = false;
        } else {
            tilHex.setError(null);
        }

        if (!valid) return;

        String normalized = TransactionLabelStore.normalizeHex(rawHex);
        Bundle result = new Bundle();
        result.putString(RESULT_LABEL_ID, labelId);
        result.putString(RESULT_LABEL_NAME, name);
        result.putString(RESULT_COLOR_HEX, normalized);
        getParentFragmentManager().setFragmentResult(REQUEST_KEY, result);
        dismiss();
    }

    private void buildPalette() {
        paletteGrid.removeAllViews();
        String[] colors = TransactionLabelStore.paletteColors();
        for (int i = 0; i < colors.length; i++) {
            String colorHex = colors[i];
            SwatchView swatch = new SwatchView(requireContext());
            swatch.setColor(Color.parseColor(colorHex));
            swatch.setTag(colorHex);
            swatch.setSelectedColor(colorHex.equals(selectedHex));
            swatch.setOnClickListener(v -> {
                String picked = v.getTag().toString();
                setHexText(picked);
                updateSelectedHex(picked, true);
                tilHex.setError(null);
            });

            GridLayout.LayoutParams params = new GridLayout.LayoutParams(
                    GridLayout.spec(i / 6),
                    GridLayout.spec(i % 6, 1f)
            );
            params.width = 0;
            params.height = dp(42);
            params.setMargins(0, 0, i % 6 == 5 ? 0 : dp(8), dp(7));
            paletteGrid.addView(swatch, params);
        }
    }

    private void updateSelectedHex(@NonNull String colorHex, boolean refreshText) {
        if (!TransactionLabelStore.isValidHex(colorHex)) return;
        selectedHex = TransactionLabelStore.normalizeHex(colorHex);
        if (refreshText) setHexText(selectedHex);
        preview.setColor(Color.parseColor(selectedHex));
        refreshPaletteSelection(selectedHex);
    }

    private void refreshPaletteSelection(@NonNull String colorHex) {
        String normalized = TransactionLabelStore.normalizeHex(colorHex);
        boolean matched = false;
        for (int i = 0; i < paletteGrid.getChildCount(); i++) {
            View child = paletteGrid.getChildAt(i);
            if (child instanceof SwatchView) {
                String childHex = child.getTag() == null ? "" : child.getTag().toString();
                boolean selected = !matched && normalized.equals(childHex);
                ((SwatchView) child).setSelectedColor(selected);
                if (selected) matched = true;
            }
        }
    }

    private void setHexText(@NonNull String value) {
        internalHexChange = true;
        etHex.setText(value);
        etHex.setSelection(etHex.length());
        internalHexChange = false;
    }

    @NonNull
    private String validOrDefault(@Nullable String colorHex) {
        String normalized = TransactionLabelStore.normalizeHex(colorHex);
        return TransactionLabelStore.isValidHex(normalized) ? normalized : "#4FA37A";
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private abstract static class SimpleWatcher implements TextWatcher {
        @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
        @Override public void onTextChanged(CharSequence s, int start, int before, int count) { }
    }

    private final class ColorPreviewView extends View {
        private final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint stroke = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final RectF rect = new RectF();

        ColorPreviewView(@NonNull Context context) {
            super(context);
            stroke.setStyle(Paint.Style.STROKE);
            stroke.setStrokeWidth(dp(2));
            stroke.setColor(ContextCompat.getColor(context, R.color.md_theme_outline));
        }

        void setColor(@ColorInt int color) {
            fill.setColor(color);
            invalidate();
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            float inset = dp(3);
            rect.set(inset, inset, getWidth() - inset, getHeight() - inset);
            canvas.drawRoundRect(rect, dp(12), dp(12), fill);
            canvas.drawRoundRect(rect, dp(12), dp(12), stroke);
        }
    }

    private final class SwatchView extends View {
        private final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint border = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint check = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint checkShadow = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final RectF rect = new RectF();
        private boolean selected;

        SwatchView(@NonNull Context context) {
            super(context);
            setLayerType(View.LAYER_TYPE_SOFTWARE, null);
            border.setStyle(Paint.Style.STROKE);
            check.setColor(Color.WHITE);
            check.setStyle(Paint.Style.STROKE);
            check.setStrokeCap(Paint.Cap.ROUND);
            check.setStrokeJoin(Paint.Join.ROUND);
            check.setTypeface(Typeface.DEFAULT_BOLD);
            checkShadow.setColor(Color.argb(105, 0, 0, 0));
            checkShadow.setStyle(Paint.Style.STROKE);
            checkShadow.setStrokeCap(Paint.Cap.ROUND);
            checkShadow.setStrokeJoin(Paint.Join.ROUND);
            setClickable(true);
        }

        void setColor(@ColorInt int color) {
            fill.setColor(color);
            invalidate();
        }

        void setSelectedColor(boolean selected) {
            this.selected = selected;
            invalidate();
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            float selectedInset = selected ? dp(3) : dp(5);
            rect.set(selectedInset, selectedInset, getWidth() - selectedInset, getHeight() - selectedInset);
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
}
