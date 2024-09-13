package app.dialog

import Styles
import app.components.StatusName
import application
import com.queatz.db.Person
import com.queatz.db.PersonStatus
import components.ProfilePhoto
import lib.formatDistanceToNow
import org.jetbrains.compose.web.css.AlignItems
import org.jetbrains.compose.web.css.Color
import org.jetbrains.compose.web.css.DisplayStyle
import org.jetbrains.compose.web.css.FlexDirection
import org.jetbrains.compose.web.css.alignItems
import org.jetbrains.compose.web.css.backgroundColor
import org.jetbrains.compose.web.css.display
import org.jetbrains.compose.web.css.flexDirection
import org.jetbrains.compose.web.css.fontSize
import org.jetbrains.compose.web.css.gap
import org.jetbrains.compose.web.css.opacity
import org.jetbrains.compose.web.css.paddingBottom
import org.jetbrains.compose.web.css.paddingTop
import org.jetbrains.compose.web.css.px
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.Span
import org.jetbrains.compose.web.dom.Text
import r
import kotlin.js.Date

suspend fun personStatusDialog(person: Person, status: PersonStatus) = dialog(
    title = null,
    confirmButton = application.appString { profile },
    cancelButton = application.appString { close },
) {
    Div({
        style {
            display(DisplayStyle.Flex)
            flexDirection(FlexDirection.Column)
            alignItems(AlignItems.Center)
        }
    }) {
        ProfilePhoto(person = person, size = 54.px)
        Div({
            style {
                fontSize(18.px)
                paddingTop(.5.r)
            }
        }) {
            Text(person.name ?: application.appString { someone })
        }
        status.statusInfo?.let { status ->
            StatusName(status)
        }
        status.note?.let { note ->
            Div({
                style {
                    paddingTop(1.r)
                    fontSize(20.px)
                }
            }) {
                Text(note)
            }
        }
        Div({
            style {
                opacity(.5f)
                paddingBottom(1.r)
            }
        }) {
            Text(
                formatDistanceToNow(
                    Date(status.createdAt!!.toEpochMilliseconds()),
                    js("{ addSuffix: true }")
                )
            )
        }
    }
}
