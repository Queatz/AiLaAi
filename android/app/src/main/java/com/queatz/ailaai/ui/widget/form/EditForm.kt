package com.queatz.ailaai.ui.widget.form

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import app.ailaai.api.card
import app.ailaai.api.script
import com.queatz.ailaai.R
import com.queatz.ailaai.data.api
import com.queatz.ailaai.extensions.notBlank
import com.queatz.ailaai.extensions.rememberStateOf
import com.queatz.ailaai.ui.components.CardItem
import com.queatz.ailaai.ui.components.Check
import com.queatz.ailaai.ui.components.ChooseGroups
import com.queatz.ailaai.ui.components.ChooseScript
import com.queatz.ailaai.ui.dialogs.ChooseCardDialog
import com.queatz.ailaai.ui.dialogs.TextFieldDialog
import com.queatz.ailaai.ui.theme.pad
import com.queatz.db.Card
import com.queatz.db.Group
import com.queatz.db.Script
import com.queatz.widgets.widgets.FormData
import com.queatz.widgets.widgets.FormOptions

@Composable
fun EditForm(
    shareToGroups: List<Group>,
    formData: FormData,
    onFormData: (FormData) -> Unit
) {
    var showEditSubmitButtonTextDialog by rememberStateOf(false)

    val submitButtonText = formData.submitButtonText?.notBlank
        ?: stringResource(R.string.submit)

    if (showEditSubmitButtonTextDialog) {
        TextFieldDialog(
            onDismissRequest = {
                showEditSubmitButtonTextDialog = false
            },
            title = stringResource(R.string.edit_submit_button),
            button = stringResource(R.string.update),
            singleLine = true,
            maxLength = 64,
            requireNotBlank = true,
            initialValue = submitButtonText,
        ) {
            onFormData(
                formData.copy(
                    submitButtonText = it
                )
            )
            showEditSubmitButtonTextDialog = false
        }
    }

    DialogDivider()

    Text(
        text = stringResource(R.string.edit_submit_button_colon),
        modifier = Modifier.padding(bottom = 1.pad),
        style = MaterialTheme.typography.labelMedium
    )

    Button(
        onClick = {
            showEditSubmitButtonTextDialog = true
        },
        colors = ButtonDefaults.filledTonalButtonColors()
    ) {
        Text(submitButtonText)
    }

    DialogDivider()

    Text(
        text = stringResource(R.string.form_submissions_page),
        modifier = Modifier.padding(bottom = 1.pad),
        style = MaterialTheme.typography.labelMedium
    )

    var showCardDialog by rememberStateOf(false)
    var card by remember { mutableStateOf<Card?>(null) }

    LaunchedEffect(formData.page) {
        if (formData.page == null) {
            card = null
        } else {
            api.card(formData.page!!) {
                card = it
            }
        }
    }

    if (showCardDialog) {
        ChooseCardDialog(
            onDismissRequest = {
                showCardDialog = false
            },
        ) { page ->
            onFormData(
                formData.copy(
                    page = page
                )
            )
            showCardDialog = false
        }
    }

    CardItem(
        onClick = {
            showCardDialog = true
        },
        card = card,
        isChoosing = true,
        placeholder = stringResource(R.string.choose_page),
        modifier = Modifier.fillMaxWidth(.75f)
    )

    DialogDivider()

    Text(
        text = stringResource(R.string.forward_form_submissions),
        modifier = Modifier.padding(bottom = 1.pad),
        style = MaterialTheme.typography.labelMedium
    )

    ChooseGroups(
        groups = shareToGroups,
    ) { groups ->
        onFormData(
            formData.copy(
                groups = groups.map { it.id!! }
            )
        )
    }

    DialogDivider()

    Text(
        text = stringResource(R.string.run_a_script),
        modifier = Modifier.padding(bottom = 1.pad),
        style = MaterialTheme.typography.labelMedium
    )

    var script by rememberStateOf<Script?>(null)

    LaunchedEffect(formData.script) {
        if (formData.script == null) {
            script = null
        } else {
            api.script(formData.script!!) {
                script = it
            }
        }
    }

    ChooseScript(
        script = script,
    ) { script ->
        onFormData(
            formData.copy(
                script = script?.id
            )
        )
    }

    DialogDivider()

    Text(
        text = stringResource(R.string.options_),
        modifier = Modifier.padding(bottom = 1.pad),
        style = MaterialTheme.typography.labelMedium
    )

    Check(
        checked = formData.options?.enableAnonymousReplies == true,
        onCheckChange = {
            onFormData(
                formData.copy(
                    options = (formData.options ?: FormOptions()).copy(
                        enableAnonymousReplies = it
                    )
                )
            )
        }
    ) {
        Text(stringResource(R.string.enable_anonymous_replies))
    }
}

@Composable
private fun DialogDivider() {
    HorizontalDivider(modifier = Modifier.fillMaxWidth().padding(vertical = 2.pad))
}
