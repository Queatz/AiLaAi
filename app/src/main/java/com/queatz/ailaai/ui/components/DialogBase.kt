package com.queatz.ailaai.ui.components

import android.os.Build
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.queatz.ailaai.ui.theme.PaddingDefault

@Composable
fun DialogBase(
    onDismissRequest: () -> Unit,
    dismissable: Boolean = true,
    dismissOnBackPress: Boolean = dismissable,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Dialog(
        onDismissRequest,
        properties = DialogProperties(
            decorFitsSystemWindows = Build.VERSION.SDK_INT < Build.VERSION_CODES.S, // Dialogs missing scrim
            usePlatformDefaultWidth = false,
            dismissOnBackPress = dismissOnBackPress,
            dismissOnClickOutside = dismissable
        )
    ) {
        Surface(
            shape = MaterialTheme.shapes.extraLarge,
            modifier = Modifier
                .padding(PaddingDefault * 2)
                .imePadding()
                .then(modifier)
        ) {
            content()
        }
    }
}
