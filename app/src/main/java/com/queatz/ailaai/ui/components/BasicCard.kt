package com.queatz.ailaai.ui.components

import android.annotation.SuppressLint
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.modifier.modifierLocalOf
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.queatz.ailaai.ui.theme.PaddingDefault
import kotlin.random.Random

@SuppressLint("UnrememberedMutableState")
@OptIn(ExperimentalMaterial3Api::class, ExperimentalComposeUiApi::class)
@Composable
fun BasicCard(navController: NavHostController, nameAndLocation: Pair<String, String>) {
    val seed = remember { Random.nextInt() }

    Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.elevatedCardColors(),
        elevation = CardDefaults.elevatedCardElevation(),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(.75f)
                .clickable {
                    navController.navigate("messages/${nameAndLocation.first}")
                },
            contentAlignment = Alignment.BottomCenter
        ) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data("https://random.imagecdn.app/v1/image?width=600&height=1200&category=girl&format=image&seed=$seed")
                    .crossfade(true)
                    .build(),
                contentDescription = "Image",
                contentScale = ContentScale.Crop,
                alignment = Alignment.TopCenter,
                modifier = Modifier.matchParentSize()
            )
            Column(
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.background.copy(alpha = .8f))
                    .padding(PaddingDefault * 2)
            ) {
                Text(
                    text = buildAnnotatedString {
                        withStyle(
                            MaterialTheme.typography.titleMedium.toSpanStyle().copy(fontWeight = FontWeight.Bold)
                        ) {
                            append(nameAndLocation.first)
                        }
                        append("  ")
                        withStyle(
                            MaterialTheme.typography.titleSmall.toSpanStyle()
                                .copy(color = MaterialTheme.colorScheme.secondary)
                        ) {
                            append(nameAndLocation.second)
                        }
                        append("\nI'm an app developer from Austin who loves prototyping and brainstorming ideas. Let's chat!")
                    },
                    style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurface),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = PaddingDefault)
                )

                val recomposeScope = currentRecomposeScope

                listOf(
                    "I want to talk about an app idea",
                    "I want to learn programming",
                    "\uD83D\uDCEC Send a message",
                )
                    .sortedBy { Random.nextInt(1, 4) }
                    .take(Random.nextInt(1, 4))
                    .forEach {
                        Button({
                            recomposeScope.invalidate()
                        }) {
                            Text(it, overflow = TextOverflow.Ellipsis, maxLines = 1)
                        }
                    }
//                IconButton({
//                    recomposeScope.invalidate()
//                }, modifier = Modifier.align(Alignment.CenterHorizontally)) {
//                    Icon(Icons.Outlined.Refresh, "Refresh")
//                }

                var openDeleteDialog by remember { mutableStateOf(false) }
                var openEditDialog by remember { mutableStateOf(false) }
                var openLocationDialog by remember { mutableStateOf(false) }

                Row(
                    modifier = Modifier
                        .background(Color.Transparent)
                        .align(Alignment.End)
                        .padding(PaddingValues(top = PaddingDefault))
                ) {
                    IconButton({
                        openLocationDialog = true
                    }) {
                        Icon(Icons.Outlined.Place, "")
                    }
                    IconButton({
                        openEditDialog = true
                    }) {
                        Icon(Icons.Outlined.Edit, "")
                    }
                    IconButton({
                        openDeleteDialog = true
                    }) {
                        Icon(Icons.Outlined.Delete, "", tint = MaterialTheme.colorScheme.error)
                    }
                }

                if (openLocationDialog) {
                    var locationName by remember { mutableStateOf("") }

                    Dialog({
                        openLocationDialog = false
                    }) {
                        Surface(
                            shape = MaterialTheme.shapes.extraLarge
                        ) {
                            val scrollState = rememberScrollState()

                            Column(
                                modifier = Modifier
                                    .padding(PaddingDefault * 3)
                                    .verticalScroll(scrollState)
                            ) {
                                Text(
                                    "Card location",
                                    style = MaterialTheme.typography.titleLarge,
                                    modifier = Modifier.padding(bottom = PaddingDefault)
                                )
                                OutlinedTextField(
                                    locationName,
                                    onValueChange = {
                                        locationName = it
                                    },
                                    label = {
                                        Text("Location name")
                                    },
                                    shape = MaterialTheme.shapes.large,
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .aspectRatio(1f)
                                        .padding(PaddingValues(vertical = PaddingDefault * 2))
                                        .clip(MaterialTheme.shapes.large)
                                        .background(Color.Magenta)
                                )
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(PaddingDefault, Alignment.End),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    TextButton(
                                        {
                                            openLocationDialog = false
                                        }
                                    ) {
                                        Text("Cancel")
                                    }
                                    TextButton(
                                        {
                                            openLocationDialog = false
                                        }
                                    ) {
                                        Text("Save")
                                    }
                                }
                            }
                        }
                    }
                }

                if (openEditDialog) {
                    var cardName by remember { mutableStateOf("") }
                    val backstack = remember { mutableListOf<ConversationItem>() }
                    var cardConversation by remember { mutableStateOf(ConversationItem()) }

                    Dialog({
                        openEditDialog = false
                    }) {
                        Surface(
                            shape = MaterialTheme.shapes.extraLarge,
                            modifier = Modifier
                                .padding(PaddingDefault * 2)
                                .fillMaxHeight(.9f)
                        ) {
                            val scrollState = rememberScrollState()

                            Column(
                                modifier = Modifier.padding(PaddingDefault * 3)
                            ) {
                                Text(
                                    "Card conversation",
                                    style = MaterialTheme.typography.titleLarge,
                                    modifier = Modifier.padding(bottom = PaddingDefault)
                                )
                                OutlinedTextField(
                                    cardName,
                                    onValueChange = {
                                        cardName = it
                                    },
                                    label = {
                                        Text("Your name")
                                    },
                                    shape = MaterialTheme.shapes.large,
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Column(
                                    verticalArrangement = Arrangement.spacedBy(PaddingDefault),
                                    modifier = Modifier
                                        .weight(1f)
                                        .verticalScroll(scrollState)
                                ) {
                                    if (backstack.isNotEmpty()) {
                                        TextButton(
                                            {
                                                cardConversation = backstack.removeLast()
                                                recomposeScope.invalidate()
                                            },
                                            modifier = Modifier.padding(PaddingValues(top = PaddingDefault * 2))
                                        ) {
                                            Icon(
                                                Icons.Outlined.ArrowBack,
                                                "Go back",
                                                modifier = Modifier.padding(end = PaddingDefault)
                                            )
                                            Text(cardConversation.title, overflow = TextOverflow.Ellipsis, maxLines = 1)
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
                                        label = { Text("Your message") },
                                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                                        modifier = Modifier.fillMaxWidth()
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
                                                    Text("Option")
                                                },
                                                shape = MaterialTheme.shapes.large,
                                                singleLine = true,
                                                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .onKeyEvent { keyEvent ->
                                                        if (it.title.isEmpty() && keyEvent.key == Key.Backspace) {
                                                            cardConversation.items.remove(it)
                                                            recomposeScope.invalidate()
                                                            true
                                                        } else false
                                                    }
                                            )
                                            if (titleState.isNotBlank()) {
                                                IconButton({
                                                    backstack.add(cardConversation)
                                                    cardConversation = it
                                                    recomposeScope.invalidate()
                                                },
                                                    colors = IconButtonDefaults.iconButtonColors(contentColor = MaterialTheme.colorScheme.primary)
                                                ) {
                                                    Icon(Icons.Outlined.ArrowForward, "Continue conversation")
                                                }
                                            }
                                        }
                                    }
                                    TextButton(
                                        {
                                            cardConversation.items.add(ConversationItem())
                                            recomposeScope.invalidate()
                                        }
                                    ) {
                                        Icon(
                                            Icons.Outlined.Add,
                                            "Add an option",
                                            modifier = Modifier.padding(end = PaddingDefault)
                                        )
                                        Text("Add an option")
                                    }
                                }
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(PaddingDefault, Alignment.End),
                                    verticalAlignment = Alignment.Bottom,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    TextButton(
                                        {
                                            openEditDialog = false
                                        }
                                    ) {
                                        Text("Cancel")
                                    }
                                    TextButton(
                                        {
                                            openEditDialog = false
                                        }
                                    ) {
                                        Text("Save")
                                    }
                                }
                            }
                        }
                    }
                }

                if (openDeleteDialog) {
                    AlertDialog(
                        {
                            openDeleteDialog = false
                        },
                        confirmButton = {
                            TextButton(
                                {
                                    openDeleteDialog = false
                                },
                                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                            ) {
                                Text("Delete card")
                            }
                        },
                        dismissButton = {
                            TextButton({
                                openDeleteDialog = false
                            }) {
                                Text("Cancel")
                            }
                        },
                        title = {
                            Text("Delete this card?")
                        },
                        text = {
                            Text("You cannot undo this.")
                        })
                }
            }
        }
    }
}

data class ConversationItem(
    var title: String = "",
    var message: String = "",
    var items: MutableList<ConversationItem> = mutableListOf()
)
