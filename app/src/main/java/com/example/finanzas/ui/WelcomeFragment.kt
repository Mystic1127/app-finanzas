package com.example.finanzas.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.border
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.Fragment
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import com.example.finanzas.R
import com.example.finanzas.ui.compose.HighlightedSentence
import com.example.finanzas.ui.compose.SpendlyAuthBackground
import com.example.finanzas.ui.compose.SpendlyComposeTheme
import com.example.finanzas.ui.compose.SpendlyLogoMark
import com.example.finanzas.ui.compose.SpendlyOutlinedButton
import com.example.finanzas.ui.compose.SpendlyPrimaryButton
import com.example.finanzas.ui.compose.SpendlySimpleDot
import com.example.finanzas.ui.compose.spendlyAuthColors
import com.example.finanzas.util.Prefs

class WelcomeFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = androidx.compose.ui.platform.ComposeView(requireContext()).apply {
        setViewCompositionStrategy(androidx.compose.ui.platform.ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
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
    val colors = spendlyAuthColors()
    SpendlyAuthBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(40.dp))

            SpendlyLogoMark(markSize = 74.dp)

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = stringResource(R.string.app_name_spendly),
                color = colors.text,
                fontSize = 40.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.sp,
                textAlign = TextAlign.Center
            )

            HighlightedSentence(
                before = "Controla ",
                highlighted = "tu dinero",
                after = " con claridad.",
                modifier = Modifier.padding(top = 2.dp),
                fontSize = 18
            )

            Text(
                text = stringResource(R.string.welcome_description),
                color = colors.muted,
                fontSize = 14.sp,
                lineHeight = 22.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .padding(top = 16.dp)
                    .fillMaxWidth(0.92f)
            )

            Spacer(modifier = Modifier.height(24.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                WelcomeInfoCard(
                    iconRes = R.drawable.ic_account_balance_wallet,
                    title = stringResource(R.string.welcome_card_budget_title),
                    description = "Planifica",
                    tint = colors.accent,
                    modifier = Modifier.weight(1f)
                )
                WelcomeInfoCard(
                    iconRes = R.drawable.ic_credit_card,
                    title = stringResource(R.string.welcome_card_cards_title),
                    description = "Gestiona",
                    tint = colors.cyan,
                    modifier = Modifier.weight(1f)
                )
                WelcomeInfoCard(
                    iconRes = R.drawable.ic_trending_up,
                    title = stringResource(R.string.welcome_card_reports_title),
                    description = "Visualiza",
                    tint = colors.accent,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            SpendlyPrimaryButton(
                text = stringResource(R.string.welcome_login),
                onClick = onLoginClick
            )

            Spacer(modifier = Modifier.height(12.dp))

            SpendlyOutlinedButton(
                text = stringResource(R.string.welcome_register),
                onClick = onRegisterClick
            )

            SpendlySimpleDot(modifier = Modifier.padding(top = 22.dp), size = 8.dp)

            Text(
                text = stringResource(R.string.welcome_beta_version),
                color = if (colors.dark) colors.muted else Color(0xFF98A2B3),
                fontSize = 13.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 16.dp, bottom = 4.dp)
            )
        }
    }
}

@Composable
private fun WelcomeInfoCard(
    iconRes: Int,
    title: String,
    description: String,
    tint: Color,
    modifier: Modifier = Modifier
) {
    val colors = spendlyAuthColors()
    Column(
        modifier = modifier
            .height(154.dp)
            .then(if (colors.dark) Modifier.shadow(6.dp, RoundedCornerShape(24.dp), clip = false) else Modifier)
            .clip(RoundedCornerShape(24.dp))
            .background(colors.surface)
            .border(1.dp, colors.border.copy(alpha = if (colors.dark) 1f else 1f), RoundedCornerShape(24.dp))
            .padding(horizontal = 8.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(60.dp)
                .clip(CircleShape)
                .background(
                    if (colors.dark) {
                        Brush.radialGradient(
                            colors = listOf(colors.bg, colors.bg)
                        )
                    } else {
                        Brush.radialGradient(
                            colors = listOf(Color(0xFFF8FAF8), Color(0xFFF8FAF8))
                        )
                    }
                )
                .border(1.dp, if (colors.dark) colors.border else colors.border, CircleShape)
                .padding(1.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
                    .padding(0.dp)
            )
            Icon(
                painter = painterResource(iconRes),
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(32.dp)
            )
        }

        Text(
            text = title,
            color = colors.text,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            maxLines = 1,
            lineHeight = 15.sp,
            modifier = Modifier.padding(top = 12.dp)
        )

        Text(
            text = description,
            color = colors.muted,
            fontSize = 13.sp,
            lineHeight = 16.sp,
            textAlign = TextAlign.Center,
            maxLines = 1,
            modifier = Modifier.padding(top = 6.dp)
        )

        SpendlySimpleDot(modifier = Modifier.padding(top = 12.dp), color = tint, size = 7.dp)
    }
}
