package com.queatz.ailaai.ui.story.contents

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AddPhotoAlternate
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.queatz.ailaai.R
import com.queatz.ailaai.api.uploadPhotosFromUris
import com.queatz.ailaai.data.api
import com.queatz.ailaai.extensions.notBlank
import com.queatz.ailaai.extensions.rememberStateOf
import com.queatz.ailaai.ui.dialogs.ChoosePhotoDialog
import com.queatz.ailaai.ui.theme.pad
import com.queatz.db.InputType
import com.queatz.db.StoryContent

fun LazyGridScope.inputItem(
    content: StoryContent.Input,
    onValueChange: (String) -> Unit
) {
    item(span = { GridItemSpan(maxLineSpan) }) {
        val scope = rememberCoroutineScope()
        val context = LocalContext.current
        var value by rememberStateOf(content.value)

        when (content.inputType) {
            InputType.Text -> {
                OutlinedTextField(
                    value = value.orEmpty(),
                    onValueChange = {
                        value = it
                        onValueChange(it)
                    },
                    shape = MaterialTheme.shapes.large,
                    placeholder = content.hint?.notBlank?.let { { Text(it) } },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            InputType.Photo -> {
                if (value.isNullOrBlank()) {
                    var showDialog by rememberStateOf(false)

                    if (showDialog) {
                        ChoosePhotoDialog(
                            scope = scope,
                            onDismissRequest = {},
                            multiple = false,
                            onPhotos = { photos ->
                                api.uploadPhotosFromUris(
                                    context = context,
                                    photos = photos
                                ) {
                                    value = it.urls.firstOrNull().orEmpty()
                                    onValueChange(value.orEmpty())
                                }
                            },
                            onGeneratedPhoto = {
                                value = it
                            }
                        )
                    }
                    OutlinedButton(
                        onClick = {
                            showDialog = true
                        },
                        shape = MaterialTheme.shapes.large,
                    ) {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(1.pad),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            Icon(
                                Icons.Outlined.AddPhotoAlternate,
                                contentDescription = stringResource(R.string.add_photo),
                            )
                            Text(
                                stringResource(R.string.photo)
                            )
                        }
                    }
                } else {
                    Box(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        AsyncImage(

                            model = ImageRequest.Builder(LocalContext.current)
                                .data(value!!.let(api::url))
                                .crossfade(true)
                                .build(),
                            contentScale = ContentScale.Fit,
                            contentDescription = null,
                            modifier = Modifier.fillMaxWidth()
                                .clip(MaterialTheme.shapes.large)
                                .background(MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp /* Card elevation */))
                        )
                        IconButton(
                            onClick = {
                                value = ""
                                onValueChange("")
                            },
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(1.pad)
                        ) {
                            Icon(
                                Icons.Outlined.Delete,
                                contentDescription = stringResource(R.string.remove_photo),
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }
        }
    }
}
