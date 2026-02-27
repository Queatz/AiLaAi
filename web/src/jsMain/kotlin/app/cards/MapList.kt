package app.cards

import androidx.compose.runtime.Composable
import com.queatz.db.Card
import com.queatz.db.Person
import components.LazyColumn
import components.TaskListItem
import org.jetbrains.compose.web.css.AlignItems
import org.jetbrains.compose.web.css.StyleScope
import org.jetbrains.compose.web.css.alignItems
import org.jetbrains.compose.web.css.gap
import r

@Composable
fun MapList(
    cards: List<Card>,
    allCards: List<Card>? = null,
    showPhoto: Boolean = true,
    people: List<Person>? = null,
    groupId: String? = null,
    isOnSurface: Boolean = false,
    onBackground: Boolean = false,
    expandedCardId: String? = null,
    onExpanded: ((Card, Boolean) -> Unit)? = null,
    onUpdated: (() -> Unit)? = null,
    styles: (StyleScope.() -> Unit)? = null,
    onSelected: (Card) -> Unit
) {
    LazyColumn({
        style {
            gap(.5.r)
            alignItems(AlignItems.Stretch)
            styles?.invoke(this)
        }
    }) {
        items(cards) { card ->
            TaskListItem(
                card = card,
                allCards = allCards,
                showPhoto = showPhoto,
                people = people,
                isOnSurface = isOnSurface,
                onBackground = onBackground,
                expanded = card.id == expandedCardId,
                onExpanded = if (onExpanded != null) { { onExpanded(card, it) } } else null,
                onUpdated = onUpdated
            ) {
                onSelected(it)
            }
        }
    }
}
