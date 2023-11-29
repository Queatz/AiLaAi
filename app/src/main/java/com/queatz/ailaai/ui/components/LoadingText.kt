package com.queatz.ailaai.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.queatz.ailaai.ui.theme.pad

@Composable
fun LoadingText(
    done: Boolean,
    text: String,
    content: @Composable () -> Unit
) {
    if (done) {
        content()
    } else {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp + 2.pad)
        ) {
            Text(
                text,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = .5f),
                modifier = Modifier
                    .align(Alignment.Center)
            )
        }
    }
}
