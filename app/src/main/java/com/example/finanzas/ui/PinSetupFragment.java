package com.example.finanzas.ui;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.example.finanzas.R;
import com.example.finanzas.util.Prefs;
import com.example.finanzas.util.PinSession;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

public class PinSetupFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_pin_setup, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View v, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(v, savedInstanceState);

        TextInputLayout tilNew = v.findViewById(R.id.tilPinNuevo);
        TextInputLayout tilConfirm = v.findViewById(R.id.tilPinConfirm);
        TextInputEditText etNew = v.findViewById(R.id.etPinNuevo);
        TextInputEditText etConfirm = v.findViewById(R.id.etPinConfirm);
        MaterialButton btnSave = v.findViewById(R.id.btnGuardarPin);
        MaterialButton btnRemove = v.findViewById(R.id.btnQuitarPin);

        if (!Prefs.hasPin(requireContext())) {
            btnRemove.setVisibility(View.GONE);
        }

        btnSave.setOnClickListener(view -> {
            String pin = etNew.getText() == null ? "" : etNew.getText().toString().trim();
            String confirm = etConfirm.getText() == null ? "" : etConfirm.getText().toString().trim();
            tilNew.setError(null);
            tilConfirm.setError(null);

            if (pin.length() != 4 || !TextUtils.isDigitsOnly(pin)) {
                tilNew.setError(getString(R.string.pin_setup_invalid));
                return;
            }
            if (!pin.equals(confirm)) {
                tilConfirm.setError(getString(R.string.pin_setup_mismatch));
                return;
            }

            Prefs.savePin(requireContext(), pin);
            PinSession.unlock();
            Toast.makeText(requireContext(), R.string.pin_setup_success, Toast.LENGTH_SHORT).show();
            Navigation.findNavController(view).popBackStack();
        });

        btnRemove.setOnClickListener(view -> new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.pin_setup_remove)
                .setMessage(R.string.pin_setup_remove_confirm)
                .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                    Prefs.clearPin(requireContext());
                    PinSession.lock();
                    Toast.makeText(requireContext(), R.string.pin_removed_success, Toast.LENGTH_SHORT).show();
                    Navigation.findNavController(view).popBackStack();
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show());
    }
}
