package com.example.finanzas.ui;

import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavOptions;
import androidx.navigation.fragment.NavHostFragment;

import com.example.finanzas.R;
import com.example.finanzas.data.api.AuthService;
import com.example.finanzas.util.Prefs;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

public class RegisterFragment extends Fragment {

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

        View content = LayoutInflater.from(requireContext()).inflate(R.layout.fragment_register, null, false);
        EditText etNombre = content.findViewById(R.id.etNombre);
        EditText etEmail = content.findViewById(R.id.etEmail);
        EditText etPass = content.findViewById(R.id.etPassword);
        EditText etConf = content.findViewById(R.id.etConfirm);
        Button btnReg = content.findViewById(R.id.btnRegistrarme);
        View linkLogin = content.findViewById(R.id.tvIrLogin);
        ProgressBar prg = content.findViewById(R.id.progress);

        linkLogin.setOnClickListener(view -> {
            NavOptions opts = new NavOptions.Builder().setPopUpTo(R.id.nav_register, true).build();
            NavHostFragment.findNavController(this).navigate(R.id.nav_login, null, opts);
        });

        btnReg.setOnClickListener(view -> {
            String nombre = etNombre.getText() == null ? "" : etNombre.getText().toString().trim();
            String email = etEmail.getText() == null ? "" : etEmail.getText().toString().trim();
            String pass = etPass.getText() == null ? "" : etPass.getText().toString().trim();
            String conf = etConf.getText() == null ? "" : etConf.getText().toString().trim();

            if (TextUtils.isEmpty(nombre) || TextUtils.isEmpty(email) ||
                    TextUtils.isEmpty(pass) || TextUtils.isEmpty(conf)) {
                Toast.makeText(requireContext(), "Completa todos los campos", Toast.LENGTH_SHORT).show();
                return;
            }
            if (!pass.equals(conf)) {
                Toast.makeText(requireContext(), "Las contraseñas no coinciden", Toast.LENGTH_SHORT).show();
                return;
            }

            btnReg.setEnabled(false);
            prg.setVisibility(View.VISIBLE);

            AuthService.register(requireContext(), nombre, email, pass, new AuthService.Callback() {
                @Override
                public void onSuccess(String token, int userId, String nom, String em) {
                    if (token != null) Prefs.setToken(requireContext(), token);
                    requireContext().getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
                            .edit()
                            .putLong("user_id", userId > 0 ? userId : 1)
                            .putString("user_email", em)
                            .putString("user_name", nom)
                            .apply();

                    prg.setVisibility(View.GONE);
                    Toast.makeText(requireContext(), "Cuenta creada. ¡Bienvenido!", Toast.LENGTH_SHORT).show();
                    NavOptions opts = new NavOptions.Builder().setPopUpTo(R.id.nav_graph, true).build();
                    NavHostFragment.findNavController(RegisterFragment.this).navigate(R.id.nav_home, null, opts);
                    dismissDialog();
                }

                @Override
                public void onError() {
                    prg.setVisibility(View.GONE);
                    btnReg.setEnabled(true);
                    Toast.makeText(requireContext(),
                            "No se pudo registrar (verifica conexión o email ya usado)",
                            Toast.LENGTH_SHORT).show();
                }
            });
        });

        dialog = new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.auth_link_register)
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
