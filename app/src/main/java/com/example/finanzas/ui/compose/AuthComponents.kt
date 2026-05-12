package com.example.finanzas.ui.compose

import androidx.annotation.DrawableRes
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.onFocusEvent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.finanzas.R
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

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
            bg = Color(0xFF020812),
            bgDeep = Color(0xFF00040A),
            surface = Color(0xB30A1320),
            field = Color(0x73060D16),
            border = Color(0xFF26384B),
            text = Color(0xFFF4F7F8),
            muted = Color(0xFFACB8C7),
            accent = Color(0xFF4C9A67),
            teal = Color(0xFF4C9A67),
            cyan = Color(0xFF36C8D6)
        )
    } else {
        SpendlyAuthColors(
            dark = false,
            bg = Color(0xFFFDFEFE),
            bgDeep = Color(0xFFF5FAF7),
            surface = Color(0xFAFFFFFF),
            field = Color.White,
            border = Color(0xFFE4EEE9),
            text = Color(0xFF071827),
            muted = Color(0xFF5F6B80),
            accent = Color(0xFF2F6B48),
            teal = Color(0xFF2F6B48),
            cyan = Color(0xFF32C5D2)
        )
    }
}

@Composable
internal fun SpendlyAuthScreenContainer(
    scrollEnabled: Boolean = true,
    scrollWhenImeVisible: Boolean = false,
    horizontalPadding: Dp = 24.dp,
    verticalPadding: Dp = 16.dp,
    content: @Composable ColumnScope.() -> Unit
) {
    val scrollState = rememberScrollState()
    val scrollModifier = if (scrollEnabled || scrollWhenImeVisible) {
        Modifier.verticalScroll(scrollState)
    } else {
        Modifier
    }
    SpendlyAuthBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .then(scrollModifier)
                .systemBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = horizontalPadding, vertical = verticalPadding),
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
                        listOf(Color(0xFF071B23), colors.bg, colors.bgDeep)
                    } else {
                        listOf(Color.White, colors.bg, colors.bgDeep)
                    },
                    center = Offset(0.50f, 0.30f),
                    radius = 980f
                )
            )
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(colors.teal.copy(alpha = if (colors.dark) 0.16f else 0.10f), Color.Transparent),
                    center = Offset(size.width * 0.50f, size.height * 0.24f),
                    radius = size.width * 0.70f
                ),
                radius = size.width * 0.70f,
                center = Offset(size.width * 0.50f, size.height * 0.24f)
            )
            drawArc(
                color = colors.accent.copy(alpha = if (colors.dark) 0.46f else 0.38f),
                startAngle = 157f,
                sweepAngle = 228f,
                useCenter = false,
                topLeft = Offset(-size.width * 0.19f, size.height * 0.12f),
                size = Size(size.width * 1.38f, size.height * 0.34f),
                style = Stroke(width = 1.25.dp.toPx(), cap = StrokeCap.Round)
            )
            drawArc(
                color = colors.cyan.copy(alpha = if (colors.dark) 0.10f else 0.20f),
                startAngle = 184f,
                sweepAngle = 165f,
                useCenter = false,
                topLeft = Offset(size.width * 0.08f, size.height * 0.03f),
                size = Size(size.width * 1.20f, size.height * 0.42f),
                style = Stroke(width = 1.dp.toPx(), cap = StrokeCap.Round)
            )
            listOf(
                Offset(size.width * 0.18f, size.height * 0.28f) to 3.2f,
                Offset(size.width * 0.78f, size.height * 0.25f) to 6f,
                Offset(size.width * 0.92f, size.height * 0.10f) to 5f
            ).forEach { (center, radius) ->
                drawCircle(colors.accent.copy(alpha = if (colors.dark) 0.85f else 0.95f), radius = radius.dp.toPx(), center = center)
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
    Box(
        modifier = modifier.size(markSize + 44.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(markSize)
                .clip(RoundedCornerShape(markSize * 0.22f)),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(R.drawable.ic_launcher_spendly_source),
                contentDescription = null,
                modifier = Modifier.size(markSize * 2.2f)
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
            withStyle(SpanStyle(color = colors.accent)) { append("Spend") }
            withStyle(SpanStyle(color = colors.text)) { append("ly") }
        },
        fontSize = fontSize.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 0.sp,
        textAlign = if (centered) TextAlign.Center else TextAlign.Start
    )
}

@Composable
internal fun SpendlyAuthCard(
    horizontalPadding: Dp = 20.dp,
    verticalPadding: Dp = 22.dp,
    cornerRadius: Dp = 30.dp,
    content: @Composable ColumnScope.() -> Unit
) {
    val colors = spendlyAuthColors()
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(cornerRadius))
            .background(colors.surface)
            .border(1.dp, colors.border.copy(alpha = 0.82f), RoundedCornerShape(cornerRadius))
    ) {
        Column(
            modifier = Modifier.padding(horizontal = horizontalPadding, vertical = verticalPadding),
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
    iconTint: Color = Color.Unspecified,
    imeAction: ImeAction = ImeAction.Next,
    onImeAction: (() -> Unit)? = null
) {
    val colors = spendlyAuthColors()
    val focusManager = LocalFocusManager.current
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier
            .fillMaxWidth()
            .spendlyBringFocusedFieldIntoView()
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
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType, imeAction = imeAction),
        keyboardActions = KeyboardActions(
            onNext = { onImeAction?.invoke() ?: focusManager.moveFocus(FocusDirection.Next) },
            onDone = { onImeAction?.invoke() ?: focusManager.clearFocus() }
        )
    )
}

@Composable
internal fun SpendlyPasswordField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    visible: Boolean,
    onToggleVisible: () -> Unit,
    enabled: Boolean,
    imeAction: ImeAction = ImeAction.Next,
    onImeAction: (() -> Unit)? = null
) {
    val colors = spendlyAuthColors()
    val focusManager = LocalFocusManager.current
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier
            .fillMaxWidth()
            .spendlyBringFocusedFieldIntoView()
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
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = imeAction),
        keyboardActions = KeyboardActions(
            onNext = { onImeAction?.invoke() ?: focusManager.moveFocus(FocusDirection.Next) },
            onDone = { onImeAction?.invoke() ?: focusManager.clearFocus() }
        ),
        shape = RoundedCornerShape(10.dp),
        colors = authTextFieldColors(colors)
    )
}

@Composable
internal fun SpendlyPinField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    visible: Boolean,
    onToggleVisible: () -> Unit,
    enabled: Boolean,
    error: String?,
    imeAction: ImeAction = ImeAction.Next,
    onImeAction: (() -> Unit)? = null
) {
    val colors = spendlyAuthColors()
    val focusManager = LocalFocusManager.current
    OutlinedTextField(
        value = value,
        onValueChange = { onValueChange(it.filter(Char::isDigit).take(4)) },
        modifier = Modifier
            .fillMaxWidth()
            .spendlyBringFocusedFieldIntoView()
            .height(if (error == null) 58.dp else 80.dp),
        enabled = enabled,
        singleLine = true,
        isError = error != null,
        supportingText = error?.let { { Text(it) } },
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
                showDescription = "Mostrar PIN",
                hideDescription = "Ocultar PIN"
            )
        },
        textStyle = TextStyle(color = colors.text, fontSize = 17.sp),
        visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword, imeAction = imeAction),
        keyboardActions = KeyboardActions(
            onNext = { onImeAction?.invoke() ?: focusManager.moveFocus(FocusDirection.Next) },
            onDone = { onImeAction?.invoke() ?: focusManager.clearFocus() }
        ),
        shape = RoundedCornerShape(10.dp),
        colors = authTextFieldColors(colors)
    )
}

@Composable
@OptIn(ExperimentalFoundationApi::class)
internal fun Modifier.spendlyBringFocusedFieldIntoView(): Modifier {
    val bringIntoViewRequester = remember { BringIntoViewRequester() }
    val coroutineScope = rememberCoroutineScope()
    return bringIntoViewRequester(bringIntoViewRequester)
        .onFocusEvent { focusState ->
            if (focusState.isFocused) {
                coroutineScope.launch {
                    delay(250)
                    bringIntoViewRequester.bringIntoView()
                }
            }
        }
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
    val buttonColor = if (colors.dark) Color(0xFF3F7A53) else Color(0xFF2F6B48)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(58.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(
                if (enabled && !loading) {
                    Brush.horizontalGradient(listOf(buttonColor, buttonColor))
                } else {
                    Brush.horizontalGradient(listOf(Color(0xFF31554A), Color(0xFF29473F)))
                }
            )
            .clickable(enabled = enabled && !loading, onClick = onClick),
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
internal fun SpendlyGoogleButton(
    text: String,
    enabled: Boolean = true,
    loading: Boolean = false,
    onClick: () -> Unit
) {
    val borderColor = Color(0xFFDADCE0)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .clip(RoundedCornerShape(28.dp))
            .background(Color.White)
            .border(1.dp, borderColor, RoundedCornerShape(28.dp))
            .clickable(enabled = enabled && !loading, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (loading) {
            CircularProgressIndicator(
                modifier = Modifier.size(22.dp),
                strokeWidth = 2.dp,
                color = Color(0xFF3C4043)
            )
        } else {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Image(
                    painter = painterResource(R.drawable.ic_google_g),
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.size(12.dp))
                Text(
                    text = text,
                    color = Color(0xFF3C4043),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
internal fun SpendlyOutlinedButton(
    text: String,
    onClick: () -> Unit
) {
    val colors = spendlyAuthColors()
    val backgroundColor = if (colors.dark) Color(0xFF06131B) else Color(0xFFFFFFFF)
    val contentColor = if (colors.dark) Color(0xFF4C9A67) else Color(0xFF2F6B48)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(backgroundColor)
            .border(1.dp, contentColor.copy(alpha = 0.95f), RoundedCornerShape(20.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = contentColor,
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
internal fun SpendlyDividerOr() {
    val colors = spendlyAuthColors()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .height(1.dp)
                .background(colors.border.copy(alpha = 0.45f))
        )
        Text(
            text = "o",
            color = colors.muted,
            fontSize = 18.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 22.dp)
        )
        Box(
            modifier = Modifier
                .weight(1f)
                .height(1.dp)
                .background(colors.border.copy(alpha = 0.45f))
        )
    }
}

@Composable
internal fun SpendlyRequirementCard(
    title: String,
    requirements: List<String>,
    iconRes: Int = R.drawable.ic_lock_outline_24,
    compact: Boolean = false
) {
    val colors = spendlyAuthColors()
    val horizontalPadding = if (compact) 14.dp else 18.dp
    val verticalPadding = if (compact) 12.dp else 18.dp
    val iconContainerSize = if (compact) 46.dp else 62.dp
    val iconSize = if (compact) 26.dp else 34.dp
    val titleSize = if (compact) 16.sp else 20.sp
    val titleLineHeight = if (compact) 20.sp else 24.sp
    val itemTopPadding = if (compact) 5.dp else 8.dp
    val itemIconSize = if (compact) 16.dp else 19.dp
    val itemTextSize = if (compact) 13.sp else 15.sp
    val itemLineHeight = if (compact) 17.sp else 20.sp
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(if (colors.dark) Color(0x331B3428) else Color(0xFFF4F8F5))
            .padding(horizontal = horizontalPadding, vertical = verticalPadding),
        horizontalArrangement = Arrangement.spacedBy(if (compact) 12.dp else 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(iconContainerSize)
                .clip(CircleShape)
                .background(colors.accent.copy(alpha = if (colors.dark) 0.20f else 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(iconRes),
                contentDescription = null,
                tint = colors.accent,
                modifier = Modifier.size(iconSize)
            )
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = colors.accent,
                fontSize = titleSize,
                lineHeight = titleLineHeight,
                fontWeight = FontWeight.Bold
            )
            requirements.forEach { item ->
                Row(
                    modifier = Modifier.padding(top = itemTopPadding),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(if (compact) 8.dp else 10.dp)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_check_circle),
                        contentDescription = null,
                        tint = colors.accent,
                        modifier = Modifier.size(itemIconSize)
                    )
                    Text(
                        text = item,
                        color = colors.muted,
                        fontSize = itemTextSize,
                        lineHeight = itemLineHeight
                    )
                }
            }
        }
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
            withStyle(SpanStyle(color = colors.accent, fontWeight = FontWeight.Bold)) { append(highlighted) }
            withStyle(SpanStyle(color = colors.muted)) { append(after) }
        },
        modifier = modifier,
        fontSize = fontSize.sp,
        lineHeight = (fontSize + 8).sp,
        textAlign = if (centered) TextAlign.Center else TextAlign.Start
    )
}
