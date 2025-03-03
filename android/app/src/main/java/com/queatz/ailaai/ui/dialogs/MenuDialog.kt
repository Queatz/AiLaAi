package com.queatz.ailaai.ui.dialogs

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.window.Dialog
import com.queatz.ailaai.ui.theme.pad

@DslMarker
annotation class MenuDialogMarker

@Composable
@MenuDialogMarker
fun Menu(
    onDismissRequest: () -> Unit,
    block: @Composable ColumnScope.() -> Unit,
) {
    Dialog(onDismissRequest) {
        Surface(
            shape = MaterialTheme.shapes.large,
            modifier = Modifier
                .padding(2.pad)
                .imePadding()
        ) {
            val scrollState = rememberScrollState()
            Column(
                modifier = Modifier
                    .verticalScroll(scrollState)
            ) {
                block()
            }
        }
    }
}

@MenuDialogMarker
@Composable
fun menuItem(title: String, icon: ImageVector? = null, action: () -> Unit) {
    DropdownMenuItem(
        text = {
            Text(title)
        },
        onClick = {
            action()
        },
        trailingIcon = {
            icon?.let { icon ->
                Icon(icon, null)
            }
        },
        modifier = Modifier
            .clip(MaterialTheme.shapes.large)
    )
}
