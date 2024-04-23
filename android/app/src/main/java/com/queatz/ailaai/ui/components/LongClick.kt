package com.queatz.ailaai.ui.components

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalViewConfiguration
import com.queatz.ailaai.extensions.rememberStateOf
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest

@Composable
fun rememberLongClickInteractionSource(
    onClick: () -> Unit,
    onLongClick: () -> Unit
): MutableInteractionSource {
    val viewConfiguration = LocalViewConfiguration.current
    val interactionSource = remember { MutableInteractionSource() }
    var isLongClick by rememberStateOf(false)

    LaunchedEffect(interactionSource) {
        interactionSource.interactions.collectLatest { interaction ->
            when (interaction) {
                is PressInteraction.Press -> {
                    isLongClick = false
                    delay(viewConfiguration.longPressTimeoutMillis)
                    isLongClick = true
                    onLongClick()
                }
                is PressInteraction.Release -> {
                    if (!isLongClick) {
                        onClick()
                    }
                }
            }
        }
    }

    return interactionSource
}
