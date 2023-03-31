package com.queatz.ailaai.ui.screens

import android.app.Activity
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import androidx.navigation.NavController
import com.queatz.ailaai.Card
import com.queatz.ailaai.Person
import com.queatz.ailaai.R
import com.queatz.ailaai.api
import com.queatz.ailaai.extensions.isFalse
import com.queatz.ailaai.extensions.isTrue
import com.queatz.ailaai.extensions.toggle
import com.queatz.ailaai.ui.components.*
import com.queatz.ailaai.ui.state.gsonSaver
import com.queatz.ailaai.ui.theme.ElevationDefault
import com.queatz.ailaai.ui.theme.PaddingDefault
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

enum class Filter {
    Active,
    NotActive
}

@OptIn(ExperimentalMaterial3Api::class)
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
    var searchText by rememberSaveable { mutableStateOf("") }
    var filters by rememberSaveable { mutableStateOf(emptySet<Filter>()) }

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

    val cards = remember(myCards, cardParentType, filters, searchText) {
        when (cardParentType) {
            CardParentType.Person -> myCards.filter { it.parent == null && it.equipped.isTrue }
            CardParentType.Map -> myCards.filter { it.parent == null && it.equipped.isFalse && it.geo != null && it.offline.isFalse }
            CardParentType.Card -> myCards.filter { it.parent != null }
            CardParentType.Offline -> myCards.filter { it.offline == true }
            else -> myCards
        }.filter {
            filters.all { filter ->
                when(filter) {
                    Filter.Active -> it.active == true
                    Filter.NotActive -> it.active != true
                }
            } &&
                    (searchText.isBlank() || it.name?.contains(searchText, true) ?: false)
        }
    }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        TopAppBar(
            {
                Text(stringResource(R.string.your_cards), maxLines = 1, overflow = TextOverflow.Ellipsis)
            },
            actions = {
                ElevatedButton(
                    {
                        inviteDialog = true
                    },
                    enabled = !inviteDialog,
                    modifier = Modifier.padding(start = PaddingDefault)
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
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState())
        ) {
            CardParentSelector(
                cardParentType,
                modifier = Modifier
                    .width(320.dp)
                    .padding(horizontal = PaddingDefault)
                    .padding(bottom = PaddingDefault / 2),
                showOffline = true
            ) {
                cardParentType = if (it == cardParentType) null else it
            }
            OutlinedButton(
                {
                    filters = filters.minus(Filter.NotActive).toggle(Filter.Active)
                },
                border = IconButtonDefaults.outlinedIconToggleButtonBorder(true, filters.contains(Filter.Active)),
                colors = if (!filters.contains(Filter.Active)) ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onBackground) else ButtonDefaults.buttonColors(),
                modifier = Modifier.padding(end = PaddingDefault)
            ) {
                Text(stringResource(R.string.active))
            }
            OutlinedButton(
                {
                    filters = filters.minus(Filter.Active).toggle(Filter.NotActive)
                },
                border = IconButtonDefaults.outlinedIconToggleButtonBorder(true, filters.contains(Filter.NotActive)),
                colors = if (!filters.contains(Filter.NotActive)) ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onBackground) else ButtonDefaults.buttonColors(),
                modifier = Modifier.padding(end = PaddingDefault)
            ) {
                Text(stringResource(R.string.not_active))
            }
        }
        Box(
            contentAlignment = Alignment.BottomCenter,
            modifier = Modifier.fillMaxWidth().weight(1f)
        ) {
            LazyVerticalGrid(
                state = state,
                contentPadding = PaddingValues(
                    PaddingDefault,
                    PaddingDefault,
                    PaddingDefault,
                    PaddingDefault + 80.dp + 50.dp
                ),
                horizontalArrangement = Arrangement.spacedBy(PaddingDefault, Alignment.CenterHorizontally),
                verticalArrangement = Arrangement.spacedBy(PaddingDefault, Alignment.Top),
                modifier = Modifier.fillMaxSize(),
                columns = GridCells.Adaptive(240.dp)
            ) {
                if (cardParentType != null) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        Text(
                            when (cardParentType) {
                                CardParentType.Map -> stringResource(R.string.at_a_location)
                                CardParentType.Card -> stringResource(R.string.inside_another_card)
                                CardParentType.Person -> stringResource(R.string.with_you)
                                else -> stringResource(R.string.offline)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = PaddingDefault),
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                }
                items(cards, key = { it.id!! }) { card ->
                    BasicCard(
                        {
                            navController.navigate("card/${card.id!!}")
                        },
                        onChange = {
                            coroutineScope.launch {
                                isLoading = true
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
                        edit = if (card.id == addedCardId) EditCard.Conversation else null,
                        isMine = true
                    )

                    if (card.id == addedCardId) {
                        addedCardId = null
                    }
                }

                if (cards.isEmpty()) {
                    if (isLoading) {
                        item(span = { GridItemSpan(maxLineSpan) }) {
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
                                stringResource(if (cardParentType == null && filters.isEmpty() && searchText.isBlank()) R.string.you_have_no_cards else R.string.no_cards_to_show),
                                color = MaterialTheme.colorScheme.secondary,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(PaddingDefault * 2)
                            )
                        }
                    }
                }
            }
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(PaddingDefault),
                modifier = Modifier
                .padding(PaddingDefault * 2)
                .widthIn(max = 480.dp)
                .fillMaxWidth()
            ) {
                ElevatedButton(
                    elevation = ButtonDefaults.elevatedButtonElevation(ElevationDefault * 2),
                    onClick = {
                        coroutineScope.launch {
                            try {
                                cardParentType = null
                                filters = emptySet()
                                addedCardId = api.newCard().id
                                myCards = api.myCards()
                                delay(100)

                                if (state.firstVisibleItemIndex > 2) {
                                    state.scrollToItem(2)
                                }

                                state.animateScrollToItem(0)
                            } catch (ex: Exception) {
                                ex.printStackTrace()
                            }
                        }
                    }
                ) {
                    Text(stringResource(R.string.add_a_card))
                }
                SearchField(searchText, { searchText = it }, modifier = Modifier.align(Alignment.CenterHorizontally))
            }
        }
    }
}
