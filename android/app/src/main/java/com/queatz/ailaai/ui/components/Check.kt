package com.queatz.ailaai.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import com.queatz.ailaai.ui.theme.pad

@Composable
fun Check(checked: Boolean, onCheckChanged: (Boolean) -> Unit, label: @Composable () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(1.pad),
        modifier = Modifier
            .clip(MaterialTheme.shapes.large)
            .clickable {
                onCheckChanged(!checked)
            }) {
        Checkbox(checked, onCheckChanged)
        label()
    }
}
