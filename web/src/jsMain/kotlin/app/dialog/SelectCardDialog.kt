package app.dialog

import Configuration
import androidx.compose.runtime.Composable
import api
import app.ailaai.api.myCollaborations
import app.nav.CardItem
import appString
import application
import com.queatz.db.Card
import kotlinx.serialization.json.Json.Default.configuration
import saves

suspend fun selectCardDialog(
    configuration: Configuration,
    title: String = application.appString { card },
    onSelected: (Card) -> Unit
) {
    val cancel = application.appString { cancel }

    searchDialog(
        configuration = configuration,
        title = title,
        confirmButton = cancel,
        load = {
            var cards = emptyList<Card>()
            api.myCollaborations {
                cards = it
            }
            cards.sortedByDescending { card ->
                saves.cards.value.any { it.id == card.id }
            }
        },
        filter = { it, value ->
            (it.name?.contains(value, true) ?: false)
        }
    ) { card, resolve ->
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
