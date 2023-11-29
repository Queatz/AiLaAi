package com.queatz.ailaai.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.queatz.ailaai.ui.theme.pad

@Composable
fun PageInput(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(1.pad),
        modifier = Modifier
            .padding(vertical = 2.pad)
            .widthIn(max = 480.dp)
            .fillMaxWidth()
            .then(modifier)
    ) {
        content()
    }
}
