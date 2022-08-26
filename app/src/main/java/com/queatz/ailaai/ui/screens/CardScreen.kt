package com.queatz.ailaai.ui.screens

import android.app.Activity
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.zIndex
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavController
import com.queatz.ailaai.*
import com.queatz.ailaai.R
import com.queatz.ailaai.ui.components.BasicCard
import com.queatz.ailaai.ui.state.gsonSaver
import com.queatz.ailaai.ui.theme.ElevationDefault
import com.queatz.ailaai.ui.theme.PaddingDefault
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CardScreen(navBackStackEntry: NavBackStackEntry, navController: NavController, me: () -> Person?) {
    val cardId = navBackStackEntry.arguments!!.getString("id")!!
    var isLoading by remember { mutableStateOf(false) }
    var card by rememberSaveable(stateSaver = gsonSaver<Card?>()) { mutableStateOf(null) }
    var cards by rememberSaveable(stateSaver = gsonSaver<List<Card>>()) { mutableStateOf(emptyList()) }
    val coroutineScope = rememberCoroutineScope()
    val state = rememberLazyListState()

    LaunchedEffect(true) {
        isLoading = true

        try {
            card = api.card(cardId)
            cards = api.cardsCards(cardId)
        } catch (ex: Exception) {
            ex.printStackTrace()
        } finally {
            isLoading = false
        }
    }

    Column(
        verticalArrangement = Arrangement.Bottom,
        modifier = Modifier.fillMaxSize()
    ) {
        SmallTopAppBar(
            {
                Column {
                    Text(card?.name ?: "", maxLines = 1, overflow = TextOverflow.Ellipsis)

                    card?.location?.takeIf { it.isNotBlank() }?.let {
                        Text(
                            it,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
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
            colors = TopAppBarDefaults.smallTopAppBarColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            ),
            modifier = Modifier.shadow(ElevationDefault / 2).zIndex(1f)
        )

        LazyColumn(
            state = state,
            contentPadding = PaddingValues(PaddingDefault),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(PaddingDefault, Alignment.Top),
            modifier = Modifier.fillMaxSize()
        ) {
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
                if (cards.isEmpty()) {
                    item {
                        Text(
                            stringResource(R.string.no_cards),
                            color = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.padding(PaddingDefault * 2)
                        )
                    }
                } else {
                    items(cards, { it.id!! }) {
                        BasicCard(
                            {
                                navController.navigate("card/${it.id!!}")
                            },
                            onReply = {
                                coroutineScope.launch {
                                    try {
                                        val groupId = api.cardGroup(it.id!!).id!!
                                        api.sendMessage(groupId, Message(attachment = gson.toJson(CardAttachment(it.id!!))))
                                        navController.navigate("group/${groupId}")
                                    } catch (ex: Exception) {
                                        ex.printStackTrace()
                                    }
                                }
                            },
                            onChange = {
                                coroutineScope.launch {
                                    isLoading = true
                                    cards = listOf()
                                    try {
                                        cards = api.cardsCards(cardId)
                                    } catch (ex: Exception) {
                                        ex.printStackTrace()
                                    } finally {
                                        isLoading = false
                                    }
                                }
                            },
                            activity = navController.context as Activity,
                            card = it,
                            isMine = it.person == me()?.id
                        )
                    }
                }
            }

            if (me()?.id == card?.person) {
                item {
                    ElevatedButton(
                        {
                            coroutineScope.launch {
                                try {
                                    api.newCard(Card(parent = cardId, name = "")).id
                                    cards = api.cardsCards(cardId)
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
