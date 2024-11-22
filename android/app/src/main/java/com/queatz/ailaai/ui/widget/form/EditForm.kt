package com.queatz.ailaai.ui.widget.form

import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.queatz.ailaai.R
import com.queatz.ailaai.extensions.notBlank
import com.queatz.ailaai.ui.components.Check
import com.queatz.widgets.widgets.FormData
import com.queatz.widgets.widgets.FormOptions

@Composable
fun EditForm(
    formData: FormData,
    onFormData: (FormData) -> Unit
) {
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
            // todo show edit text dialog
        },
        colors = ButtonDefaults.filledTonalButtonColors()
    ) {
        Text(
            formData.submitButtonText?.notBlank
                ?: stringResource(R.string.submit)
        )
    }
}
