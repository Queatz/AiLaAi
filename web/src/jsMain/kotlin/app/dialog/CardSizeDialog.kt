package app.dialog

import api
import app.ailaai.api.updateCard
import appString
import application
import com.queatz.db.Card
import org.jetbrains.compose.web.attributes.InputType
import org.jetbrains.compose.web.dom.Text

suspend fun cardSizeDialog(
    card: Card,
    onCardUpdated: (Card) -> Unit,
) {
    val result = inputDialog(
        title = application.appString { pageSize },
        placeholder = "",
        confirmButton = application.appString { update },
        defaultValue = card.size?.toString() ?: "",
        type = InputType.Number,
        decimal = true
    )

    if (result != null) {
        api.updateCard(
            id = card.id!!,
            card = Card(size = result.toDoubleOrNull() ?: 0.0)
        ) {
            onCardUpdated(it)
        }
    }
}
