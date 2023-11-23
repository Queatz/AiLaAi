package com.queatz.ailaai.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.layout.boundsInParent
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.navigation.NavController
import app.ailaai.api.*
import com.queatz.ailaai.R
import com.queatz.ailaai.api.sendAudioFromUri
import com.queatz.ailaai.api.sendMediaFromUri
import com.queatz.ailaai.api.sendVideosFromUri
import com.queatz.ailaai.data.api
import com.queatz.ailaai.data.getAttachment
import com.queatz.ailaai.data.json
import com.queatz.ailaai.extensions.*
import com.queatz.ailaai.group.GroupJoinRequest
import com.queatz.ailaai.helpers.OnStart
import com.queatz.ailaai.helpers.audioRecorder
import com.queatz.ailaai.services.*
import com.queatz.ailaai.ui.components.*
import com.queatz.ailaai.ui.dialogs.*
import com.queatz.ailaai.ui.stickers.StickerPacks
import com.queatz.ailaai.ui.theme.ElevationDefault
import com.queatz.ailaai.ui.theme.PaddingDefault
import com.queatz.db.*
import io.ktor.utils.io.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock.System.now
import kotlinx.datetime.Instant
import kotlinx.datetime.Instant.Companion.fromEpochMilliseconds
import kotlinx.serialization.encodeToString
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupScreen(groupId: String, navController: NavController, me: () -> Person?) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var sendMessage by remember { mutableStateOf("") }
    var groupExtended by remember { mutableStateOf<GroupExtended?>(null) }
    var messages by remember { mutableStateOf<List<Message>>(listOf()) }
    var isLoading by rememberStateOf(false)
    var showGroupNotFound by rememberStateOf(false)
    var showLeaveGroup by rememberStateOf(false)
    var showManageDialog by rememberStateOf(false)
    var showChangeGroupStatus by rememberStateOf(false)
    var showReportDialog by rememberStateOf(false)
    var showDescriptionDialog by rememberStateOf(false)
    var showRenameGroup by rememberStateOf(false)
    var showGroupMembers by rememberStateOf(false)
    var showRemoveGroupMembers by rememberStateOf(false)
    var showInviteMembers by rememberStateOf(false)
    var showJoinDialog by rememberStateOf(false)
    var showSnoozeDialog by rememberStateOf(false)
    var showPhoto by remember { mutableStateOf<String?>(null) }
    var stageReply by remember { mutableStateOf<Message?>(null) }
    var showDescription by remember { mutableStateOf(ui.getShowDescription(groupId)) }
    val focusRequester = remember { FocusRequester() }
    var hasOlderMessages by rememberStateOf(true)
    var isRecordingAudio by rememberStateOf(false)
    var showMore by rememberStateOf(false)
    var recordingAudioDuration by rememberStateOf(0L)
    var maxInputAreaHeight by rememberStateOf(0f)
    val stickerPacks by stickers.rememberStickerPacks()
    var selectedMessages by rememberStateOf(emptySet<Message>())

    val allJoinRequests by joins.joins.collectAsState()
    val myJoinRequests by joins.myJoins.collectAsState()
    var joinRequests by remember {
        mutableStateOf(emptyList<JoinRequestAndPerson>())
    }

    LaunchedEffect(allJoinRequests) {
        joinRequests = allJoinRequests.filter { it.joinRequest?.group == groupId }
    }

    LaunchedEffect(Unit) {
        push.clear(groupId)
    }

    LaunchedEffect(Unit) {
        if (stickerPacks.isNullOrEmpty()) {
            stickers.reload()
        }
    }

    suspend fun reloadMessages() {
        api.messages(groupId) {
            messages = it
        }
    }

    val audioRecorder = audioRecorder(
        { isRecordingAudio = it },
        { recordingAudioDuration = it },
    ) { file ->
        api.sendAudioFromUri(groupId, file, stageReply?.id?.let {
            Message(attachments = listOf(json.encodeToString(ReplyAttachment(it))))
        }) {
            stageReply = null
            reloadMessages()
        }
    }

    suspend fun loadMore() {
        if (!hasOlderMessages || messages.isEmpty()) {
            return
        }

        val oldest = messages.lastOrNull()?.createdAt ?: return
        api.messagesBefore(groupId, oldest) { older ->
            val newMessages = (messages + older).distinctBy { it.id }

            if (messages.size == newMessages.size) {
                hasOlderMessages = false
            } else {
                messages = newMessages
            }
        }
    }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.PickMultipleVisualMedia()) { uris ->
        if (uris.isNotEmpty()) {
            scope.launch {
                val videos = uris.filter { it.isVideo(context) }
                val photos = uris.filter { it.isPhoto(context) }

                if (photos.isNotEmpty()) {
                    api.sendMediaFromUri(
                        context,
                        groupId,
                        uris,
                        stageReply?.id?.let {
                            Message(attachments = listOf(json.encodeToString(ReplyAttachment(it))))
                        }
                    )
                }

                if (videos.isNotEmpty()) {
                    api.sendVideosFromUri(
                        context,
                        groupId,
                        videos,
                        if (photos.isEmpty()) {
                            stageReply?.id?.let {
                                Message(attachments = listOf(json.encodeToString(ReplyAttachment(it))))
                            }
                        } else {
                            null
                        },
                        processingCallback = {

                        },
                        uploadCallback = {

                        }
                    )
                }

                stageReply = null
                reloadMessages()
            }
        }
    }

    LaunchedEffect(Unit) {
        isLoading = true
        api.group(
            groupId,
            onError = { ex ->
                if (ex is CancellationException || ex is InterruptedException) {
                    // Ignore
                } else {
                    showGroupNotFound = true
                }
            }) {
            groupExtended = it
        }
        isLoading = false
    }

    suspend fun reload() {
        api.group(groupId) {
            groupExtended = it
        }
    }

    OnStart {
        reloadMessages()
    }

    LaunchedEffect(Unit) {
        push.latestMessage
            .filter { it != null }
            .conflate()
            .catch { it.printStackTrace() }
            .onEach {
                reloadMessages()
            }
            .launchIn(scope)
    }

    Column(
        verticalArrangement = Arrangement.Bottom,
        modifier = Modifier.fillMaxSize()
    ) {
        if (groupExtended == null) {
            if (isLoading) {
                Loading()
            }
        } else {
            val allMembers = groupExtended!!.members
                ?.sortedByDescending { it.person?.seen ?: fromEpochMilliseconds(0) }
                ?: emptyList()
            val myMember = groupExtended!!.members?.find { it.person?.id == me()?.id }
            val otherMembers = groupExtended!!.members?.filter { it.person?.id != me()?.id } ?: emptyList()
            val state = rememberLazyListState()

            var latestMessage by remember { mutableStateOf<Instant?>(null) }

            LaunchedEffect(messages) {
                val latest = messages.firstOrNull()?.createdAt
                if (latestMessage == null || (latest != null && latestMessage!! < latest)) {
                    state.animateScrollToItem(0)
                }
                latestMessage = latest
            }

            LaunchedEffect(sendMessage) {
                if (showMore && sendMessage.isNotBlank()) {
                    showMore = false
                }
            }

            fun snooze(snoozed: Boolean) {
                scope.launch {
                    api.updateMember(myMember!!.member!!.id!!, Member(snoozed = snoozed)) {
                        context.toast(if (snoozed) R.string.group_snoozed else R.string.group_unsnoozed)
                        reload()
                    }
                }
            }

            fun snooze(snoozedUntil: Instant) {
                scope.launch {
                    api.updateMember(myMember!!.member!!.id!!, Member(snoozedUntil = snoozedUntil)) {
                        context.toast(R.string.group_snoozed)
                        reload()
                    }
                }
            }

            TopAppBar(
                {
                    Column(
                        modifier = Modifier
                            .clickable(
                                interactionSource = MutableInteractionSource(),
                                indication = null
                            ) {
                                if (state.firstVisibleItemIndex > 2) {
                                    scope.launch {
                                        state.scrollToTop()
                                    }
                                } else {
                                    if (otherMembers.size == 1) {
                                        navController.navigate("profile/${otherMembers.first().person!!.id!!}")
                                    } else {
                                        showGroupMembers = true
                                    }
                                }
                            }
                    ) {
                        val someone = stringResource(R.string.someone)
                        val emptyGroup = stringResource(R.string.empty_group_name)
                        Text(
                            groupExtended!!.name(
                                someone,
                                emptyGroup,
                                me()?.id?.let(::listOf) ?: emptyList()
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        val details = listOfNotNull(
                            if (groupExtended?.group?.open == true) stringResource(R.string.open_group) else null,
                            groupExtended?.seenText(stringResource(R.string.active), me())
                        )

                        if (details.isNotEmpty()) {
                            Text(
                                details.joinToString(" • "),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                    }
                },
                navigationIcon = {
                    BackButton(navController)
                },
                actions = {
                    var showMenu by rememberStateOf(false)

                    val isSnoozed =
                        myMember?.member?.snoozed == true || myMember?.member?.snoozedUntil?.takeIf { it > now() } != null

                    if (!showDescription && groupExtended?.group?.description?.isBlank() == false) {
                        IconButton({
                            showDescription = !showDescription
                            ui.setShowDescription(groupId, showDescription)
                        }) {
                            Icon(Icons.Outlined.Info, stringResource(R.string.introduction))
                        }
                    }

                    if (isSnoozed) {
                        IconButton({
                            snooze(false)
                        }) {
                            Icon(
                                Icons.Outlined.NotificationsPaused,
                                stringResource(R.string.unsnooze),
                                tint = MaterialTheme.colorScheme.tertiary.copy(alpha = .5f)
                            )
                        }
                    }

                    IconButton({
                        showMenu = !showMenu
                    }) {
                        Icon(Icons.Outlined.MoreVert, stringResource(R.string.more))
                    }

                    Dropdown(showMenu, { showMenu = false }) {
                        if (myMember != null) {
                            DropdownMenuItem({
                                Text(stringResource(R.string.invite_someone))
                            }, {
                                showMenu = false
                                showInviteMembers = true
                            })
                        }
                        DropdownMenuItem({
                            Text(stringResource(R.string.members))
                        }, {
                            showMenu = false
                            showGroupMembers = true
                        })
                        if (myMember != null) {
                            DropdownMenuItem({
                                Text(stringResource(R.string.rename))
                            }, {
                                showMenu = false
                                showRenameGroup = true
                            })
                            DropdownMenuItem({
                                Text(stringResource(R.string.introduction))
                            }, {
                                showMenu = false
                                showDescriptionDialog = true
                            })
                            if (myMember.member?.host == true) {
                                DropdownMenuItem({
                                    Text(stringResource(R.string.manage))
                                }, {
                                    showMenu = false
                                    showManageDialog = true
                                })
                            }
                            DropdownMenuItem({
                                Text(stringResource(R.string.leave))
                            }, {
                                showMenu = false
                                showLeaveGroup = true
                            })
                            val hidden = myMember.member?.hide == true
                            DropdownMenuItem({
                                Text(
                                    if (hidden) stringResource(R.string.show) else stringResource(
                                        R.string.hide
                                    )
                                )
                            }, {
                                scope.launch {
                                    api.updateMember(myMember.member!!.id!!, Member(hide = !hidden)) {
                                        context.toast(R.string.group_hidden)
                                        navController.popBackStack()
                                    }
                                }
                                showMenu = false
                            })
                            if (isSnoozed) {
                                DropdownMenuItem({
                                    Column {
                                        Text(stringResource(R.string.unsnooze))
                                        Text(
                                            if (myMember.member?.snoozed == true) {
                                                stringResource(R.string.indefinitely)
                                            } else {
                                                stringResource(R.string.until_x, myMember.member?.snoozedUntil?.formatFuture() ?: "")
                                            },
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.secondary
                                        )
                                    }
                                }, {
                                    showMenu = false
                                    snooze(false)
                                })
                            } else {
                                DropdownMenuItem({ Text(stringResource(R.string.snooze)) }, {
                                    showMenu = false
                                    showSnoozeDialog = true
                                })
                            }
                            DropdownMenuItem({ Text(stringResource(R.string.report)) }, {
                                showMenu = false
                                showReportDialog = true
                            })
                        }
                    }
                },
                modifier = Modifier.zIndex(1f)
            )
            if (joinRequests.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .padding(horizontal = PaddingDefault)
                        .shadow(1.dp, MaterialTheme.shapes.large)
                        .clip(MaterialTheme.shapes.large)
                        .background(MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp))
                ) {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .wrapContentHeight()
                            .heightIn(max = 160.dp),
                    ) {
                        items(joinRequests) {
                            GroupJoinRequest(it, navController) {
                                scope.launch {
                                    reload()
                                }
                            }
                        }
                    }
                }
            } else {
                AnimatedVisibility(showDescription && groupExtended?.group?.description?.isBlank() == false) {
                    OutlinedCard(
                        onClick = {
                            showDescription = false
                            ui.setShowDescription(groupId, showDescription)
                        },
                        shape = MaterialTheme.shapes.large,
                        elevation = CardDefaults.elevatedCardElevation(ElevationDefault),
                        modifier = Modifier
                            .padding(PaddingDefault)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(PaddingDefault, Alignment.End),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            var viewport by remember { mutableStateOf(Size(0f, 0f)) }
                            val textScrollState = rememberScrollState()
                            LinkifyText(
                                groupExtended?.group?.description ?: "",
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(max = 128.dp)
                                    .verticalScroll(textScrollState)
                                    .onPlaced { viewport = it.boundsInParent().size }
                                    .fadingEdge(viewport, textScrollState)
                                    .padding(PaddingDefault * 1.5f)
                            )
                            Icon(
                                Icons.Outlined.Close,
                                null,
                                modifier = Modifier
                                    .padding(end = PaddingDefault * 1.5f)
                            )
                        }
                    }
                }
            }
            LazyColumn(reverseLayout = true, state = state, modifier = Modifier.weight(1f)) {
                itemsIndexed(messages, key = { _, it -> it.id!! }) { index, it ->
                    MessageItem(
                        it,
                        index.takeIf { it < messages.lastIndex }?.let { it + 1 }?.let { messages[it] },
                        selectedMessages = selectedMessages,
                        onSelectedChange = { message, selected ->
                            selectedMessages = if (selected) {
                                selectedMessages + message
                            } else {
                                selectedMessages - message
                            }
                        },
                        getPerson = {
                            groupExtended?.members?.find { member -> member.member?.id == it }?.person
                        },
                        getMessage = { messageId ->
                            var message: Message? = messages.find { it.id == messageId }
                            api.message(messageId) {
                                message = it
                            }
                            message
                        },
                        me = myMember?.member?.id,
                        onDeleted = {
                            scope.launch {
                                reloadMessages()
                            }
                        },
                        onReply = { stageReply = it },
                        onShowPhoto = { showPhoto = it },
                        navController = navController,
                    )
                }
                item {
                    AnimatedVisibility(hasOlderMessages && messages.isNotEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(PaddingDefault * 2)
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier
                                    .align(Alignment.Center)
                            )
                            LaunchedEffect(Unit) {
                                loadMore()
                            }
                        }
                    }
                }
            }

            fun send() {
                if (sendMessage.isNotBlank()) {
                    val text = sendMessage.trim()
                    val stagedReply = stageReply
                    scope.launch {
                        api.sendMessage(groupId, Message(text = text).also {
                            it.attachments = stagedReply?.id?.let { listOf(json.encodeToString(ReplyAttachment(it))) }
                        }, onError = {
                            if (sendMessage.isBlank()) {
                                sendMessage = text
                            }
                            if (stageReply == null) {
                                stageReply = stagedReply
                            }
                            context.showDidntWork()
                        }) {
                            reloadMessages()
                        }
                    }
                }

                stageReply = null
                sendMessage = ""
                focusRequester.requestFocus()
            }

            fun sendSticker(sticker: Sticker) {
                val stagedReply = stageReply
                scope.launch {
                    api.sendMessage(
                        groupId,
                        Message(
                            attachment = json.encodeToString(
                                StickerAttachment(
                                    photo = sticker.photo,
                                    sticker = sticker.id,
                                    message = sticker.message
                                )
                            )
                        ).also {
                            it.attachments =
                                stagedReply?.id?.let { listOf(json.encodeToString(ReplyAttachment(it))) }
                        }
                    )
                    reloadMessages()
                }
            }

            if (myMember == null) {
                val joinRequestId = myJoinRequests.find { it.joinRequest?.group == groupId }?.joinRequest?.id

                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(PaddingDefault)
                ) {
                    if (joinRequestId == null) {
                        Button(
                            onClick = {
                                showJoinDialog = true
                            }
                        ) {
                            Text(stringResource(R.string.join_group))
                        }
                    } else {
                        OutlinedButton(
                            onClick = {
                                scope.launch {
                                    joins.delete(joinRequestId)
                                }
                            }
                        ) {
                            Text(stringResource(R.string.cancel_join_request))
                        }
                    }
                }
            } else {
                AnimatedVisibility(stageReply != null) {
                    var stagedReply by remember { mutableStateOf(stageReply) }
                    stagedReply = stageReply ?: stagedReply
                    stagedReply?.let { message ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .padding(
                                    top = PaddingDefault,
                                    start = PaddingDefault,
                                    end = PaddingDefault
                                )
                                .clip(MaterialTheme.shapes.large)
                                .background(MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp))
                        ) {
                            Icon(
                                Icons.Outlined.Reply,
                                null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = .5f),
                                modifier = Modifier
                                    .padding(start = PaddingDefault * 2, end = PaddingDefault)
                                    .requiredSize(16.dp)
                            )
                            Text(
                                message.text ?: message.attachmentText(context) ?: "",
                                style = MaterialTheme.typography.bodySmall,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(PaddingDefault / 2)
                                    .heightIn(max = 64.dp)
                            )
                            IconButton(
                                onClick = {
                                    stageReply = null
                                }
                            ) {
                                Icon(
                                    Icons.Outlined.Close,
                                    null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                val showTools = isRecordingAudio || selectedMessages.isNotEmpty()

                Row(
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .onPlaced {
                            maxInputAreaHeight = maxInputAreaHeight.coerceAtLeast(it.boundsInParent().size.height)
                        }
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                    ) {
                        Crossfade(showTools, label = "") { show ->
                            when (show) {
                                true -> {
                                    if (selectedMessages.isNotEmpty()) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier
                                                .heightIn(min = maxInputAreaHeight.inDp())
                                        ) {
                                            Text(
                                                pluralStringResource(
                                                    R.plurals.x_selected,
                                                    selectedMessages.size,
                                                    selectedMessages.size
                                                ),
                                                overflow = TextOverflow.Ellipsis,
                                                textAlign = TextAlign.Center,
                                                color = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier
                                                    .padding(PaddingDefault)
                                                    .fillMaxWidth()
                                            )
                                        }
                                    } else if (isRecordingAudio) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier
                                                .heightIn(min = maxInputAreaHeight.inDp())
                                        ) {
                                            IconButton(
                                                {
                                                    audioRecorder.cancelRecording()
                                                }
                                            ) {
                                                Icon(
                                                    Icons.Outlined.Delete,
                                                    stringResource(R.string.discard_recording),
                                                    tint = MaterialTheme.colorScheme.error
                                                )
                                            }
                                            Text(
                                                stringResource(
                                                    R.string.recording_audio,
                                                    recordingAudioDuration.formatTime()
                                                ),
                                                overflow = TextOverflow.Ellipsis,
                                                textAlign = TextAlign.Center,
                                                color = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier
                                                    .padding(PaddingDefault)
                                                    .fillMaxWidth()
                                            )
                                        }
                                    }
                                }

                                else -> {
                                    OutlinedTextField(
                                        value = sendMessage,
                                        onValueChange = {
                                            sendMessage = it
                                        },
                                        trailingIcon = {
                                            Crossfade(targetState = sendMessage.isNotBlank()) { show ->
                                                when (show) {
                                                    true -> IconButton({ send() }) {
                                                        Icon(
                                                            Icons.Default.Send,
                                                            Icons.Default.Send.name,
                                                            tint = MaterialTheme.colorScheme.primary
                                                        )
                                                    }

                                                    false -> {}
                                                }
                                            }
                                        },
                                        placeholder = {
                                            Text(
                                                stringResource(R.string.message),
                                                modifier = Modifier.alpha(.5f)
                                            )
                                        },
                                        keyboardOptions = KeyboardOptions(
                                            capitalization = KeyboardCapitalization.Sentences,
                                            imeAction = ImeAction.Default
                                        ),
                                        keyboardActions = KeyboardActions(
                                            onSend = {
                                                send()
                                            }
                                        ),
                                        shape = MaterialTheme.shapes.large,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .heightIn(max = 128.dp)
                                            .padding(PaddingDefault)
                                            .focusRequester(focusRequester)
                                    )
                                }
                            }
                        }
                    }
                    AnimatedVisibility(sendMessage.isBlank()) {
                        Row {
                            if (selectedMessages.isNotEmpty()) {
                                IconButton(
                                    {
                                        selectedMessages.sortedBy {
                                            it.createdAt
                                        }.joinToString("\n") { it.text ?: "" }
                                            .copyToClipboard(context)
                                        context.toast(R.string.copied)
                                        selectedMessages = emptySet()
                                    }
                                ) {
                                    Icon(
                                        Icons.Outlined.ContentCopy,
                                        null
                                    )
                                }
                                IconButton(
                                    {
                                        selectedMessages = emptySet()
                                    }
                                ) {
                                    Icon(
                                        Icons.Outlined.Close,
                                        null
                                    )
                                }
                            } else {
                                IconButton(
                                    onClick = {
                                        if (isRecordingAudio) {
                                            audioRecorder.sendActiveRecording()
                                        } else {
                                            audioRecorder.recordAudio()
                                        }
                                    }
                                ) {
                                    Icon(
                                        if (isRecordingAudio) Icons.Default.Send else Icons.Outlined.Mic,
                                        stringResource(R.string.record_audio),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            AnimatedVisibility(!showTools) {
                                Row {
                                    IconButton(
                                        onClick = {
                                            launcher.launch(PickVisualMediaRequest())
                                        }
                                    ) {
                                        Icon(
                                            Icons.Outlined.Photo,
                                            stringResource(R.string.add),
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    IconButton(
                                        onClick = {
                                            showMore = !showMore
                                        },
                                        modifier = Modifier
                                            .padding(end = PaddingDefault)
                                    ) {
                                        Icon(
                                            if (showMore) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
                                            null,
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                AnimatedVisibility(showMore) {
                    Box(
                        modifier = Modifier.height(240.dp)
                            .background(MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp))
                            .shadow(0.25.dp)
                    ) {
                        StickerPacks(
                            stickerPacks ?: emptyList(),
                            onStickerLongClick = {
                                scope.launch {
                                    say.say(it.message)
                                }
                            },
                            onStickerPack = {
                                navController.navigate("sticker-pack/${it.id!!}")
                            },
                            modifier = Modifier.fillMaxSize()
                        ) { sticker ->
                            showMore = false
                            focusRequester.requestFocus()
                            sendSticker(sticker)
                        }
                        FloatingActionButton(
                            onClick = {
                                navController.navigate("sticker-packs")
                            },
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(bottom = PaddingDefault * 2, end = PaddingDefault * 2)
                                .size(32.dp + PaddingDefault)
                        ) {
                            Icon(Icons.Outlined.Add, null, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }

            if (showPhoto != null) {
                PhotoDialog(
                    {
                        showPhoto = null
                    },
                    Media.Photo(showPhoto!!),
                    messages.photos().map { Media.Photo(it) }
                )
            }

            if (showGroupMembers) {
                PeopleDialog(
                    stringResource(R.string.members),
                    {
                        showGroupMembers = false
                    },
                    people = allMembers.map { it.person!! },
                    infoFormatter = { person ->
                        person.seenText(context.getString(R.string.active))
                    },
                    extraButtons = {
                        if (myMember?.member?.host == true) {
                            TextButton(
                                {
                                    showGroupMembers = false
                                    showRemoveGroupMembers = true
                                }
                            ) {
                                Text(stringResource(R.string.manage))
                            }
                        }
                    }
                ) {
                    showGroupMembers = false
                    navController.navigate("profile/${it.id!!}")
                }
            }

            if (showRemoveGroupMembers) {
                val someone = stringResource(R.string.someone)
                val members = groupExtended!!.members!!
                    .mapNotNull { it.person?.id }
                    .filter { it != me()?.id }
                ChoosePeopleDialog(
                    {
                        showRemoveGroupMembers = false
                    },
                    title = stringResource(R.string.manage),
                    confirmFormatter = defaultConfirmFormatter(
                        R.string.remove,
                        R.string.remove_person,
                        R.string.remove_people,
                        R.string.remove_x_people
                    ) { it.name ?: someone },
                    onPeopleSelected = { people ->
                        var anySucceeded = false
                        var anyFailed = false
                        people.forEach { person ->
                            api.removeMember(
                                otherMembers.find { member -> member.person?.id == person.id }?.member?.id
                                    ?: return@forEach,
                                onError = {
                                    anyFailed = true
                                }
                            ) {
                                context.toast(
                                    context.getString(
                                        R.string.x_removed,
                                        person.name?.nullIfBlank ?: someone
                                    )
                                )
                                anySucceeded = true
                            }
                        }
                        if (anySucceeded) {
                            reload()
                        }
                        if (anyFailed) {
                            context.showDidntWork()
                        }
                    },
                    omit = { it.id!! !in members }
                )
            }

            if (showReportDialog) {
                ReportDialog("group/$groupId") {
                    showReportDialog = false
                }
            }

            if (showSnoozeDialog) {
                Menu({
                    showSnoozeDialog = false
                }) {
                    menuItem(stringResource(R.string.for_an_hour)) {
                        showSnoozeDialog = false
                        snooze(now() + 1.hours)
                    }
                    menuItem(stringResource(R.string.for_3_hours)) {
                        showSnoozeDialog = false
                        snooze(now() + 3.hours)
                    }
                    menuItem(stringResource(R.string.for_6_hours)) {
                        showSnoozeDialog = false
                        snooze(now() + 6.hours)
                    }
                    menuItem(stringResource(R.string.for_12_hours)) {
                        showSnoozeDialog = false
                        snooze(now() + 12.hours)
                    }
                    menuItem(stringResource(R.string.for_a_day)) {
                        showSnoozeDialog = false
                        snooze(now() + 1.days)
                    }
                    menuItem(stringResource(R.string.for_a_week)) {
                        showSnoozeDialog = false
                        snooze(now() + 7.days)
                    }
                    menuItem(stringResource(R.string.for_a_month)) {
                        showSnoozeDialog = false
                        snooze(now() + 30.days)
                    }
                    menuItem(stringResource(R.string.indefinitely)) {
                        showSnoozeDialog = false
                        snooze(true)
                    }
                }
            }

            if (showInviteMembers) {
                val someone = stringResource(R.string.someone)
                val omit = groupExtended!!.members!!.mapNotNull { it.person?.id }
                ChoosePeopleDialog(
                    {
                        showInviteMembers = false
                    },
                    title = stringResource(R.string.invite_someone),
                    confirmFormatter = defaultConfirmFormatter(
                        R.string.invite_someone,
                        R.string.invite_person,
                        R.string.invite_x_and_y,
                        R.string.invite_x_people
                    ) { it.name ?: someone },
                    onPeopleSelected = { people ->
                        var anySucceeded = false
                        var anyFailed = false
                        people.forEach { person ->
                            api.createMember(Member().apply {
                                from = person.id!!
                                to = groupId
                            }, onError = { anyFailed = true }) {
                                context.toast(
                                    context.getString(
                                        R.string.person_invited,
                                        person.name?.nullIfBlank ?: someone
                                    )
                                )
                                anySucceeded = true
                            }
                        }
                        if (anySucceeded) {
                            reload()
                        }
                        if (anyFailed) {
                            context.showDidntWork()
                        }
                    },
                    omit = { it.id!! in omit }
                )
            }

            val recomposeScope = currentRecomposeScope

            if (showRenameGroup) {
                RenameGroupDialog({
                    showRenameGroup = false
                }, groupExtended!!.group!!, {
                    groupExtended!!.group = it
                    recomposeScope.invalidate()
                })
            }

            if (showDescriptionDialog) {
                GroupDescriptionDialog({
                    showDescriptionDialog = false
                }, groupExtended!!.group!!, {
                    groupExtended!!.group = it
                    recomposeScope.invalidate()
                })
            }

            if (showLeaveGroup) {
                AlertDialog(
                    {
                        showLeaveGroup = false
                    },
                    title = {
                        Text(stringResource(R.string.leave_group))
                    },
                    text = {
                    },
                    confirmButton = {
                        TextButton({
                            scope.launch {
                                api.removeMember(myMember!!.member!!.id!!) {
                                    showLeaveGroup = false
                                    navController.popBackStack()
                                }
                            }
                        }) {
                            Text(stringResource(R.string.leave))
                        }
                    },
                    dismissButton = {
                        TextButton({
                            showLeaveGroup = false
                        }) {
                            Text(stringResource(R.string.cancel))
                        }
                    },
                )
            }

            if (showGroupNotFound) {
                AlertDialog(
                    {
                        showGroupNotFound = false
                    },
                    title = {
                        Text(stringResource(R.string.group_not_found))
                    },
                    text = {
                    },
                    confirmButton = {
                        TextButton({
                            showGroupNotFound = false
                            navController.popBackStack()
                        }) {
                            Text(stringResource(R.string.leave))
                        }
                    }
                )
            }

            if (showJoinDialog) {
                TextFieldDialog(
                    {
                        showJoinDialog = false
                    },
                    title = stringResource(R.string.join_group),
                    button = stringResource(R.string.send_request),
                    placeholder = stringResource(R.string.message),
                    requireModification = false,
                    showDismiss = true
                ) {
                    joins.join(groupId, it)
                    showJoinDialog = false
                }
            }

            if (showManageDialog) {
                Menu(
                    { showManageDialog = false }
                ) {
                    if (groupExtended?.group?.open == true) {
                        menuItem(stringResource(R.string.make_group_closed)) {
                            showManageDialog = false
                            showChangeGroupStatus = true
                        }
                    } else {
                        menuItem(stringResource(R.string.make_group_open)) {
                            showManageDialog = false
                            showChangeGroupStatus = true
                        }
                    }
                }
            }

            if (showChangeGroupStatus) {
                val open = groupExtended?.group?.open == true
                AlertDialog(
                    {
                        showChangeGroupStatus = false
                    },
                    title = {
                        if (!open) Text(stringResource(R.string.action_open_group))
                        else Text(stringResource(R.string.action_close_group))
                    },
                    text = {
                        if (!open) Text(stringResource(R.string.make_group_open_description))
                        else Text(stringResource(R.string.make_group_closed_description))
                    },
                    confirmButton = {
                        Button({
                            scope.launch {
                                api.updateGroup(groupExtended!!.group!!.id!!, Group(open = !open)) {
                                    reload()
                                }
                                showChangeGroupStatus = false
                            }
                        }) {
                            if (!open) Text(stringResource(R.string.make_group_open))
                            else Text(stringResource(R.string.make_group_closed))
                        }
                    },
                    dismissButton = {
                        TextButton({
                            showChangeGroupStatus = false
                        }) {
                            Text(stringResource(R.string.cancel))
                        }
                    }
                )
            }
        }
    }
}

// Todo should support videos
private fun List<Message>.photos() =
    flatMap { message -> (message.getAttachment() as? PhotosAttachment)?.photos?.asReversed() ?: emptyList() }

fun Person.seenText(active: String) = seen?.timeAgo()?.let { timeAgo ->
    "$active ${timeAgo.lowercase()}"
}

fun Person.seenText() = seen?.timeAgo()?.lowercase()
