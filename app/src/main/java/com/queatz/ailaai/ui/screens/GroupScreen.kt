package com.queatz.ailaai.ui.screens

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavController
import com.queatz.ailaai.*
import com.queatz.ailaai.R
import com.queatz.ailaai.extensions.timeAgo
import com.queatz.ailaai.ui.components.MessageItem
import com.queatz.ailaai.ui.theme.ElevationDefault
import com.queatz.ailaai.ui.theme.PaddingDefault
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch

@Composable
fun GroupScreen(navBackStackEntry: NavBackStackEntry, navController: NavController, me: () -> Person?) {
    val groupId = navBackStackEntry.arguments!!.getString("id")!!
    var sendMessage by remember { mutableStateOf("") }
    var groupExtended by remember { mutableStateOf<GroupExtended?>(null) }
    var messages by remember { mutableStateOf<List<Message>>(listOf()) }
    var isLoading by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(true) {
        isLoading = true
        try {
            groupExtended = api.group(groupId)
        } catch (ex: Exception) {
            ex.printStackTrace()
        } finally {
            isLoading = false
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
        push.latestMessage.filter { it != null }.collect {
            try {
                messages = api.messages(groupId)
            } catch (ex: Exception) {
                ex.printStackTrace()
            }
        }
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
            val otherMember = groupExtended!!.members?.find { it.person?.id != me()?.id }

            SmallTopAppBar(
                {
                    Column {
                        Text(otherMember?.person?.name ?: stringResource(R.string.someone))

                        otherMember?.person?.seen?.let {
                            Text(
                                "${stringResource(R.string.active)} ${it.timeAgo()}",
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

                        DropdownMenuItem({ Text(if (hidden) stringResource(R.string.show_conversation) else stringResource(
                                                    R.string.hide_conversation)
                                                ) }, {
                            coroutineScope.launch {
                                try {
                                    api.updateMember(myMember.member!!.id!!, Member(hide = !hidden))

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
                colors = TopAppBarDefaults.smallTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                ),
                modifier = Modifier.shadow(ElevationDefault / 2).zIndex(1f)
            )
            LazyColumn(reverseLayout = true, modifier = Modifier.weight(1f)) {
                items(messages) {
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
                        }
                    )
                }
            }

            fun send() {
                if (sendMessage.isNotBlank()) {
                    val text = sendMessage
                    coroutineScope.launch {
                        try {
                            api.sendMessage(groupId, Message(text = text.trim()))
                            messages = api.messages(groupId)
                        } catch (ex: Exception) {
                            ex.printStackTrace()
                        }
                    }
                }

                sendMessage = ""
            }

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
                shape = MaterialTheme.shapes.large, modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 128.dp)
                    .padding(PaddingDefault)
            )
        }
    }
}
