package com.queatz.ailaai.ui.components

import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.queatz.ailaai.extensions.ContactPhoto
import com.queatz.ailaai.extensions.hint
import com.queatz.ailaai.extensions.inList
import com.queatz.db.Card

@Composable
fun CardsBar(
    cards: List<Card>,
    modifier: Modifier = Modifier,
    onLongClick: (Card) -> Unit = {},
    onClick: (Card) -> Unit
) {
    ButtonBar(
        items = cards,
        itemModifier = { Modifier.requiredWidth(64.dp) },
        onLongClick = onLongClick,
        onClick = onClick,
        modifier = modifier,
        photo = {
            GroupPhoto(
                ContactPhoto(it.name.orEmpty(), it.photo).inList(),
                padding = 0.dp,
                size = 54.dp
            )
        },
        title = {
            it.name.orEmpty()
        },
        subtitle = {
            it.hint
        }
    )
}
