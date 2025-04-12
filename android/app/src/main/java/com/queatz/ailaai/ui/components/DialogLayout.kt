package com.queatz.ailaai.ui.components

import android.R.attr.minHeight
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.unit.Dp
import com.queatz.ailaai.extensions.inDp
import com.queatz.ailaai.extensions.rememberStateOf
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
