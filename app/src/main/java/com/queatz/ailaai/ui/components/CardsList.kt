package com.queatz.ailaai.ui.components

import android.app.Activity
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import at.bluesource.choicesdk.maps.common.LatLng
import com.queatz.ailaai.Card
import com.queatz.ailaai.R
import com.queatz.ailaai.extensions.distance
import com.queatz.ailaai.extensions.reply
import com.queatz.ailaai.ui.theme.PaddingDefault
import kotlinx.coroutines.launch

@Composable
fun CardsList(cards: List<Card>, isMine: (Card) -> Boolean, geo: LatLng?, isLoading: Boolean, isError: Boolean, value: String, valueChange: (String) -> Unit, navController: NavController, aboveSearchFieldContent: @Composable () -> Unit = {}) {
    val coroutineScope = rememberCoroutineScope()
    Box(contentAlignment = Alignment.BottomCenter, modifier = Modifier.fillMaxSize()) {
        if (isLoading) {
            LinearProgressIndicator(
                color = MaterialTheme.colorScheme.tertiary,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = PaddingDefault * 2, vertical = PaddingDefault + 80.dp)
            )
        } else if (isError || cards.isEmpty()) {
            Text(
                stringResource(if (isError) R.string.didnt_work else R.string.no_cards_to_show),
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier
                    .padding(horizontal = PaddingDefault * 2, vertical = PaddingDefault + 80.dp)
            )
        } else {
            LazyVerticalGrid(
                contentPadding = PaddingValues(
                    PaddingDefault,
                    PaddingDefault,
                    PaddingDefault,
                    PaddingDefault + 80.dp
                ),
                horizontalArrangement = Arrangement.spacedBy(PaddingDefault, Alignment.Start),
                verticalArrangement = Arrangement.spacedBy(PaddingDefault, Alignment.Top),
                modifier = Modifier.fillMaxSize(),
                columns = GridCells.Adaptive(240.dp)
            ) {
                @Composable fun basicCard(it: Card) {
                    BasicCard(
                        {
                            navController.navigate("card/${it.id!!}")
                        },
                        onReply = { conversation ->
                            coroutineScope.launch {
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
                val nearbyCards = cards.takeWhile {
                    geo != null && it.geo != null && it.latLng!!.distance(geo) < farDistance
                }
                val remainingCards = cards.drop(nearbyCards.size)
                items(items = nearbyCards, key = { it.id!! }) {
                    basicCard(it)
                }
                if (remainingCards.isNotEmpty()) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        if (geo != null) {
                            Text(
                                if (nearbyCards.isEmpty()) {
                                    stringResource(R.string.no_cards_nearby)
                                } else {
                                    stringResource(R.string.no_more_cards_nearby)
                                },
                                textAlign = TextAlign.Center,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(PaddingDefault)
                            )
                        }
                    }
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
                .fillMaxWidth()
        ) {
            aboveSearchFieldContent()
            SearchField(value, valueChange, modifier = Modifier)
        }
    }
}

val farDistance = 1000
