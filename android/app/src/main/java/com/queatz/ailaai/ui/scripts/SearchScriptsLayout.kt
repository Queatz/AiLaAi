package com.queatz.ailaai.ui.scripts

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import app.ailaai.api.myScripts
import app.ailaai.api.scripts
import com.queatz.ailaai.R
import com.queatz.ailaai.data.api
import com.queatz.ailaai.extensions.notBlank
import com.queatz.ailaai.extensions.rememberStateOf
import com.queatz.ailaai.ui.theme.pad
import com.queatz.db.Script

@Composable
fun ColumnScope.SearchScriptsLayout(
    selected: Script? = null,
    onlyMine: Boolean = false,
    onScript: (Script) -> Unit
) {
    val focusRequester = remember { FocusRequester() }
    var search by rememberStateOf("")
    var scripts by rememberStateOf(emptyList<Script>())

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    LaunchedEffect(search) {
        if (onlyMine) {
            api.myScripts {
                scripts = it.filter {
                    it.name?.contains(search, ignoreCase = true) == true ||
                            it.description?.contains(search, ignoreCase = true) == true
                }
            }
        } else {
            api.scripts(search.notBlank) {
                scripts = it
            }
        }
    }

    OutlinedTextField(
        value = search,
        onValueChange = { search = it },
        label = { Text(stringResource(R.string.search_scripts)) },
        shape = MaterialTheme.shapes.large,
        singleLine = true,
        keyboardOptions = KeyboardOptions(
            capitalization = KeyboardCapitalization.Sentences
        ),
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 1.pad)
            .focusRequester(focusRequester)
    )

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(1.pad),
        modifier = Modifier
            .weight(1f)
    ) {
        items(scripts) { script ->
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .let {
                        if (selected?.id == script.id) {
                            it.border(1.dp, MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.shapes.large)
                        } else {
                            it
                        }
                    }
                    .clip(MaterialTheme.shapes.large)
                    .clickable {
                        onScript(script)
                    }
                    .padding(1.pad)
            ) {
                Text(
                    text = script.name?.notBlank ?: stringResource(R.string.new_script),
                )
                script.categories?.firstOrNull()?.let { category ->
                    Text(
                        text = category,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }
        }
    }
}
