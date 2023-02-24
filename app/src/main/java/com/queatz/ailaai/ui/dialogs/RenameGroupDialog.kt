package com.queatz.ailaai.ui.dialogs

import android.widget.Toast
import android.widget.Toast.LENGTH_SHORT
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.window.Dialog
import com.queatz.ailaai.*
import com.queatz.ailaai.R
import com.queatz.ailaai.ui.theme.PaddingDefault
import kotlinx.coroutines.launch

@OptIn(ExperimentalComposeUiApi::class, ExperimentalMaterial3Api::class)
@Composable
fun RenameGroupDialog(onDismissRequest: () -> Unit, group: Group, onGroupUpdated: (Group) -> Unit) {
    val keyboardController = LocalSoftwareKeyboardController.current!!
    var disableSubmit by remember { mutableStateOf(true) }
    val coroutineScope = rememberCoroutineScope()
    var text by remember { mutableStateOf(group.name ?: "") }
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    Dialog(onDismissRequest) {
        Surface(
            shape = MaterialTheme.shapes.extraLarge,
            modifier = Modifier
                .padding(PaddingDefault * 2)
        ) {
            Column(
                modifier = Modifier
                    .padding(PaddingDefault * 3)
            ) {
                Text(
                    stringResource(R.string.rename_group),
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(bottom = PaddingDefault)
                )
                OutlinedTextField(
                    text,
                    onValueChange = {
                        text = it
                        disableSubmit = false
                    },
                    label = { Text(stringResource(R.string.group_name)) },
                    shape = MaterialTheme.shapes.large,
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Words,
                        imeAction = ImeAction.Next
                    ),
                    keyboardActions = KeyboardActions(onSearch = {
                        keyboardController.hide()
                    }),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = PaddingDefault)
                        .focusRequester(focusRequester)
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(PaddingDefault, Alignment.End),
                    verticalAlignment = Alignment.Bottom,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val context = LocalContext.current
                    val didntWork = stringResource(R.string.didnt_work)
                    TextButton(
                        {
                            disableSubmit = true

                            coroutineScope.launch {
                                try {
                                    val group = api.updateGroup(group.id!!, Group().apply { name = text })
                                    onGroupUpdated(group)
                                    onDismissRequest()
                                } catch (ex: Exception) {
                                    Toast.makeText(context, didntWork, LENGTH_SHORT).show()
                                    ex.printStackTrace()
                                } finally {
                                    disableSubmit = false
                                }
                            }
                        },
                        enabled = !disableSubmit,
                        modifier = Modifier.align(Alignment.CenterVertically)
                    ) {
                        Text(stringResource(R.string.rename), textAlign = TextAlign.End)
                    }
                }
            }
        }
    }
}
