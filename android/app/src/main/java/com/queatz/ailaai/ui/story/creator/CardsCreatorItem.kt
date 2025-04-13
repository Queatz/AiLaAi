package com.queatz.ailaai.ui.story.creator

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import app.ailaai.api.card
import com.queatz.ailaai.AppNav
import com.queatz.ailaai.R
import com.queatz.ailaai.data.api
import com.queatz.ailaai.extensions.appNavigate
import com.queatz.ailaai.extensions.rememberStateOf
import com.queatz.ailaai.nav
import com.queatz.ailaai.ui.components.CardItem
import com.queatz.ailaai.ui.dialogs.ChooseCardDialog
import com.queatz.ailaai.ui.dialogs.Menu
import com.queatz.ailaai.ui.dialogs.menuItem
import com.queatz.ailaai.ui.story.CreatorScope
import com.queatz.ailaai.ui.story.ReorderDialog
import com.queatz.db.Card
import com.queatz.db.StoryContent

fun LazyGridScope.cardsCreatorItem(creatorScope: CreatorScope<StoryContent.Cards>) = with(creatorScope) {
    itemsIndexed(part.cards, key = { index, it -> "${part.hashCode()}.$it" }) { index, cardId ->
        var showCardMenu by rememberStateOf(false)
        var showAddCardDialog by rememberStateOf(false)
        var showReorderDialog by rememberStateOf(false)
        var card by remember { mutableStateOf<Card?>(null) }
        val nav = nav

        if (showAddCardDialog) {
            ChooseCardDialog(
                onDismissRequest = {
                    showAddCardDialog = false
                },
            ) {
                edit {
                    copy(cards = (cards + it).distinctBy { it })
                }
                showAddCardDialog = false
            }
        }

        if (showReorderDialog) {
            ReorderDialog(
                onDismissRequest = { showReorderDialog = false },
                onMove = { from, to ->
                    edit {
                        copy(
                            cards = cards.toMutableList().apply {
                                add(to.index, removeAt(from.index))
                            }
                        )
                    }
                },
                items = part.cards,
                key = { it }
            ) { cardId, elevation ->
                var card by remember { mutableStateOf<Card?>(null) }

                LaunchedEffect(cardId) {
                    api.card(cardId) { card = it }
                }

                CardItem(
                    onClick = null,
                    card = card,
                    isChoosing = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        LaunchedEffect(cardId) {
            api.card(cardId) { card = it }
        }

        if (showCardMenu) {
            Menu(
                {
                    showCardMenu = false
                }
            ) {
                menuItem(stringResource(R.string.add_card)) {
                    showCardMenu = false
                    showAddCardDialog = true
                }
                menuItem(stringResource(R.string.open_card)) {
                    showCardMenu = false
                    nav.appNavigate(AppNav.Page(cardId))
                }
                if (part.cards.size > 1) {
                    menuItem(stringResource(R.string.reorder)) {
                        showCardMenu = false
                        showReorderDialog = true
                    }
                }
                menuItem(stringResource(R.string.remove)) {
                    showCardMenu = false
                    if (part.cards.size == 1) {
                        showCardMenu = false
                        remove(partIndex)
                    } else {
                        edit {
                            copy(
                                cards = cards.toMutableList().apply {
                                    removeAt(index)
                                }
                            )
                        }
                    }
                }
                if (part.cards.size > 1) {
                    menuItem(stringResource(R.string.remove_all)) {
                        showCardMenu = false
                        remove(partIndex)
                    }
                }
            }
        }

        CardItem(
            onClick = {
                showCardMenu = true
            },
            onCategoryClick = {},
            card = card,
            isChoosing = true,
            modifier = Modifier.fillMaxWidth(.75f)
        )
    }
}
