package com.queatz.ailaai.ui.sheet

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.selection.DisableSelection
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import com.queatz.ailaai.R
import com.queatz.ailaai.ui.components.EmptyText
import com.queatz.ailaai.ui.components.Loading
import com.queatz.ailaai.ui.story.SheetContent
import com.queatz.ailaai.ui.theme.pad

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SheetHeader(
    title: String?,
    selected: SheetContent,
    onSelected: (SheetContent) -> Unit,
    onTitleClick: (() -> Unit)? = null,
    onExpandRequest: () -> Unit,
    isLoading: Boolean,
    isEmpty: Boolean,
) {
    DisableSelection {
        Column {
            Text(
                text = title ?: stringResource(R.string.earth),
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 1.pad)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                    ) {
                        onTitleClick?.invoke()
                    }
            )
            FlowRow(
                verticalArrangement = Arrangement.spacedBy(.5f.pad),
                horizontalArrangement = Arrangement.spacedBy(1.pad),
                modifier = Modifier
            ) {
                // todo
                listOf(
                    SheetContent.Posts,
                    SheetContent.Groups,
                    SheetContent.Pages,
                    // SheetContent.Events
                ).forEach { button ->
                    FilledTonalButton(
                        onClick = {
                            onSelected(button)
                            onExpandRequest()
                        },
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = if (selected == button) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = if (selected == button) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    ) {
                        // todo: translate
                        Text(button.name)
                        // todo load counts from server
                        //Text(" ${Random(button.name.hashCode()).nextInt(20)}", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                    }
                }
            }
            if (isLoading) {
                Loading(
                    modifier = Modifier
                        .padding(1.pad)
                )
            } else if (isEmpty) {
                EmptyText(stringResource(R.string.no_stories_to_read))
            }
        }
    }
}
