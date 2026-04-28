package com.example.finanzas.ui

import android.os.Bundle
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import com.example.finanzas.R
import com.example.finanzas.data.api.AuthService
import com.example.finanzas.util.Prefs
import kotlinx.coroutines.launch

class RegisterFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_register, container, false)

    override fun onViewCreated(v: View, savedInstanceState: Bundle?) {
        super.onViewCreated(v, savedInstanceState)

        val etNombre = v.findViewById<EditText>(R.id.etNombre)
        val etEmail = v.findViewById<EditText>(R.id.etEmail)
        val etPass = v.findViewById<EditText>(R.id.etPassword)
        val etConf = v.findViewById<EditText>(R.id.etConfirm)
        val btnReg = v.findViewById<Button>(R.id.btnRegistrarme)
        val linkLogin = v.findViewById<View>(R.id.tvIrLogin)
        val prg = v.findViewById<ProgressBar>(R.id.progress)

        linkLogin.setOnClickListener {
            val opts = NavOptions.Builder().setPopUpTo(R.id.nav_register, true).build()
            findNavController().navigate(R.id.nav_login, null, opts)
        }

        btnReg.setOnClickListener {
            val nombre = etNombre.text?.toString()?.trim().orEmpty()
            val email = etEmail.text?.toString()?.trim().orEmpty()
            val pass = etPass.text?.toString()?.trim().orEmpty()
            val conf = etConf.text?.toString()?.trim().orEmpty()

            if (TextUtils.isEmpty(nombre) || TextUtils.isEmpty(email) ||
                TextUtils.isEmpty(pass) || TextUtils.isEmpty(conf)
            ) {
                Toast.makeText(requireContext(), "Completa todos los campos", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (pass != conf) {
                Toast.makeText(requireContext(), "Las contraseñas no coinciden", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            btnReg.isEnabled = false
            prg.visibility = View.VISIBLE

            viewLifecycleOwner.lifecycleScope.launch {
                val result = AuthService.register(requireContext(), nombre, email, pass)
                if (!isAdded) return@launch

                if (result != null) {
                    Prefs.setToken(requireContext(), result.token)
                    Prefs.setUserSession(
                        requireContext(),
                        if (result.userId > 0) result.userId.toLong() else 1L,
                        result.email,
                        result.nombre
                    )

                    prg.visibility = View.GONE
                    Toast.makeText(requireContext(), "Cuenta creada. ¡Bienvenido!", Toast.LENGTH_SHORT).show()
                    val opts = NavOptions.Builder().setPopUpTo(R.id.nav_graph, true).build()
                    findNavController().navigate(R.id.nav_home, null, opts)
                } else {
                    prg.visibility = View.GONE
                    btnReg.isEnabled = true
                    Toast.makeText(
                        requireContext(),
                        "No se pudo registrar (verifica conexión o email ya usado)",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }
}
