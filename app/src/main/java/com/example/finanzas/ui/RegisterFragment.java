package com.example.finanzas.ui;

import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavOptions;
import androidx.navigation.Navigation;

import com.example.finanzas.R;
import com.example.finanzas.data.api.AuthService;
import com.example.finanzas.util.Prefs;

public class RegisterFragment extends Fragment {

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_register, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View v, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(v, savedInstanceState);

        EditText etNombre = v.findViewById(R.id.etNombre);
        EditText etEmail  = v.findViewById(R.id.etEmail);
        EditText etPass   = v.findViewById(R.id.etPassword);
        EditText etConf   = v.findViewById(R.id.etConfirm);
        Button btnReg     = v.findViewById(R.id.btnRegistrarme);
        View linkLogin    = v.findViewById(R.id.tvIrLogin);
        ProgressBar prg   = v.findViewById(R.id.progress);

        linkLogin.setOnClickListener(view -> {
            NavOptions opts = new NavOptions.Builder().setPopUpTo(R.id.nav_register, true).build();
            Navigation.findNavController(view).navigate(R.id.nav_login, null, opts);
        });

        btnReg.setOnClickListener(view -> {
            String nombre = etNombre.getText() == null ? "" : etNombre.getText().toString().trim();
            String email  = etEmail.getText()  == null ? "" : etEmail.getText().toString().trim();
            String pass   = etPass.getText()   == null ? "" : etPass.getText().toString().trim();
            String conf   = etConf.getText()   == null ? "" : etConf.getText().toString().trim();

            if (TextUtils.isEmpty(nombre) || TextUtils.isEmpty(email) ||
                    TextUtils.isEmpty(pass)   || TextUtils.isEmpty(conf)) {
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
                @Override public void onSuccess(String token, int userId, String nom, String em) {
                    if (token != null) Prefs.setToken(requireContext(), token);
                    requireContext().getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
                            .edit()
                            .putLong("user_id", userId > 0 ? userId : 1)
                            .putString("user_email", em)
                            .putString("user_name",  nom)
                            .apply();

                    prg.setVisibility(View.GONE);
                    Toast.makeText(requireContext(), "Cuenta creada. ¡Bienvenido!", Toast.LENGTH_SHORT).show();
                    NavOptions opts = new NavOptions.Builder().setPopUpTo(R.id.nav_graph, true).build();
                    Navigation.findNavController(view).navigate(R.id.nav_home, null, opts);
                }

                @Override public void onError() {
                    prg.setVisibility(View.GONE);
                    btnReg.setEnabled(true);
                    Toast.makeText(requireContext(),
                            "No se pudo registrar (verifica conexión o email ya usado)",
                            Toast.LENGTH_SHORT).show();
                }
            });
        });
    }
}
