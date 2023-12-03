package app.group

import Styles
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import app.dialog.inputDialog
import appText
import application
import com.queatz.db.GroupExtended
import joins
import kotlinx.coroutines.launch
import org.jetbrains.compose.web.css.*
import org.jetbrains.compose.web.dom.Button
import org.jetbrains.compose.web.dom.Div
import r

@Composable
fun JoinGroupLayout(group: GroupExtended) {
    val scope = rememberCoroutineScope()
    val joinRequests by joins.myJoins.collectAsState()
    val joinRequestId = joinRequests.find { it.joinRequest?.group == group.group?.id }?.joinRequest?.id

    fun join() {
        scope.launch {
            val result = inputDialog(
                application.appString { joinGroup },
                placeholder = application.appString { message },
                confirmButton = application.appString { sendRequest },
                singleLine = false
            )

            if (result != null) {
                joins.join(group.group!!.id!!, result)
            }
        }
    }

    fun cancelJoin() {
        scope.launch {
            joinRequestId?.let {
                joins.delete(it)
            }
        }
    }

    Div({
        style {
            padding(1.r)
            display(DisplayStyle.Flex)
            justifyContent(JustifyContent.Center)
        }
    }) {
        if (joinRequestId != null) {
            Button({
                classes(Styles.outlineButton)
                onClick {
                    cancelJoin()
                }
            }) {
                appText { cancelJoinRequest }
            }
        } else {
            Button({
                classes(Styles.button)
                onClick {
                    join()
                }
            }) {
                appText { joinGroup }
            }
        }
    }
}
