package app.invites

import Styles
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import api
import app.ailaai.api.createInvite
import app.dialog.inputDialog
import app.reminder.ReminderDateTime
import appText
import application
import com.queatz.db.Invite
import kotlinx.datetime.toKotlinInstant
import lib.addDays
import lib.format
import notBlank
import org.jetbrains.compose.web.attributes.InputType
import org.jetbrains.compose.web.attributes.min
import org.jetbrains.compose.web.attributes.type
import org.jetbrains.compose.web.css.marginBottom
import org.jetbrains.compose.web.css.padding
import org.jetbrains.compose.web.css.percent
import org.jetbrains.compose.web.css.width
import org.jetbrains.compose.web.dom.CheckboxInput
import org.jetbrains.compose.web.dom.Label
import org.jetbrains.compose.web.dom.Text
import org.jetbrains.compose.web.dom.TextInput
import parseDateTime
import r
import kotlin.js.Date
import kotlin.math.exp

suspend fun createInviteDialog(
    group: String,
    onError: () -> Unit,
    onInvite: (Invite) -> Unit
) {
    val initialDate = addDays(Date(), amount = 1.0)
    var hasTotal by mutableStateOf(false)
    var total by mutableStateOf(10)
    var expires by mutableStateOf(false)
    var expiresDate by mutableStateOf(format(initialDate, "yyyy-MM-dd"))
    var expiresTime by mutableStateOf(format(initialDate, "HH:mm"))

    val about = inputDialog(
        title = application.appString { createInviteLink },
        placeholder = application.appString { descriptionOptional },
        confirmButton = application.appString { create },
        singleLine = false,
        content = { resolve, _, _ ->
            Label(attrs = {
                style {
                    padding(1.r, .5.r)
                }
            }) {
                CheckboxInput(hasTotal) {
                    onChange {
                        hasTotal = it.value
                    }
                }
                appText { multipleUses }
            }

            if (hasTotal) {
                TextInput(total.toString()) {
                    classes(Styles.textarea)
                    style {
                        width(100.percent)
                        marginBottom(1.r)
                    }

                    type(InputType.Number)

                    min("1")

                    onKeyDown {
                        if (it.key == "Enter") {
                            it.preventDefault()
                            it.stopPropagation()
                            resolve(true)
                        }
                    }

                    onInput {
                        total = it.value.toIntOrNull() ?: 0
                    }
                }
            }

            Label(attrs = {
                style {
                    padding(0.r, .5.r, 1.r, .5.r)
                }
            }) {
                CheckboxInput(expires) {
                    onChange {
                        expires = it.value
                    }
                }
                appText { this.expires }
            }

            if (expires) {
                ReminderDateTime(
                    date = expiresDate,
                    time = expiresTime,
                    onDate = { expiresDate = it },
                    onTime = { expiresTime = it },
                    styles = {
                        padding(0.r, .5.r)
                    }
                )
            }
        }
    )

    if (about != null) {
        api.createInvite(
            invite = Invite(
                group = group,
                about = about.notBlank,
                expiry = if (expires) {
                    parseDateTime(expiresDate, expiresTime).toKotlinInstant()
                } else {
                    null
                },
                total = if (hasTotal) total.takeIf { it > 0 } else null,
            ),
            onError = {
                onError()
            }
        ) { invite ->
            onInvite(invite)
        }
    }
}
