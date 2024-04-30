package com.queatz.ailaai.ui.components

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush.Companion.linearGradient
import androidx.compose.ui.text.style.TextAlign
import com.queatz.ailaai.ui.theme.pad

@Composable
fun DisplayText(text: String) {
    Text(
        text,
        textAlign = TextAlign.Center,
        style = MaterialTheme.typography.headlineMedium.copy(
            brush = linearGradient(
                listOf(
                    MaterialTheme.colorScheme.tertiary,
                    MaterialTheme.colorScheme.primary
                )
            )
        ),
        modifier = Modifier
            .padding(2.pad)
    )
}
