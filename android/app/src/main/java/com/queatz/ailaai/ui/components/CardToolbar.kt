package com.queatz.ailaai.ui.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.boundsInParent
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.queatz.ailaai.extensions.horizontalFadingEdge
import com.queatz.ailaai.ui.theme.pad

class ToolbarScope internal constructor() {
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
                    LoadingIcon()
                } else {
                    Icon(
                        imageVector = icon,
                        contentDescription = "",
                        tint = if (selected) MaterialTheme.colorScheme.primary else (color ?: MaterialTheme.colorScheme.onSurfaceVariant)
                    )
                }
                Text(
                    text = name,
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

@Composable
fun Toolbar(
    modifier: Modifier = Modifier,
    singleLine: Boolean = false,
    items: @Composable ToolbarScope.() -> Unit
) {
    if (singleLine) {
        var viewport by remember { mutableStateOf(Size(0f, 0f)) }
        val scrollState = rememberScrollState()
        Row(
            modifier = modifier
                .fillMaxWidth()
                .horizontalScroll(scrollState)
                .onPlaced { viewport = it.boundsInParent().size }
                .horizontalFadingEdge(viewport, scrollState)
        ) {
            ToolbarScope().apply {
                items()
            }
        }
    } else {
        val configuration = LocalConfiguration.current
        FlowRow(
            horizontalArrangement = Arrangement.Center,
            verticalArrangement = Arrangement.spacedBy(1.pad, Alignment.CenterVertically),
            modifier = modifier
                .widthIn(max = configuration.screenWidthDp.dp)
                .fillMaxWidth()
        ) {
            ToolbarScope().apply {
                items()
            }
        }
    }
}
