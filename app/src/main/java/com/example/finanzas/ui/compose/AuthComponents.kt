package com.example.finanzas.ui.compose

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.finanzas.R

internal val SpendlyAuthBg = Color(0xFF020812)
internal val SpendlyAuthBgDeep = Color(0xFF00040A)
internal val SpendlyAuthSurface = Color(0xB30A1320)
internal val SpendlyAuthField = Color(0x73060D16)
internal val SpendlyAuthBorder = Color(0xFF26384B)
internal val SpendlyAuthText = Color(0xFFF4F7F8)
internal val SpendlyAuthMuted = Color(0xFFACB8C7)
internal val SpendlyAuthGreen = Color(0xFF00B985)
internal val SpendlyAuthTeal = Color(0xFF20D4B0)
internal val SpendlyAuthCyan = Color(0xFF55D8E6)

internal data class SpendlyAuthColors(
    val dark: Boolean,
    val bg: Color,
    val bgDeep: Color,
    val surface: Color,
    val field: Color,
    val border: Color,
    val text: Color,
    val muted: Color,
    val accent: Color,
    val teal: Color,
    val cyan: Color
)

@Composable
internal fun spendlyAuthColors(): SpendlyAuthColors {
    val dark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    return if (dark) {
        SpendlyAuthColors(
            dark = true,
            bg = Color(0xFF07111A),
            bgDeep = Color(0xFF07111A),
            surface = Color(0xFF0D1B2A),
            field = Color(0xFF0D1B2A),
            border = Color(0xFF17324A),
            text = Color(0xFFF8FAFC),
            muted = Color(0xFF94A3B8),
            accent = Color(0xFF19B47A),
            teal = Color(0xFF36D399),
            cyan = Color(0xFF22D3EE)
        )
    } else {
        SpendlyAuthColors(
            dark = false,
            bg = Color(0xFFF8FAF8),
            bgDeep = Color(0xFFF8FAF8),
            surface = Color(0xFFFFFFFF),
            field = Color(0xFFFFFFFF),
            border = Color(0xFFE7F5E8),
            text = Color(0xFF0F172A),
            muted = Color(0xFF667085),
            accent = Color(0xFF7BC47F),
            teal = Color(0xFF92D496),
            cyan = Color(0xFF36C2CF)
        )
    }
}

@Composable
internal fun SpendlyAuthScreenContainer(content: @Composable ColumnScope.() -> Unit) {
    SpendlyAuthBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .navigationBarsPadding()
                .imePadding()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            content = content
        )
    }
}

@Composable
internal fun SpendlyAuthBackground(content: @Composable () -> Unit) {
    val colors = spendlyAuthColors()
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = if (colors.dark) {
                        listOf(Color(0xFF0A1723), colors.bg, colors.bgDeep)
                    } else {
                        listOf(colors.bg, colors.bg, colors.bgDeep)
                    },
                    center = Offset(0.50f, 0.30f),
                    radius = 980f
                )
            )
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(if (colors.dark) Color(0xFF1A3A4D).copy(alpha = 0.20f) else colors.teal.copy(alpha = 0.22f), Color.Transparent),
                    center = Offset(size.width * 0.50f, size.height * 0.24f),
                    radius = size.width * 0.70f
                ),
                radius = size.width * 0.70f,
                center = Offset(size.width * 0.50f, size.height * 0.24f)
            )
            drawArc(
                color = if (colors.dark) Color(0xFF123348).copy(alpha = 0.30f) else Color(0xFFD7ECD9).copy(alpha = 0.72f),
                startAngle = 157f,
                sweepAngle = 228f,
                useCenter = false,
                topLeft = Offset(-size.width * 0.19f, size.height * 0.12f),
                size = Size(size.width * 1.38f, size.height * 0.34f),
                style = Stroke(width = 1.25.dp.toPx(), cap = StrokeCap.Round)
            )
            drawArc(
                color = if (colors.dark) Color(0xFF16384D).copy(alpha = 0.30f) else Color(0xFFD8F3F6).copy(alpha = 0.72f),
                startAngle = 184f,
                sweepAngle = 165f,
                useCenter = false,
                topLeft = Offset(size.width * 0.08f, size.height * 0.03f),
                size = Size(size.width * 1.20f, size.height * 0.42f),
                style = Stroke(width = 1.dp.toPx(), cap = StrokeCap.Round)
            )
            listOf(
                Triple(Offset(size.width * 0.18f, size.height * 0.28f), 3.2f, colors.accent),
                Triple(Offset(size.width * 0.78f, size.height * 0.25f), 6f, if (colors.dark) colors.cyan else colors.teal),
                Triple(Offset(size.width * 0.92f, size.height * 0.10f), 5f, colors.accent)
            ).forEach { (center, radius, dotColor) ->
                drawCircle(dotColor.copy(alpha = if (colors.dark) 0.50f else 0.58f), radius = radius.dp.toPx(), center = center)
            }
        }

        content()
    }
}

@Composable
internal fun SpendlyTopBar(
    title: String,
    onBackClick: () -> Unit,
    backIconTint: Color = Color.Unspecified
) {
    val colors = spendlyAuthColors()
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(54.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .size(48.dp)
                .clip(CircleShape)
                .background(if (colors.dark) Color(0x3309121F) else Color(0xFFFFFFFF))
                .border(1.dp, colors.border.copy(alpha = 0.78f), CircleShape)
                .clickable(onClick = onBackClick),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_arrow_back),
                contentDescription = null,
                tint = if (backIconTint == Color.Unspecified) colors.accent else backIconTint,
                modifier = Modifier.size(24.dp)
            )
        }

        Text(
            text = title,
            color = colors.text,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
internal fun SpendlyLogoMark(
    modifier: Modifier = Modifier,
    markSize: Dp = 124.dp
) {
    val colors = spendlyAuthColors()
    Box(
        modifier = modifier.size(markSize + 44.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f, size.height / 2f)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(if (colors.dark) Color(0xFF1A3A4D).copy(alpha = 0.20f) else colors.accent.copy(alpha = 0.18f), Color.Transparent),
                    center = center,
                    radius = size.minDimension * 0.48f
                ),
                radius = size.minDimension * 0.48f,
                center = center
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(colors.cyan.copy(alpha = if (colors.dark) 0.10f else 0.12f), Color.Transparent),
                    center = Offset(size.width * 0.60f, size.height * 0.42f),
                    radius = size.minDimension * 0.34f
                ),
                radius = size.minDimension * 0.34f,
                center = Offset(size.width * 0.60f, size.height * 0.42f)
            )
        }

        Box(
            modifier = Modifier
                .size(markSize)
                .clip(RoundedCornerShape(markSize * 0.22f))
                .background(if (colors.dark) colors.surface else Color(0x8CFFFFFF))
                .border(1.dp, if (colors.dark) colors.border else colors.teal.copy(alpha = 0.58f), RoundedCornerShape(markSize * 0.22f))
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawCircle(
                    color = if (colors.dark) Color(0xFF1A3A4D).copy(alpha = 0.20f) else Color.White.copy(alpha = 0.50f),
                    radius = size.minDimension * 0.36f,
                    center = Offset(size.width * 0.25f, size.height * 0.27f)
                )
                drawCircle(
                    color = colors.accent.copy(alpha = if (colors.dark) 0.28f else 0.35f),
                    radius = size.minDimension * 0.33f,
                    center = Offset(size.width * 0.72f, size.height * 0.70f)
                )
                drawRoundRect(
                    color = Color(0xD8050B13),
                    topLeft = Offset(size.width * 0.28f, size.height * 0.36f),
                    size = Size(size.width * 0.50f, size.height * 0.30f),
                    cornerRadius = CornerRadius(size.minDimension * 0.14f, size.minDimension * 0.14f)
                )
                drawCircle(
                    color = colors.accent,
                    radius = size.minDimension * 0.07f,
                    center = Offset(size.width * 0.62f, size.height * 0.50f)
                )
            }

            Icon(
                painter = painterResource(R.drawable.ic_shopping_bag),
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.10f),
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(markSize * 0.32f)
            )
        }
    }
}

@Composable
internal fun SpendlyBrandTitle(
    fontSize: Int,
    centered: Boolean = true
) {
    val colors = spendlyAuthColors()
    Text(
        text = buildAnnotatedString {
            withStyle(SpanStyle(color = colors.text)) { append("Spend") }
            withStyle(SpanStyle(color = colors.text)) { append("ly") }
        },
        fontSize = fontSize.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 0.sp,
        textAlign = if (centered) TextAlign.Center else TextAlign.Start
    )
}

@Composable
internal fun SpendlyAuthCard(content: @Composable ColumnScope.() -> Unit) {
    val colors = spendlyAuthColors()
    val radius = if (colors.dark) 30.dp else 24.dp
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (colors.dark) Modifier.shadow(6.dp, RoundedCornerShape(radius), clip = false) else Modifier)
            .clip(RoundedCornerShape(radius))
            .background(colors.surface)
            .border(1.dp, colors.border.copy(alpha = if (colors.dark) 0.82f else 1f), RoundedCornerShape(radius))
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 22.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            content = content
        )
    }
}

@Composable
internal fun SpendlyAuthHeader(
    appName: String,
    title: String,
    subtitle: String
) {
    val colors = spendlyAuthColors()
    SpendlyBrandTitle(fontSize = 46)
    Text(
        text = subtitle.ifBlank { title },
        color = colors.muted,
        fontSize = 18.sp,
        textAlign = TextAlign.Center,
        modifier = Modifier.padding(top = 8.dp)
    )
    if (appName.isBlank()) {
        Spacer(modifier = Modifier.height(0.dp))
    }
}

@Composable
internal fun SpendlyAuthField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    @DrawableRes iconRes: Int,
    enabled: Boolean,
    keyboardType: KeyboardType = KeyboardType.Text,
    iconTint: Color = Color.Unspecified
) {
    val colors = spendlyAuthColors()
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier
            .fillMaxWidth()
            .height(58.dp),
        enabled = enabled,
        singleLine = true,
        placeholder = {
            Text(placeholder, color = colors.muted, fontSize = 18.sp)
        },
        leadingIcon = {
            Icon(
                painter = painterResource(iconRes),
                contentDescription = null,
                tint = if (iconTint == Color.Unspecified) colors.accent else iconTint,
                modifier = Modifier.size(24.dp)
            )
        },
        textStyle = TextStyle(color = colors.text, fontSize = 17.sp),
        shape = RoundedCornerShape(10.dp),
        colors = authTextFieldColors(colors),
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType)
    )
}

@Composable
internal fun SpendlyPasswordField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    visible: Boolean,
    onToggleVisible: () -> Unit,
    enabled: Boolean
) {
    val colors = spendlyAuthColors()
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier
            .fillMaxWidth()
            .height(58.dp),
        enabled = enabled,
        singleLine = true,
        placeholder = {
            Text(label, color = colors.muted, fontSize = 18.sp)
        },
        leadingIcon = {
            Icon(
                painter = painterResource(R.drawable.ic_lock_outline_24),
                contentDescription = null,
                tint = colors.accent,
                modifier = Modifier.size(24.dp)
            )
        },
        trailingIcon = {
            SpendlyVisibilityToggle(
                visible = visible,
                onToggleVisible = onToggleVisible,
                enabled = enabled,
                showDescription = "Mostrar contraseña",
                hideDescription = "Ocultar contraseña"
            )
        },
        textStyle = TextStyle(color = colors.text, fontSize = 17.sp),
        visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        shape = RoundedCornerShape(10.dp),
        colors = authTextFieldColors(colors)
    )
}

@Composable
internal fun SpendlyVisibilityToggle(
    visible: Boolean,
    onToggleVisible: () -> Unit,
    enabled: Boolean = true,
    showDescription: String,
    hideDescription: String
) {
    val colors = spendlyAuthColors()
    IconButton(onClick = onToggleVisible, enabled = enabled) {
        Icon(
            painter = painterResource(if (visible) R.drawable.ic_visibilityoff else R.drawable.ic_visibility),
            contentDescription = if (visible) hideDescription else showDescription,
            tint = colors.accent,
            modifier = Modifier.size(22.dp)
        )
    }
}

@Composable
private fun authTextFieldColors(colors: SpendlyAuthColors) = OutlinedTextFieldDefaults.colors(
    focusedTextColor = colors.text,
    unfocusedTextColor = colors.text,
    disabledTextColor = colors.text.copy(alpha = 0.55f),
    focusedContainerColor = colors.field,
    unfocusedContainerColor = colors.field,
    disabledContainerColor = colors.field.copy(alpha = 0.55f),
    cursorColor = colors.teal,
    focusedBorderColor = colors.border.copy(alpha = 0.95f),
    unfocusedBorderColor = colors.border.copy(alpha = 0.95f),
    disabledBorderColor = colors.border.copy(alpha = 0.38f),
    focusedLeadingIconColor = colors.accent,
    unfocusedLeadingIconColor = colors.accent,
    focusedTrailingIconColor = colors.accent,
    unfocusedTrailingIconColor = colors.accent
)

@Composable
internal fun SpendlyPrimaryButton(
    text: String,
    loading: Boolean = false,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    val colors = spendlyAuthColors()
    val shape = RoundedCornerShape(20.dp)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(58.dp)
            .then(if (!colors.dark && enabled && !loading) Modifier.shadow(4.dp, shape, clip = false) else Modifier)
            .clip(shape)
            .background(
                if (enabled && !loading) {
                    Brush.horizontalGradient(listOf(colors.accent, colors.accent))
                } else {
                    Brush.horizontalGradient(
                        listOf(
                            if (colors.dark) Color(0xFF31554A) else Color(0xFFD0D5DD),
                            if (colors.dark) Color(0xFF29473F) else Color(0xFFD0D5DD)
                        )
                    )
                }
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(color = if (colors.dark) Color(0xFF169B69) else Color(0xFF5FA764)),
                enabled = enabled && !loading,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        if (loading) {
            CircularProgressIndicator(
                modifier = Modifier.size(24.dp),
                strokeWidth = 2.dp,
                color = Color.White
            )
        } else {
            Text(
                text = text,
                color = Color.White,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
internal fun SpendlyOutlinedButton(
    text: String,
    onClick: () -> Unit
) {
    val colors = spendlyAuthColors()
    val shape = RoundedCornerShape(20.dp)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .clip(shape)
            .background(if (colors.dark) colors.bg else Color(0xFFFFFFFF))
            .border(1.dp, colors.accent.copy(alpha = 0.95f), shape)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(color = if (colors.dark) colors.border else Color(0xFFDFF3E1)),
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = if (colors.dark) colors.teal else colors.accent,
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
internal fun ColumnScope.SpendlySecondaryTextButton(
    text: String,
    enabled: Boolean,
    onClick: () -> Unit
) {
    val colors = spendlyAuthColors()
    TextButton(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier
            .align(Alignment.CenterHorizontally)
            .padding(top = 18.dp)
    ) {
        Text(
            text = text,
            color = colors.accent,
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
internal fun SpendlyDividerDot() {
    val colors = spendlyAuthColors()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 22.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .height(1.dp)
                .background(colors.border.copy(alpha = 0.45f))
        )
        Box(
            modifier = Modifier
                .padding(horizontal = 14.dp)
                .size(26.dp)
                .clip(CircleShape)
                .background(colors.accent.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(colors.accent.copy(alpha = 0.70f))
            )
        }
        Box(
            modifier = Modifier
                .weight(1f)
                .height(1.dp)
                .background(colors.border.copy(alpha = 0.45f))
        )
    }
}

@Composable
internal fun SpendlySimpleDot(
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    size: Dp = 8.dp
) {
    val colors = spendlyAuthColors()
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(if (color == Color.Unspecified) colors.accent else color)
    )
}

@Composable
internal fun HighlightedSentence(
    before: String,
    highlighted: String,
    after: String,
    modifier: Modifier = Modifier,
    fontSize: Int = 19,
    centered: Boolean = true
) {
    val colors = spendlyAuthColors()
    Text(
        text = buildAnnotatedString {
            withStyle(SpanStyle(color = colors.muted)) { append(before) }
            withStyle(SpanStyle(color = if (colors.dark) colors.teal else colors.accent, fontWeight = FontWeight.Bold)) { append(highlighted) }
            withStyle(SpanStyle(color = colors.muted)) { append(after) }
        },
        modifier = modifier,
        fontSize = fontSize.sp,
        lineHeight = (fontSize + 8).sp,
        textAlign = if (centered) TextAlign.Center else TextAlign.Start
    )
}
