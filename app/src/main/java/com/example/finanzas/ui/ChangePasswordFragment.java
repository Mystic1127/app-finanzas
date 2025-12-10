package com.example.finanzas.ui;

import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import com.example.finanzas.R;
import com.example.finanzas.data.api.UserService;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;

public class ChangePasswordFragment extends Fragment {

    private AlertDialog dialog;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return new FrameLayout(inflater.getContext());
    }

    @Override
    public void onViewCreated(@NonNull View v, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(v, savedInstanceState);

        View content = LayoutInflater.from(requireContext()).inflate(R.layout.fragment_change_password, null, false);
        TextInputEditText etOld = content.findViewById(R.id.etOldPass);
        TextInputEditText etNew = content.findViewById(R.id.etNewPass);
        TextInputEditText etConf = content.findViewById(R.id.etConfirmPass);
        MaterialButton btnSave = content.findViewById(R.id.btnGuardarPass);

        btnSave.setOnClickListener(view -> {
            String oldP = etOld.getText() == null ? "" : etOld.getText().toString().trim();
            String newP = etNew.getText() == null ? "" : etNew.getText().toString().trim();
            String conf = etConf.getText() == null ? "" : etConf.getText().toString().trim();

            if (TextUtils.isEmpty(oldP) || TextUtils.isEmpty(newP) || TextUtils.isEmpty(conf)) {
                Toast.makeText(requireContext(), "Completa todos los campos", Toast.LENGTH_SHORT).show();
                return;
            }
            if (!newP.equals(conf)) {
                Toast.makeText(requireContext(), "La confirmación no coincide", Toast.LENGTH_SHORT).show();
                return;
            }

            long userId = requireContext().getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
                    .getLong("user_id", -1);
            if (userId <= 0) {
                Toast.makeText(requireContext(), "Inicia sesión nuevamente", Toast.LENGTH_SHORT).show();
                return;
            }

            UserService.changePassword(requireContext(), oldP, newP, new UserService.SimpleCb() {
                @Override
                public void onOk() {
                    Toast.makeText(requireContext(), "Contraseña actualizada correctamente.", Toast.LENGTH_SHORT).show();

                    requireContext().getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
                            .edit().clear().apply();
                    com.example.finanzas.util.Prefs.setToken(requireContext(), null);

                    NavHostFragment.findNavController(ChangePasswordFragment.this).navigate(R.id.nav_login);
                    dismissDialog();
                }

                @Override
                public void onFail() {
                    Toast.makeText(requireContext(),
                            "No se pudo actualizar en el servidor. Verifica tu contraseña o conexión.",
                            Toast.LENGTH_LONG).show();
                }
            });
        });

        dialog = new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.perfil_cambiar_pass)
                .setView(content)
                .setCancelable(true)
                .create();
        dialog.show();
    }

    @Override
    public void onDestroyView() {
        dismissDialog();
        super.onDestroyView();
    }

    private void dismissDialog() {
        if (dialog != null && dialog.isShowing()) {
            dialog.dismiss();
        }
    }
}
