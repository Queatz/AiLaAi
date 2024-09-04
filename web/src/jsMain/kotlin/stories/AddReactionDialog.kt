package stories

import androidx.compose.runtime.Composable
import app.components.EditField
import appString
import components.IconButton
import org.jetbrains.compose.web.css.JustifyContent
import org.jetbrains.compose.web.css.justifyContent
import org.jetbrains.compose.web.css.percent
import org.jetbrains.compose.web.css.width
import org.jetbrains.compose.web.dom.Div
import stories.StoryStyles.buttonRow
import stories.StoryStyles.dialog
import stories.StoryStyles.dialogContent
import stories.StoryStyles.reactionButton

@Composable
fun AddReactionDialog(
    onDismissRequest: () -> Unit,
    onAddReaction: (String) -> Unit
) {
    val availableReactions = listOf(
        "\uD83D\uDE02",
        "\uD83D\uDE0E",
        "\uD83D\uDE32",
        "\uD83E\uDD73",
        "\uD83E\uDD17",
        "\uD83E\uDD14",
        "\uD83D\uDE18",
        "\uD83D\uDE2C",
    )

    Div(attrs = {
        classes(dialog)
        onClick { onDismissRequest() }
    }) {
        Div(attrs = {
            classes(dialogContent)
            onClick { it.stopPropagation() }
        }) {
            Div(attrs = {
                classes(buttonRow)
            }) {
                availableReactions.forEach { reaction ->
                    Div(attrs = {
                        classes(reactionButton)
                    }) {
                        IconButton(
                            name = reaction,
                            title = reaction,
                            onClick = {
                                onAddReaction(reaction)
                                onDismissRequest()
                            }
                        )
                    }

                }
            }
            EditField(
                placeholder = appString { customReaction },
                styles = {
                    width(100.percent)
                },
                buttonBarStyles = {
                    width(100.percent)
                    justifyContent(JustifyContent.End)
                },
                showDiscard = false,
                resetOnSubmit = true,
                button = appString { addButton }
            ) {
                val success = false
                onAddReaction(it)
                onDismissRequest()
                success
            }

        }
    }
}
