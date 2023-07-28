package com.queatz.ailaai.ui.stickers

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.zIndex
import androidx.navigation.NavController
import com.queatz.ailaai.R
import com.queatz.ailaai.api.createStickerPack
import com.queatz.ailaai.api.myStickerPacks
import com.queatz.ailaai.data.Person
import com.queatz.ailaai.data.StickerPack
import com.queatz.ailaai.data.api
import com.queatz.ailaai.extensions.rememberStateOf
import com.queatz.ailaai.services.say
import com.queatz.ailaai.services.stickers
import com.queatz.ailaai.ui.components.BackButton
import com.queatz.ailaai.ui.components.EmptyText
import com.queatz.ailaai.ui.components.Loading
import com.queatz.ailaai.ui.dialogs.TextFieldDialog
import com.queatz.ailaai.ui.theme.PaddingDefault
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StickerPacksScreen(navController: NavController, me: () -> Person?) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var showCreateStickerPackDialog by rememberStateOf(false)
    var stickerPacks by rememberStateOf(emptyList<StickerPack>())
    var isLoading by rememberStateOf(true)

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
                navController.navigate("sticker-pack/${it.id}/edit")
            }
        }
    }

    Column(
        verticalArrangement = Arrangement.Top,
        modifier = Modifier.fillMaxSize()
    ) {
        TopAppBar(
            title = {
                Text(stringResource(R.string.sticker_pack_editor), maxLines = 1, overflow = TextOverflow.Ellipsis)
            },
            navigationIcon = {
                BackButton(navController)
            },
            modifier = Modifier.zIndex(1f)
        )

        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            if (isLoading) {
                Loading(
                    modifier = Modifier.padding(top = PaddingDefault)
                )
            } else if (stickerPacks.isEmpty()) {
                EmptyText(stringResource(R.string.create_and_share_sticker_packs))
            } else {
                StickerPacks(
                    stickerPacks,
                    edit = true,
                    onEdit = {
                        navController.navigate("sticker-pack/${it.id}/edit")
                    },
                    onStickerLongClick = {
                        scope.launch {
                            say.say(it.message)
                        }
                    }
                ) {
                    navController.navigate("sticker-pack/${it.pack}/edit")
                }
            }
            FloatingActionButton(
                onClick = {
                    showCreateStickerPackDialog = true
                },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(bottom = PaddingDefault * 2, end = PaddingDefault * 2)
            ) {
                Icon(Icons.Outlined.Add, null)
            }
        }
    }
}
