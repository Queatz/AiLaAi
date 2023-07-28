package com.queatz.ailaai.ui.stickers

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.zIndex
import androidx.navigation.NavController
import com.queatz.ailaai.R
import com.queatz.ailaai.api.stickerPack
import com.queatz.ailaai.data.Person
import com.queatz.ailaai.data.StickerPack
import com.queatz.ailaai.data.api
import com.queatz.ailaai.extensions.rememberStateOf
import com.queatz.ailaai.services.say
import com.queatz.ailaai.services.stickers
import com.queatz.ailaai.ui.components.BackButton
import com.queatz.ailaai.ui.dialogs.ReportDialog
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StickerPackScreen(navController: NavController, stickerPackId: String, me: () -> Person?) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var stickerPack by rememberStateOf<StickerPack?>(null)
    var showReportDialog by rememberStateOf(false)

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
        TopAppBar(
            title = {
                Text(
                    stickerPack?.name ?: stringResource(R.string.sticker_pack),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            },
            navigationIcon = {
                BackButton(navController)
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
                        DropdownMenu(showMenu, { showMenu = false }) {
                            DropdownMenuItem({
                                Text(stringResource(R.string.view_creator))
                            }, {
                                showMenu = false
                                navController.navigate("profile/${stickerPack.person!!}")
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
