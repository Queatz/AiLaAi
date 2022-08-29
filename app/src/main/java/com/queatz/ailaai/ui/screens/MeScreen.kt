package com.queatz.ailaai.ui.screens

import android.app.Activity
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import androidx.navigation.NavController
import com.queatz.ailaai.Card
import com.queatz.ailaai.Person
import com.queatz.ailaai.R
import com.queatz.ailaai.api
import com.queatz.ailaai.ui.components.BasicCard
import com.queatz.ailaai.ui.components.CardParentSelector
import com.queatz.ailaai.ui.components.CardParentType
import com.queatz.ailaai.ui.state.gsonSaver
import com.queatz.ailaai.ui.theme.PaddingDefault
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalComposeUiApi::class, ExperimentalMaterial3Api::class)
@Composable
fun MeScreen(navController: NavController, me: () -> Person?) {
    var myCards by rememberSaveable(stateSaver = gsonSaver<List<Card>>()) { mutableStateOf(listOf()) }
    var addedCardId by remember { mutableStateOf<String?>(null) }
    var inviteDialog by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(true) }
    val coroutineScope = rememberCoroutineScope()
    var inviteCode by remember { mutableStateOf("") }
    val state = rememberLazyGridState()
    var cardParentType by rememberSaveable { mutableStateOf<CardParentType?>(null) }

    val errorString = stringResource(R.string.error)

    LaunchedEffect(inviteDialog) {
        if (inviteDialog) {
            inviteCode = ""

            inviteCode = try {
                api.invite().code ?: ""
            } catch (ex: Exception) {
                ex.printStackTrace()
                errorString
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
                        SelectionContainer {
                            Text(inviteCode, style = MaterialTheme.typography.displayMedium)
                        }
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
                Text(stringResource(R.string.your_cards))
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
        CardParentSelector(cardParentType, modifier = Modifier
            .padding(horizontal = PaddingDefault)
            .padding(bottom = PaddingDefault / 2)
        ) {
            cardParentType = if (it == cardParentType) null else it
        }
        LazyVerticalGrid(
            state = state,
            contentPadding = PaddingValues(PaddingDefault),
            horizontalArrangement = Arrangement.spacedBy(PaddingDefault, Alignment.CenterHorizontally),
            verticalArrangement = Arrangement.spacedBy(PaddingDefault, Alignment.Bottom),
            modifier = Modifier.fillMaxWidth().weight(1f),
            columns = GridCells.Adaptive(240.dp)
        ) {
            val cards = when (cardParentType) {
                CardParentType.Person -> myCards.filter { it.parent == null && it.equipped == true }
                CardParentType.Map -> myCards.filter { it.parent == null && it.equipped != true }
                CardParentType.Card -> myCards.filter { it.parent != null }
                else -> myCards
            }
            items(cards, key = { it.id!! }) { card ->
                BasicCard(
                    {
                        navController.navigate("card/${card.id!!}")
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
                    activity = navController.context as Activity,
                    card = card,
                    edit = card.id == addedCardId,
                    isMine = true
                )

                if (card.id == addedCardId) {
                    addedCardId = null
                }
            }

            if (cards.isEmpty()) {
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
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        Text(
                            stringResource(if (cardParentType == null) R.string.you_have_no_cards else R.string.no_cards_to_show),
                            color = MaterialTheme.colorScheme.secondary,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(PaddingDefault * 2)
                        )
                    }
                }
            }

            item(span = { GridItemSpan(maxLineSpan) }) {
                Box(contentAlignment = Alignment.Center) {
                    ElevatedButton(
                        {
                            coroutineScope.launch {
                                try {
                                    cardParentType = null
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
}
