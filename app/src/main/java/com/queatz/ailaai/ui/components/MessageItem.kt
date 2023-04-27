package com.queatz.ailaai.ui.components

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat.getSystemService
import androidx.core.graphics.drawable.toBitmapOrNull
import androidx.navigation.NavController
import coil.compose.AsyncImage
import coil.imageLoader
import coil.request.ImageRequest
import com.queatz.ailaai.*
import com.queatz.ailaai.R
import com.queatz.ailaai.extensions.*
import com.queatz.ailaai.ui.dialogs.Menu
import com.queatz.ailaai.ui.dialogs.item
import com.queatz.ailaai.ui.screens.exploreInitialCategory
import com.queatz.ailaai.ui.theme.PaddingDefault
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MessageItem(
    message: Message,
    getPerson: (String) -> Person?,
    isMe: Boolean,
    onDeleted: () -> Unit,
    onShowPhoto: (String) -> Unit,
    navController: NavController,
) {
    var showMessageDialog by remember { mutableStateOf(false) }
    var showDeleteMessageDialog by remember { mutableStateOf(false) }
    var showTime by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var attachedCardId by remember { mutableStateOf<String?>(null) }
    var attachedPhotos by remember { mutableStateOf<List<String>?>(null) }
    var attachedCard by remember { mutableStateOf<Card?>(null) }
    var selectedBitmap by remember { mutableStateOf<String?>(null) }

    attachedCardId = (message.getAttachment() as? CardAttachment)?.card
    attachedPhotos = (message.getAttachment() as? PhotosAttachment)?.photos

    LaunchedEffect(Unit) {
        attachedCardId?.let { cardId ->
            try {
                attachedCard = api.card(cardId)
            } catch (e: Exception) {
                e.printStackTrace()
                // todo show failed to load
            }
        }
    }

    if (showDeleteMessageDialog) {
        var disableSubmit by remember { mutableStateOf(false) }
        AlertDialog(
            {
                showDeleteMessageDialog = false
            },
            title = {
                Text(stringResource(R.string.delete_this_message))
            },
            text = {
                Text(stringResource(R.string.delete_message_description))
            },
            confirmButton = {
                TextButton(
                    {
                        coroutineScope.launch {
                            try {
                                disableSubmit = true
                                api.deleteMessage(message.id!!)
                                onDeleted()
                                showDeleteMessageDialog = false
                            } catch (ex: Exception) {
                                ex.printStackTrace()
                            } finally {
                                disableSubmit = false
                            }
                        }
                    },
                    enabled = !disableSubmit,
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(stringResource(R.string.delete))
                }
            },
            dismissButton = {
                TextButton({
                    showDeleteMessageDialog = false
                }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    if (showMessageDialog) {
        val messageString = stringResource(R.string.message)
        Menu(
            {
                showMessageDialog = false
            }
        ) {
            if (attachedPhotos?.isNotEmpty() == true && selectedBitmap != null) {
//                item(stringResource(R.string.send)) {
//                    showSendPhoto = true
//                }
                item(stringResource(R.string.share)) {
                    coroutineScope.launch {
                        context.imageLoader.execute(
                            ImageRequest.Builder(context)
                                .data(selectedBitmap!!)
                                .target { drawable ->
                                    drawable.toBitmapOrNull()?.share(context, null)
                                }
                                .build()
                        )
                    }
                }
            }

            if (message.text?.isBlank() == false) {
                item(stringResource(R.string.copy)) {
                    (message.text ?: "").copyToClipboard(context, messageString)
                    showMessageDialog = false
                }
            }

            if (isMe) {
                item(stringResource(R.string.delete)) {
                    showDeleteMessageDialog = true
                    showMessageDialog = false
                }
            }
        }
    }

    Row(modifier = Modifier
        .fillMaxWidth()
        .clickable(
            interactionSource = MutableInteractionSource(),
            indication = null
        ) {
            showTime = !showTime
        }) {
        if (!isMe) {
            ProfileImage(
                getPerson(message.member!!),
                PaddingValues(PaddingDefault, PaddingDefault, 0.dp, PaddingDefault),
            ) { person ->
                navController.navigate("profile/${person.id!!}")
            }
        }

        Column(modifier = Modifier.weight(1f)) {
            attachedCardId?.let {
                Box(modifier = Modifier
                    .padding(PaddingDefault)
                    .widthIn(max = 320.dp)
                    .let {
                        when (isMe) {
                            true -> it.padding(PaddingValues(start = PaddingDefault * 12))
                                .align(Alignment.End)

                            false -> it.padding(PaddingValues(end = PaddingDefault * 12))
                                .align(Alignment.Start)
                        }
                    }
                ) {
                    BasicCard(
                        {
                            navController.navigate("card/$it")
                        },
                        onCategoryClick = {
                            exploreInitialCategory = it
                            navController.navigate("explore")
                        },
                        activity = navController.context as Activity,
                        card = attachedCard,
                        isChoosing = true
                    )
                }
            }
            attachedPhotos?.ifNotEmpty?.let { photos ->
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(
                        PaddingDefault,
                        if (isMe) Alignment.End else Alignment.Start
                    ),
                    contentPadding = PaddingValues(PaddingDefault),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(photos, key = { it }) { photo ->
                        val data = api.url(photo)
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(data)
                                .crossfade(true)
                                .build(),
                            contentDescription = "",
                            contentScale = ContentScale.Fit,
                            alignment = Alignment.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(320.dp)
                                .clip(MaterialTheme.shapes.large)
                                .combinedClickable(
                                    onClick = { onShowPhoto(photo) },
                                    onLongClick = {
                                        selectedBitmap = data
                                        showMessageDialog = true
                                    }
                                )
                        )
                    }
                }
            }
            if (message.text != null) {
                LinkifyText(
                    message.text ?: "",
                    color = if (isMe) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier
                        .align(if (isMe) Alignment.End else Alignment.Start)
                        .padding(PaddingDefault)
                        .let {
                            when (isMe) {
                                true -> it.padding(PaddingValues(start = PaddingDefault * 8))
                                false -> it.padding(PaddingValues(end = PaddingDefault * 8))
                            }
                        }
                        .clip(MaterialTheme.shapes.large)
                        .background(if (isMe) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.background)
                        .border(
                            if (isMe) 0.dp else 1.dp,
                            MaterialTheme.colorScheme.secondaryContainer,
                            MaterialTheme.shapes.large
                        )
                        .combinedClickable(
                            onClick = { showTime = !showTime },
                            onLongClick = { showMessageDialog = true }
                        )
                        .padding(PaddingDefault * 2, PaddingDefault)
                )
            }
            AnimatedVisibility(showTime) {
                Text(
                    "${message.createdAt!!.timeAgo()}, ${getPerson(message.member!!)?.name ?: stringResource(R.string.someone)}",
                    color = MaterialTheme.colorScheme.secondary,
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = if (isMe) TextAlign.End else TextAlign.Start,
                    modifier = Modifier
                        .padding(horizontal = PaddingDefault)
                        .fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun ProfileImage(person: Person?, padding: PaddingValues, onClick: (Person) -> Unit) {
    if (person?.photo?.nullIfBlank == null) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .padding(padding)
                .requiredSize(32.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.secondaryContainer)
        ) {
            Text(
                person?.name?.take(1) ?: "",
                style = MaterialTheme.typography.titleMedium
            )
        }
    } else {
        AsyncImage(
            model = person.photo?.let { api.url(it) } ?: "",
            contentDescription = "",
            contentScale = ContentScale.Crop,
            alignment = Alignment.Center,
            modifier = Modifier
                .padding(padding)
                .requiredSize(32.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.secondaryContainer)
                .clickable {
                    onClick(person)
                }
        )
    }
}
