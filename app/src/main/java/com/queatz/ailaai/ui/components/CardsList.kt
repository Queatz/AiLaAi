package com.queatz.ailaai.ui.components

import android.app.Activity
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.layout.boundsInParent
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import at.bluesource.choicesdk.maps.common.LatLng
import com.queatz.ailaai.Card
import com.queatz.ailaai.R
import com.queatz.ailaai.extensions.distance
import com.queatz.ailaai.extensions.inDp
import com.queatz.ailaai.extensions.reply
import com.queatz.ailaai.ui.screens.exploreInitialCategory
import com.queatz.ailaai.ui.theme.PaddingDefault
import kotlinx.coroutines.launch

@Composable
fun CardsList(
    cards: List<Card>,
    isMine: (Card) -> Boolean,
    geo: LatLng?,
    isLoading: Boolean,
    isError: Boolean,
    value: String,
    valueChange: (String) -> Unit,
    navController: NavController,
    state: LazyGridState = rememberLazyGridState(),
    useDistance: Boolean = false,
    action: (@Composable () -> Unit)? = null,
    onAction: (() -> Unit)? = null,
    aboveSearchFieldContent: @Composable () -> Unit = {},
) {
    var viewport by remember { mutableStateOf(Size(0f, 0f)) }
    val scope = rememberCoroutineScope()
    Box(contentAlignment = Alignment.BottomCenter, modifier = Modifier.fillMaxSize()) {
        if (isLoading) {
            LinearProgressIndicator(
                color = MaterialTheme.colorScheme.tertiary,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = PaddingDefault * 2, vertical = PaddingDefault * 3.5f + viewport.height.inDp())
            )
        } else if (isError || cards.isEmpty()) {
            Text(
                stringResource(if (isError) R.string.didnt_work else R.string.no_cards_to_show),
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier
                    .padding(horizontal = PaddingDefault * 2, vertical = PaddingDefault * 3.5f + viewport.height.inDp())
            )
        } else {
            LazyVerticalGrid(
                state = state,
                contentPadding = PaddingValues(
                    PaddingDefault,
                    PaddingDefault,
                    PaddingDefault,
                    PaddingDefault * 3.5f + viewport.height.inDp()
                ),
                horizontalArrangement = Arrangement.spacedBy(PaddingDefault, Alignment.Start),
                verticalArrangement = Arrangement.spacedBy(PaddingDefault, Alignment.Top),
                modifier = Modifier.fillMaxSize(),
                columns = GridCells.Adaptive(240.dp)
            ) {
                @Composable
                fun basicCard(it: Card) {
                    BasicCard(
                        {
                            navController.navigate("card/${it.id!!}")
                        },
                        onCategoryClick = {
                            exploreInitialCategory = it
                            navController.navigate("explore")
                        },
                        onReply = { conversation ->
                            scope.launch {
                                it.reply(conversation) { groupId ->
                                    navController.navigate("group/${groupId}")
                                }
                            }
                        },
                        showDistance = geo,
                        card = it,
                        activity = navController.context as Activity,
                        isMine = isMine(it),
                        isMineToolbar = false
                    )
                }

                val nearbyCards = if (useDistance && geo != null) cards.takeWhile {
                     it.geo != null && it.latLng!!.distance(geo) < farDistance
                } else emptyList()

                val remainingCards = cards.drop(nearbyCards.size)

                items(items = nearbyCards, key = { it.id!! }) {
                    basicCard(it)
                }
                if (remainingCards.isNotEmpty()) {
                    items(items = remainingCards, key = { it.id!! }) {
                        basicCard(it)
                    }
                }
            }
        }
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(PaddingDefault),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(PaddingDefault * 2)
                .widthIn(max = 480.dp)

            .onPlaced { viewport = it.boundsInParent().size }
                .fillMaxWidth()
        ) {
            aboveSearchFieldContent()
            if (action == null) {
                SearchField(value, valueChange)
            } else {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .wrapContentWidth()
                    ) {
                        SearchField(value, valueChange)
                    }
                    FloatingActionButton(
                        onClick = {
                            onAction?.invoke()
                        },
                        modifier = Modifier
                            .padding(start = PaddingDefault * 2)
                    ) {
                        action()
                    }
                }
            }
        }
    }
}

val farDistance = 100_000
