package com.example.finanzas.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.Fragment
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import com.example.finanzas.R
import com.example.finanzas.ui.compose.SpendlyComposeTheme
import com.example.finanzas.util.Prefs

class WelcomeFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = ComposeView(requireContext()).apply {
        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        setContent {
            SpendlyComposeTheme {
                WelcomeScreen(
                    onLoginClick = { findNavController().navigate(R.id.nav_login) },
                    onRegisterClick = { findNavController().navigate(R.id.nav_register) }
                )
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (Prefs.isLoggedIn(requireContext())) {
            val opts = NavOptions.Builder().setPopUpTo(R.id.nav_graph, true).build()
            findNavController().navigate(R.id.nav_home, null, opts)
        }
    }
}

@Composable
private fun WelcomeScreen(
    onLoginClick: () -> Unit,
    onRegisterClick: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .padding(horizontal = 28.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.weight(0.6f))

            SpendlyWelcomeVisual()

            Spacer(modifier = Modifier.height(36.dp))

            Text(
                text = stringResource(R.string.app_name_spendly),
                color = MaterialTheme.colorScheme.onBackground,
                fontSize = 46.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.sp,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = stringResource(R.string.welcome_tagline),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.weight(1f))

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = onLoginClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text(text = stringResource(R.string.welcome_login))
                }

                OutlinedButton(
                    onClick = onRegisterClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text(text = stringResource(R.string.welcome_register))
                }
            }
        }
    }
}

@Composable
private fun SpendlyWelcomeVisual() {
    val colors = MaterialTheme.colorScheme
    Box(
        modifier = Modifier
            .size(144.dp)
            .clip(RoundedCornerShape(36.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(colors.primaryContainer, colors.surfaceVariant),
                    start = Offset.Zero,
                    end = Offset.Infinite
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(
                color = Color.White.copy(alpha = 0.18f),
                radius = size.minDimension * 0.34f,
                center = Offset(size.width * 0.28f, size.height * 0.28f)
            )
            drawCircle(
                color = colors.primary.copy(alpha = 0.36f),
                radius = size.minDimension * 0.28f,
                center = Offset(size.width * 0.72f, size.height * 0.68f)
            )
            drawRoundRect(
                color = colors.surface.copy(alpha = 0.78f),
                topLeft = Offset(size.width * 0.24f, size.height * 0.34f),
                size = androidx.compose.ui.geometry.Size(size.width * 0.52f, size.height * 0.32f),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(18.dp.toPx())
            )
            drawCircle(
                color = colors.primary,
                radius = size.minDimension * 0.055f,
                center = Offset(size.width * 0.62f, size.height * 0.50f)
            )
        }
    }
}
