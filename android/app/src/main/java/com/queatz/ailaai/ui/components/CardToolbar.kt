package com.queatz.ailaai.ui.components

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
        selected: Boolean = false,
        onClick: () -> Unit
    ) {
        TextButton(onClick) {
            Column(
                verticalArrangement = Arrangement.spacedBy(.5f.pad, Alignment.CenterVertically),
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .padding(.5f.pad)
                    .width(54.dp)
            ) {
                Icon(
                    icon,
                    "",
                    tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
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
