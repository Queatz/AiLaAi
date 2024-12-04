package com.queatz.ailaai.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.vector.ImageVector
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
    useMaxWidth: Boolean = true,
    useMaxHeight: Boolean = false,
    autoFocus: Boolean = false,
    icon: ImageVector? = null,
    endIcon: ImageVector? = null,
    endIconTitle: String? = null,
    imeAction: ImeAction = if (singleLine) ImeAction.Search else ImeAction.Default,
    onEndAction: () -> Unit = {},
    onClear: () -> Unit = { onValueChange("") },
    onAction: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val keyboardController = LocalSoftwareKeyboardController.current!!
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(autoFocus) {
        if (autoFocus) {
            focusRequester.requestFocus()
        }
    }

    Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.elevatedCardColors(),
        elevation = CardDefaults.elevatedCardElevation(.5f.elevation),
        modifier = modifier
            .then(if (useMaxWidth) Modifier.widthIn(max = 480.dp) else Modifier)
            .fillMaxWidth()
    ) {
        OutlinedTextField(
            value = value,
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
                }
            ),
            leadingIcon = if (icon != null) {
                {
                    Icon(icon, contentDescription = null)
                }
            } else {
                null
            },
            trailingIcon = if (showClear && value.isNotEmpty()) {
                {
                    Icon(
                        imageVector = Icons.Outlined.Close,
                        contentDescription = stringResource(R.string.clear),
                        modifier = Modifier
                            .clip(MaterialTheme.shapes.medium)
                            .clickable {
                                onClear()
                            }
                    )
                }
            } else if (endIcon != null) {
                {
                    Icon(
                        imageVector = endIcon,
                        contentDescription = endIconTitle,
                        modifier = Modifier
                            .clip(MaterialTheme.shapes.medium)
                            .clickable {
                                onEndAction()
                            }
                    )
                }
            } else {
                null
            },
            modifier = Modifier
                .fillMaxWidth()
                .then(if (useMaxHeight) Modifier.heightIn(max = 128.dp) else Modifier)
                .clip(MaterialTheme.shapes.large)
                .background(MaterialTheme.colorScheme.surface)
                .focusRequester(focusRequester)
        )
    }
}
