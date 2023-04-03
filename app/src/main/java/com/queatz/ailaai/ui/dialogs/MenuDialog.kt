package com.queatz.ailaai.ui.dialogs

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.window.Dialog
import com.queatz.ailaai.ui.theme.PaddingDefault

@DslMarker
annotation class MenuDialogMarker


@Composable
@MenuDialogMarker
fun Menu(onDismissRequest: () -> Unit, block: @Composable ColumnScope.() -> Unit) {
    Dialog(onDismissRequest) {
        Surface(
            shape = MaterialTheme.shapes.extraLarge,
            modifier = Modifier
                .padding(PaddingDefault * 2)
                .wrapContentHeight()
        ) {
            val scrollState = rememberScrollState()
            Column(
                modifier = Modifier
                    .padding(PaddingDefault * 1)
                    .verticalScroll(scrollState)
            ) {
                block()
            }
        }
    }
}

@MenuDialogMarker
@Composable
fun item(title: String, action: () -> Unit) {
    DropdownMenuItem(
        text = {
            Text(title)
        },
        onClick = {
            action()
        },
        modifier = Modifier
            .clip(MaterialTheme.shapes.large)
    )
}
