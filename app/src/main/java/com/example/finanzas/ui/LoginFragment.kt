package com.example.finanzas.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import com.example.finanzas.R
import com.example.finanzas.data.api.AuthService
import com.example.finanzas.util.Prefs
import kotlinx.coroutines.launch

class LoginFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_login, container, false)

    override fun onViewCreated(v: View, savedInstanceState: Bundle?) {
        super.onViewCreated(v, savedInstanceState)

        if (Prefs.isLoggedIn(requireContext())) {
            val opts = NavOptions.Builder().setPopUpTo(R.id.nav_graph, true).build()
            findNavController().navigate(R.id.nav_home, null, opts)
            return
        }

        val etEmail = v.findViewById<EditText>(R.id.etEmail)
        val etPassword = v.findViewById<EditText>(R.id.etPassword)
        val btnLogin = v.findViewById<Button>(R.id.btnLogin)
        val linkRegistro = v.findViewById<View>(R.id.tvIrRegistro)

        linkRegistro.setOnClickListener { findNavController().navigate(R.id.nav_register) }

        btnLogin.setOnClickListener {
            val email = etEmail.text?.toString()?.trim().orEmpty()
            val pass = etPassword.text?.toString()?.trim().orEmpty()

            if (email.isEmpty() || pass.isEmpty()) {
                Toast.makeText(requireContext(), "Completa email y contraseña", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            btnLogin.isEnabled = false

            viewLifecycleOwner.lifecycleScope.launch {
                val result = AuthService.login(requireContext(), email, pass)
                if (!isAdded) return@launch

                if (result != null) {
                    Prefs.setToken(requireContext(), result.token)
                    Prefs.setUserSession(
                        requireContext(),
                        if (result.userId > 0) result.userId.toLong() else 1L,
                        result.email,
                        result.nombre
                    )

                    Toast.makeText(requireContext(), "¡Bienvenido, ${result.nombre}!", Toast.LENGTH_SHORT).show()
                    val opts = NavOptions.Builder().setPopUpTo(R.id.nav_graph, true).build()
                    findNavController().navigate(R.id.nav_home, null, opts)
                } else {
                    btnLogin.isEnabled = true
                    Toast.makeText(
                        requireContext(),
                        "Credenciales inválidas o servidor no disponible",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }
}
