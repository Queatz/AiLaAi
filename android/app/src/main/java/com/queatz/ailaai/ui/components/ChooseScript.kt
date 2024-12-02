package com.queatz.ailaai.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.GroupAdd
import androidx.compose.material.icons.outlined.HistoryEdu
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.unit.dp
import com.queatz.ailaai.R
import com.queatz.ailaai.extensions.bold
import com.queatz.ailaai.extensions.rememberStateOf
import com.queatz.ailaai.ui.scripts.SelectScriptDialog
import com.queatz.ailaai.ui.theme.pad
import com.queatz.db.Script

@Composable
fun ChooseScript(
    script: Script?,
    onScript: (Script?) -> Unit
) {
    var showScriptsDialog by rememberStateOf(false)

    if (showScriptsDialog) {
        SelectScriptDialog(
            selected = script,
            onDismissRequest = {
                showScriptsDialog = false
            }
        ) {
            showScriptsDialog = false
            onScript(it)
        }
    }

    if (script != null) {
        Row(
            horizontalArrangement = Arrangement.Start,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .clip(MaterialTheme.shapes.medium)
                .clickable {
                    showScriptsDialog = true
                }
                .padding(vertical = .5f.pad)
        ) {
            Icon(
                imageVector = Icons.Outlined.HistoryEdu,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(end = 1.pad)
            )
            Text(
                buildAnnotatedString {
                    append(stringResource(R.string.run))
                    append(" ")
                    bold {
                        append(script.name ?: "")
                    }
                },
                modifier = Modifier
                    .weight(1f, fill = false)
            )
            Icon(
                imageVector = Icons.Outlined.Edit,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .padding(horizontal = .5f.pad)
                    .size(16.dp)
            )
        }
    } else {
        OutlinedButton(
            onClick = {
                showScriptsDialog = true
            }
        ) {
            Icon(
                imageVector = Icons.Outlined.HistoryEdu,
                contentDescription = null,
                modifier = Modifier.padding(end = 1.pad)
            )
            Text(stringResource(R.string.select_script))
        }
    }
}
