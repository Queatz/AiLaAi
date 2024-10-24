package app.dialog

import app.components.StatusName
import application
import baseUrl
import com.queatz.db.Person
import com.queatz.db.PersonStatus
import components.ProfilePhoto
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import lib.formatDistanceToNow
import org.jetbrains.compose.web.css.AlignItems
import org.jetbrains.compose.web.css.DisplayStyle
import org.jetbrains.compose.web.css.FlexDirection
import org.jetbrains.compose.web.css.alignItems
import org.jetbrains.compose.web.css.backgroundImage
import org.jetbrains.compose.web.css.backgroundPosition
import org.jetbrains.compose.web.css.backgroundSize
import org.jetbrains.compose.web.css.borderRadius
import org.jetbrains.compose.web.css.cursor
import org.jetbrains.compose.web.css.display
import org.jetbrains.compose.web.css.flexDirection
import org.jetbrains.compose.web.css.fontSize
import org.jetbrains.compose.web.css.height
import org.jetbrains.compose.web.css.marginTop
import org.jetbrains.compose.web.css.opacity
import org.jetbrains.compose.web.css.paddingBottom
import org.jetbrains.compose.web.css.paddingTop
import org.jetbrains.compose.web.css.px
import org.jetbrains.compose.web.css.width
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.Text
import r
import kotlin.js.Date

suspend fun personStatusDialog(person: Person, status: PersonStatus, scope: CoroutineScope) = dialog(
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
        status.photo?.let { photo ->
            Div({
                style {
                    marginTop(1.r)
                    backgroundImage("url($baseUrl$photo)")
                    backgroundPosition("center")
                    backgroundSize("cover")
                    borderRadius(1.r)
                    height(4.r)
                    width(4.r)
                    cursor("pointer")
                }

                onClick {
                    scope.launch {
                        photoDialog("$baseUrl$photo")
                    }
                }
            }) {}
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
