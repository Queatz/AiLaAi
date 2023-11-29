package com.queatz.ailaai.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.queatz.ailaai.extensions.rememberStateOf
import com.queatz.ailaai.ui.theme.pad
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.seconds

@Composable
fun Loading(
    modifier: Modifier = Modifier
) {
    var show by rememberStateOf(false)

    LaunchedEffect(Unit) {
        delay(.5.seconds)
        show = true
    }

    if (show) {
        LinearProgressIndicator(
            color = MaterialTheme.colorScheme.tertiary,
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 1.pad)
        )
    }
}
