package com.queatz.ailaai.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProgressIndicatorDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.queatz.ailaai.ui.theme.pad

class CardToolbarScope internal constructor() {
    @Composable
    fun item(
        icon: ImageVector,
        name: String,
        color: Color? = null,
        selected: Boolean = false,
        isLoading: Boolean = false,
        onClick: () -> Unit
    ) {
        TextButton(onClick, enabled = !isLoading) {
            Column(
                verticalArrangement = Arrangement.spacedBy(.5f.pad, Alignment.CenterVertically),
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .padding(.5f.pad)
                    .width(54.dp)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        strokeWidth = ProgressIndicatorDefaults.CircularStrokeWidth / 2,
                        modifier = Modifier.size(24.dp)
                    )
                } else {
                    Icon(
                        icon,
                        "",
                        tint = if (selected) MaterialTheme.colorScheme.primary else (color ?: MaterialTheme.colorScheme.onSurfaceVariant)
                    )
                }
                Text(
                    name,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                    maxLines = 2
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CardToolbar(
    modifier: Modifier = Modifier,
    items: @Composable CardToolbarScope.() -> Unit
) {
    FlowRow(
        horizontalArrangement = Arrangement.Center,
        modifier = modifier
            .fillMaxWidth()
    ) {
        CardToolbarScope().apply {
            items()
        }
    }
}
