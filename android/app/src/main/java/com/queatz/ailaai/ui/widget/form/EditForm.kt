package com.queatz.ailaai.ui.widget.form

import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import com.queatz.ailaai.R
import com.queatz.ailaai.extensions.notBlank
import com.queatz.ailaai.extensions.rememberStateOf
import com.queatz.ailaai.ui.components.Check
import com.queatz.ailaai.ui.dialogs.TextFieldDialog
import com.queatz.widgets.widgets.FormData
import com.queatz.widgets.widgets.FormOptions

@Composable
fun EditForm(
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
            title = "Edit submit button",
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
    Button(
        onClick = {
            showEditSubmitButtonTextDialog = true
        },
        colors = ButtonDefaults.filledTonalButtonColors()
    ) {
        Text(submitButtonText)
    }
}
