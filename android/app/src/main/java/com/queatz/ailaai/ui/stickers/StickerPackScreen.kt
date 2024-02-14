package com.queatz.ailaai.ui.stickers

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.zIndex
import app.ailaai.api.stickerPack
import com.queatz.ailaai.R
import com.queatz.ailaai.data.api
import com.queatz.ailaai.extensions.rememberStateOf
import com.queatz.ailaai.nav
import com.queatz.ailaai.services.say
import com.queatz.ailaai.services.stickers
import com.queatz.ailaai.ui.components.AppBar
import com.queatz.ailaai.ui.components.BackButton
import com.queatz.ailaai.ui.components.Dropdown
import com.queatz.ailaai.ui.dialogs.ReportDialog
import com.queatz.db.StickerPack
import kotlinx.coroutines.launch

@Composable
fun StickerPackScreen(stickerPackId: String) {
    val scope = rememberCoroutineScope()
    var stickerPack by rememberStateOf<StickerPack?>(null)
    var showReportDialog by rememberStateOf(false)
    val nav = nav

    LaunchedEffect(stickerPackId) {
        api.stickerPack(stickerPackId) {
            stickerPack = it
        }
    }

    if (showReportDialog) {
        ReportDialog("stickerpack/$stickerPackId") {
            showReportDialog = false
        }
    }

    Column(
        verticalArrangement = Arrangement.Top,
        modifier = Modifier.fillMaxSize()
    ) {
        AppBar(
            title = {
                Text(
                    stickerPack?.name ?: stringResource(R.string.sticker_pack),
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
                    UseStickerPackButton(stickerPack) {
                        scope.launch {
                            stickers.reload()
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
                                Text(stringResource(R.string.view_creator))
                            }, {
                                showMenu = false
                                nav.navigate("profile/${stickerPack.person!!}")
                            })
                            DropdownMenuItem({
                                Text(stringResource(R.string.report))
                            }, {
                                showMenu = false
                                showReportDialog = true
                            })
                        }
                    }
                }
            },
            modifier = Modifier.zIndex(1f)
        )
        Box(modifier = Modifier.weight(1f)) {

            stickerPack?.let { stickerPack ->
                StickerPackContents(
                    stickerPack,
                    modifier = Modifier
                        .fillMaxSize()
                ) {
                    scope.launch {
                        say.say(it.message)
                    }
                }
            }
        }
    }
}
