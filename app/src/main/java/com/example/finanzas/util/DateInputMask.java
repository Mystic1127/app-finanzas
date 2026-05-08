package com.example.finanzas.util;

import android.text.Editable;
import android.text.TextWatcher;

import com.google.android.material.textfield.TextInputEditText;

public class DateInputMask implements TextWatcher {

    private boolean updating;
    private final TextInputEditText editText;

    public DateInputMask(TextInputEditText editText) {
        this.editText = editText;
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) { }

    @Override
    public void afterTextChanged(Editable s) {
        if (updating) return;
        updating = true;

        String digits = s.toString().replaceAll("[^0-9]", "");
        if (digits.length() > 8) {
            digits = digits.substring(0, 8);
        }

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < digits.length(); i++) {
            sb.append(digits.charAt(i));
            if ((i == 3 || i == 5) && i < digits.length() - 1) {
                sb.append("-");
            }
        }

        editText.removeTextChangedListener(this);
        editText.setText(sb.toString());
        if (editText.getText() != null) {
            editText.setSelection(editText.getText().length());
        }
        editText.addTextChangedListener(this);

        updating = false;
    }
}
