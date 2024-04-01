package com.queatz.ailaai.ui.stickers

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.zIndex
import app.ailaai.api.createStickerPack
import app.ailaai.api.myStickerPacks
import com.queatz.ailaai.AppNav
import com.queatz.ailaai.R
import com.queatz.ailaai.data.api
import com.queatz.ailaai.extensions.navigate
import com.queatz.ailaai.extensions.rememberStateOf
import com.queatz.ailaai.nav
import com.queatz.ailaai.services.say
import com.queatz.ailaai.services.stickers
import com.queatz.ailaai.ui.components.AppBar
import com.queatz.ailaai.ui.components.BackButton
import com.queatz.ailaai.ui.components.EmptyText
import com.queatz.ailaai.ui.components.Loading
import com.queatz.ailaai.ui.dialogs.TextFieldDialog
import com.queatz.ailaai.ui.theme.pad
import com.queatz.db.StickerPack
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StickerPacksScreen() {
    val scope = rememberCoroutineScope()
    var showCreateStickerPackDialog by rememberStateOf(false)
    var stickerPacks by rememberStateOf(emptyList<StickerPack>())
    var isLoading by rememberStateOf(true)
    val nav = nav

    LaunchedEffect(Unit) {
        api.myStickerPacks {
            stickerPacks = it
        }
        isLoading = false
    }

    if (showCreateStickerPackDialog) {
        TextFieldDialog(
            {
                showCreateStickerPackDialog = false
            },
            title = stringResource(R.string.new_sticker_pack),
            button = stringResource(R.string.create),
            placeholder = stringResource(R.string.name),
            singleLine = true
        ) { value ->
            api.createStickerPack(StickerPack(name = value)) {
                stickers.reload()
                showCreateStickerPackDialog = false
                nav.navigate(AppNav.EditStickerPack(it.id!!))
            }
        }
    }

    Column(
        verticalArrangement = Arrangement.Top,
        modifier = Modifier.fillMaxSize()
    ) {
        AppBar(
            title = {
                Text(stringResource(R.string.sticker_pack_editor), maxLines = 1, overflow = TextOverflow.Ellipsis)
            },
            navigationIcon = {
                BackButton()
            },
            modifier = Modifier.zIndex(1f)
        )

        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            if (isLoading) {
                Loading(
                    modifier = Modifier.padding(top = 1.pad)
                )
            } else if (stickerPacks.isEmpty()) {
                EmptyText(stringResource(R.string.create_and_share_sticker_packs))
            } else {
                StickerPacks(
                    stickerPacks,
                    edit = true,
                    onEdit = {
                        nav.navigate(AppNav.EditStickerPack(it.id!!))
                    },
                    onStickerLongClick = {
                        scope.launch {
                            say.say(it.message)
                        }
                    }
                ) {
                    nav.navigate(AppNav.EditStickerPack(it.pack!!))
                }
            }
            FloatingActionButton(
                onClick = {
                    showCreateStickerPackDialog = true
                },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(bottom = 2.pad, end = 2.pad)
            ) {
                Icon(Icons.Outlined.Add, null)
            }
        }
    }
}
