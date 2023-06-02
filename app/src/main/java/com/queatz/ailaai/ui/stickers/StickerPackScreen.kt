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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.zIndex
import androidx.navigation.NavController
import com.queatz.ailaai.*
import com.queatz.ailaai.R
import com.queatz.ailaai.api.stickerPack
import com.queatz.ailaai.extensions.rememberStateOf
import com.queatz.ailaai.ui.components.BackButton
import com.queatz.ailaai.ui.theme.ElevationDefault
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StickerPackScreen(navController: NavController, stickerPackId: String, me: () -> Person?) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var stickerPack by rememberStateOf<StickerPack?>(null)

    LaunchedEffect(stickerPackId) {
        api.stickerPack(stickerPackId) {
            stickerPack = it
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
                        }
                    }
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            ),
            modifier = Modifier.shadow(ElevationDefault / 2).zIndex(1f)
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
