package app.cards

import Styles
import app.dialog.dialog
import application
import com.queatz.db.Card
import components.IconButton
import org.jetbrains.compose.web.dom.Div

suspend fun mapListDialog(
    cards: List<Card>,
    groupId: String? = null,
    onUpdated: (() -> Unit)? = null,
    onCard: (Card) -> Unit,
) {
    dialog(
        title = application.appString { this.cards },
        confirmButton = application.appString { close },
        cancelButton = null,
        actions = { resolve ->
            IconButton("close", application.appString { close }) {
                resolve(false)
            }
        }
    ) {
        Div({
            classes(Styles.navContent)
        }) {
            MapList(
                cards = cards,
                groupId = groupId,
                onUpdated = onUpdated
            ) { card ->
                onCard(card)
            }
        }
    }
}
