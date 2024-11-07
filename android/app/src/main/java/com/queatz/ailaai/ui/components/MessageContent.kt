package com.queatz.ailaai.ui.components

import android.Manifest
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Reply
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Reply
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.boundsInParent
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmapOrNull
import app.ailaai.api.card
import app.ailaai.api.deleteMessage
import app.ailaai.api.group
import app.ailaai.api.sticker
import app.ailaai.api.updateMessage
import coil.compose.AsyncImage
import coil.imageLoader
import coil.request.ImageRequest
import com.queatz.ailaai.AppNav
import com.queatz.ailaai.R
import com.queatz.ailaai.api.story
import com.queatz.ailaai.data.api
import com.queatz.ailaai.data.getAllAttachments
import com.queatz.ailaai.extensions.appNavigate
import com.queatz.ailaai.extensions.copyToClipboard
import com.queatz.ailaai.extensions.ifNotEmpty
import com.queatz.ailaai.extensions.inDp
import com.queatz.ailaai.extensions.rememberStateOf
import com.queatz.ailaai.extensions.save
import com.queatz.ailaai.extensions.share
import com.queatz.ailaai.extensions.shareAudio
import com.queatz.ailaai.extensions.showDidntWork
import com.queatz.ailaai.extensions.status
import com.queatz.ailaai.extensions.timeAgo
import com.queatz.ailaai.extensions.toast
import com.queatz.ailaai.nav
import com.queatz.ailaai.services.say
import com.queatz.ailaai.trade.ActiveTradeItem
import com.queatz.ailaai.trade.TradeDialog
import com.queatz.ailaai.ui.dialogs.Menu
import com.queatz.ailaai.ui.dialogs.RationaleDialog
import com.queatz.ailaai.ui.dialogs.SelectTextDialog
import com.queatz.ailaai.ui.dialogs.TextFieldDialog
import com.queatz.ailaai.ui.dialogs.menuItem
import com.queatz.ailaai.ui.permission.permissionRequester
import com.queatz.ailaai.ui.screens.exploreInitialCategory
import com.queatz.ailaai.ui.stickers.StickerPhoto
import com.queatz.ailaai.ui.story.StoryCard
import com.queatz.ailaai.ui.theme.pad
import com.queatz.db.AudioAttachment
import com.queatz.db.Bot
import com.queatz.db.Card
import com.queatz.db.CardAttachment
import com.queatz.db.GroupAttachment
import com.queatz.db.GroupExtended
import com.queatz.db.Message
import com.queatz.db.Person
import com.queatz.db.PhotosAttachment
import com.queatz.db.ReplyAttachment
import com.queatz.db.Sticker
import com.queatz.db.StickerAttachment
import com.queatz.db.Story
import com.queatz.db.StoryAttachment
import com.queatz.db.TradeAttachment
import com.queatz.db.TradeExtended
import com.queatz.db.UrlAttachment
import com.queatz.db.VideosAttachment
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.launch
import trade

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ColumnScope.MessageContent(
    message: Message,
    isMe: Boolean,
    isHost: Boolean,
    me: String?,
    showTime: Boolean,
    onShowTime: (Boolean) -> Unit,
    showMessageDialog: Boolean,
    onShowMessageDialog: (Boolean) -> Unit,
    getPerson: (String) -> Person?,
    getBot: (String) -> Bot?,
    getMessage: suspend (String) -> Message?,
    canReply: Boolean,
    onReply: (Message) -> Unit,
    onReplyInNewGroup: (Message) -> Unit,
    onUpdated: () -> Unit,
    onShowPhoto: (String) -> Unit,
    isReply: Boolean = false,
    selected: Boolean = false,
    onSelectedChange: ((Boolean) -> Unit)? = null,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val isMeActual = me == message.member
    var showDeleteMessageDialog by rememberStateOf(false)
    var showEditMessageDialog by rememberStateOf(false)
    var showSelectTextDialog by rememberStateOf<String?>(null)
    var autoSpeakText by rememberStateOf(false)
    var attachedCardId by remember { mutableStateOf<String?>(null) }
    var attachedTradeId by remember { mutableStateOf<String?>(null) }
    var attachedReplyId by remember { mutableStateOf<String?>(null) }
    var attachedStoryId by remember { mutableStateOf<String?>(null) }
    var attachedGroupId by remember { mutableStateOf<String?>(null) }
    var attachedPhotos by remember { mutableStateOf<List<String>?>(null) }
    var attachedVideos by remember { mutableStateOf<List<String>?>(null) }
    var attachedSticker by remember { mutableStateOf<Sticker?>(null) }
    var attachedTrade by remember { mutableStateOf<TradeExtended?>(null) }
    var attachedCard by remember { mutableStateOf<Card?>(null) }
    var attachedReply by remember { mutableStateOf<Message?>(null) }
    var attachedStory by remember { mutableStateOf<Story?>(null) }
    var attachedStoryNotFound by remember { mutableStateOf(false) }
    var attachedGroup by remember { mutableStateOf<GroupExtended?>(null) }
    var attachedGroupNotFound by remember { mutableStateOf(false) }
    var attachedAudio by remember { mutableStateOf<String?>(null) }
    var attachedUrls by remember(message) { mutableStateOf<List<UrlAttachment>>(emptyList()) }
    var selectedBitmap by remember { mutableStateOf<String?>(null) }
    val nav = nav
    val writeExternalStoragePermissionRequester = permissionRequester(Manifest.permission.WRITE_EXTERNAL_STORAGE)
    var showStoragePermissionDialog by rememberStateOf(false)

    if (showStoragePermissionDialog) {
        RationaleDialog(
            {
                showStoragePermissionDialog = false
            },
            stringResource(R.string.permission_request)
        )
    }

    // todo: support multiple attachments of the same type
    // todo: right now the only possibility for 2 attachments is with message replies
    LaunchedEffect(message) {
        message.getAllAttachments().forEach { attachment ->
            when (attachment) {
                is CardAttachment -> {
                    attachedCardId = attachment.card
                }

                is PhotosAttachment -> {
                    attachedPhotos = attachment.photos
                }

                is VideosAttachment -> {
                    attachedVideos = attachment.videos
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

                is GroupAttachment -> {
                    attachedGroupId = attachment.group
                }

                is TradeAttachment -> {
                    attachedTradeId = attachment.trade
                }

                is StickerAttachment -> {
                    attachedSticker = Sticker(
                        photo = attachment.photo,
                        message = attachment.message,
                    ).apply {
                        id = attachment.sticker
                    }
                }
                is UrlAttachment -> {
                    attachedUrls += attachment
                }
            }
        }
    }

    if (showMessageDialog) {
        val messageString = stringResource(R.string.message)
        val savedString = stringResource(R.string.saved)
        Menu(
            {
                onShowMessageDialog(false)
            }
        ) {
            if (canReply) {
                menuItem(stringResource(R.string.reply)) {
                    onShowMessageDialog(false)
                    onReply(message)
                }
            }
            menuItem(stringResource(R.string.reply_in_new_group)) {
                onShowMessageDialog(false)
                onReplyInNewGroup(message)
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
                menuItem(stringResource(R.string.save)) {
                    onShowMessageDialog(false)
                    scope.launch {
                        context.imageLoader.execute(
                            ImageRequest.Builder(context)
                                .data(selectedBitmap!!)
                                .target { drawable ->
                                    drawable.toBitmapOrNull()?.let { bitmap ->
                                        writeExternalStoragePermissionRequester.use(
                                            onPermanentlyDenied = {
                                                showStoragePermissionDialog = true
                                            }
                                        ) {
                                            scope.launch {
                                                bitmap.save(context)?.also {
                                                    context.toast(savedString)
                                                } ?: context.showDidntWork()
                                            }
                                        }
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

            if (message.text?.isNotBlank() == true) {
                menuItem(stringResource(R.string.copy)) {
                    (message.text ?: "").copyToClipboard(context, messageString)
                    context.toast(R.string.copied)
                    onShowMessageDialog(false)
                }
                menuItem(stringResource(R.string.select_text)) {
                    autoSpeakText = false
                    showSelectTextDialog = message.text
                    onShowMessageDialog(false)
                }
                menuItem(stringResource(R.string.speak)) {
                    autoSpeakText = true
                    showSelectTextDialog = message.text
                    onShowMessageDialog(false)
                }
                menuItem(stringResource(R.string.select_multiple)) {
                    onSelectedChange?.invoke(true)
                    onShowMessageDialog(false)
                }
            }

            if ((isMe || isHost) && !isReply) {
                menuItem(stringResource(R.string.edit)) {
                    showEditMessageDialog = true
                    onShowMessageDialog(false)
                }
                menuItem(stringResource(R.string.delete)) {
                    showDeleteMessageDialog = true
                    onShowMessageDialog(false)
                }
            }
        }
    }

    if (showEditMessageDialog) {
        TextFieldDialog(
            onDismissRequest = {
                showEditMessageDialog = false
            },
            title = stringResource(R.string.edit_message),
            button = stringResource(R.string.update),
            singleLine = false,
            showDismiss = true,
            initialValue = message.text.orEmpty(),
        ) { value ->
            api.updateMessage(message.id!!, Message(text = value.trim())) {
                onUpdated()
                showEditMessageDialog = false
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
                                onUpdated()
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

    if (showSelectTextDialog != null) {
        SelectTextDialog(
            onDismissRequest = {
                showSelectTextDialog = null
            },
            text = showSelectTextDialog ?: "",
            autoSpeak = autoSpeakText
        )
    }

    LaunchedEffect(attachedCardId) {
        attachedCardId?.let { cardId ->
            api.card(cardId, onError = {
                // todo show failed to load
            }) { attachedCard = it }
        }
    }

    suspend fun reloadTrade() {
        attachedTradeId?.let { tradeId ->
            api.trade(tradeId, onError = {
                // todo show failed to load
            }) { attachedTrade = it }
        }
    }

    LaunchedEffect(attachedTradeId) {
        reloadTrade()
    }

    LaunchedEffect(attachedReplyId) {
        attachedReplyId?.let { messageId ->
            attachedReply = getMessage(messageId)
        }
    }

    LaunchedEffect(attachedStoryId) {
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

    LaunchedEffect(attachedGroupId) {
        attachedGroupId?.let { groupId ->
            api.group(
                groupId,
                onError = {
                    if (it.status == HttpStatusCode.NotFound) {
                        attachedGroupNotFound = true
                    }
                }
            ) {
                attachedGroup = it
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
                        true -> it.padding(start = 8.pad)
                        false -> it.padding(end = 8.pad)
                    }
                }
                .padding(horizontal = 1.pad)
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
                            true -> it.padding(end = 4.dp + 1.pad)
                            false -> it.padding(start = 4.dp + 1.pad)
                        }
                    }
            ) {
                MessageContent(
                    message = reply,
                    isMe = isMe,
                    isHost = isHost,
                    me = me,
                    showTime = showReplyTime,
                    onShowTime = { showReplyTime = it },
                    showMessageDialog = showReplyMessageDialog,
                    onShowMessageDialog = { showReplyMessageDialog = it },
                    getPerson = getPerson,
                    getBot = getBot,
                    getMessage = getMessage,
                    canReply = canReply,
                    onReply = onReply,
                    onReplyInNewGroup = onReplyInNewGroup,
                    onUpdated = {}, // todo delete from reply
                    onShowPhoto = onShowPhoto,
                    isReply = true
                )
            }
            Icon(
                Icons.AutoMirrored.Outlined.Reply,
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
            .padding(1.pad)
            .widthIn(max = 320.dp)
            .let {
                if (isReply) {
                    it
                } else {
                    when (isMe) {
                        true -> it.padding(start = 12.pad)
                            .align(Alignment.End)

                        false -> it.padding(end = 12.pad)
                            .align(Alignment.Start)
                    }
                }
            }
        ) {
            CardItem(
                {
                    nav.appNavigate(AppNav.Page(it))
                },
                onCategoryClick = {
                    exploreInitialCategory = it
                    nav.appNavigate(AppNav.Explore)
                },
                card = attachedCard,
                isChoosing = true
            )
        }
    }

    attachedPhotos?.ifNotEmpty?.let { photos ->
        Column(
            verticalArrangement = Arrangement.spacedBy(1.pad),
            horizontalAlignment = if (isMe) Alignment.End else Alignment.Start,
            modifier = Modifier
                .padding(1.pad)
                .let {
                    if (isReply) {
                        it
                    } else {
                        when (isMe) {
                            true -> it.padding(start = 12.pad)
                                .align(Alignment.End)

                            false -> it.padding(end = 12.pad)
                                .align(Alignment.Start)
                        }
                    }
                }
        ) {
            photos.forEach {
                PhotoItem(
                    it,
                    onClick = {
                        onShowPhoto(it)
                    },
                    onLongClick = {
                        selectedBitmap = api.url(it)
                        onShowMessageDialog(true)
                    }
                )
            }
        }
    }

    attachedVideos?.ifNotEmpty?.let { videos ->
        Column(
            verticalArrangement = Arrangement.spacedBy(1.pad),
            horizontalAlignment = if (isMe) Alignment.End else Alignment.Start,
            modifier = Modifier
                .padding(1.pad)
                .let {
                    if (isReply) {
                        it
                    } else {
                        when (isMe) {
                            true -> it.padding(start = 12.pad)
                                .align(Alignment.End)

                            false -> it.padding(end = 12.pad)
                                .align(Alignment.Start)
                        }
                    }
                }
        ) {
            videos.forEach {
                var isPlaying by remember {
                    mutableStateOf(false)
                }
                // todo loading state
                Box {
                    Video(
                        it.let(api::url),
                        isPlaying = isPlaying,
                        modifier = Modifier.clip(MaterialTheme.shapes.large).clickable {
                            isPlaying = !isPlaying
                        }
                    )
                    if (!isPlaying) {
                        Icon(
                            Icons.Outlined.PlayArrow,
                            null,
                            modifier = Modifier
                                .padding(1.pad)
                                .align(Alignment.Center)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surface.copy(alpha = .5f))
                                .padding(1.pad)
                        )
                    }
                }
            }
        }
    }

    attachedAudio?.let { audioUrl ->
        Card(
            shape = MaterialTheme.shapes.large,
            modifier = Modifier
                .padding(1.pad)
                .let {
                    if (isReply) {
                        it
                    } else {
                        if (isMe) {
                            it.padding(start = 8.pad)
                        } else {
                            it.padding(end = 8.pad)
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
            modifier = Modifier.padding(1.pad).let {
                if (isReply) {
                    it
                } else {
                    when (isMe) {
                        true -> it.padding(start = 8.pad)
                            .align(Alignment.End)

                        false -> it.padding(end = 8.pad)
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
                        nav.appNavigate(AppNav.StickerPack(it.pack!!))
                    }
                }
            }
        ) {
            scope.launch {
                say.say(attachedSticker?.message)
            }
        }
    }

    attachedGroupId?.let { groupId ->
        Box(
            modifier = Modifier.padding(1.pad).then(
                if (isReply) {
                    Modifier
                } else {
                    when (isMe) {
                        true -> Modifier.padding(start = 8.pad)
                            .align(Alignment.End)

                        false -> Modifier.padding(end = 8.pad)
                            .align(Alignment.Start)
                    }
                }
            )
        ) {
            LoadingText(
                attachedGroup != null,
                if (attachedGroupNotFound) stringResource(R.string.group_not_found) else stringResource(R.string.loading_group)
            ) {
                ContactItem(
                    SearchResult.Group(attachedGroup!!),
                    onChange = {},
                    info = GroupInfo.Members,
                )
            }
        }
    }

    attachedStoryId?.also { storyId ->
        StoryCard(
            attachedStory,
            isLoading = !attachedStoryNotFound,
            modifier = Modifier.padding(1.pad).then(
                if (isReply) {
                    Modifier
                } else {
                    when (isMe) {
                        true -> Modifier.padding(start = 8.pad)
                            .align(Alignment.End)

                        false -> Modifier.padding(end = 8.pad)
                            .align(Alignment.Start)
                    }
                }
            )
        ) {
            if (!attachedStoryNotFound) {
                nav.appNavigate(AppNav.Story(storyId))
            }
        }
    }

    attachedUrls.ifNotEmpty?.let { urls ->
        Column(
            verticalArrangement = Arrangement.spacedBy(1.pad),
            modifier = Modifier
                .padding(1.pad)
                .let {
                    if (isReply) {
                        it
                    } else {
                        if (isMe) {
                            it.padding(start = 8.pad)
                        } else {
                            it.padding(end = 8.pad)
                        }
                    }
                }
        ) {
            urls.forEach {
                UrlPreview(it)
            }
        }
    }

    attachedTrade?.let { trade ->
        var showTradeDialog by rememberStateOf(false)

        if (showTradeDialog) {
            TradeDialog(
                onDismissRequest = {
                    showTradeDialog = false
                },
                tradeId = trade.trade!!.id!!,
                onTradeUpdated = {
                    scope.launch {
                        reloadTrade()
                    }
                },
                onTradeCancelled = {
                    scope.launch {
                        reloadTrade()
                    }
                },
                onTradeCompleted = {
                    scope.launch {
                        reloadTrade()
                    }
                }
            )
        }

        ActiveTradeItem(
            trade,
            modifier = Modifier
                .align(if (isMe) Alignment.End else Alignment.Start)
                .padding(1.pad)
                .let {
                    if (isReply) {
                        it
                    } else {
                        when (isMe) {
                            true -> it.padding(start = 8.pad)
                            false -> it.padding(end = 8.pad)
                        }
                    }
                }
        ) {
            showTradeDialog = true
        }
    }

    if (!message.text.isNullOrBlank()) {
        LinkifyText(
            message.text ?: "",
            color = if (isMeActual) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onBackground,
            modifier = Modifier
                .align(if (isMe) Alignment.End else Alignment.Start)
                .padding(1.pad)
                .let {
                    if (isReply) {
                        it
                    } else {
                        when (isMe) {
                            true -> it.padding(start = 8.pad)
                            false -> it.padding(end = 8.pad)
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
                .let {
                    val selectedBorderDp by animateDpAsState(if (selected) 2.dp else 0.dp)
                    if (selectedBorderDp != 0.dp) {
                        it.border(selectedBorderDp, MaterialTheme.colorScheme.primary, MaterialTheme.shapes.large)
                    } else {
                        it
                    }
                }
                .combinedClickable(
                    onClick = { onShowTime(!showTime) },
                    onLongClick = { onShowMessageDialog(true) }
                )
                .padding(2.pad, 1.pad)
        )
    }

    AnimatedVisibility(showTime, modifier = Modifier.align(if (isMe) Alignment.End else Alignment.Start)) {
        Text(
            "${message.createdAt!!.timeAgo()}, ${message.member?.let { getPerson(it)?.name } ?: message.bot?.let { getBot(it)?.name } ?: stringResource(R.string.someone)}",
            color = MaterialTheme.colorScheme.secondary,
            style = MaterialTheme.typography.bodySmall,
            textAlign = if (isMe) TextAlign.End else TextAlign.Start,
            modifier = Modifier
                .padding(horizontal = 1.pad)
                .let {
                    if (isReply) {
                        it.padding(bottom = .5f.pad)
                    } else {
                        it
                    }
                }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PhotoItem(photo: String, onClick: () -> Unit, onLongClick: () -> Unit) {
    var aspect by remember(photo) {
        mutableFloatStateOf(0.75f)
    }
    var isLoaded by remember(photo) {
        mutableStateOf(false)
    }
    AsyncImage(
        model = ImageRequest.Builder(LocalContext.current)
            .data(api.url(photo))
            .crossfade(true)
            .build(),
        alpha = if (isLoaded) 1f else .125f,
//        placeholder = rememberVectorPainter(Icons.Outlined.Photo),
        contentScale = ContentScale.Fit,
        onSuccess = {
            isLoaded = true
            aspect = it.result.drawable.intrinsicWidth.toFloat() / it.result.drawable.intrinsicHeight.toFloat()
        },
        contentDescription = "",
        alignment = Alignment.Center,
        modifier = Modifier
            .clip(MaterialTheme.shapes.large)
            .background(MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp /* Card elevation */))
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .heightIn(min = 80.dp, max = 320.dp)
            .widthIn(min = 80.dp, max = 320.dp)
            .aspectRatio(aspect)
    )
}
