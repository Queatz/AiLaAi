package app.cards

import androidx.compose.runtime.Composable
import com.queatz.db.Card
import components.CardListItem
import components.LazyColumn
import org.jetbrains.compose.web.css.AlignItems
import org.jetbrains.compose.web.css.alignItems
import org.jetbrains.compose.web.css.gap
import org.jetbrains.compose.web.css.padding
import r

@Composable
fun MapList(
    cards: List<Card>,
    onSelected: (Card) -> Unit
) {
    LazyColumn({
        style {
            gap(.5.r)
            alignItems(AlignItems.Stretch)
            padding(.5.r)
        }
    }) {
        items(cards) { card ->
            CardListItem(card) {
                onSelected(card)
            }
        }
    }
}
