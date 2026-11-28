package com.queatz.ailaai.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.queatz.ailaai.R
import com.queatz.ailaai.extensions.rememberAnimatedGradientBrush
import com.queatz.ailaai.services.push
import com.queatz.ailaai.services.signals
import com.queatz.ailaai.ui.theme.pad
import com.queatz.push.SignalPushData
import com.queatz.push.SignalReplyPushData
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.filter
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SignalsButton(
    onClick: () -> Unit,
    onLongClick: () -> Unit = {}
) {
    val activeSignals by signals.rememberActiveSignals()
    val count = activeSignals?.let { it.mine.size + it.others.size } ?: 0

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
        label = "SignalsButton"
    )

    val borderBrush = rememberAnimatedGradientBrush(
        colors = borderColors,
        label = "SignalsButtonBorder"
    )

    LaunchedEffect(Unit) {
        while (true) {
            signals.reload()
            delay(30.seconds)
        }
    }

    LaunchedEffect(push) {
        push.events
            .filter { it is SignalPushData || it is SignalReplyPushData }
            .collect {
                signals.reload()
            }
    }

    val text = when (count) {
        0 -> "Signals"
        1 -> activeSignals?.mine?.firstOrNull()?.signal?.name ?: activeSignals?.others?.firstOrNull()?.signal?.name ?: "1 signal"
        else -> pluralStringResource(R.plurals.x_signals, count, count)
    }

    Box(
        modifier = Modifier
            .clip(CircleShape)
            .background(brush)
            .border(2.dp, borderBrush, CircleShape)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(horizontal = 1.5f.pad, vertical = 0.5f.pad),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            color = if (isDark) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onPrimaryContainer,
            textAlign = TextAlign.Center,
            modifier = Modifier.widthIn(max = 200.dp)
        )
    }
}
