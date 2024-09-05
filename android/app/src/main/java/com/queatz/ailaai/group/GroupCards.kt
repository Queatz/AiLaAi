package com.queatz.ailaai.group

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.ailaai.api.groupCards
import com.queatz.ailaai.AppNav
import com.queatz.ailaai.data.api
import com.queatz.ailaai.extensions.appNavigate
import com.queatz.ailaai.extensions.isAtTop
import com.queatz.ailaai.extensions.rememberAutoplayIndex
import com.queatz.ailaai.extensions.rememberStateOf
import com.queatz.ailaai.nav
import com.queatz.ailaai.ui.components.CardLayout
import com.queatz.ailaai.ui.components.Loading
import com.queatz.ailaai.ui.theme.pad
import com.queatz.db.Card
import com.queatz.db.GroupExtended

@Composable
fun GroupCards(group: GroupExtended) {
    var isLoading by rememberStateOf(false)
    var cards by rememberStateOf(emptyList<Card>())
    val state = rememberLazyGridState()
    val scope = rememberCoroutineScope()
    val nav = nav
    val isAtTop by state.isAtTop()
    var playingVideo by remember { mutableStateOf<Card?>(null) }
    val autoplayIndex by state.rememberAutoplayIndex()
    LaunchedEffect(autoplayIndex) {
        playingVideo = cards.getOrNull(
            (autoplayIndex - 1).coerceAtLeast(0)
        )
    }

    suspend fun reload() {
        api.groupCards(group.group!!.id!!) {
            cards = it
        }
        isLoading = false
    }

    LaunchedEffect(group) {
        reload()
    }

    if (isLoading) {
        Loading()
    } else {
        LazyVerticalGrid(
            state = state,
            contentPadding = PaddingValues(
                bottom = 1.pad
            ),
            horizontalArrangement = Arrangement.spacedBy(1.pad, Alignment.Start),
            verticalArrangement = Arrangement.spacedBy(1.pad, Alignment.Top),
            modifier = Modifier.fillMaxSize(),
            columns = GridCells.Adaptive(240.dp)
        ) {
            items(cards, key = { it.id!! }) { card ->
                CardLayout(
                    card = card,
                    showTitle = true,
                    onClick = {
                        nav.appNavigate(AppNav.Page(card.id!!))
                    },
                    scope = scope,
                    playVideo = card == playingVideo && !isAtTop,
                    modifier = Modifier.padding(horizontal = 1.pad)
                )
            }
        }
    }
}
