package com.queatz.ailaai.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.queatz.ailaai.ui.theme.PaddingDefault

@Composable
fun Loading(
    modifier: Modifier = Modifier
) {
    LinearProgressIndicator(
        color = MaterialTheme.colorScheme.tertiary,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = PaddingDefault)
    )
}
