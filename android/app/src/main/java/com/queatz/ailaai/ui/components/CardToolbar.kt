package com.queatz.ailaai.ui.components

import android.annotation.SuppressLint
import android.widget.ProgressBar
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
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
@SuppressLint("MissingPermission", "UnrememberedMutableState")
@Composable
fun CardToolbar(
    modifier: Modifier = Modifier,
    items: @Composable CardToolbarScope.() -> Unit
) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(1.pad, Alignment.CenterHorizontally),
        verticalArrangement = Arrangement.spacedBy(1.pad),
        modifier = modifier
            .fillMaxWidth()
    ) {
        CardToolbarScope().apply {
            items()
        }
    }
}
