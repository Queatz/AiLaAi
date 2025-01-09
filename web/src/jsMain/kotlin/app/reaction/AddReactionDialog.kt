package app.reaction

import Styles
import androidx.compose.runtime.remember
import app.dialog.inputDialog
import org.jetbrains.compose.web.css.DisplayStyle
import org.jetbrains.compose.web.css.display
import org.jetbrains.compose.web.css.fontSize
import org.jetbrains.compose.web.css.gap
import org.jetbrains.compose.web.css.maxWidth
import org.jetbrains.compose.web.css.overflow
import org.jetbrains.compose.web.css.paddingBottom
import org.jetbrains.compose.web.css.px
import org.jetbrains.compose.web.dom.Button
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.Text
import r

suspend fun addReactionDialog() = inputDialog(
    title = null,
    // todo: translate
    placeholder = "Custom",
    // todo: translate
    confirmButton = "React",
    maxLength = 64,
    topContent = { resolve, value, onValue ->
        val common = remember {
            // todo include from GroupExtended.topReactions and from my top reactions
            listOf(
                "\uD83D\uDE02",
                "\uD83D\uDE0E",
                "\uD83D\uDE32",
                "\uD83E\uDD73",
                "\uD83E\uDD17",
                "\uD83E\uDD14",
                "\uD83D\uDE18",
                "\uD83D\uDE2C",
            ).shuffled()
        }

        Div({
            style {
                display(DisplayStyle.Flex)
                gap(.5.r)
                fontSize(18.px)
                paddingBottom(1.r)
                maxWidth(32.r)
                overflow("auto")
            }
        }) {
            common.forEach { reaction ->
                Button(
                    {
                        classes(Styles.outlineButton)

                        onClick {
                            onValue(reaction)
                            resolve(true)
                        }
                    }
                ) {
                    Text(reaction)
                }
            }
        }
    }
)
