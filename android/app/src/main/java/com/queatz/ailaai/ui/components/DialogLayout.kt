package com.queatz.ailaai.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.queatz.ailaai.ui.theme.pad

@Composable
fun DialogLayout(
    content: @Composable () -> Unit,
    actions: @Composable () -> Unit
) {
    Column(
        modifier = Modifier
            .padding(3.pad)
            .verticalScroll(rememberScrollState())
    ) {
        content()
        Row(
            horizontalArrangement = Arrangement.spacedBy(1.pad, Alignment.End),
            verticalAlignment = Alignment.Bottom,
            modifier = Modifier.fillMaxWidth().wrapContentHeight()
        ) {
            actions()
        }
    }
}
