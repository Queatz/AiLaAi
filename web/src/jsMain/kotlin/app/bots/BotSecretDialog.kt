package app.bots

import app.dialog.inputDialog
import application
import org.jetbrains.compose.web.css.marginTop
import org.jetbrains.compose.web.css.maxWidth
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.Text
import r

suspend fun botSecretDialog(secret: String) = inputDialog(
    // todo: translate
    title = "Secret",
    defaultValue = secret,
    confirmButton = application.appString { update },
) { _, _, _ ->
    Div({
        style {
            marginTop(.5.r)
            maxWidth(24.r)
        }
    }) {
        // todo: translate
        Text("The bot secret is sent along with bot installs to allow the bot server to verify the bot. It's not required.")
    }
}
