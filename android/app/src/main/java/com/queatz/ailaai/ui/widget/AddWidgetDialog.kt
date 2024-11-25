package com.queatz.ailaai.ui.widget

import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.res.stringResource
import com.queatz.ailaai.R
import com.queatz.ailaai.data.api
import com.queatz.ailaai.data.json
import com.queatz.ailaai.ui.dialogs.ChooseCardDialog
import com.queatz.ailaai.ui.dialogs.TextFieldDialog
import com.queatz.ailaai.ui.widget.form.EditFormDialog
import com.queatz.db.Widget
import com.queatz.widgets.Widgets
import com.queatz.widgets.widgets.FormData
import com.queatz.widgets.widgets.FormOptions
import com.queatz.widgets.widgets.PageTreeData
import com.queatz.widgets.widgets.WebData
import createWidget
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString

@Composable
fun AddWidgetDialog(
    onDismissRequest: () -> Unit,
    widget: Widgets,
    onWidget: (widget: Widget) -> Unit
) {
    val scope = rememberCoroutineScope()

    when (widget) {
        Widgets.Script -> {
            AddScriptWidgetDialog(onDismissRequest, onWidget)
        }
        Widgets.Web -> {
            TextFieldDialog(
                onDismissRequest = onDismissRequest,
                title = stringResource(R.string.url),
                button = stringResource(R.string.add),
                singleLine = true,
                requireModification = false,
                requireNotBlank = true
            ) { url ->
                scope.launch {
                    api.createWidget(
                        widget = Widgets.Web,
                        data = json.encodeToString(WebData(url = url))
                    ) {
                        onWidget(it)
                    }
                }
            }
        }
        Widgets.PageTree -> {
            ChooseCardDialog(
                onDismissRequest = onDismissRequest
            ) { card ->
                scope.launch {
                    api.createWidget(
                        widget = Widgets.PageTree,
                        data = json.encodeToString(PageTreeData(card = card))
                    ) {
                        onWidget(it)
                    }
                }
            }
        }
        Widgets.Form -> {
            EditFormDialog(
                onDismissRequest = onDismissRequest
            ) {
                scope.launch {
                    api.createWidget(
                        widget = Widgets.Form,
                        data = json.encodeToString(FormData(options = FormOptions(enableAnonymousReplies = true)))
                    ) {
                        onWidget(it)
                    }
                }
            }
        }
        else -> {
            Throwable().printStackTrace()
            // Unsupported widget. Should never get here
        }
    }
}
