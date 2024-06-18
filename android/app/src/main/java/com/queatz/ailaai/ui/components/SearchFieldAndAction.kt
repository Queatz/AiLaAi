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
import com.queatz.ailaai.ui.theme.pad

@Composable
fun SearchFieldAndAction(
    value: String,
    valueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = stringResource(R.string.search),
    showClear: Boolean = true,
    singleLine: Boolean = true,
    action: (@Composable () -> Unit)? = null,
    onAction: (() -> Unit)? = null
) {
    if (action == null) {
        SearchField(
            value = value,
            onValueChange = valueChange,
            placeholder = placeholder,
            showClear = showClear,
            singleLine = singleLine,
            modifier = modifier
                .padding(horizontal = 2.pad)
        )
    } else {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = modifier
                .padding(horizontal = 2.pad)
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .wrapContentWidth()
            ) {
                SearchField(
                    value = value,
                    onValueChange = valueChange,
                    placeholder = placeholder,
                    showClear = showClear,
                    singleLine = singleLine
                )
            }
            FloatingActionButton(
                onClick = {
                    onAction?.invoke()
                },
                modifier = Modifier
                    .padding(start = 2.pad)
            ) {
                action()
            }
        }
    }
}
