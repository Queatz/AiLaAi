package com.queatz.ailaai.item

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.queatz.ailaai.R
import com.queatz.ailaai.api.uploadPhotosFromUris
import com.queatz.ailaai.data.api
import com.queatz.ailaai.extensions.isNumericTextInput
import com.queatz.ailaai.extensions.notBlank
import com.queatz.ailaai.extensions.rememberStateOf
import com.queatz.ailaai.extensions.toItemQuantity
import com.queatz.ailaai.ui.components.Check
import com.queatz.ailaai.ui.components.DialogBase
import com.queatz.ailaai.ui.components.DialogLayout
import com.queatz.ailaai.ui.dialogs.*
import com.queatz.ailaai.ui.theme.pad
import com.queatz.db.Item
import createItem
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes

enum class LifespanDuration {
    Second,
    Minute,
    Hour,
    Day,
    Week,
    Month,
    Year
}

@Composable
fun CreateItemDialog(
    onDismissRequest: () -> Unit,
    onItem: suspend (Item) -> Unit
) {
    val scope = rememberCoroutineScope()
    var name by rememberStateOf("")
    var photo by rememberStateOf("")
    var description by rememberStateOf("")
    var divisible by rememberStateOf(false)
    var hasLifespan by rememberStateOf(false)
    var showLifespanDurationDialog by rememberStateOf(false)
    var choosePhotoDialog by rememberStateOf(false)
    var showPhotoDialog by rememberStateOf(false)
    var isLoading by rememberStateOf(false)
    var isGeneratingPhoto by rememberStateOf(false)
    var lifespan by rememberStateOf("1")
    var lifespanDuration by rememberStateOf(LifespanDuration.Month)
    val setPhotoState = remember(name) {
        ChoosePhotoDialogState(mutableStateOf(name))
    }

    val enabled = !isLoading
            && name.isNotBlank()
            && photo.isNotBlank()
            && description.isNotBlank()
            && (!hasLifespan || lifespan.toDoubleOrNull() != null)

    DialogBase(onDismissRequest) {
        DialogLayout(
            content = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(1.pad)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                    ) {
                        if (photo.isNotBlank() && !isGeneratingPhoto) {
                            Column(
                                verticalArrangement = Arrangement.spacedBy(1.pad),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .align(Alignment.Center)
                            ) {
                                AsyncImage(
                                    model = photo.let { api.url(it) },
                                    contentDescription = "",
                                    contentScale = ContentScale.Crop,
                                    alignment = Alignment.Center,
                                    modifier = Modifier
                                        .requiredSize(64.dp)
                                        .clip(MaterialTheme.shapes.large)
                                        .background(MaterialTheme.colorScheme.secondaryContainer)
                                        .clickable {
                                            showPhotoDialog = true
                                        }
                                )
                                OutlinedButton(
                                    {
                                        choosePhotoDialog = true
                                    }
                                ) {
                                    Text(stringResource(R.string.change_photo))
                                }
                            }
                        } else {
                            TextButton(
                                {
                                    choosePhotoDialog = true
                                },
                                modifier = Modifier
                                    .align(Alignment.Center)
                            ) {
                                Column(
                                    verticalArrangement = Arrangement.spacedBy(1.pad),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    if (isGeneratingPhoto) {
                                        CircularProgressIndicator(
                                            strokeWidth = ProgressIndicatorDefaults.CircularStrokeWidth / 2,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    } else {
                                        Icon(
                                            Icons.Outlined.CameraAlt, ""
                                        )
                                    }
                                    Text(stringResource(R.string.photo))
                                }
                            }
                        }
                    }
                    OutlinedTextField(
                        name,
                        { name = it },
                        shape = MaterialTheme.shapes.large,
                        singleLine = true,
                        label = {
                            Text(stringResource(R.string.name))
                        },
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Sentences,
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        description,
                        { description = it },
                        shape = MaterialTheme.shapes.large,
                        label = {
                            Text(stringResource(R.string.details))
                        },
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Sentences,
                        ),
                        placeholder = {
                            Text(
                                stringResource(R.string.describe_how_this_item_can_be_used),
                                modifier = Modifier
                                    .alpha(.5f)
                            )
                        },
                        singleLine = false,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Check(
                        divisible,
                        { divisible = it }
                    ) {
                        Column {
                            Text(stringResource(R.string.divisible))
                            Text(
                                stringResource(R.string.divisible_description),
                                color = MaterialTheme.colorScheme.secondary,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                    Check(
                        hasLifespan,
                        { hasLifespan = it }
                    ) {
                        Text(stringResource(R.string.has_lifespan))
                    }
                    if (hasLifespan) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(1.pad)
                        ) {
                            OutlinedTextField(
                                lifespan,
                                {
                                    if (it.isNumericTextInput(allowDecimal = lifespanDuration != LifespanDuration.Second)) {
                                        lifespan = it
                                    }
                                },
                                shape = MaterialTheme.shapes.large,
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = when (lifespanDuration) {
                                        LifespanDuration.Second -> KeyboardType.Number
                                        else -> KeyboardType.Decimal
                                    }
                                ),
                                singleLine = true,
                                modifier = Modifier
                                    .width(64.dp)
                                    .weight(1f)
                            )
                            TextButton(
                                onClick = {
                                    showLifespanDurationDialog = true
                                },
                                modifier = Modifier
                                    .weight(1f)
                            ) {
                                val count = remember(lifespan) {
                                    lifespan.takeIf { it.toItemQuantity() == 1.0 }?.let { 1 } ?: 2
                                }
                                Text(
                                    when (lifespanDuration) {
                                        LifespanDuration.Second -> pluralStringResource(
                                            R.plurals.inline_second,
                                            count
                                        )

                                        LifespanDuration.Minute -> pluralStringResource(
                                            R.plurals.inline_minute,
                                            count
                                        )

                                        LifespanDuration.Hour -> pluralStringResource(
                                            R.plurals.inline_hour,
                                            count
                                        )

                                        LifespanDuration.Day -> pluralStringResource(
                                            R.plurals.inline_day,
                                            count
                                        )

                                        LifespanDuration.Week -> pluralStringResource(
                                            R.plurals.inline_week,
                                            count
                                        )

                                        LifespanDuration.Month -> pluralStringResource(
                                            R.plurals.inline_month,
                                            count
                                        )

                                        LifespanDuration.Year -> pluralStringResource(
                                            R.plurals.inline_year,
                                            count
                                        )
                                    },
                                    textAlign = TextAlign.Start,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                        // todo becomes another item when it expires
                    }
                }
            },
            actions = {
                TextButton(
                    {
                        onDismissRequest()
                    }
                ) {
                    Text(stringResource(R.string.cancel))
                }
                TextButton(
                    {
                        isLoading = true
                        scope.launch {
                            api.createItem(
                                Item(
                                    name = name.trim(),
                                    description = description.trim(),
                                    photo = photo.notBlank,
                                    divisible = divisible,
                                    lifespan = if (hasLifespan) {
                                        lifespan.toDoubleOrNull()?.asSeconds(lifespanDuration)?.toInt()
                                    } else {
                                        null
                                    }
                                )
                            ) {
                                onItem(it)
                            }
                            isLoading = false
                        }
                    },
                    enabled = enabled
                ) {
                    Text(stringResource(R.string.create_item))
                }
            }
        )
    }

    if (showLifespanDurationDialog) {
        Menu(
            {
                showLifespanDurationDialog = false
            }
        ) {
            menuItem(
                pluralStringResource(
                    R.plurals.second, 1
                )
            ) {
                lifespanDuration = LifespanDuration.Second
                showLifespanDurationDialog = false
            }
            menuItem(
                pluralStringResource(
                    R.plurals.minute, 1
                )
            ) {
                lifespanDuration = LifespanDuration.Minute
                showLifespanDurationDialog = false
            }
            menuItem(
                pluralStringResource(
                    R.plurals.hour, 1
                )
            ) {
                lifespanDuration = LifespanDuration.Hour
                showLifespanDurationDialog = false
            }
            menuItem(
                pluralStringResource(
                    R.plurals.day, 1
                )
            ) {
                lifespanDuration = LifespanDuration.Day
                showLifespanDurationDialog = false
            }
            menuItem(
                pluralStringResource(
                    R.plurals.week, 1
                )
            ) {
                lifespanDuration = LifespanDuration.Week
                showLifespanDurationDialog = false
            }
            menuItem(
                pluralStringResource(
                    R.plurals.month, 1
                )
            ) {
                lifespanDuration = LifespanDuration.Month
                showLifespanDurationDialog = false
            }
            menuItem(
                pluralStringResource(
                    R.plurals.year, 1
                )
            ) {
                lifespanDuration = LifespanDuration.Year
                showLifespanDurationDialog = false
            }
        }
    }

    if (showPhotoDialog) {
        PhotoDialog(
            {
                showPhotoDialog = false
            },
            initialMedia = Media.Photo(photo),
            medias = listOf(Media.Photo(photo))
        )
    }

    if (choosePhotoDialog) {
        val context = LocalContext.current

        ChoosePhotoDialog(
            scope = scope,
            state = setPhotoState,
            onDismissRequest = { choosePhotoDialog = false },
            multiple = false,
            imagesOnly = true,
            onPhotos = { photos ->
                scope.launch {
                    isGeneratingPhoto = true
                    api.uploadPhotosFromUris(context, photos) {
                        photo = it.urls.first()
                    }
                    isGeneratingPhoto = false
                }
            },
            onGeneratedPhoto = {
                photo = it
            },
            onIsGeneratingPhoto = {
                isGeneratingPhoto = it
            }
        )
    }
}

private fun Double.asSeconds(lifespanDuration: LifespanDuration) = when (lifespanDuration) {
    LifespanDuration.Second -> toLong()
    LifespanDuration.Minute -> minutes.inWholeSeconds
    LifespanDuration.Hour -> hours.inWholeSeconds
    LifespanDuration.Day -> days.inWholeSeconds
    LifespanDuration.Week -> (days * 7).inWholeSeconds
    LifespanDuration.Month -> (days * 30).inWholeSeconds
    LifespanDuration.Year -> (days * 365).inWholeSeconds
}
