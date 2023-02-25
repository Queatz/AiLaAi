package com.queatz.ailaai.ui.components

import android.app.Activity
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
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
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.queatz.ailaai.*
import com.queatz.ailaai.R
import com.queatz.ailaai.ui.theme.PaddingDefault
import kotlinx.coroutines.launch

@Composable
fun CardsList(cards: List<Card>, isLoading: Boolean, value: String, valueChange: (String) -> Unit, navController: NavController, aboveSearchFieldContent: @Composable () -> Unit = {}) {
    val coroutineScope = rememberCoroutineScope()
    Box(contentAlignment = Alignment.BottomCenter, modifier = Modifier.fillMaxSize()) {
        if (isLoading) {
            LinearProgressIndicator(
                color = MaterialTheme.colorScheme.tertiary,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = PaddingDefault * 2, vertical = PaddingDefault + 80.dp)
            )
        } else if (cards.isEmpty()) {
            Text(
                stringResource(R.string.no_cards_to_show),
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
                items(items = cards, key = { it.id!! }) {
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
                        activity = navController.context as Activity,
                        card = it
                    )
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
