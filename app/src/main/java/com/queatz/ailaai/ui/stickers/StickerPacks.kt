package com.queatz.ailaai.ui.stickers

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.layout.boundsInParent
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.queatz.ailaai.R
import com.queatz.ailaai.extensions.fadingEdge
import com.queatz.ailaai.extensions.rememberStateOf
import com.queatz.ailaai.ui.components.Dropdown
import com.queatz.ailaai.ui.theme.pad
import com.queatz.db.Sticker
import com.queatz.db.StickerPack

@Composable
fun StickerPacks(
    stickerPacks: List<StickerPack>,
    modifier: Modifier = Modifier,
    edit: Boolean = false,
    onEdit: ((stickerPack: StickerPack) -> Unit)? = null,
    onStickerPack: ((stickerPack: StickerPack) -> Unit)? = null,
    onStickerLongClick: ((sticker: Sticker) -> Unit)? = null,
    onSticker: (sticker: Sticker) -> Unit
) {
    if (stickerPacks.isEmpty()) {
        Box(
            modifier = modifier
        ) {
            Text(
                text = stringResource(R.string.no_sticker_packs),
                modifier = Modifier.align(Alignment.Center).padding(2.pad)
            )
        }
    } else {
        val state = rememberLazyListState()
        var viewport by rememberStateOf(Size(0f, 0f))
        LazyColumn(
            state = state,
            modifier = modifier
                .onPlaced { viewport = it.boundsInParent().size }
                .fadingEdge(viewport, state, 12f)
        ) {
            items(stickerPacks, key = { it.id!! }) { stickerPack ->
                Column {
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        var showStickerPackMenu by rememberStateOf(false)
                        Text(
                            stickerPack.name ?: "",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(1.pad)
                        )
                        if (edit) {
                            IconButton(
                                {
                                    onEdit?.invoke(stickerPack)
                                }
                            ) {
                                Icon(
                                    Icons.Outlined.Edit,
                                    null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        } else {
                            IconButton(
                                {
                                    showStickerPackMenu = true
                                }
                            ) {
                                Icon(
                                    Icons.Outlined.MoreVert,
                                    null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(16.dp)
                                )
                                Dropdown(showStickerPackMenu, { showStickerPackMenu = false }) {
                                    DropdownMenuItem(
                                        text = {
                                            Text(stringResource(R.string.open_sticker_pack))
                                        },
                                        onClick = {
                                            onStickerPack?.invoke(stickerPack)
                                            showStickerPackMenu = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(1.pad),
                        contentPadding = PaddingValues(1.pad)
                    ) {
                        items(stickerPack.stickers ?: emptyList(), key = { it.id!! }) { sticker ->
                            StickerItem(
                                sticker,
                                onLongClick = {
                                    onStickerLongClick?.invoke(sticker)
                                }
                            ) {
                                onSticker(sticker)
                            }
                        }
                    }
                }
            }
        }
    }
}
