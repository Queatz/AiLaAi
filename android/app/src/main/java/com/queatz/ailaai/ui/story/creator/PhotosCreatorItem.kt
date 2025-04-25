package com.queatz.ailaai.ui.story.creator

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.queatz.ailaai.R
import com.queatz.ailaai.api.uploadCardContentPhotosFromUri
import com.queatz.ailaai.api.uploadProfileContentPhotosFromUri
import com.queatz.ailaai.api.uploadStoryPhotosFromUri
import com.queatz.ailaai.data.api
import com.queatz.ailaai.extensions.rememberStateOf
import com.queatz.ailaai.ui.dialogs.ChoosePhotoDialog
import com.queatz.ailaai.ui.dialogs.Menu
import com.queatz.ailaai.ui.dialogs.menuItem
import com.queatz.ailaai.ui.story.CreatorScope
import com.queatz.ailaai.ui.story.ReorderDialog
import com.queatz.ailaai.ui.story.StorySource
import com.queatz.db.StoryContent
import kotlinx.coroutines.launch

fun LazyGridScope.photosCreatorItem(creatorScope: CreatorScope<StoryContent.Photos>) = with(creatorScope) {
    itemsIndexed(
        items = part.photos,
        span = { index, item ->
            GridItemSpan(if (index == 0) maxLineSpan else if (index % 3 == 1) 1 else maxCurrentLineSpan)
        },
        key = { index, it -> "${creatorScope.id}.$it" }
    ) { index, it ->
        val scope = rememberCoroutineScope()
        val context = LocalContext.current
        var showPhotoMenu by rememberStateOf(false)
        var showPhotoAspectMenu by rememberStateOf(false)
        var showReorderDialog by rememberStateOf(false)
        var showPhotoDialog by rememberStateOf(false)

        if (showPhotoDialog) {
            ChoosePhotoDialog(
                scope = scope,
                onDismissRequest = { showPhotoDialog = false },
                imagesOnly = true,
                onPhotos = {
                    scope.launch {
                        when (source) {
                            is StorySource.Story -> {
                                api.uploadStoryPhotosFromUri(context, source.id, it) { photoUrls ->
                                    edit {
                                        copy(
                                            photos = photos + photoUrls
                                        )
                                    }
                                }
                            }

                            is StorySource.Card -> {
                                api.uploadCardContentPhotosFromUri(
                                    context = context,
                                    card = source.id,
                                    media = it
                                ) { photoUrls ->
                                    edit {
                                        copy(
                                            photos = photos + photoUrls
                                        )
                                    }
                                }
                            }

                            is StorySource.Profile -> {
                                api.uploadProfileContentPhotosFromUri(
                                    context = context,
                                    card = source.id,
                                    media = it
                                ) { photoUrls ->
                                    edit {
                                        copy(
                                            photos = photos + photoUrls
                                        )
                                    }
                                }
                            }

                            is StorySource.Reminder -> {
                                api.uploadProfileContentPhotosFromUri(
                                    context = context,
                                    card = source.id,
                                    media = it
                                ) { photoUrls ->
                                    edit {
                                        copy(
                                            photos = photos + photoUrls
                                        )
                                    }
                                }
                            }

                            else -> {}
                        }
                    }
                },
                onGeneratedPhoto = {
                    val photoUrls = listOf(it)
                    scope.launch {
                        when (source) {
                            is StorySource.Story -> {
                                edit {
                                    copy(
                                        photos = photos + photoUrls
                                    )
                                }
                            }

                            is StorySource.Card -> {
                                edit {
                                    copy(
                                        photos = photos + photoUrls
                                    )
                                }
                            }

                            is StorySource.Profile -> {
                                edit {
                                    copy(
                                        photos = photos + photoUrls
                                    )
                                }
                            }

                            is StorySource.Reminder -> {
                                edit {
                                    copy(
                                        photos = photos + photoUrls
                                    )
                                }
                            }

                            else -> {}
                        }
                    }
                }
            )
        }

        if (showPhotoAspectMenu) {
            Menu(
                {
                    showPhotoAspectMenu = false
                }
            ) {
                menuItem(stringResource(R.string.none)) {
                    showPhotoAspectMenu = false
                    edit {
                        copy(
                            aspect = null
                        )
                    }
                }
                menuItem(stringResource(R.string.portrait)) {
                    showPhotoAspectMenu = false
                    edit {
                        copy(
                            aspect = .75f
                        )
                    }
                }
                menuItem(stringResource(R.string.landscape)) {
                    showPhotoAspectMenu = false
                    edit {
                        copy(
                            aspect = 1.5f
                        )
                    }
                }
                menuItem(stringResource(R.string.square)) {
                    showPhotoAspectMenu = false
                    edit {
                        copy(
                            aspect = 1f
                        )
                    }
                }
            }
        }

        if (showReorderDialog) {
            ReorderDialog(
                { showReorderDialog = false },
                onMove = { from, to ->
                    edit {
                        copy(
                            photos = photos.toMutableList().apply {
                                add(to.index, removeAt(from.index))
                            }
                        )
                    }
                },
                items = part.photos,
                key = { it }
            ) { photo, elevation ->
                AsyncImage(
                    model = api.url(photo),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    alignment = Alignment.Center,
                    modifier = Modifier
                        .shadow(elevation, shape = MaterialTheme.shapes.large)
                        .clip(MaterialTheme.shapes.large)
                        .background(MaterialTheme.colorScheme.secondaryContainer)
                        .then(
                            if (part.aspect != null) {
                                Modifier
                                    .aspectRatio(part.aspect!!)
                                    .heightIn(min = 240.dp)
                            } else {
                                Modifier
                                    .heightIn(max = 240.dp)
                            }
                        )
                )
            }
        }

        if (showPhotoMenu) {
            Menu(
                {
                    showPhotoMenu = false
                }
            ) {
                menuItem(stringResource(R.string.add_photo)) {
                    showPhotoMenu = false
                    showPhotoDialog = true
                }
                menuItem(stringResource(R.string.change_aspect_ratio)) {
                    showPhotoMenu = false
                    showPhotoAspectMenu = true
                }
                if (part.photos.size > 1) {
                    menuItem(stringResource(R.string.reorder)) {
                        showReorderDialog = true
                        showPhotoMenu = false
                    }
                }
                menuItem(stringResource(R.string.remove)) {
                    showPhotoMenu = false
                    if (part.photos.size == 1) {
                        showPhotoMenu = false
                        remove(partIndex)
                    } else {
                        edit {
                            copy(
                                photos = photos.toMutableList().apply {
                                    removeAt(index)
                                }
                            )
                        }
                    }
                }
                if (part.photos.size > 1) {
                    menuItem(stringResource(R.string.remove_all)) {
                        showPhotoMenu = false
                        remove(partIndex)
                    }
                }
            }
        }

        AsyncImage(
            model = api.url(it),
            contentDescription = "",
            contentScale = ContentScale.Crop,
            alignment = Alignment.Center,
            modifier = Modifier
                .clip(MaterialTheme.shapes.large)
                .fillMaxWidth()
                .then(
                    if (part.aspect != null) {
                        Modifier
                            .aspectRatio(part.aspect!!)
                            .heightIn(min = 240.dp)
                    } else {
                        Modifier
                    }
                )
                .background(MaterialTheme.colorScheme.secondaryContainer)
                .clickable {
                    showPhotoMenu = true
                }
        )
    }
}
