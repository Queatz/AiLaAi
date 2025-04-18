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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
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
fun menuItem(
    title: String,
    icon: ImageVector? = null,
    textIcon: String? = null,
    modifier: Modifier = Modifier,
    action: () -> Unit
) {
    DropdownMenuItem(
        text = {
            Text(title)
        },
        onClick = {
            action()
        },
        trailingIcon = {
            when {
                textIcon != null -> {
                    Text(
                        text = textIcon,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        modifier = Modifier
                            .padding(.5f.pad)
                            .alpha(0.5f)
                    )
                }
                icon != null -> {
                    Icon(icon, null)
                }
            }
        },
        modifier = modifier
            .clip(MaterialTheme.shapes.large)
    )
}
