package com.queatz.ailaai.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import com.queatz.ailaai.ui.theme.pad

@Composable
fun IconAndCount(
    icon: @Composable BoxScope.() -> Unit,
    count: Int,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    if (count > 0) {
        Box(modifier = modifier) {
            IconButton(onClick = onClick) {
                icon()
            }
            Text(
                count.coerceAtMost(99).toString(),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .offset(-.25f.pad, .25f.pad)
                    .align(Alignment.TopEnd)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(1.pad, .25f.pad)
            )
        }
    }
}
