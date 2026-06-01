package com.queatz.ailaai.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize
import com.queatz.ailaai.R
import app.ailaai.api.activeSignals
import com.queatz.ailaai.data.api
import com.queatz.ailaai.extensions.greetingRes
import com.queatz.ailaai.extensions.rememberAnimatedGradientBrush
import com.queatz.ailaai.ui.theme.pad
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.seconds

@Composable
fun SignalsButton(
    onClick: () -> Unit
) {
    var count by remember { mutableIntStateOf(0) }

    var size by remember { mutableStateOf(androidx.compose.ui.geometry.Size.Zero) }

    val isDark = isSystemInDarkTheme()

    val colors = if (isDark) listOf(
        MaterialTheme.colorScheme.primary,
        MaterialTheme.colorScheme.tertiary,
        MaterialTheme.colorScheme.secondary,
        MaterialTheme.colorScheme.primary
    ) else listOf(
        MaterialTheme.colorScheme.primaryContainer,
        MaterialTheme.colorScheme.tertiaryContainer,
        MaterialTheme.colorScheme.secondaryContainer,
        MaterialTheme.colorScheme.primaryContainer
    )

    val borderColors = if (isDark) listOf(
        MaterialTheme.colorScheme.secondary,
        MaterialTheme.colorScheme.primary,
        MaterialTheme.colorScheme.tertiary,
        MaterialTheme.colorScheme.secondary
    ) else listOf(
        MaterialTheme.colorScheme.secondaryContainer,
        MaterialTheme.colorScheme.primaryContainer,
        MaterialTheme.colorScheme.tertiaryContainer,
        MaterialTheme.colorScheme.secondaryContainer
    )

    val brush = rememberAnimatedGradientBrush(
        colors = colors,
        size = size,
        label = "SignalsButton"
    )

    val borderBrush = rememberAnimatedGradientBrush(
        colors = borderColors,
        size = size,
        label = "SignalsButtonBorder"
    )

    LaunchedEffect(Unit) {
        while (true) {
            api.activeSignals(onError = {}) {
                count = it.mine.size + it.others.size
            }
            delay(30.seconds)
        }
    }

    Box(
        modifier = Modifier
            .onSizeChanged { size = it.toSize() }
            .clip(CircleShape)
            .background(brush)
            .border(2.dp, borderBrush, CircleShape)
            .clickable { onClick() }
            .padding(horizontal = 1.5f.pad, vertical = 0.5f.pad),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = if (count == 0) stringResource(greetingRes) else pluralStringResource(R.plurals.x_signals, count, count),
            style = MaterialTheme.typography.labelLarge,
            color = if (isDark) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onPrimaryContainer,
            textAlign = TextAlign.Center,
            modifier = Modifier.widthIn(max = 200.dp)
        )
    }
}
