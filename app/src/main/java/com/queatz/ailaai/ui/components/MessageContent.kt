package com.queatz.ailaai.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Photo
import androidx.compose.material.icons.outlined.Reply
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.boundsInParent
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmapOrNull
import androidx.navigation.NavController
import coil.compose.AsyncImage
import coil.imageLoader
import coil.request.ImageRequest
import com.queatz.ailaai.R
import com.queatz.ailaai.api.card
import com.queatz.ailaai.api.deleteMessage
import com.queatz.ailaai.api.sticker
import com.queatz.ailaai.api.story
import com.queatz.ailaai.data.*
import com.queatz.ailaai.extensions.*
import com.queatz.ailaai.services.say
import com.queatz.ailaai.ui.dialogs.Menu
import com.queatz.ailaai.ui.dialogs.menuItem
import com.queatz.ailaai.ui.screens.exploreInitialCategory
import com.queatz.ailaai.ui.stickers.StickerPhoto
import com.queatz.ailaai.ui.story.StoryAuthors
import com.queatz.ailaai.ui.story.textContent
import com.queatz.ailaai.ui.theme.PaddingDefault
import io.ktor.http.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun ColumnScope.MessageContent(
    message: Message,
    isMe: Boolean,
    me: String?,
    showTime: Boolean,
    onShowTime: (Boolean) -> Unit,
    showMessageDialog: Boolean,
    onShowMessageDialog: (Boolean) -> Unit,
    getPerson: (String) -> Person?,
    getMessage: suspend (String) -> Message?,
    onReply: (Message) -> Unit,
    onDeleted: () -> Unit,
    onShowPhoto: (String) -> Unit,
    navController: NavController,
    isReply: Boolean = false,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val isMeActual = me == message.member
    var showDeleteMessageDialog by rememberStateOf(false)
    var attachedCardId by remember { mutableStateOf<String?>(null) }
    var attachedReplyId by remember { mutableStateOf<String?>(null) }
    var attachedStoryId by remember { mutableStateOf<String?>(null) }
    var attachedPhotos by remember { mutableStateOf<List<String>?>(null) }
    var attachedSticker by remember { mutableStateOf<Sticker?>(null) }
    var attachedCard by remember { mutableStateOf<Card?>(null) }
    var attachedReply by remember { mutableStateOf<Message?>(null) }
    var attachedStory by remember { mutableStateOf<Story?>(null) }
    var attachedStoryNotFound by remember { mutableStateOf(false) }
    var attachedAudio by remember { mutableStateOf<String?>(null) }
    var selectedBitmap by remember { mutableStateOf<String?>(null) }

    // todo: support multiple attachments of the same type
    // todo: right now the only possibility for 2 attachments is with message replies
    message.getAllAttachments().forEach { attachment ->
        when (attachment) {
            is CardAttachment -> {
                attachedCardId = attachment.card
            }

            is PhotosAttachment -> {
                attachedPhotos = attachment.photos
            }

            is AudioAttachment -> {
                attachedAudio = attachment.audio
            }

            is ReplyAttachment -> {
                attachedReplyId = attachment.message
            }

            is StoryAttachment -> {
                attachedStoryId = attachment.story
            }

            is StickerAttachment -> {
                attachedSticker = Sticker(
                    photo = attachment.photo,
                    message = attachment.message,
                ).apply {
                    id = attachment.sticker
                }
            }
        }
    }

    if (showMessageDialog) {
        val messageString = stringResource(R.string.message)
        Menu(
            {
                onShowMessageDialog(false)
            }
        ) {
            menuItem(stringResource(R.string.reply)) {
                onShowMessageDialog(false)
                onReply(message)
            }
            if (attachedPhotos?.isNotEmpty() == true && selectedBitmap != null) {
//                item(stringResource(R.string.send)) {
//                    showSendPhoto = true
//                }
                menuItem(stringResource(R.string.share)) {
                    onShowMessageDialog(false)
                    scope.launch {
                        context.imageLoader.execute(
                            ImageRequest.Builder(context)
                                .data(selectedBitmap!!)
                                .target { drawable ->
                                    scope.launch {
                                        drawable.toBitmapOrNull()?.share(context, null)
                                    }
                                }
                                .build()
                        )
                    }
                }
            }

            if (attachedAudio != null) {
//                item(stringResource(R.string.send)) {
//                    showSendPhoto = true
//                }
                menuItem(stringResource(R.string.share)) {
                    onShowMessageDialog(false)
                    scope.launch {
                        api.url(attachedAudio!!).shareAudio(context, null)
                    }
                }
            }

            if (message.text?.isBlank() == false) {
                menuItem(stringResource(R.string.copy)) {
                    (message.text ?: "").copyToClipboard(context, messageString)
                    context.toast(R.string.copied)
                    onShowMessageDialog(false)
                }
            }

            if (isMe && !isReply) {
                menuItem(stringResource(R.string.delete)) {
                    showDeleteMessageDialog = true
                    onShowMessageDialog(false)
                }
            }
        }
    }

    if (showDeleteMessageDialog) {
        var disableSubmit by rememberStateOf(false)
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
                        scope.launch {
                            disableSubmit = true
                            api.deleteMessage(message.id!!) {
                                onDeleted()
                                showDeleteMessageDialog = false
                            }
                            disableSubmit = false
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

    LaunchedEffect(Unit) {
        attachedCardId?.let { cardId ->
            api.card(cardId, onError = {
                // todo show failed to load
            }) { attachedCard = it }
        }
    }

    LaunchedEffect(Unit) {
        attachedReplyId?.let { messageId ->
            attachedReply = getMessage(messageId)
        }
    }

    LaunchedEffect(Unit) {
        attachedStoryId?.let { storyId ->
            api.story(
                storyId,
                onError = {
                    if (it.status == HttpStatusCode.NotFound) {
                        attachedStoryNotFound = true
                    }
                }
            ) {
                attachedStory = it
            }
        }
    }

    attachedReply?.takeIf { !isReply }?.let { reply ->
        var viewport by remember { mutableStateOf(Size(0f, 0f)) }
        var showReplyTime by rememberStateOf(false)
        var showReplyMessageDialog by rememberStateOf(false)
        Box(
            modifier = Modifier
                .align(if (isMe) Alignment.End else Alignment.Start)
                .let {
                    when (isMe) {
                        true -> it.padding(start = PaddingDefault * 8)
                        false -> it.padding(end = PaddingDefault * 8)
                    }
                }
                .padding(horizontal = PaddingDefault)
                .clip(
                    when (isMe) {
                        true -> RoundedCornerShape(
                            MaterialTheme.shapes.large.topStart,
                            MaterialTheme.shapes.medium.topEnd,
                            MaterialTheme.shapes.medium.bottomEnd,
                            MaterialTheme.shapes.large.bottomStart
                        )

                        false -> RoundedCornerShape(
                            MaterialTheme.shapes.medium.topStart,
                            MaterialTheme.shapes.large.topEnd,
                            MaterialTheme.shapes.large.bottomEnd,
                            MaterialTheme.shapes.medium.bottomStart
                        )
                    }
                )
                .background(MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp))
                .combinedClickable(
                    onClick = { showReplyTime = !showReplyTime },
                    onLongClick = { showReplyMessageDialog = true }
                )
        ) {
            Box(
                modifier = Modifier
                    .align(if (isMe) Alignment.CenterEnd else Alignment.CenterStart)
                    .fillMaxHeight()
                    .heightIn(min = viewport.height.inDp()) // todo why is even it needed
                    .requiredWidth(4.dp)
                    .clip(MaterialTheme.shapes.large)
                    .background(MaterialTheme.colorScheme.secondaryContainer)
            ) {}
            Column(
                modifier = Modifier
                    .onPlaced {
                        viewport = it.boundsInParent().size
                    }
                    .let {
                        when (isMe) {
                            true -> it.padding(end = 4.dp + PaddingDefault)
                            false -> it.padding(start = 4.dp + PaddingDefault)
                        }
                    }
            ) {
                MessageContent(
                    reply,
                    isMe,
                    me,
                    showReplyTime,
                    { showReplyTime = it },
                    showReplyMessageDialog,
                    { showReplyMessageDialog = it },
                    getPerson,
                    getMessage,
                    onReply,
                    {}, // todo delete from reply
                    {}, // todo open photo in reply
                    navController,
                    isReply = true
                )
            }
            Icon(
                Icons.Outlined.Reply,
                null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = .5f),
                modifier = Modifier
                    .padding(vertical = 2.dp, horizontal = 6.dp)
                    .align(if (isMe) Alignment.TopEnd else Alignment.TopStart)
                    .requiredSize(12.dp)
            )
        }
    }

    attachedCardId?.let {
        Box(modifier = Modifier
            .padding(PaddingDefault)
            .widthIn(max = 320.dp)
            .let {
                if (isReply) {
                    it
                } else {
                    when (isMe) {
                        true -> it.padding(start = PaddingDefault * 12)
                            .align(Alignment.End)

                        false -> it.padding(end = PaddingDefault * 12)
                            .align(Alignment.Start)
                    }
                }
            }
        ) {
            CardItem(
                {
                    navController.navigate("card/$it")
                },
                onCategoryClick = {
                    exploreInitialCategory = it
                    navController.navigate("explore")
                },
                navController = navController,
                card = attachedCard,
                isChoosing = true
            )
        }
    }
    attachedPhotos?.ifNotEmpty?.let { photos ->
        val state = rememberLazyListState()
        var viewport by rememberStateOf(Size(0f, 0f))

        LazyRow(
            state = state,
            horizontalArrangement = Arrangement.spacedBy(
                PaddingDefault,
                if (isMe) Alignment.End else Alignment.Start
            ),
            verticalAlignment = Alignment.Bottom,
            contentPadding = PaddingValues(PaddingDefault),
            modifier = Modifier.let {
                if (isReply) {
                    it
                } else {
                    it.fillMaxWidth()
                }
                    .onPlaced { viewport = it.boundsInParent().size }
                    .horizontalFadingEdge(viewport, state, 12f)
            }
        ) {
            items(photos, key = { it }) { photo ->
                var isLoaded by rememberStateOf(false)
                val data = api.url(photo)
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(data)
                        .crossfade(true)
                        .build(),
                    alpha = if (isLoaded) 1f else .125f,
                    placeholder = rememberVectorPainter(Icons.Outlined.Photo),
                    onSuccess = {
                        isLoaded = true
                    },
                    contentDescription = "",
                    contentScale = if (isLoaded) ContentScale.Fit else ContentScale.Inside,
                    alignment = Alignment.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(MaterialTheme.shapes.large)
                        .background(MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp /* Card elevation */))
                        .combinedClickable(
                            onClick = { onShowPhoto(photo) },
                            onLongClick = {
                                selectedBitmap = data
                                onShowMessageDialog(true)
                            }
                        )
                        .let {
                            if (isLoaded) {
                                it.heightIn(min = PaddingDefault * 2, max = 320.dp)
                            } else {
                                it.padding(
                                    horizontal = 40.dp,
                                    vertical = 80.dp
                                )
                            }
                        }
                )
            }
        }
    }
    attachedAudio?.let { audioUrl ->
        Card(
            shape = MaterialTheme.shapes.large,
            modifier = Modifier
                .padding(PaddingDefault)
                .let {
                    if (isReply) {
                        it
                    } else {
                        if (isMe) {
                            it.padding(start = PaddingDefault * 8)
                        } else {
                            it.padding(end = PaddingDefault * 8)
                        }
                    }
                }
                .clip(MaterialTheme.shapes.large)
        ) {
            Audio(
                api.url(audioUrl),
                modifier = Modifier
                    .fillMaxSize()
            )
        }
    }
    attachedSticker?.photo?.let { stickerPhoto ->
        StickerPhoto(
            stickerPhoto,
            modifier = Modifier.padding(PaddingDefault).let {
                if (isReply) {
                    it
                } else {
                    when (isMe) {
                        true -> it.padding(start = PaddingDefault * 8)
                            .align(Alignment.End)

                        false -> it.padding(end = PaddingDefault * 8)
                            .align(Alignment.Start)
                    }
                }
            },
            onLongClick = {
                scope.launch {
                    api.sticker(
                        id = attachedSticker?.id ?: return@launch context.showDidntWork(),
                        onError = {
                            when (it.status) {
                                HttpStatusCode.NotFound -> context.toast(R.string.sticker_pack_not_found)
                                else -> context.showDidntWork()
                            }
                        }
                    ) {
                        val id = it.pack!!
                        navController.navigate("sticker-pack/$id")
                    }
                }
            }
        ) {
            scope.launch {
                say.say(attachedSticker?.message)
            }
        }
    }
    attachedStoryId?.also { storyId ->
        Card(
            onClick = {
                if (!attachedStoryNotFound) {
                    navController.navigate("story/$storyId")
                }
            },
            shape = MaterialTheme.shapes.large,
            modifier = Modifier.padding(PaddingDefault).let {
                if (isReply) {
                    it
                } else {
                    when (isMe) {
                        true -> it.padding(start = PaddingDefault * 8)
                            .align(Alignment.End)

                        false -> it.padding(end = PaddingDefault * 8)
                            .align(Alignment.Start)
                    }
                }
            }
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(PaddingDefault / 2),
                modifier = Modifier
                    .padding(PaddingDefault * 2)
            ) {
                attachedStory?.let { story ->
                    Text(
                        story.title ?: "",
                        style = MaterialTheme.typography.headlineSmall
                    )
                    StoryAuthors(
                        navController,
                        story.publishDate,
                        story.authors ?: emptyList()
                    )
                    Text(story.textContent(), maxLines = 3, overflow = TextOverflow.Ellipsis)
                } ?: run {
                    Text(
                        if (attachedStoryNotFound) stringResource(R.string.story_not_found) else stringResource(R.string.please_wait),
                        style = MaterialTheme.typography.headlineSmall,
                        modifier = Modifier.alpha(0.5f)
                    )
                }
            }
        }
    }
    if (message.text != null) {
        LinkifyText(
            message.text ?: "",
            color = if (isMeActual) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onBackground,
            modifier = Modifier
                .align(if (isMe) Alignment.End else Alignment.Start)
                .padding(PaddingDefault)
                .let {
                    if (isReply) {
                        it
                    } else {
                        when (isMe) {
                            true -> it.padding(start = PaddingDefault * 8)
                            false -> it.padding(end = PaddingDefault * 8)
                        }
                    }
                }
                .clip(MaterialTheme.shapes.large)
                .background(if (isMeActual) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.background)
                .border(
                    if (isMeActual) 0.dp else 1.dp,
                    MaterialTheme.colorScheme.secondaryContainer,
                    MaterialTheme.shapes.large
                )
                .combinedClickable(
                    onClick = { onShowTime(!showTime) },
                    onLongClick = { onShowMessageDialog(true) }
                )
                .padding(PaddingDefault * 2, PaddingDefault)
        )
    }

    AnimatedVisibility(showTime, modifier = Modifier.align(if (isMe) Alignment.End else Alignment.Start)) {
        Text(
            "${message.createdAt!!.timeAgo()}, ${getPerson(message.member!!)?.name ?: stringResource(R.string.someone)}",
            color = MaterialTheme.colorScheme.secondary,
            style = MaterialTheme.typography.bodySmall,
            textAlign = if (isMe) TextAlign.End else TextAlign.Start,
            modifier = Modifier
                .padding(horizontal = PaddingDefault)
                .let {
                    if (isReply) {
                        it.padding(bottom = PaddingDefault / 2)
                    } else {
                        it
                    }
                }
        )
    }
}
