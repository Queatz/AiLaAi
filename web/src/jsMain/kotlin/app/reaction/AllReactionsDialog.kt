package app.reaction

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import api
import app.AppStyles
import app.ailaai.api.messageReactions
import app.dialog.dialog
import appString
import application
import bulletedString
import com.queatz.db.ReactionAndPerson
import components.Loading
import components.ProfilePhoto
import kotlinx.browser.window
import lib.formatDistanceToNow
import org.jetbrains.compose.web.css.DisplayStyle
import org.jetbrains.compose.web.css.FlexDirection
import org.jetbrains.compose.web.css.display
import org.jetbrains.compose.web.css.flexDirection
import org.jetbrains.compose.web.css.gap
import org.jetbrains.compose.web.css.maxWidth
import org.jetbrains.compose.web.css.overflow
import org.jetbrains.compose.web.css.percent
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.Text
import r
import kotlin.js.Date

suspend fun allReactionsDialog(messageId: String) = dialog(
    // todo: show count in title
    title = application.appString { messageReactions },
    cancelButton = null,
    confirmButton = application.appString { close }
) {
    var reactions by remember {
        mutableStateOf(emptyList<ReactionAndPerson>())
    }

    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        api.messageReactions(messageId) {
            reactions = it
        }
        isLoading = false
    }

    if (isLoading) {
        Loading()
    } else {
        reactions.forEach { reaction ->
            Div(
                {
                    classes(AppStyles.groupItem, AppStyles.groupItemOnSurface)

                    style {
                        gap(1.r)
                    }

                    onClick {
                        window.open("/profile/${reaction.person!!.id}", "_blank")
                    }

                    title(application.appString { viewProfile })
                }
            ) {
                ProfilePhoto(reaction.person!!)
                Div({
                    style {
                        display(DisplayStyle.Flex)
                        flexDirection(FlexDirection.Column)
                        overflow("hidden")
                        maxWidth(100.percent)
                    }
                }) {
                    Div({
                        classes(AppStyles.groupItemName)
                    }) {
                        Text(reaction.person?.name ?: appString { someone })
                    }
                    Div({
                        classes(AppStyles.groupItemMessage)
                    }) {
                        Text(
                            bulletedString(
                                reaction.reaction?.comment,
                                reaction.reaction?.reaction,
                                reaction.reaction?.createdAt?.let {
                                    formatDistanceToNow(
                                        Date(it.toEpochMilliseconds()),
                                        js("{ addSuffix: true }")
                                    )
                                }
                            )
                        )
                    }
                }
            }
        }
    }
}
