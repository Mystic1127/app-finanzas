package com.example.finanzas.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.example.finanzas.R;
import com.example.finanzas.data.api.UserService;
import com.example.finanzas.util.Prefs;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;


public class PerfilFragment extends Fragment {

    private MaterialButton btnRemovePin;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_perfil, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View v, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(v, savedInstanceState);

        TextView tvNombre = v.findViewById(R.id.tvPerfilNombre);
        TextView tvEmail  = v.findViewById(R.id.tvPerfilEmail);
        MaterialButton btnCambiarPass = v.findViewById(R.id.btnCambiarPass);
        MaterialButton btnConfigPin = v.findViewById(R.id.btnConfigPin);
        btnRemovePin = v.findViewById(R.id.btnRemovePin);

        String cachedNombre = Prefs.getCurrentUserName(requireContext());
        String cachedEmail  = Prefs.getCurrentUserEmail(requireContext());
        if (cachedNombre == null || cachedNombre.isEmpty()) cachedNombre = "—";
        if (cachedEmail == null || cachedEmail.isEmpty()) cachedEmail = "—";
        tvNombre.setText(cachedNombre);
        tvEmail.setText(cachedEmail);

        if (getContext() != null && cachedEmail != null && !"—".equals(cachedEmail)) {
            UserService.getMe(requireContext(), cachedEmail, new UserService.MeCb() {
                @Override public void onOk(int id, String nom, String em) {
                    if (!isAdded()) return;
                    tvNombre.setText(nom);
                    tvEmail.setText(em);
                    Prefs.setUserSession(requireContext(), id, em, nom);
                }
                @Override public void onFail() { }
            });
        }

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

    private void updatePinButtons() {
        if (btnRemovePin == null || getContext() == null) return;
        btnRemovePin.setVisibility(Prefs.hasPin(requireContext()) ? View.VISIBLE : View.GONE);
    }
}
