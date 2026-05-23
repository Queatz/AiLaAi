package com.queatz.ailaai.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.pluralStringResource
import com.queatz.ailaai.R
import app.ailaai.api.activeSignals
import com.queatz.ailaai.data.api
import com.queatz.ailaai.ui.theme.pad
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.seconds

@Composable
fun SignalsButton(
    onClick: () -> Unit
) {
    var count by remember { mutableIntStateOf(0) }

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
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primaryContainer)
            .clickable { onClick() }
            .padding(horizontal = 1.5f.pad, vertical = 0.5f.pad),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = if (count == 0) stringResource(R.string.signals) else pluralStringResource(R.plurals.x_signals, count, count),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
    }
}
