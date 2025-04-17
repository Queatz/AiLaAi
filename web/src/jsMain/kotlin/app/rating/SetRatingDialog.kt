package app.rating


import Styles
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import app.dialog.dialog
import app.dialog.inputDialog
import appString
import appText
import application
import com.queatz.db.PersonStatus
import com.queatz.db.Rating
import components.IconButton
import kotlinx.coroutines.launch
import org.jetbrains.compose.web.attributes.InputType
import org.jetbrains.compose.web.css.DisplayStyle
import org.jetbrains.compose.web.css.display
import org.jetbrains.compose.web.css.flexShrink
import org.jetbrains.compose.web.css.fontSize
import org.jetbrains.compose.web.css.gap
import org.jetbrains.compose.web.css.maxWidth
import org.jetbrains.compose.web.css.overflow
import org.jetbrains.compose.web.css.paddingBottom
import org.jetbrains.compose.web.css.percent
import org.jetbrains.compose.web.css.px
import org.jetbrains.compose.web.css.whiteSpace
import org.jetbrains.compose.web.css.width
import org.jetbrains.compose.web.dom.Button
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.Text
import r
import withPlus

// todo: numbers only
suspend fun setRatingDialog(
    initialRating: Int? = null,
    confirmButton: String = application.appString { rate },
    onRemoveRating: () -> Unit
) = inputDialog(
    title = null,
    placeholder = application.appString { custom },
    confirmButton = confirmButton,
    singleLine = true,
    inputStyles = {
        width(100.percent)
    },
    type = InputType.Number,
    defaultValue = initialRating?.toString().orEmpty(),
    extraButtons = { resolve ->
        val scope = rememberCoroutineScope()
        if (initialRating != null) {
            IconButton("delete", title = appString { removeRating }) {
                onRemoveRating()
                resolve(false)
            }
        } else {
            IconButton("help", appString { help }) {
                scope.launch {
                    dialog(title = null, cancelButton = null) {
                        appText { yourRatingsAreVisible }
                    }
                }
            }
        }
    },
    topContent = { resolve, value, onValue ->
        val common = remember {
            listOf(
                -2,
                -1,
                0,
                1,
                2
            )
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
            common.forEach { rating ->
                Button(
                    attrs = {
                        classes(Styles.outlineButton, Styles.outlineButtonAlt)

                        style {
                            flexShrink(0)
                            whiteSpace("nowrap")

                        }

                        onClick {
                            onValue(rating.withPlus())
                            resolve(true)
                        }
                    }
                ) {
                    Text(rating.withPlus())
                }
            }
        }
    }
)
