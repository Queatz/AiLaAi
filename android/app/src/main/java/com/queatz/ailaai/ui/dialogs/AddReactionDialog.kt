package com.queatz.ailaai.ui.dialogs

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.layout.boundsInParent
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.res.stringResource
import com.queatz.ailaai.R
import com.queatz.ailaai.extensions.horizontalFadingEdge
import com.queatz.ailaai.ui.theme.pad

@Composable
fun AddReactionDialog(onDismissRequest: () -> Unit, onReaction: (String) -> Unit) {
    TextFieldDialog(
        onDismissRequest = onDismissRequest,
        title = null,
        button = stringResource(R.string.add),
        requireNotBlank = true,
        placeholder = stringResource(R.string.custom),
        maxLength = 64,
        extraContent = {
            var viewport by remember { mutableStateOf(Size(0f, 0f)) }
            val scrollState = rememberScrollState()
            val common = remember {
                listOf(
                    "\uD83D\uDE02",
                    "\uD83D\uDE0E",
                    "\uD83D\uDE32",
                    "\uD83E\uDD73",
                    "\uD83E\uDD17",
                    "\uD83E\uDD14",
                    "\uD83D\uDE18",
                    "\uD83D\uDE2C",
                ).shuffled()
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(1.pad),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 2.pad)
                    .horizontalScroll(scrollState)
                    .horizontalFadingEdge(viewport, scrollState)
                    .onPlaced { viewport = it.boundsInParent().size }
            ) {
                common.forEach {
                    OutlinedButton(
                        onClick = {
                            onReaction(it)
                        }
                    ) {
                        Text(it, style = MaterialTheme.typography.headlineSmall)
                    }
                }
            }
        }
    ) { prompt ->
        onReaction(prompt.trim())
    }
}
