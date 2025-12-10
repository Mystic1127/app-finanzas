package com.example.finanzas.ui;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
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

public class LoginFragment extends Fragment {

    private AlertDialog dialog;

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return new FrameLayout(inflater.getContext());
    }

    @Override
    public void onViewCreated(@NonNull View v, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(v, savedInstanceState);

        if (Prefs.getToken(requireContext()) != null ||
                requireContext().getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
                        .getLong("user_id", -1) > 0) {
            NavOptions opts = new NavOptions.Builder().setPopUpTo(R.id.nav_graph, true).build();
            NavHostFragment.findNavController(this).navigate(R.id.nav_home, null, opts);
            return;
        }

        View content = LayoutInflater.from(requireContext()).inflate(R.layout.fragment_login, null, false);
        EditText etEmail    = content.findViewById(R.id.etEmail);
        EditText etPassword = content.findViewById(R.id.etPassword);
        Button btnLogin     = content.findViewById(R.id.btnLogin);
        View linkRegistro   = content.findViewById(R.id.tvIrRegistro);

        linkRegistro.setOnClickListener(view ->
                NavHostFragment.findNavController(this).navigate(R.id.nav_register));

        btnLogin.setOnClickListener(view -> {
            String email = etEmail.getText()    == null ? "" : etEmail.getText().toString().trim();
            String pass  = etPassword.getText() == null ? "" : etPassword.getText().toString().trim();
            if (email.isEmpty() || pass.isEmpty()) {
                Toast.makeText(requireContext(), "Completa email y contraseña", Toast.LENGTH_SHORT).show();
                return;
            }

            btnLogin.setEnabled(false);

            AuthService.login(requireContext(), email, pass, new AuthService.Callback() {
                @Override public void onSuccess(String token, int userId, String nombre, String em) {
                    if (token != null) Prefs.setToken(requireContext(), token);
                    requireContext().getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
                            .edit()
                            .putLong("user_id", userId > 0 ? userId : 1)
                            .putString("user_email", em)
                            .putString("user_name",  nombre == null ? "" : nombre)
                            .apply();

                    Toast.makeText(requireContext(), "¡Bienvenido, " + nombre + "!", Toast.LENGTH_SHORT).show();
                    NavOptions opts = new NavOptions.Builder().setPopUpTo(R.id.nav_graph, true).build();
                    NavHostFragment.findNavController(LoginFragment.this).navigate(R.id.nav_home, null, opts);
                    dismissDialog();
                }

                @Override public void onError() {
                    btnLogin.setEnabled(true);
                    Toast.makeText(requireContext(),
                            "Credenciales inválidas o servidor no disponible",
                            Toast.LENGTH_SHORT).show();
                }
            });
        });

        dialog = new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.auth_titulo_login)
                .setView(content)
                .setCancelable(false)
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
