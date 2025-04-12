package com.queatz.ailaai.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import com.queatz.ailaai.ui.theme.pad

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DialogLayout(
    scrollable: Boolean = true,
    padding: Dp = 3.pad,
    horizontalAlignment: Alignment.Horizontal = Alignment.Start,
    content: @Composable ColumnScope.() -> Unit,
    actions: @Composable RowScope.() -> Unit
) {
    Column(
        horizontalAlignment = horizontalAlignment,
        modifier = Modifier
            .padding(padding)
            .then(if (scrollable) Modifier.verticalScroll(rememberScrollState()) else Modifier)
    ) {
        content()
        Row(
            horizontalArrangement = Arrangement.spacedBy(1.pad, Alignment.End),
            modifier = Modifier.fillMaxWidth()
        ) {
            actions()
        }
    }
}
