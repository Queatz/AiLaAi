package com.queatz.ailaai.ui.screens

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavController
import com.queatz.ailaai.GroupExtended
import com.queatz.ailaai.Message
import com.queatz.ailaai.Person
import com.queatz.ailaai.api
import com.queatz.ailaai.ui.components.MessageItem
import com.queatz.ailaai.ui.theme.ElevationDefault
import com.queatz.ailaai.ui.theme.PaddingDefault
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
                        Text(otherMember?.person?.name ?: "Someone")
                        Text(
                            "Active now",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.secondary
                        )
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
                        navController.popBackStack()
                    }) {
                        Icon(Icons.Outlined.AccountCircle, "View card")
                    }

                    IconButton({
                        showMenu = !showMenu
                    }) {
                        Icon(Icons.Outlined.MoreVert, "More")
                    }

                    DropdownMenu(showMenu, { showMenu = false }) {
                        DropdownMenuItem({ Text("Delete") }, { showMenu = false })
                        DropdownMenuItem({ Text("Get help") }, { showMenu = false })
                    }
                },
                colors = TopAppBarDefaults.smallTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                ),
                modifier = Modifier.shadow(ElevationDefault / 2).zIndex(1f)
            )
            LazyColumn(reverseLayout = true, modifier = Modifier.weight(1f)) {
                items(messages) {
                    MessageItem(it, {
                        groupExtended?.members?.find { member -> member.member?.id == it }?.person
                    }, myMember?.member?.id == it.member)
                }
            }

            fun send() {
                if (sendMessage.isNotBlank()) {
                    val text = sendMessage
                    coroutineScope.launch {
                        try {
                            api.sendMessage(groupId, Message(text = text))
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
                    Text("Message")
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
