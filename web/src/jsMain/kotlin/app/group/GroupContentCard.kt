package app.group

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import api
import app.ailaai.api.card
import com.queatz.db.Card
import com.queatz.db.GroupContent as GroupContentModel
import components.Content

@Composable
fun GroupContentCard(
    content: GroupContentModel.Card,
    setTitle: (String?) -> Unit
) {
    content.cardId?.let { cardId ->
        var card by remember { mutableStateOf<Card?>(null) }
        LaunchedEffect(cardId) {
            api.card(cardId) {
                card = it
                setTitle(it.name)
            }
        }

        card?.let { card ->
            Content(
                content = card.content,
                cardId = card.id,
            )
        }
    }
}
