package com.example.finanzas.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import com.example.finanzas.R
import com.example.finanzas.util.Prefs
import com.google.android.material.button.MaterialButton

class WelcomeFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_welcome, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (Prefs.isLoggedIn(requireContext())) {
            val opts = NavOptions.Builder().setPopUpTo(R.id.nav_graph, true).build()
            findNavController().navigate(R.id.nav_home, null, opts)
            return
        }

        view.findViewById<MaterialButton>(R.id.btnWelcomeLogin).setOnClickListener {
            findNavController().navigate(R.id.nav_login)
        }
        view.findViewById<MaterialButton>(R.id.btnWelcomeRegister).setOnClickListener {
            findNavController().navigate(R.id.nav_register)
        }
    }
}
