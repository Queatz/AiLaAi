package com.queatz.ailaai.ui.components

import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ProgressIndicatorDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun LoadingIcon() {
    CircularProgressIndicator(
        strokeWidth = ProgressIndicatorDefaults.CircularStrokeWidth / 2,
        modifier = Modifier.size(24.dp)
    )
}

@Composable
fun LoadingIcon(progress: Float) {
    CircularProgressIndicator(
        progress = { progress },
        strokeWidth = ProgressIndicatorDefaults.CircularStrokeWidth / 2,
        modifier = Modifier.size(24.dp)
    )
}
