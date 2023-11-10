package com.queatz.ailaai.ui.components

import android.annotation.SuppressLint
import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Photo
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.layout.boundsInParent
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import app.ailaai.api.updateCard
import com.queatz.ailaai.R
import com.queatz.ailaai.api.uploadCardPhotoFromUri
import com.queatz.ailaai.api.uploadCardVideoFromUri
import com.queatz.ailaai.data.api
import com.queatz.ailaai.extensions.horizontalFadingEdge
import com.queatz.ailaai.extensions.isPhoto
import com.queatz.ailaai.extensions.isVideo
import com.queatz.ailaai.extensions.rememberStateOf
import com.queatz.ailaai.ui.dialogs.*
import com.queatz.ailaai.ui.theme.PaddingDefault
import com.queatz.db.Card
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

@SuppressLint("MissingPermission", "UnrememberedMutableState")
@Composable
fun CardToolbar(
    navController: NavController,
    activity: Activity,
    onChange: () -> Unit,
    onDelete: () -> Unit,
    card: Card,
    modifier: Modifier = Modifier,
) {
    var openDeleteDialog by rememberStateOf(false)
    var openEditDialog by remember { mutableStateOf(false) }
    var openLocationDialog by remember { mutableStateOf(false) }
    val scrollState = rememberScrollState()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var viewport by rememberStateOf(Size(0f, 0f))
    var uploadJob by remember { mutableStateOf<Job?>(null) }
    var isUploadingVideo by rememberStateOf(false)
    var videoUploadProgress by remember { mutableStateOf(0f) }
    var videoUploadStage by remember { mutableStateOf(ProcessingVideoStage.Processing) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) {
        if (it == null) return@rememberLauncherForActivityResult

        uploadJob = scope.launch {
            videoUploadProgress = 0f
            if (it.isVideo(context)) {
                isUploadingVideo = true
                api.uploadCardVideoFromUri(
                    context,
                    card!!.id!!,
                    it,
                    context.contentResolver.getType(it) ?: "video/*",
                    it.lastPathSegment ?: "video.${
                        context.contentResolver.getType(it)?.split("/")?.lastOrNull() ?: ""
                    }",
                    processingCallback = {
                        videoUploadStage = ProcessingVideoStage.Processing
                        videoUploadProgress = it
                    },
                    uploadCallback = {
                        videoUploadStage = ProcessingVideoStage.Uploading
                        videoUploadProgress = it
                    }
                )
            } else if (it.isPhoto(context)) {
                api.uploadCardPhotoFromUri(context, card!!.id!!, it)
            }
            onChange()
            isUploadingVideo = false
            uploadJob = null
        }
    }
    if (isUploadingVideo) {
        ProcessingVideoDialog(
            onDismissRequest = { isUploadingVideo = false },
            onCancelRequest = { uploadJob?.cancel() },
            stage = videoUploadStage,
            progress = videoUploadProgress
        )
    }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(PaddingValues(top = PaddingDefault))
            .horizontalScroll(scrollState)
            .onPlaced { viewport = it.boundsInParent().size }
            .horizontalFadingEdge(viewport, scrollState)
    ) {
        var active by remember { mutableStateOf(card.active ?: false) }
        var activeCommitted by remember { mutableStateOf(active) }
        val coroutineScope = rememberCoroutineScope()

        Switch(
            active,
            {
                active = it
                coroutineScope.launch {
                    api.updateCard(card.id!!, Card(active = active)) {
                        card.active = it.active
                        activeCommitted = it.active ?: false
                        active = activeCommitted
                    }
                }
            }
        )
        if (activeCommitted) {
            Text(
                stringResource(R.string.published),
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier
                    .align(Alignment.CenterVertically)
                    .padding(start = PaddingDefault)
            )
        }
        Box(modifier = Modifier.weight(1f))
        TextButton({
            openLocationDialog = true
        }) {
            Icon(Icons.Outlined.Place, "", modifier = Modifier.padding(end = 8.dp))
            Text(
                when {
                    card.parent != null -> stringResource(R.string.inside_another_card)
                    card.equipped == true -> stringResource(R.string.on_profile)
                    card.offline != true -> stringResource(R.string.at_a_location)
                    else -> stringResource(R.string.none)
                }
            )
        }
        IconButton({
            openEditDialog = true
        }) {
            Icon(Icons.Outlined.Edit, stringResource(R.string.edit), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        IconButton({
            launcher.launch(PickVisualMediaRequest())
        }) {
            Icon(
                Icons.Outlined.Photo,
                stringResource(R.string.set_photo),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        IconButton({
            openDeleteDialog = true
        }) {
            Icon(Icons.Outlined.Delete, "", tint = MaterialTheme.colorScheme.error)
        }
    }

    if (openLocationDialog) {
        EditCardLocationDialog(card, navController = navController, activity, {
            openLocationDialog = false
        }, onChange)
    }

    if (openEditDialog) {
        EditCardDialog(card, {
            openEditDialog = false
        }, onChange = { onChange() })
    }

    if (openDeleteDialog) {
        DeleteCardDialog(card, {
            openDeleteDialog = false
        }, onDelete)
    }
}
