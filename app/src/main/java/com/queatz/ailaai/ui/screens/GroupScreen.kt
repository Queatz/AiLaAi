package com.queatz.ailaai.ui.screens

import android.app.Activity
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.outlined.AddPhotoAlternate
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.zIndex
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.queatz.ailaai.*
import com.queatz.ailaai.R
import com.queatz.ailaai.extensions.name
import com.queatz.ailaai.extensions.nullIfBlank
import com.queatz.ailaai.extensions.timeAgo
import com.queatz.ailaai.ui.components.MessageItem
import com.queatz.ailaai.ui.dialogs.ChoosePeopleDialog
import com.queatz.ailaai.ui.dialogs.GroupMembersDialog
import com.queatz.ailaai.ui.dialogs.RenameGroupDialog
import com.queatz.ailaai.ui.dialogs.defaultConfirmFormatter
import com.queatz.ailaai.ui.theme.ElevationDefault
import com.queatz.ailaai.ui.theme.PaddingDefault
import io.ktor.utils.io.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.datetime.Instant

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun GroupScreen(navBackStackEntry: NavBackStackEntry, navController: NavController, me: () -> Person?) {
    val groupId = navBackStackEntry.arguments!!.getString("id")!!
    var sendMessage by remember { mutableStateOf("") }
    var groupExtended by remember { mutableStateOf<GroupExtended?>(null) }
    var messages by remember { mutableStateOf<List<Message>>(listOf()) }
    var isLoading by remember { mutableStateOf(false) }
    var showGroupNotFound by remember { mutableStateOf(false) }
    var showLeaveGroup by remember { mutableStateOf(false) }
    var showRenameGroup by remember { mutableStateOf(false) }
    var showGroupMembers by remember { mutableStateOf(false) }
    var showRemoveGroupMembers by remember { mutableStateOf(false) }
    var showInviteMembers by remember { mutableStateOf(false) }
    var showPhoto by remember { mutableStateOf<String?>(null) }
    val coroutineScope = rememberCoroutineScope()
    val focusRequester = remember { FocusRequester() }
    val context = LocalContext.current

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        coroutineScope.launch {
            try {
                api.sendPhoto(groupId, uri ?: return@launch)
                messages = api.messages(groupId)
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(context, context.getString(R.string.didnt_work), Toast.LENGTH_LONG).show()
            }
        }
    }

    LaunchedEffect(true) {
        isLoading = true

        try {
            groupExtended = api.group(groupId)
            isLoading = false
        } catch (ex: Exception) {
            if (ex is CancellationException || ex is InterruptedException) {
                // Ignore
            } else {
                ex.printStackTrace()
                showGroupNotFound = true
                isLoading = false
            }
        }
    }

    suspend fun reload() {
        try {
            groupExtended = api.group(groupId)
        } catch (ex: Exception) {
            ex.printStackTrace()
        }
    }

    LaunchedEffect(true) {
        try {
            messages = api.messages(groupId)
        } catch (ex: Exception) {
            ex.printStackTrace()
        }
    }

    LaunchedEffect(true) {
        push.latestMessage
            .filter { it != null }
            .conflate()
            .catch { it.printStackTrace() }
            .onEach {
                try {
                    messages = api.messages(groupId)
                } catch (ex: Exception) {
                    ex.printStackTrace()
                }
            }
            .launchIn(coroutineScope)
    }

    Column(
        verticalArrangement = Arrangement.Bottom,
        modifier = Modifier.fillMaxSize()
    ) {
        if (groupExtended == null) {
            if (isLoading) {
                LinearProgressIndicator(
                    color = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(PaddingDefault)
                )
            }
        } else {
            val myMember = groupExtended!!.members?.find { it.person?.id == me()?.id }
            val otherMembers = groupExtended!!.members?.filter { it.person?.id != me()?.id } ?: emptyList()
            val state = rememberLazyListState()

            LaunchedEffect(messages) {
                state.animateScrollToItem(0)
            }

            TopAppBar(
                {
                    Column {
                        val someone = stringResource(R.string.someone)
                        val emptyGroup = stringResource(R.string.empty_group_name)
                        Text(
                            groupExtended!!.name(
                                someone,
                                emptyGroup,
                                me()?.id?.let(::listOf) ?: emptyList()
                            ), maxLines = 1, overflow = TextOverflow.Ellipsis
                        )

                        otherMembers.maxByOrNull {
                            it.person?.seen ?: Instant.fromEpochMilliseconds(0)
                        }?.person?.seen?.let {
                            Text(
                                "${stringResource(R.string.active)} ${it.timeAgo().lowercase()}",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton({
                        navController.popBackStack()
                    }) {
                        Icon(Icons.Outlined.ArrowBack, Icons.Outlined.ArrowBack.name)
                    }
                },
                actions = {
                    var showMenu by remember { mutableStateOf(false) }

                    IconButton({
                        showMenu = !showMenu
                    }) {
                        Icon(Icons.Outlined.MoreVert, stringResource(R.string.more))
                    }

                    DropdownMenu(showMenu, { showMenu = false }) {
                        val hidden = myMember!!.member?.hide == true

                        DropdownMenuItem({
                            Text(stringResource(R.string.invite))
                        }, {
                            showMenu = false
                            showInviteMembers = true
                        })
                        DropdownMenuItem({
                            Text(stringResource(R.string.members))
                        }, {
                            showMenu = false
                            showGroupMembers = true
                        })
                        DropdownMenuItem({
                            Text(stringResource(R.string.rename))
                        }, {
                            showMenu = false
                            showRenameGroup = true
                        })
                        DropdownMenuItem({
                            Text(stringResource(R.string.leave))
                        }, {
                            showMenu = false
                            showLeaveGroup = true
                        })
                        DropdownMenuItem({
                            Text(
                                if (hidden) stringResource(R.string.show) else stringResource(
                                    R.string.hide
                                )
                            )
                        }, {
                            coroutineScope.launch {
                                try {
                                    api.updateMember(myMember.member!!.id!!, Member(hide = !hidden))
                                    Toast.makeText(
                                        navController.context,
                                        navController.context.getString(R.string.group_hidden),
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    navController.popBackStack()
                                } catch (ex: Exception) {
                                    ex.printStackTrace()
                                }
                            }
                            showMenu = false
                        })
//                        DropdownMenuItem({ Text("Get help") }, { showMenu = false })
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                ),
                modifier = Modifier.shadow(ElevationDefault / 2).zIndex(1f)
            )
            LazyColumn(reverseLayout = true, state = state, modifier = Modifier.weight(1f)) {
                items(messages, { it.id!! }) {
                    MessageItem(
                        it,
                        {
                            groupExtended?.members?.find { member -> member.member?.id == it }?.person
                        },
                        myMember?.member?.id == it.member,
                        {
                            coroutineScope.launch {
                                try {
                                    messages = api.messages(groupId)
                                } catch (ex: Exception) {
                                    ex.printStackTrace()
                                }
                            }
                        },
                        onShowPhoto = { showPhoto = it },
                        navController.context as Activity,
                        navController
                    )
                }
            }

            fun send() {
                if (sendMessage.isNotBlank()) {
                    val text = sendMessage.trim()
                    coroutineScope.launch {
                        try {
                            api.sendMessage(groupId, Message(text = text))
                            messages = api.messages(groupId)
                        } catch (ex: Exception) {
                            ex.printStackTrace()
                            if (sendMessage.isBlank()) {
                                sendMessage = text
                            }
                            Toast.makeText(context, context.getString(R.string.didnt_work), Toast.LENGTH_SHORT).show()
                        }
                    }
                }

                sendMessage = ""
                focusRequester.requestFocus()
            }

            Row(
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(modifier = Modifier.weight(1f)) {
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
                            Text(stringResource(R.string.message))
                        },
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Sentences,
                            imeAction = ImeAction.Send
                        ),
                        keyboardActions = KeyboardActions(onSend = {
                            send()
                        }),
                        shape = MaterialTheme.shapes.large,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 128.dp)
                            .padding(PaddingDefault)
                            .focusRequester(focusRequester)
                    )
                }
                AnimatedVisibility(sendMessage.isBlank()) {
                    IconButton(
                        onClick = {
                            launcher.launch("image/*")
                        },
                        modifier = Modifier
                            .padding(end = PaddingDefault)
                    ) {
                        Icon(Icons.Outlined.AddPhotoAlternate, stringResource(R.string.add))
                    }
                }
            }

            if (showPhoto != null) {
                Dialog(
                    {
                        showPhoto = null
                    }, properties = DialogProperties(
                        usePlatformDefaultWidth = false
                    )
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clickable(
                                interactionSource = MutableInteractionSource(),
                                indication = null
                            ) {
                                showPhoto = null
                            }
                    ) {
                        val photos = messages.photos()
                        val photosListState = rememberLazyListState(photos.indexOf(showPhoto))
                        LazyRow(
                            state = photosListState,
                            flingBehavior = rememberSnapFlingBehavior(photosListState),
                            reverseLayout = true,
                            horizontalArrangement = Arrangement.spacedBy(PaddingDefault * 2),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(photos, key = { it }) { photo ->
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .align(Alignment.Center)
                                ) {
                                    AsyncImage(
                                        model = ImageRequest.Builder(LocalContext.current)
                                            .data(api.url(photo))
                                            .crossfade(true)
                                            .build(),
                                        contentDescription = "",
                                        contentScale = ContentScale.Fit,
                                        alignment = Alignment.Center,
                                        modifier = Modifier
                                            .fillParentMaxSize()
                                    )
                                }
                            }
                        }
                    }
                }
            }

            if (showGroupMembers) {
                GroupMembersDialog({
                    showGroupMembers = false
                },
                    otherMembers.map { it.person!! },
                    infoFormatter = { person ->
                        person.seen?.timeAgo()?.let { timeAgo ->
                            "${context.getString(R.string.active)} ${timeAgo.lowercase()}"
                        }
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
                    }) {
                    coroutineScope.launch {
                        val group = api.createGroup(listOf(myMember!!.person!!.id!!, it.id!!), reuse = true)
                        navController.navigate("group/${group.id!!}")
                    }
                }
            }

            if (showRemoveGroupMembers) {
                val context = LocalContext.current
                val didntWork = stringResource(R.string.didnt_work)
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
                            try {
                                api.removeMember(
                                    otherMembers.find { member -> member.person?.id == person.id }?.member?.id
                                        ?: return@forEach
                                )
                                Toast.makeText(
                                    context,
                                    context.getString(R.string.x_removed, person.name?.nullIfBlank ?: someone),
                                    Toast.LENGTH_SHORT
                                ).show()
                                anySucceeded = true
                            } catch (ex: Exception) {
                                anyFailed = true
                                ex.printStackTrace()
                            }
                        }
                        if (anySucceeded) {
                            reload()
                        }
                        if (anyFailed) {
                            Toast.makeText(context, didntWork, Toast.LENGTH_SHORT).show()
                        }
                    },
                    omit = { it.id!! !in members }
                )
            }

            if (showInviteMembers) {
                val context = LocalContext.current
                val didntWork = stringResource(R.string.didnt_work)
                val someone = stringResource(R.string.someone)
                val omit = groupExtended!!.members!!.mapNotNull { it.person?.id }
                ChoosePeopleDialog(
                    {
                        showInviteMembers = false
                    },
                    title = stringResource(R.string.invite),
                    confirmFormatter = defaultConfirmFormatter(
                        R.string.invite,
                        R.string.invite_person,
                        R.string.invite_x_and_y,
                        R.string.invite_x_people
                    ) { it.name ?: someone },
                    onPeopleSelected = { people ->
                        var anySucceeded = false
                        var anyFailed = false
                        people.forEach { person ->
                            try {
                                api.createMember(Member().apply {
                                    from = person.id!!
                                    to = groupId
                                })
                                Toast.makeText(
                                    context,
                                    context.getString(R.string.person_invited, person.name?.nullIfBlank ?: someone),
                                    Toast.LENGTH_SHORT
                                ).show()
                                anySucceeded = true
                            } catch (ex: Exception) {
                                anyFailed = true
                                ex.printStackTrace()
                            }
                        }
                        if (anySucceeded) {
                            reload()
                        }
                        if (anyFailed) {
                            Toast.makeText(context, didntWork, Toast.LENGTH_SHORT).show()
                        }
                    },
                    omit = { it.id!! in omit }
                )
            }

            if (showRenameGroup) {
                val scope = currentRecomposeScope
                RenameGroupDialog({
                    showRenameGroup = false
                }, groupExtended!!.group!!, {
                    groupExtended!!.group = it
                    scope.invalidate()
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
                            coroutineScope.launch {
                                try {
                                    api.removeMember(myMember!!.member!!.id!!)
                                    showLeaveGroup = false
                                    navController.popBackStack()
                                } catch (ex: Exception) {
                                    ex.printStackTrace()
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
        }
    }
}

private fun List<Message>.photos() =
    flatMap { message -> (message.getAttachment() as? PhotosAttachment)?.photos ?: emptyList() }
