package app.cards

import Styles
import androidx.compose.runtime.Composable
import com.queatz.db.Card
import com.queatz.db.Person
import components.LazyColumn
import components.TaskListItem
import org.jetbrains.compose.web.css.*
import r

@Composable
fun MapList(
    cards: List<Card>,
    allCards: List<Card>? = null,
    showPhoto: Boolean = true,
    people: List<Person>? = null,
    groupId: String? = null,
    isOnSurface: Boolean = false,
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
                onUpdated = onUpdated
            ) {
                onSelected(it)
            }
        }
    }
}
