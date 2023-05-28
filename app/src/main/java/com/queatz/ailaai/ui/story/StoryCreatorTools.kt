package com.queatz.ailaai.ui.story

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.layout.boundsInParent
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.platform.LocalContext
import com.queatz.ailaai.api
import com.queatz.ailaai.api.uploadStoryAudio
import com.queatz.ailaai.api.uploadStoryPhotos
import com.queatz.ailaai.extensions.rememberStateOf
import com.queatz.ailaai.extensions.showDidntWork
import com.queatz.ailaai.ui.components.horizontalFadingEdge
import com.queatz.ailaai.ui.dialogs.ChooseCardDialog
import com.queatz.ailaai.ui.theme.PaddingDefault
import kotlinx.coroutines.launch

@Composable
fun StoryCreatorTools(storyId: String, addPart: (part: StoryContent) -> Unit) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var showCardSelectorDialog by rememberStateOf(false)

    val photoLauncher = rememberLauncherForActivityResult(ActivityResultContracts.PickMultipleVisualMedia()) {
        if (it.isEmpty()) return@rememberLauncherForActivityResult

        scope.launch {
            try {
                val photoUrls = api.uploadStoryPhotos(storyId, it)
                addPart(StoryContent.Photos(photoUrls))
                addPart(
                    StoryContent.Text("")
                )
            } catch (ex: Exception) {
                ex.printStackTrace()
                context.showDidntWork()
            }
        }
    }

    val audioLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) {
        if (it == null) return@rememberLauncherForActivityResult

        scope.launch {
            try {
                val audioUrl = api.uploadStoryAudio(storyId, it)
                addPart(StoryContent.Audio(audioUrl))
                addPart(
                    StoryContent.Text("")
                )
            } catch (ex: Exception) {
                ex.printStackTrace()
                context.showDidntWork()
            }
        }
    }

    if (showCardSelectorDialog) {
        ChooseCardDialog(
            {
                showCardSelectorDialog = false
            }
        ) { cardId ->
            addPart(
                StoryContent.Cards(listOf(cardId))
            )
            addPart(
                StoryContent.Text("")
            )
        }
    }

    Card {
        var viewport by remember { mutableStateOf(Size(0f, 0f)) }
        val scrollState = rememberScrollState()
        Row(
            horizontalArrangement = Arrangement.spacedBy(PaddingDefault),
            modifier = Modifier
//                    .padding(PaddingDefault)
                .horizontalScroll(scrollState)
                .onPlaced { viewport = it.boundsInParent().size }
                .horizontalFadingEdge(viewport, scrollState)
        ) {
            listOf(
                Icons.Outlined.Title to {
                    addPart(
                        StoryContent.Section("")
                    )
                },
                Icons.Outlined.Notes to {
                    addPart(
                        StoryContent.Text("")
                    )
                },
                Icons.Outlined.AccountBox to {
                    showCardSelectorDialog = true
                },
                Icons.Outlined.Photo to {
                    photoLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                },
                Icons.Outlined.PlayCircle to {
                    audioLauncher.launch("audio/*")
                }
            ).forEach {
                IconButton(
                    onClick = it.second
                ) {
                    Icon(it.first, null)
                }
            }
        }
    }
}
