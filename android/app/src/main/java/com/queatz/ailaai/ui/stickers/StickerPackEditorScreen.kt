package com.queatz.ailaai.ui.stickers

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.zIndex
import app.ailaai.api.*
import com.queatz.ailaai.R
import com.queatz.ailaai.api.createStickerFromUri
import com.queatz.ailaai.data.api
import com.queatz.ailaai.extensions.*
import com.queatz.ailaai.nav
import com.queatz.ailaai.services.say
import com.queatz.ailaai.services.stickers
import com.queatz.ailaai.ui.components.AppBar
import com.queatz.ailaai.ui.components.BackButton
import com.queatz.ailaai.ui.components.Dropdown
import com.queatz.ailaai.ui.components.Loading
import com.queatz.ailaai.ui.dialogs.Alert
import com.queatz.ailaai.ui.dialogs.Menu
import com.queatz.ailaai.ui.dialogs.TextFieldDialog
import com.queatz.ailaai.ui.dialogs.menuItem
import com.queatz.ailaai.ui.theme.pad
import com.queatz.db.Sticker
import com.queatz.db.StickerPack
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StickerPackEditorScreen(stickerPackId: String) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var stickerPack by rememberStateOf<StickerPack?>(null)
    var showDeleteDialog by rememberStateOf(false)
    var showRenameDialog by rememberStateOf(false)
    var showDescriptionDialog by rememberStateOf(false)
    var showStickerMenu by rememberStateOf<Sticker?>(null)
    var showRenameStickerDialog by rememberStateOf<Sticker?>(null)
    var showStickerMessageDialog by rememberStateOf<Sticker?>(null)
    var showDeleteStickerDialog by rememberStateOf<Sticker?>(null)
    val recomposeScope = currentRecomposeScope
    val nav = nav

    suspend fun reload() {
        api.stickerPack(stickerPackId) {
            stickerPack = it
            stickers.reload()
            recomposeScope.invalidate() // because stickers.has() may have changed
        }
    }

    showStickerMenu?.let { sticker ->
        Menu(
            {
                showStickerMenu = null
            }
        ) {
            menuItem(if (sticker.name.isNullOrBlank()) stringResource(R.string.add_name) else stringResource(R.string.rename)) {
                showStickerMenu = null
                showRenameStickerDialog = sticker
            }
            menuItem(stringResource(R.string.message_noun)) {
                showStickerMenu = null
                showStickerMessageDialog = sticker
            }
            menuItem(stringResource(R.string.delete)) {
                showStickerMenu = null
                showDeleteStickerDialog = sticker
            }
        }
    }

    showRenameStickerDialog?.let { sticker ->
        TextFieldDialog(
            {
                showRenameStickerDialog = null
            },
            title = stringResource(R.string.sticker_name),
            button = stringResource(R.string.rename),
            placeholder = stringResource(R.string.name),
            initialValue = sticker.name ?: "",
            singleLine = true
        ) { value ->
            api.updateSticker(sticker.id!!, Sticker(name = value)) {
                reload()
                showRenameStickerDialog = null
            }
        }
    }

    showStickerMessageDialog?.let { sticker ->
        TextFieldDialog(
            {
                showStickerMessageDialog = null
            },
            title = stringResource(R.string.message_noun),
            button = stringResource(R.string.update),
            initialValue = sticker.message ?: "",
            singleLine = true
        ) { value ->
            api.updateSticker(sticker.id!!, Sticker(message = value)) {
                reload()
                showStickerMessageDialog = null
            }
        }
    }

    showDeleteStickerDialog?.let { sticker ->
        Alert(
            { showDeleteStickerDialog = null },
            title = stringResource(R.string.delete_sticker),
            text = stringResource(R.string.you_cannot_undo_this_sticker),
            dismissButton = stringResource(R.string.cancel),
            confirmButton = stringResource(R.string.delete),
            confirmColor = MaterialTheme.colorScheme.error
        ) {
            scope.launch {
                api.deleteSticker(sticker.id!!) {
                    reload()
                    showDeleteStickerDialog = null
                }
            }
        }
    }

    LaunchedEffect(stickerPackId) {
        api.stickerPack(stickerPackId) {
            stickerPack = it
        }
    }

    if (showRenameDialog) {
        TextFieldDialog(
            {
                showRenameDialog = false
            },
            title = stringResource(R.string.rename_sticker_pack),
            button = stringResource(R.string.rename),
            placeholder = stringResource(R.string.name),
            initialValue = stickerPack?.name ?: "",
            singleLine = true
        ) { value ->
            api.updateStickerPack(stickerPackId, StickerPack(name = value)) {

                stickerPack = it
                showRenameDialog = false
            }
        }
    }

    if (showDescriptionDialog) {
        TextFieldDialog(
            {
                showDescriptionDialog = false
            },
            title = stringResource(R.string.introduction),
            button = stringResource(R.string.update),
            initialValue = stickerPack?.description ?: ""
        ) { value ->
            api.updateStickerPack(stickerPackId, StickerPack(description = value)) {
                stickerPack = it
                showDescriptionDialog = false
            }
        }
    }

    if (showDeleteDialog) {
        Alert(
            { showDeleteDialog = false },
            title = stringResource(R.string.delete_sticker_pack),
            text = stringResource(R.string.you_cannot_undo_this_sticker_pack),
            dismissButton = stringResource(R.string.cancel),
            confirmButton = stringResource(R.string.delete),
            confirmColor = MaterialTheme.colorScheme.error
        ) {
            scope.launch {
                api.deleteStickerPack(stickerPackId) {
                    stickers.reload()
                    showDeleteDialog = false
                    nav.popBackStack()
                }
            }
        }
    }

    val photoLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetMultipleContents()) {
        if (it.isEmpty()) return@rememberLauncherForActivityResult

        scope.launch {
            it.forEach { file ->
                api.createStickerFromUri(
                    stickerPackId,
                    file,
                    context,
                    onError = {
                        if (it is FileSizeException) {
                            context.toast(R.string.max_sticker_size)
                        } else {
                            context.showDidntWork()
                        }
                    }
                ) {}
            }
            reload()
        }
    }

    Column(
        verticalArrangement = Arrangement.Top,
        modifier = Modifier.fillMaxSize()
    ) {
        AppBar(
            title = {
                Text(
                    stickerPack?.name?.notBlank ?: stringResource(R.string.sticker_pack),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            },
            navigationIcon = {
                BackButton()
            },
            actions = {
                var showMenu by rememberStateOf(false)
                stickerPack?.let { stickerPack ->
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(1.pad)
                    ) {
                        UseStickerPackButton(stickerPack) {
                            scope.launch {
                                reload()
                            }
                        }
                        IconButton(
                            {
                                showMenu = true
                            }
                        ) {
                            Icon(Icons.Outlined.MoreVert, null)
                            Dropdown(showMenu, { showMenu = false }) {
                                DropdownMenuItem({
                                    Text(stringResource(R.string.rename))
                                }, {
                                    showMenu = false
                                    showRenameDialog = true
                                })
                                DropdownMenuItem({
                                    Text(stringResource(R.string.introduction))
                                }, {
                                    showMenu = false
                                    showDescriptionDialog = true
                                })
                                DropdownMenuItem({
                                    Text(stringResource(R.string.delete))
                                }, {
                                    showMenu = false
                                    showDeleteDialog = true
                                })
                            }
                        }
                    }
                }
            },
            modifier = Modifier.zIndex(1f)
        )

        Column(
            modifier = Modifier
                .weight(1f)
        ) {
            val stickerPack = stickerPack
            if (stickerPack == null) {
                Loading(
                    modifier = Modifier.padding(top = 1.pad)
                )
            } else {
                StickerPackContents(
                    stickerPack,
                    showAddStickerButton = true,
                    onAddStickerClick = {
                        photoLauncher.launch("image/*")
                    },
                    onStickerLongClick = {
                        scope.launch {
                            say.say(it.message)
                        }
                    },
                    modifier = Modifier
                        .fillMaxSize()
                ) {
                    showStickerMenu = it
                }
            }
        }
    }
}
