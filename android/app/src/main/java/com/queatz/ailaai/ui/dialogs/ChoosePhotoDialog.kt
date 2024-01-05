package com.queatz.ailaai.ui.dialogs

import aiPhoto
import aiStyles
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.Photo
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.queatz.ailaai.R
import com.queatz.ailaai.data.api
import com.queatz.ailaai.extensions.*
import com.queatz.ailaai.ui.components.CardToolbar
import com.queatz.db.AiPhotoRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

private var _allStyles = emptyList<Pair<String, String>>()
private var _selectedStyle: String? = null

class ChoosePhotoDialogState(
    var prompt: MutableState<String>
)

@Composable
fun ChoosePhotoDialog(
    scope: CoroutineScope,
    state: ChoosePhotoDialogState = remember { ChoosePhotoDialogState(mutableStateOf("")) },
    multiple: Boolean = true,
    imagesOnly: Boolean = false,
    onDismissRequest: () -> Unit,
    onIsGeneratingPhoto: (Boolean) -> Unit = {},
    onPhotos: (List<Uri>) -> Unit,
    onVideos: (List<Uri>) -> Unit = {},
    onGeneratedPhoto: (String) -> Unit,
) {
    var aiStyleMenu by rememberStateOf(false)
    val aiPrompt by state::prompt
    var allStyles by rememberStateOf(_allStyles)
    var selectedStyle by rememberStateOf(_selectedStyle)
    val context = LocalContext.current

    val cameraUri = "photo.jpg".asCacheFileUri(context)

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture(),
        onResult = { success ->
            if (success) {
                onDismissRequest()
                scope.launch {
                    onPhotos(cameraUri.inList())
                }
            }
        }
    )

    val launcher = when (multiple) {
        true -> rememberLauncherForActivityResult(ActivityResultContracts.PickMultipleVisualMedia()) { uris ->
            if (uris.isNotEmpty()) {
                onDismissRequest()
                scope.launch {
                    val photos = uris.filter { it.isPhoto(context) }
                    val videos = uris.filter { it.isVideo(context) }

                    if (photos.isNotEmpty()) {
                        onPhotos(photos)
                    }

                    if (videos.isNotEmpty()) {
                        onVideos(videos)
                    }
                }
            }
        }

        false -> rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
            if (uri != null) {
                onDismissRequest()
                scope.launch {
                    if (uri.isPhoto(context)) {
                        onPhotos(uri.inList())
                    }

                    if (uri.isVideo(context)) {
                        onVideos(uri.inList())
                    }
                }
            }
        }
    }

    LaunchedEffect(aiStyleMenu) {
        if (aiStyleMenu && allStyles.isEmpty()) {
            api.aiStyles {
                allStyles = it
                _allStyles = allStyles
            }
        }
    }

    if (aiStyleMenu && allStyles.isNotEmpty()) {
        Menu({
            aiStyleMenu = false
        }) {
            allStyles.forEach {
                menuItem(it.first) {
                    aiStyleMenu = false
                    selectedStyle = if (selectedStyle == it.second) {
                        null
                    } else {
                        it.second
                    }
                    _selectedStyle = selectedStyle
                }
            }
        }
    }

    TextFieldDialog(
        {
            onDismissRequest()
        },
        title = null,
        initialValue = aiPrompt.value,
        button = stringResource(R.string.generate_photo),
        requireNotBlank = true,
        requireModification = false,
        placeholder = stringResource(R.string.describe_photo),
        extraContent = {
            CardToolbar {
                item(Icons.Outlined.Photo, stringResource(R.string.set_photo)) {
                    launcher.launch(PickVisualMediaRequest(if (imagesOnly) ActivityResultContracts.PickVisualMedia.ImageOnly else ActivityResultContracts.PickVisualMedia.ImageAndVideo))
                }

                item(Icons.Outlined.CameraAlt, stringResource(R.string.take_photo)) {
                    cameraLauncher.launch(cameraUri)
                }

                item(
                    Icons.Outlined.AutoAwesome,
                    allStyles.firstOrNull { it.second == selectedStyle }?.first ?: stringResource(
                        R.string.style
                    ),
                    selected = selectedStyle != null
                ) {
                    aiStyleMenu = true
                }
            }
        }
    ) { prompt ->
        aiPrompt.value = prompt
        onDismissRequest()
        scope.launch {
            onIsGeneratingPhoto(true)
            api.aiPhoto(AiPhotoRequest(prompt, selectedStyle)) { response ->
                onGeneratedPhoto(response.photo)
            }
            onIsGeneratingPhoto(false)
        }
    }
}
