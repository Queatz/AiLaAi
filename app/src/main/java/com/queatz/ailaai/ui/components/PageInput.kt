package com.queatz.ailaai.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.queatz.ailaai.ui.theme.PaddingDefault

@Composable
fun PageInput(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(PaddingDefault),
        modifier = Modifier
            .padding(vertical = PaddingDefault * 2)
            .widthIn(max = 480.dp)
            .fillMaxWidth()
            .then(modifier)
    ) {
        content()
    }
}
