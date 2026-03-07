package app.dialog

import api
import app.ailaai.api.createStatus
import application
import com.queatz.db.Status
import kotlinx.coroutines.CompletableDeferred
import org.jetbrains.compose.web.attributes.InputType
import org.jetbrains.compose.web.css.*
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.Input
import r

suspend fun createStatusDialog(
    initialColor: String = "#ffffff"
): Status? {
    var colorValue = initialColor
    val result = inputDialog(
        title = application.appString { custom },
        placeholder = application.appString { status },
        confirmButton = application.appString { create },
        content = { _, _, _ ->
            Div({
                style {
                    display(DisplayStyle.Flex)
                    flexDirection(FlexDirection.Column)
                    gap(.5.r)
                    padding(1.r, 0.r)
                }
            }) {
                Input(InputType.Color) {
                    defaultValue(colorValue)
                    onInput {
                        colorValue = it.value
                    }
                    style {
                        width(100.percent)
                        height(2.r)
                        cursor("pointer")
                    }
                }
            }
        }
    )

    if (result != null) {
        val deferred = CompletableDeferred<Status?>()
        api.createStatus(Status(name = result, color = colorValue), onError = {
            deferred.complete(null)
        }) {
            deferred.complete(it)
        }
        return deferred.await()
    }
    return null
}
