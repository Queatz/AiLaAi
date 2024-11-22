package com.queatz.ailaai.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import com.queatz.ailaai.ui.theme.pad

@Composable
fun Check(
    checked: Boolean,
    onCheckChange: (Boolean) -> Unit,
    padding: PaddingValues = PaddingValues(end = 2.pad),
    label: @Composable () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(1.pad),
        modifier = Modifier
            .clip(MaterialTheme.shapes.large)
            .clickable {
                onCheckChange(!checked)
            }
            .padding(padding)
    ) {
        Checkbox(checked, onCheckChange)
        label()
    }
}
