package com.queatz.ailaai.ui.story.contents

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.text.selection.DisableSelection
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import app.ailaai.api.card
import com.queatz.ailaai.AppNav
import com.queatz.ailaai.data.api
import com.queatz.ailaai.extensions.appNavigate
import com.queatz.ailaai.extensions.inDp
import com.queatz.ailaai.nav
import com.queatz.ailaai.ui.components.CardItem
import com.queatz.db.Card
import com.queatz.db.StoryContent

fun LazyGridScope.cardsItem(content: StoryContent.Cards, viewHeight: Float) {
    items(content.cards) {
        val nav = nav
        DisableSelection {
            var card by remember { mutableStateOf<Card?>(null) }
            LaunchedEffect(Unit) {
                api.card(it) { card = it }
            }
            CardItem(
                {
                    nav.appNavigate(AppNav.Page(it))
                },
                card = card,
                isChoosing = true,
                modifier = Modifier
                    .fillMaxWidth(.75f)
                    .heightIn(max = viewHeight.inDp())
            )
        }
    }
}
