package com.queatz.ailaai.ui.dialogs

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import com.queatz.ailaai.Card
import com.queatz.ailaai.R
import com.queatz.ailaai.api
import com.queatz.ailaai.api.updateCard
import com.queatz.ailaai.extensions.rememberStateOf
import com.queatz.ailaai.json
import com.queatz.ailaai.ui.components.ConversationItem
import com.queatz.ailaai.ui.components.DialogBase
import com.queatz.ailaai.ui.theme.PaddingDefault
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString

@SuppressLint("UnrememberedMutableState")
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun EditCardDialog(card: Card, onDismissRequest: () -> Unit, onChange: () -> Unit) {
    val keyboardController = LocalSoftwareKeyboardController.current!!

    val conversation = remember {
        card.conversation?.let {
            json.decodeFromString<ConversationItem>(it)
        } ?: ConversationItem()
    }

    var cardName by remember { mutableStateOf(card.name ?: "") }
    val backstack = remember { mutableListOf<ConversationItem>() }
    var cardConversation by remember { mutableStateOf(conversation) }
    val scope = rememberCoroutineScope()

    DialogBase(onDismissRequest, dismissable = false, modifier = Modifier.wrapContentHeight()) {
        val scrollState = rememberScrollState()
        val currentRecomposeScope = currentRecomposeScope
        fun invalidate() {
            currentRecomposeScope.invalidate()
        }

        Column(
            modifier = Modifier
                .padding(PaddingDefault * 3)
                .verticalScroll(scrollState)
        ) {
            Text(
                stringResource(R.string.edit_card),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(bottom = PaddingDefault)
            )
            OutlinedTextField(
                cardName,
                onValueChange = {
                    cardName = it
                },
                label = {
                    Text(stringResource(R.string.card_name))
                },
                shape = MaterialTheme.shapes.large,
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences,
                    imeAction = ImeAction.Next
                ),
                keyboardActions = KeyboardActions(
                    onNext = {
                        keyboardController.hide()
                    }
                ),
                modifier = Modifier.fillMaxWidth()
            )
            Column(
                verticalArrangement = Arrangement.spacedBy(PaddingDefault),
                modifier = Modifier
            ) {
                if (backstack.isNotEmpty()) {
                    TextButton(
                        {
                            cardConversation = backstack.removeLast()
                            invalidate()
                        },
                        modifier = Modifier.padding(PaddingValues(top = PaddingDefault * 2))
                    ) {
                        Icon(
                            Icons.Outlined.ArrowBack,
                            stringResource(R.string.go_back),
                            modifier = Modifier.padding(end = PaddingDefault)
                        )
                        Text(
                            backstack.last().message.takeIf { it.isNotBlank() }
                                ?: stringResource(R.string.go_back),
                            overflow = TextOverflow.Ellipsis,
                            maxLines = 1
                        )
                    }
                }

                var messageState by mutableStateOf(cardConversation.message)

                OutlinedTextField(
                    messageState,
                    {
                        messageState = it
                        cardConversation.message = it
                    },
                    shape = MaterialTheme.shapes.large,
                    label = {
                        Text(stringResource(R.string.your_message))
                    },
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Sentences,
                        imeAction = ImeAction.Default
                    ),
                    keyboardActions = KeyboardActions(
                        onNext = {
                            keyboardController.hide()
                        }
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Text(
                    if (backstack.isEmpty()) {
                        stringResource(R.string.card_message_description)
                    } else {
                        stringResource(R.string.card_reply_description, cardConversation.title)
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.padding(PaddingValues(bottom = PaddingDefault))
                )

                cardConversation.items.forEach {
                    var titleState by mutableStateOf(it.title)

                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            titleState,
                            { value ->
                                titleState = value
                                it.title = value
                            },
                            placeholder = {
                                Text(stringResource(R.string.option))
                            },
                            shape = MaterialTheme.shapes.large,
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                capitalization = KeyboardCapitalization.Sentences,
                                imeAction = ImeAction.Next
                            ),
                            keyboardActions = KeyboardActions(onSearch = {
                                keyboardController.hide()
                            }),
                            modifier = Modifier
                                .weight(1f)
                                .onKeyEvent { keyEvent ->
                                    if (it.title.isEmpty() && keyEvent.key == Key.Backspace) {
                                        cardConversation.items.remove(it)
                                        invalidate()
                                        true
                                    } else false
                                }
                        )
                        if (titleState.isNotBlank()) {
                            IconButton(
                                {
                                    backstack.add(cardConversation)
                                    cardConversation = it
                                    invalidate()
                                },
                                colors = IconButtonDefaults.iconButtonColors(contentColor = MaterialTheme.colorScheme.primary)
                            ) {
                                Icon(
                                    Icons.Outlined.ArrowForward,
                                    stringResource(R.string.continue_conversation)
                                )
                            }
                        }
                    }
                }
                if (cardConversation.items.size < 4) {
                    TextButton(
                        {
                            cardConversation.items.add(ConversationItem())
                            invalidate()
                        }
                    ) {
                        Icon(
                            Icons.Outlined.Add,
                            stringResource(R.string.add_an_option),
                            modifier = Modifier.padding(end = PaddingDefault)
                        )
                        Text(stringResource(R.string.add_an_option))
                    }
                }
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(PaddingDefault, Alignment.End),
                verticalAlignment = Alignment.Bottom,
                modifier = Modifier.fillMaxWidth()
            ) {
                var disableSaveButton by rememberStateOf(false)

                TextButton(
                    {
                        onDismissRequest()
                    }
                ) {
                    Text(stringResource(R.string.cancel))
                }
                TextButton(
                    {
                        disableSaveButton = true

                        fun trim(it: ConversationItem) {
                            it.title = it.title.trim()
                            it.message = it.message.trim()
                            it.items = it.items.onEach { trim(it) }
                        }

                        trim(conversation)

                        scope.launch {
                            api.updateCard(
                                card.id!!,
                                Card(name = cardName.trim(), conversation = json.encodeToString(conversation))
                            ) { update ->
                                card.name = update.name
                                card.conversation = update.conversation

                                onDismissRequest()
                                onChange()
                            }
                            disableSaveButton = false
                        }
                    },
                    enabled = !disableSaveButton
                ) {
                    Text(stringResource(R.string.save))
                }
            }
        }
    }
}

