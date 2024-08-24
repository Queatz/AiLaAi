package components

import androidx.compose.runtime.Composable
import app.AppStyles
import com.queatz.db.Card
import focusable
import hint
import notBlank
import org.jetbrains.compose.web.css.fontWeight
import org.jetbrains.compose.web.css.marginLeft
import org.jetbrains.compose.web.css.padding
import org.jetbrains.compose.web.css.px
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.Text
import r

@Composable
fun CardListItem(card: Card, onClick: () -> Unit) {
    Div({
        classes(AppStyles.groupItem)

        focusable()

        style {
            padding(.5.r)
        }

        onClick {
            onClick()
        }
    }) {
        // todo: translate
        ProfilePhoto(card.photo, card.name ?: "New page", size = 54.px)
        Div({
            style {
                marginLeft(1.r)
                property("max-width", "calc(100% - 4rem)")
            }
        }) {
            Div({
                classes(AppStyles.groupItemName)

                style {
                    fontWeight("bold")
                }
            }) {
                // todo: translate
                Text(card.name ?: "New page")
            }
            Div({
                classes(AppStyles.groupItemMessage)
            }) {
                Text(
                    listOfNotNull(
                        card.categories?.first(),
                        card.hint.notBlank,
                        card.getConversation().message.notBlank
                    ).joinToString(" • ")
                )
            }
        }
    }
}
