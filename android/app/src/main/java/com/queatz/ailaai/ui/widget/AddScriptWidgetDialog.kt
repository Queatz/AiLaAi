package com.queatz.ailaai.ui.widget

import androidx.compose.runtime.Composable
import com.queatz.ailaai.data.api
import com.queatz.ailaai.data.json
import com.queatz.ailaai.ui.scripts.NewScriptDecision
import com.queatz.ailaai.ui.scripts.PreviewScriptAction
import com.queatz.ailaai.ui.scripts.ScriptsDialog
import com.queatz.db.Script
import com.queatz.db.Widget
import com.queatz.widgets.Widgets
import com.queatz.widgets.widgets.ScriptData
import createWidget
import kotlinx.serialization.encodeToString

@Composable
fun AddScriptWidgetDialog(
    onDismissRequest: () -> Unit,
    onWidget: (widget: Widget) -> Unit
) {
    ScriptsDialog(
        onDismissRequest,
        previewScriptAction = PreviewScriptAction.AddData,
        onNewScript = {
            NewScriptDecision.AddData
        },
        onScriptWithData = { script: Script, data: String? ->
            api.createWidget(
                Widgets.Script,
                data = json.encodeToString(ScriptData(script.id!!, data))
            ) {
                onWidget(it)
            }
        }
    )
}
