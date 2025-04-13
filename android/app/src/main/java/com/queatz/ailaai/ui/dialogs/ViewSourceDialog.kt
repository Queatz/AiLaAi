package com.queatz.ailaai.ui.dialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.ailaai.api.script
import com.queatz.ailaai.R
import com.queatz.ailaai.data.api
import com.queatz.ailaai.data.json
import com.queatz.ailaai.extensions.rememberStateOf
import com.queatz.ailaai.ui.components.DialogBase
import com.queatz.ailaai.ui.components.DialogLayout
import com.queatz.ailaai.ui.components.EmptyText
import com.queatz.ailaai.ui.story.asStoryContents
import com.queatz.ailaai.ui.theme.pad
import com.queatz.db.Script
import com.queatz.db.StoryContent
import com.queatz.widgets.Widgets
import com.queatz.widgets.widgets.ScriptData
import widget

@Composable
fun ViewSourceDialog(onDismissRequest: () -> Unit, content: String?) {
    DialogBase(
        onDismissRequest
    ) {
        DialogLayout(
            content = {
                val contents by remember(content) { mutableStateOf(content?.asStoryContents()) }
                val scriptsWidgets = remember(contents) {
                    contents?.filter { (it as? StoryContent.Widget)?.widget == Widgets.Script }
                }

                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                ) {
                    Text(
                        stringResource(R.string.view_source),
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier
                            .padding(bottom = 1.pad)
                    )
                }

                if (scriptsWidgets.isNullOrEmpty()) {
                    EmptyText(stringResource(R.string.this_page_has_no_scripts))
                } else {
                    SelectionContainer(
                        modifier = Modifier
                            .fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                        ) {
                            scriptsWidgets.forEach { widget ->
                                val id = (widget as StoryContent.Widget).id
                                key(id) {
                                    var script by rememberStateOf<Script?>(null)

                                    LaunchedEffect(widget) {
                                        api.widget(id) { widget ->
                                            try {
                                                val data = json.decodeFromString<ScriptData>(widget.data!!)
                                                api.script(data.script!!) {
                                                    script = it
                                                }
                                            } catch (_: Throwable) {
                                                // Ignored
                                            }
                                        }
                                    }

                                    script?.let { script ->
                                        Column(
                                            verticalArrangement = Arrangement.spacedBy(1.pad),
                                            modifier = Modifier
                                                .padding(bottom = 1.pad)
                                        ) {
                                            Text(
                                                script.name ?: stringResource(R.string.new_script),
                                                style = MaterialTheme.typography.titleMedium
                                            )
                                            Text(
                                                script.source ?: "",
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clip(MaterialTheme.shapes.large)
                                                    .background(MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp))
                                                    .padding(1.5f.pad)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            },
            actions = {
                DialogCloseButton(onDismissRequest)
            }
        )
    }
}
