package com.queatz.ailaai.ui.dialogs

import aiPhoto
import aiStyles
import android.Manifest
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Draw
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.Photo
import androidx.compose.material.icons.outlined.Queue
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import app.ailaai.api.prompts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import com.queatz.ailaai.R
import com.queatz.ailaai.api.uploadPhotosFromUris
import com.queatz.ailaai.data.api
import com.queatz.ailaai.extensions.asCacheFileUri
import com.queatz.ailaai.extensions.cameraSupported
import com.queatz.ailaai.extensions.inList
import com.queatz.ailaai.extensions.isPhoto
import com.queatz.ailaai.extensions.isVideo
import com.queatz.ailaai.extensions.rememberStateOf
import com.queatz.ailaai.extensions.showDidntWork
import com.queatz.ailaai.extensions.uri
import com.queatz.ailaai.ui.components.EmptyText
import com.queatz.ailaai.ui.components.Toolbar
import com.queatz.ailaai.ui.permission.permissionRequester
import com.queatz.ailaai.ui.theme.pad
import com.queatz.db.AiPhotoRequest
import com.queatz.db.Prompt
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

private var _allStyles = emptyList<Pair<String, String>>()
private var _selectedStyle: String? = null

class ChoosePhotoDialogState(
    var prompt: MutableState<String>,
    var count: MutableState<Int> = mutableStateOf(1)
)

@Composable
fun ChoosePhotoDialog(
    scope: CoroutineScope,
    state: ChoosePhotoDialogState = remember { ChoosePhotoDialogState(mutableStateOf("")) },
    multiple: Boolean = true,
    imagesOnly: Boolean = false,
    aspect: Double = 1.5,
    allowGenerateMultiple: Boolean = false,
    transparentBackground: Boolean = false,
    crop: Boolean = false,
    onDismissRequest: () -> Unit,
    onRemove: (() -> Unit)? = null,
    onIsGeneratingPhoto: (Boolean) -> Unit = {},
    onPhotos: suspend (List<Uri>) -> Unit,
    onVideos: suspend (List<Uri>) -> Unit = {},
    onGeneratedPhoto: suspend (String) -> Unit,
) {
    var aiStyleMenu by rememberStateOf(false)
    var promptHistoryMenu by rememberStateOf(false)
    var countDialog by rememberStateOf(false)
    var promptHistory by rememberStateOf(emptyList<Prompt>())
    var searchQuery by rememberStateOf("")
    val aiPrompt by state::prompt
    // Create a new state variable for the dialog value
    var dialogValue by rememberStateOf(aiPrompt.value)
    var allStyles by rememberStateOf(_allStyles)
    var selectedStyle by rememberStateOf(_selectedStyle)
    val context = LocalContext.current
    val cameraPermissionRequester = permissionRequester(Manifest.permission.CAMERA)
    var showCameraRationale by rememberStateOf(false)
    var showDrawing by rememberStateOf(false)

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

    fun camera() {
        cameraPermissionRequester.use(
            onPermanentlyDenied = {
                showCameraRationale = true
            }
        ) {
            runCatching {
                cameraLauncher.launch(cameraUri)
            }.onFailure {
                context.showDidntWork()
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

    LaunchedEffect(promptHistoryMenu) {
        if (promptHistoryMenu && promptHistory.isEmpty()) {
            api.prompts {
                promptHistory = it
            }
        }
    }

    if (aiStyleMenu && allStyles.isNotEmpty()) {
        Menu({
            aiStyleMenu = false
        }) {
            Text(
                text = stringResource(R.string.general).uppercase(),
                style = MaterialTheme.typography.labelSmall.copy(
                    color = MaterialTheme.colorScheme.outlineVariant,
                    fontWeight = FontWeight.Black
                ),
                modifier = Modifier
                    .padding(horizontal = 1.5f.pad)
                    .padding(top = 1.5f.pad)
            )
            var previousItem: String? = null
            allStyles.forEach {
                val prevItem = previousItem
                if (prevItem != null && prevItem.endsWith("HD)") && !it.first.endsWith("HD)")) {
                    Text(
                        text = stringResource(R.string.stylized).uppercase(),
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = MaterialTheme.colorScheme.outlineVariant,
                            fontWeight = FontWeight.Black
                        ),
                        modifier = Modifier
                            .padding(horizontal = 1.5f.pad)
                            .padding(top = 1.5f.pad)
                    )
                }

                menuItem(
                    title = it.first,
                    icon = if (selectedStyle == it.second) Icons.Outlined.Check else null
                ) {
                    aiStyleMenu = false
                    selectedStyle = if (selectedStyle == it.second) {
                        null
                    } else {
                        it.second
                    }
                    _selectedStyle = selectedStyle
                }
                previousItem = it.first
            }
        }
    }

    if (promptHistoryMenu && promptHistory.isNotEmpty()) {
        Menu(
            onDismissRequest = {
                promptHistoryMenu = false
                searchQuery = ""
            }
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = stringResource(R.string.history).uppercase(),
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = MaterialTheme.colorScheme.outlineVariant,
                        fontWeight = FontWeight.Black
                    ),
                    modifier = Modifier
                        .padding(horizontal = 1.5f.pad)
                        .padding(top = 1.5f.pad)
                        .weight(1f)
                )

                IconButton(
                    onClick = {
                        promptHistoryMenu = false
                        searchQuery = ""
                    }
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Close,
                        contentDescription = stringResource(R.string.close)
                    )
                }
            }

            // Search field
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text(stringResource(R.string.search)) },
                shape = MaterialTheme.shapes.large,
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 1.5f.pad)
                    .padding(top = 1.5f.pad, bottom = 1f.pad)
            )

            // Filter prompts based on search query
            val filteredPrompts = remember(searchQuery, promptHistory) {
                if (searchQuery.isBlank()) {
                    promptHistory
                } else {
                    promptHistory.filter {
                        it.prompt?.contains(searchQuery, ignoreCase = true) == true
                    }
                }
            }

            if (filteredPrompts.isEmpty() && searchQuery.isNotBlank()) {
                EmptyText(
                    stringResource(id = R.string.no_prompts),
                    modifier = Modifier
                        .padding(horizontal = 1.5f.pad)
                        .padding(vertical = 1f.pad)
                )
            } else {
                filteredPrompts.forEach { prompt ->
                    menuItem(
                        title = prompt.prompt ?: "",
                        modifier = Modifier.padding(vertical = 0.5f.pad)
                    ) {
                        promptHistoryMenu = false
                        searchQuery = ""
                        // Update both the state and the dialog value
                        state.prompt.value = prompt.prompt ?: ""
                        aiPrompt.value = prompt.prompt ?: ""
                        dialogValue = prompt.prompt ?: ""
                    }
                }
            }
        }
    }

    if (countDialog) {
        Menu({
            countDialog = false
        }) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = stringResource(R.string.photo_count).uppercase(),
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = MaterialTheme.colorScheme.outlineVariant,
                        fontWeight = FontWeight.Black
                    ),
                    modifier = Modifier
                        .padding(horizontal = 1.5f.pad)
                        .padding(top = 1.5f.pad)
                        .weight(1f)
                )

                IconButton(onClick = { countDialog = false }) {
                    Icon(
                        imageVector = Icons.Outlined.Close,
                        contentDescription = stringResource(R.string.close)
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .padding(horizontal = 1.5f.pad)
                    .padding(vertical = 1.5f.pad)
                    .fillMaxWidth()
            ) {
                // Decrease button
                IconButton(
                    onClick = {
                        if (state.count.value > 1) {
                            state.count.value--
                        }
                    }
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Remove,
                        contentDescription = null
                    )
                }

                // Count display
                Text(
                    text = state.count.value.toString(),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(horizontal = 0.5f.pad)
                )

                // Increase button
                IconButton(
                    onClick = {
                        if (state.count.value < 10) {
                            state.count.value++
                        }
                    }
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Add,
                        contentDescription = null
                    )
                }
            }
        }
    }

    TextFieldDialog(
        onDismissRequest = {
            onDismissRequest()
        },
        title = null,
        initialValue = dialogValue,
        button = stringResource(R.string.generate_photo),
        requireNotBlank = true,
        requireModification = false,
        placeholder = stringResource(R.string.describe_photo),
        dismissButtonText = if (onRemove != null) stringResource(R.string.remove) else null,
        showDismiss = onRemove != null,
        onDismiss = onRemove,
        extraContent = {
            // Toolbar
            Toolbar {
                item(Icons.Outlined.Photo, stringResource(R.string.set_photo)) {
                    launcher.launch(PickVisualMediaRequest(if (imagesOnly) ActivityResultContracts.PickVisualMedia.ImageOnly else ActivityResultContracts.PickVisualMedia.ImageAndVideo))
                }

                if (context.cameraSupported()) {
                    item(Icons.Outlined.CameraAlt, stringResource(R.string.take_photo)) {
                        camera()
                    }
                }

                item(Icons.Outlined.Draw, stringResource(R.string.draw)) {
                    showDrawing = !showDrawing
                }

                item(Icons.Outlined.ExpandMore, stringResource(R.string.history)) {
                    promptHistoryMenu = true
                }

                if (allowGenerateMultiple) {
                    item(
                        icon = Icons.Outlined.Queue,
                        name = state.count.value.toString(),
                        selected = state.count.value > 1
                    ) {
                        countDialog = true
                    }
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

                if (showDrawing) {
                    DrawDialog(
                        {
                            showDrawing = false
                        }
                    ) { bitmap ->
                        onDismissRequest()
                        scope.launch {
                            onIsGeneratingPhoto(true)
                            api.uploadPhotosFromUris(
                                context = context,
                                photos = bitmap.asAndroidBitmap().uri(context).inList(),
                                removeBackground = transparentBackground,
                                crop = crop,
                            ) { response ->
                                onGeneratedPhoto(response.urls.first())
                            }
                            onIsGeneratingPhoto(false)
                        }
                    }
                }
            }
        }
    ) { prompt ->
        // Update both the state and the dialog value
        aiPrompt.value = prompt
        dialogValue = prompt
        onDismissRequest()
        scope.launch {
            onIsGeneratingPhoto(true)

            // Generate multiple photos based on count
            val photoCount = state.count.value
            var completedCount = 0

            repeat(photoCount) {
                api.aiPhoto(
                    request = AiPhotoRequest(
                        prompt = prompt,
                        style = selectedStyle,
                        aspect = aspect,
                        removeBackground = transparentBackground.takeIf { it },
                        crop = crop
                    )
                ) { response ->
                    onGeneratedPhoto(response.photo)
                    completedCount++
                    if (completedCount >= photoCount) {
                        onIsGeneratingPhoto(false)
                    }
                }
            }
        }
    }

    if (showCameraRationale) {
        RationaleDialog(
            {
                showCameraRationale = false
            },
            stringResource(R.string.camera_disabled_description)
        )
    }
}
