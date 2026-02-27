package app.dialog

import Configuration
import androidx.compose.runtime.Composable
import api
import app.AppStyles
import app.ailaai.api.myCollaborations
import app.nav.CardItem
import appString
import application
import com.queatz.db.Card
import focusable
import org.jetbrains.compose.web.css.marginRight
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.Span
import org.jetbrains.compose.web.dom.Text
import r
import saves

suspend fun selectCardDialog(
    configuration: Configuration,
    title: String = application.appString { card },
    allCards: List<Card>? = null,
    showNone: Boolean = false,
    onSelected: (Card?) -> Unit
) {
    val cancel = application.appString { cancel }

    searchDialog<Card?>(
        configuration = configuration,
        title = title,
        confirmButton = cancel,
        load = {
            val cards = if (allCards != null) {
                allCards
            } else {
                var loadedCards = emptyList<Card>()
                api.myCollaborations {
                    loadedCards = it
                }
                loadedCards
            }
            val sortedCards = cards.sortedByDescending { card ->
                saves.cards.value.any { it.id == card.id }
            }
            if (showNone) {
                listOf(null) + sortedCards
            } else {
                sortedCards
            }
        },
        filter = { it, value ->
            (it?.name?.contains(value, true) ?: false)
        }
    ) { card, resolve ->
        if (card == null) {
            Div({
                classes(AppStyles.groupItem, AppStyles.groupItemNone)
                focusable()
                onClick {
                    onSelected(null)
                    resolve(false)
                }
            }) {
                Span({
                    classes("material-symbols-outlined")
                    style { marginRight(.5.r) }
                }) {
                    Text("block")
                }
                Text(application.appString { none })
            }
        } else {
            CardItem(
                card = card,
                scroll = false,
                selected = false,
                saved = saves.cards.value.any { it.id == card.id },
                published = card.active == true
            ) {
                if (!it) {
                    onSelected(card)
                    resolve(false)
                }
            }
        }
    }
}
