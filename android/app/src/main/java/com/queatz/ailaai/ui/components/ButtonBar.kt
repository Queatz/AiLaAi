package com.queatz.ailaai.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp
import com.queatz.ailaai.ui.theme.pad


@OptIn(ExperimentalFoundationApi::class)
@Composable
fun <T> ButtonBar(
    items: List<T>,
    modifier: Modifier = Modifier,
    itemModifier: @Composable (T) -> Modifier = { Modifier },
    onLongClick: (T) -> Unit = {},
    onClick: (T) -> Unit,
    photo: @Composable (T) -> Unit,
    title: @Composable (T) -> String,
    subtitle: @Composable (T) -> String? = { null },
) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(2.pad),
        contentPadding = PaddingValues(horizontal = 2.pad, vertical = 1.pad),
        modifier = modifier
            .fillMaxWidth()
    ) {
        items(items) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = itemModifier(it)
                    .combinedClickable(
                        onLongClick = {
                            onLongClick(it)
                        },
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        onClick(it)
                    }
            ) {
                photo(it)
                Text(
                    text = title(it),
                    style = MaterialTheme.typography.labelSmall,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .padding(top = 1.pad)
                )
                subtitle(it)?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                        color = MaterialTheme.colorScheme.secondary.copy(alpha = .5f),
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}
