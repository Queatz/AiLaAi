package com.queatz.ailaai.ui.stickers

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.queatz.ailaai.R
import com.queatz.ailaai.extensions.notBlank
import com.queatz.ailaai.ui.theme.pad
import com.queatz.db.Sticker
import com.queatz.db.StickerPack

@Composable
fun StickerPackContents(
    stickerPack: StickerPack,
    modifier: Modifier = Modifier,
    showAddStickerButton: Boolean = false,
    onAddStickerClick: (() -> Unit)? = null,
    onStickerLongClick: ((Sticker) -> Unit)? = null,
    onStickerClick: (Sticker) -> Unit,
) {
    LazyVerticalGrid(
        verticalArrangement = Arrangement.spacedBy(1.pad),
        horizontalArrangement = Arrangement.spacedBy(1.pad),
        contentPadding = PaddingValues(1.pad),
        columns = GridCells.Adaptive(80.dp),
        modifier = modifier
    ) {
        stickerPack.description?.notBlank?.let { description ->
            item(span = { GridItemSpan(maxLineSpan) }) {
                Text(
                    description,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .padding(2.pad)
                )
            }
        }

        items(stickerPack.stickers ?: emptyList(), key = { it.id!! }) { sticker ->
            StickerItem(
                sticker,
                showName = true,
                onLongClick = {
                    onStickerLongClick?.invoke(sticker)
                }
            ) {
                onStickerClick(sticker)
            }
        }

        if (showAddStickerButton) {
            item {
                Column(
                    verticalArrangement = Arrangement.spacedBy(1.pad, Alignment.CenterVertically),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(MaterialTheme.shapes.large)
                        .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f), MaterialTheme.shapes.large)
                        .clickable {
                            onAddStickerClick?.invoke()
                        }
                        .padding(2.pad)
                ) {
                    Icon(Icons.Outlined.Add, null, tint = MaterialTheme.colorScheme.primary)
                    Text(
                        stringResource(R.string.add_sticker),
                        color = MaterialTheme.colorScheme.primary,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}
