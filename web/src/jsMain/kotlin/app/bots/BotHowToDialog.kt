package app.bots

import app.AppStyles
import app.dialog.dialog
import org.jetbrains.compose.web.attributes.AttrsScope
import org.jetbrains.compose.web.css.Color
import org.jetbrains.compose.web.css.LineStyle
import org.jetbrains.compose.web.css.background
import org.jetbrains.compose.web.css.backgroundColor
import org.jetbrains.compose.web.css.border
import org.jetbrains.compose.web.css.borderRadius
import org.jetbrains.compose.web.css.margin
import org.jetbrains.compose.web.css.padding
import org.jetbrains.compose.web.css.px
import org.jetbrains.compose.web.css.whiteSpace
import org.jetbrains.compose.web.dom.B
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.H2
import org.jetbrains.compose.web.dom.H3
import org.jetbrains.compose.web.dom.H4
import org.jetbrains.compose.web.dom.Pre
import org.jetbrains.compose.web.dom.Text
import org.w3c.dom.HTMLElement
import r

suspend fun botHowToDialog() {
    dialog(
        "Bot specifications",
        cancelButton = null
    ) {
        Div {
            Div {
                Text("Bots should implement the following routes.")
            }
            H2 {
                Text("GET /")
            }
            Div {
                Text("Returns information about the bot.")
            }
            H4 {
                Text("Response")
            }
            Pre({
                codeBlock()
            }) {
                Text("""
                    {
                      name: "Bot name",
                      description: "Bot description",
                      keywords: ["keyword"] // null or empty to match all messages
                      config: [
                        { key: string, label: string, placeholder: string, type: "string"|"number", required: boolean}
                      ]
                    }
                """.trimIndent())
            }
            H2 {
                Text("POST /install")
            }
            Div {
                Text("Called when a bot is installed in a group.")
            }
            H4 {
                Text("Request")
            }
            Pre({
                codeBlock()
            }) {
                Text("""
                   {
                      groupId: string,
                      groupName: string,
                      webhook: string,
                      config: [ { key: string, value: string } ] // note that value is always a string, even if type was "number"
                      secret?: string // useful to verify the request is coming from the correct bot
                    }
                """.trimIndent())
            }
            H4 {
                Text("Response")
            }
            Pre({
                codeBlock()
            }) {
                Text("""
                   { token: string } // represents a bot installed in a group
                """.trimIndent())
            }
            H2 {
                Text("POST /reinstall")
            }
            Div {
                Text("Called when a bot config is updated for a group.")
            }
            H4 {
                Text("Request")
            }
            Div {
                B {
                    Text("Authorization: Bearer <token>")
                }
            }
            Pre({
                codeBlock()
            }) {
                Text("""
                   { config: [ { key: string, value: string } ] }
                """.trimIndent())
            }
            H4 {
                Text("Response")
            }
            Div {
                B {
                    Text("2XX OK")
                }
            }
            H2 {
                Text("POST /uninstall")
            }
            Div {
                Text("Called after bot was uninstalled from a group")
            }
            H4 {
                Text("Request")
            }
            Div {
                B {
                    Text("Authorization: Bearer <token>")
                }
            }
            H4 {
                Text("Response")
            }
            Div {
                B {
                    Text("2XX OK")
                }
            }
            H2 {
                Text("POST /message")
            }
            Div {
                Text("Called when a message in the group matches the keyword(s).")
            }
            H4 {
                Text("Request")
            }
            Div {
                B {
                    Text("Authorization: Bearer <token>")
                }
            }
            Pre({
                codeBlock()
            }) {
                Text("""
                   {
                       message: "A message",
                       person: { id: string, name: string } // for additional fields, GET api.ailaai.app/people/{id}/profile
                   }
                """.trimIndent())
            }
            H4 {
                Text("Response")
            }
            Pre({
                codeBlock()
            }) {
                Text("""
                   {
                       success: false | true,
                       note: "Optional reason",
                       actions?: [{ message: "Message to send to the group" }]
                   }
                """.trimIndent())
            }
            Text(
                """
                POST to the provided webhook for async actions
                """.trimIndent()
            )
        }
    }
}

fun AttrsScope<HTMLElement>.codeBlock() = style {
    backgroundColor(Color("#88888811"))
    border(1.px, LineStyle.Solid, Color("#88888844"))
    borderRadius(1.r)
    padding(1.r)
}
