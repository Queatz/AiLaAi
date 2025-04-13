package com.queatz.ailaai.ui.scripts

import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.queatz.ailaai.R
import com.queatz.ailaai.ui.components.DialogBase
import com.queatz.ailaai.ui.components.DialogLayout
import com.queatz.ailaai.ui.dialogs.DialogCloseButton
import com.queatz.db.Script

@Composable
fun SelectScriptDialog(
    selected: Script?,
    onDismissRequest: () -> Unit,
    onScript: (Script?) -> Unit
) {
    DialogBase(
        onDismissRequest = onDismissRequest
    ) {
        DialogLayout(
            scrollable = false,
            content = {
                SearchScriptsLayout(selected = selected, onScript = onScript)
            },
            actions = {
                if (selected != null) {
                    TextButton(
                        onClick = {
                            onScript(null)
                        },
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text(stringResource(R.string.remove))
                    }
                }
                DialogCloseButton(onDismissRequest)
            }
        )
    }
}
