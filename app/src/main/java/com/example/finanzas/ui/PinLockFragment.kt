package com.example.finanzas.ui;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
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

public class PinLockFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_pin_lock, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View v, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(v, savedInstanceState);

        TextInputLayout tilPin = v.findViewById(R.id.tilPin);
        TextInputEditText etPin = v.findViewById(R.id.etPin);
        MaterialButton btnUnlock = v.findViewById(R.id.btnUnlock);
        View tvForgot = v.findViewById(R.id.tvForgotPin);

        requireActivity().getOnBackPressedDispatcher().addCallback(getViewLifecycleOwner(),
                new OnBackPressedCallback(true) {
                    @Override
                    public void handleOnBackPressed() {
                        // Evitar salir sin PIN
                    }
                });

        btnUnlock.setOnClickListener(view -> {
            String pin = etPin.getText() == null ? "" : etPin.getText().toString().trim();
            tilPin.setError(null);
            if (pin.length() != 4 || !TextUtils.isDigitsOnly(pin)) {
                tilPin.setError(getString(R.string.pin_setup_invalid));
                return;
            }

            if (!Prefs.verifyPin(requireContext(), pin)) {
                tilPin.setError(getString(R.string.pin_lock_wrong));
                return;
            }

            PinSession.unlock();
            Navigation.findNavController(view).popBackStack();
        });

        tvForgot.setOnClickListener(view -> new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.pin_lock_forgot)
                .setMessage(R.string.pin_lock_clear_confirm)
                .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                    Prefs.clearPin(requireContext());
                    PinSession.lock();
                    Toast.makeText(requireContext(), R.string.pin_lock_cleared, Toast.LENGTH_SHORT).show();
                    Navigation.findNavController(view).popBackStack();
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show());
    }
}
