package com.queatz.ailaai.ui.screens

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.core.content.ContextCompat
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.queatz.ailaai.*
import com.queatz.ailaai.R
import com.queatz.ailaai.ui.components.BasicCard
import com.queatz.ailaai.ui.components.CardConversation
import com.queatz.ailaai.ui.state.gsonSaver
import com.queatz.ailaai.ui.theme.ElevationDefault
import com.queatz.ailaai.ui.theme.PaddingDefault
import kotlinx.coroutines.CoroutineScope
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
    val state = rememberLazyGridState()
    val context = LocalContext.current

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

    val isMine = me()?.id == card?.person

    Column(
        verticalArrangement = Arrangement.Top,
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
            actions = {
                var showMenu by remember { mutableStateOf(false) }

                IconButton({
                    showMenu = !showMenu
                }) {
                    Icon(Icons.Outlined.MoreVert, stringResource(R.string.more))
                }

                val cardString = stringResource(R.string.card)

                DropdownMenu(showMenu, { showMenu = false }) {
                    DropdownMenuItem({
                        Text(stringResource(R.string.copy_link))
                    }, {
                        card?.let { card ->
                            ContextCompat.getSystemService(context, ClipboardManager::class.java)?.setPrimaryClip(
                                ClipData.newPlainText(card.name ?: cardString, "${appDomain}/card/${card.id}")
                            )
                        }
                        showMenu = false
                    })
                }
            },
            colors = TopAppBarDefaults.smallTopAppBarColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            ),
            modifier = Modifier
                .shadow(ElevationDefault / 2)
                .zIndex(1f)
        )

        if (isLoading) {
            LinearProgressIndicator(
                color = MaterialTheme.colorScheme.tertiary,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = PaddingDefault * 2, vertical = PaddingDefault)
            )
        } else {
            val isLandscape = LocalConfiguration.current.screenWidthDp > LocalConfiguration.current.screenHeightDp

            Row(
                modifier = Modifier.fillMaxSize()
            ) {
                if (isLandscape) {
                    LazyVerticalGrid(
                        contentPadding = PaddingValues(PaddingDefault),
                        horizontalArrangement = Arrangement.spacedBy(PaddingDefault, Alignment.CenterHorizontally),
                        verticalArrangement = Arrangement.spacedBy(PaddingDefault, Alignment.Top),
                        columns = GridCells.Fixed(1),
                        modifier = Modifier
                            .width(240.dp)
                            .fillMaxHeight()
                            .shadow(ElevationDefault)
                            .background(MaterialTheme.colorScheme.surfaceColorAtElevation(ElevationDefault))
                    ) {
                        cardHeaderItems(card, isMine, coroutineScope, navController)
                    }
                }

                LazyVerticalGrid(
                    state = state,
                    contentPadding = PaddingValues(PaddingDefault),
                    horizontalArrangement = Arrangement.spacedBy(PaddingDefault, Alignment.CenterHorizontally),
                    verticalArrangement = Arrangement.spacedBy(PaddingDefault, Alignment.Top),
                    modifier = Modifier.fillMaxSize(),
                    columns = GridCells.Adaptive(240.dp)
                ) {
                    if (!isLandscape) {
                        cardHeaderItems(card, isMine, coroutineScope, navController)
                    }
                    if (cards.isEmpty()) {
                        item(span = { GridItemSpan(maxLineSpan) }) {
                            Text(
                                stringResource(R.string.no_cards),
                                textAlign = TextAlign.Center,
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
                                            api.sendMessage(
                                                groupId,
                                                Message(attachment = gson.toJson(CardAttachment(it.id!!)))
                                            )
                                            navController.navigate("group/${groupId}")
                                        } catch (ex: Exception) {
                                            ex.printStackTrace()
                                        }
                                    }
                                },
                                onChange = {
                                    coroutineScope.launch {
                                        isLoading = true
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
                    if (isMine) {
                        item(span = { GridItemSpan(maxLineSpan) }) {
                            Box(contentAlignment = Alignment.Center) {
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
        }
    }
}

private fun LazyGridScope.cardHeaderItems(
    card: Card?,
    isMine: Boolean,
    coroutineScope: CoroutineScope,
    navController: NavController
) {
    card?.photo?.also {
        item(span = { GridItemSpan(maxCurrentLineSpan) }) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(api.url(it))
                    .crossfade(true)
                    .build(),
                contentDescription = "",
                contentScale = ContentScale.Crop,
                alignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(MaterialTheme.shapes.large)
                    .aspectRatio(1.5f)
            )
        }
    }
    card?.let {
        item {
            CardConversation(
                it,
                interactable = true,
                showTitle = false,
                isMine = isMine,
                onReply = {
                    coroutineScope.launch {
                        try {
                            val groupId = api.cardGroup(it.id!!).id!!
                            api.sendMessage(
                                groupId,
                                Message(attachment = gson.toJson(CardAttachment(it.id!!)))
                            )
                            navController.navigate("group/${groupId}")
                        } catch (ex: Exception) {
                            ex.printStackTrace()
                        }
                    }
                },
                modifier = Modifier
                    .padding(PaddingDefault)
            )
        }
    }
}
