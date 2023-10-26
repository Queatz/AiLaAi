package com.queatz.ailaai.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material3.FloatingActionButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.queatz.ailaai.R
import com.queatz.ailaai.ui.theme.PaddingDefault

@Composable
fun SearchFieldAndAction(
    value: String,
    valueChange: (String) -> Unit,
    placeholder: String = stringResource(R.string.search),
    action: (@Composable () -> Unit)? = null,
    onAction: (() -> Unit)? = null
) {
    if (action == null) {
        SearchField(
            value,
            valueChange,
            placeholder
        )
    } else {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .wrapContentWidth()
            ) {
                SearchField(
                    value,
                    valueChange,
                    placeholder
                )
            }
            FloatingActionButton(
                onClick = {
                    onAction?.invoke()
                },
                modifier = Modifier
                    .padding(start = PaddingDefault * 2)
            ) {
                action()
            }
        }
    }
}
