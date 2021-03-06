package com.queatz.ailaai.ui.screens

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.window.DialogProperties
import androidx.navigation.NavController
import com.queatz.ailaai.Card
import com.queatz.ailaai.Person
import com.queatz.ailaai.R
import com.queatz.ailaai.api
import com.queatz.ailaai.ui.components.BasicCard
import com.queatz.ailaai.ui.theme.PaddingDefault
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun MeScreen(navController: NavController, me: () -> Person?) {
    var myCards by remember { mutableStateOf(listOf<Card>()) }
    var addedCardId by remember { mutableStateOf<String?>(null) }
    var inviteDialog by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(true) }
    val coroutineScope = rememberCoroutineScope()
    var inviteCode by remember { mutableStateOf("") }
    val state = rememberLazyListState()

    val errorString = stringResource(R.string.error)

    LaunchedEffect(inviteDialog) {
        if (inviteDialog) {
            inviteCode = ""

            try {
                inviteCode = api.invite().code ?: ""
            } catch (ex: Exception) {
                ex.printStackTrace()
                inviteCode = errorString
            }
        }
    }

    if (inviteDialog) {
        AlertDialog(
            {
                inviteDialog = false
            },
            {
                TextButton(
                    {
                        inviteDialog = false
                    }
                ) {
                    Text(stringResource(R.string.close))
                }
            },
            properties = DialogProperties(usePlatformDefaultWidth = false),
            title = { Text(stringResource(R.string.invite_code)) },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(PaddingDefault)
                ) {
                    if (inviteCode.isBlank()) {
                        CircularProgressIndicator()
                    } else {
                        Text(inviteCode, style = MaterialTheme.typography.displayMedium)
                        Text(
                            stringResource(R.string.invite_code_description),
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                }
            }
        )
    }

    LaunchedEffect(true) {
        try {
            myCards = api.myCards()
        } catch (ex: Exception) {
            ex.printStackTrace()
        } finally {
            isLoading = false
        }
    }

    Column {
        SmallTopAppBar(
            {
                Text(stringResource(R.string.my_cards))
            },
            actions = {
                ElevatedButton(
                    {
                        inviteDialog = true
                    },
                    enabled = !inviteDialog
                ) {
                    Icon(
                        Icons.Outlined.Add,
                        stringResource(R.string.invite),
                        modifier = Modifier.padding(end = PaddingDefault)
                    )
                    Text(stringResource(R.string.invite))
                }
                IconButton({
                    navController.navigate("settings")
                }) {
                    Icon(Icons.Outlined.Settings, Icons.Outlined.Settings.name)
                }
            }
        )
        LazyColumn(
            state = state,
            contentPadding = PaddingValues(PaddingDefault),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(PaddingDefault, Alignment.Bottom),
            modifier = Modifier.fillMaxWidth().weight(1f)
        ) {
            items(myCards, key = { it.id!! }) { card ->
                val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) {
                    if (it == null) return@rememberLauncherForActivityResult

                    coroutineScope.launch {
                        try {
                            api.uploadCardPhoto(card.id!!, it)
                            myCards = api.myCards()
                        } catch (ex: Exception) {
                            ex.printStackTrace()
                        }
                    }
                }

                BasicCard(
                    {
                        launcher.launch("image/*")
                    },
                    onChange = {
                        coroutineScope.launch {
                            isLoading = true
                            myCards = listOf()
                            try {
                                myCards = api.myCards()
                            } catch (ex: Exception) {
                                ex.printStackTrace()
                            } finally {
                                isLoading = false
                            }
                        }
                    },
                    navController.context as Activity,
                    card,
                    card.id == addedCardId,
                    true
                )

                if (card.id == addedCardId) {
                    addedCardId = null
                }
            }

            if (myCards.isEmpty()) {
                if (isLoading) {
                    item {
                        LinearProgressIndicator(
                            color = MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = PaddingDefault)
                        )
                    }
                } else {
                    item {
                        Text(
                            stringResource(R.string.you_have_no_cards),
                            color = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.padding(PaddingDefault * 2)
                        )
                    }
                }
            }

            item {
                ElevatedButton(
                    {
                        coroutineScope.launch {
                            try {
                                addedCardId = api.newCard().id
                                myCards = api.myCards()
                                delay(100)
                                state.animateScrollToItem(0)
                            } catch (ex: Exception) {
                                ex.printStackTrace()
                            }
                        }
                    }
                ) {
                    Text(stringResource(R.string.add_a_card))
                }
            }
        }
    }
}
