package com.queatz.ailaai.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.queatz.ailaai.R
import com.queatz.ailaai.ui.theme.elevation

@Composable
fun SearchField(
    value: String,
    onValueChange: (value: String) -> Unit,
    placeholder: String = stringResource(R.string.search),
    showClear: Boolean = true,
    singleLine: Boolean = true,
    imeAction: ImeAction = if (singleLine) ImeAction.Search else ImeAction.Default,
    onAction: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val keyboardController = LocalSoftwareKeyboardController.current!!
    Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.elevatedCardColors(),
        elevation = CardDefaults.elevatedCardElevation(.5f.elevation),
        modifier = modifier
            .widthIn(max = 480.dp)
            .fillMaxWidth()
    ) {
        OutlinedTextField(
            value,
            onValueChange = onValueChange,
            placeholder = {
                Text(
                    placeholder,
                    maxLines = 1,
                    style = MaterialTheme.typography.bodyMedium,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.alpha(.5f)
                )
            },
            shape = MaterialTheme.shapes.large,
            singleLine = singleLine,
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Sentences,
                imeAction = imeAction
            ),
            keyboardActions = KeyboardActions(
                onSearch = {
                keyboardController.hide()
                onAction()
            }, onDone = {
                keyboardController.hide()
                onAction()
            }),
            trailingIcon =
            if (showClear && value.isNotEmpty()) {
                {
                    Icon(
                        Icons.Outlined.Close,
                        stringResource(R.string.clear),
                        modifier = Modifier
                            .clip(MaterialTheme.shapes.medium)
                            .clickable {
                                onValueChange("")
                            }
                    )
                }
            } else null,
            modifier = Modifier
                .fillMaxWidth()
                .clip(MaterialTheme.shapes.large)
                .background(MaterialTheme.colorScheme.surface)
        )
    }
}
