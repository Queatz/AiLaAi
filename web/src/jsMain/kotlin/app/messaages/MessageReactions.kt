package app.messaages

import Styles
import androidx.compose.runtime.Composable
import application
import com.queatz.db.ReactionSummary
import org.jetbrains.compose.web.css.JustifyContent
import org.jetbrains.compose.web.css.justifyContent
import org.jetbrains.compose.web.css.marginTop
import org.jetbrains.compose.web.css.paddingLeft
import org.jetbrains.compose.web.dom.B
import org.jetbrains.compose.web.dom.Button
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.Span
import org.jetbrains.compose.web.dom.Text
import r

@Composable
fun MessageReactions(
    reactions: ReactionSummary,
    isMe: Boolean,
    onReact: (String) -> Unit,
    onRemoveReaction: (String) -> Unit
) {
    Div({
        classes(Styles.reactionsLayout)

        style {
            marginTop(.5.r)

            if (isMe) {
                justifyContent(JustifyContent.FlexEnd)
            }
        }
    }) {
        reactions.all.forEach { reaction ->
            val isMyReaction = reactions.mine?.any { it.reaction == reaction.reaction } == true

            Button({
                classes(Styles.outlineButton, Styles.outlineButtonSmall)

                if (isMyReaction) {
                    classes(Styles.outlineButtonTonal)
                }

                onClick {
                    if (isMyReaction) {
                        onRemoveReaction(reaction.reaction)
                    } else {
                        onReact(reaction.reaction)
                    }
                }

                title(
                    if (isMyReaction) {
                        application.appString { tapToRemove }
                    } else {
                        // todo: translate
                        "Tap to react"
                    }
                )
            }) {
                Span {
                    Text(reaction.reaction)
                }
                B({
                    style {
                        paddingLeft(.25.r)
                    }
                }) {
                    Text("${reaction.count}")
                }
            }
        }
    }
}
