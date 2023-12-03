package app.group

import Styles
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import app.AppStyles
import appText
import com.queatz.db.JoinRequestAndPerson
import components.ProfilePhoto
import joins
import kotlinx.browser.window
import kotlinx.coroutines.launch
import notBlank
import org.jetbrains.compose.web.css.*
import org.jetbrains.compose.web.dom.Button
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.Text
import r

@Composable
fun GroupJoinRequest(joinRequest: JoinRequestAndPerson, onChange: () -> Unit) {
    val scope = rememberCoroutineScope()

    Div({
        style {
            margin(
                0.r,
                1.r,
                1.r,
                1.r
            )
            padding(1.r)
            borderRadius(1.r)
            backgroundColor(Styles.colors.dark.background)
            display(DisplayStyle.Flex)
            alignItems(AlignItems.Center)
        }
    }) {
        ProfilePhoto(joinRequest.person!!, size = 54.px, onClick = {
            window.open("/profile/${joinRequest.person!!.id}", "_blank")
        }) {
            marginRight(1.r)
        }
        Div({
            style {
                width(0.px)
                flexGrow(1)
            }
        }) {
            Div({

            }) {
                Text(joinRequest.person!!.name ?: "Someone")
            }
            joinRequest.joinRequest?.message?.notBlank?.let { message ->
                Div({
                    classes(AppStyles.groupItemMessage)
                }) {
                    Text(message)
                }
            }
        }
        Button({
            classes(Styles.textButton)
            style {
                marginRight(1.r)
            }
            onClick {
                scope.launch {
                    joins.delete(joinRequest.joinRequest!!.id!!)
                    onChange()
                }
            }
        }) {
            appText { delete }
        }
        Button({
            classes(Styles.button)
            onClick {
                scope.launch {
                    joins.accept(joinRequest.joinRequest!!.id!!)
                    onChange()
                }
            }
        }) {
            appText { accept }
        }
    }
}
