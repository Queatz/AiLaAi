package com.queatz.ailaai.ui.dialogs

import android.widget.Toast
import android.widget.Toast.LENGTH_SHORT
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.queatz.ailaai.Person
import com.queatz.ailaai.R
import com.queatz.ailaai.api

@Composable
fun EditProfileNameDialog(onDismissRequest: () -> Unit, initialValue: String, onUpdated: () -> Unit) {
    val context = LocalContext.current
    val didntWork = stringResource(R.string.didnt_work)

    TextFieldDialog(
        onDismissRequest,
        stringResource(R.string.your_name),
        stringResource(R.string.update_your_name),
        true,
        initialValue,
    ) { value ->
        try {
            api.updateMe(Person(name = value.trim()))
            onUpdated()
            onDismissRequest()
        } catch (ex: Exception) {
            Toast.makeText(context, didntWork, LENGTH_SHORT).show()
            ex.printStackTrace()
        }
    }
}
