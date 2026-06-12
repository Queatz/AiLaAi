package com.queatz.ailaai.ui.sheet

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.sp
import com.queatz.ailaai.R
import com.queatz.ailaai.extensions.bulletedString
import com.queatz.ailaai.extensions.notBlank
import com.queatz.ailaai.ui.components.EmptyText
import com.queatz.ailaai.ui.components.Loading
import com.queatz.ailaai.ui.story.SheetContent
import com.queatz.ailaai.ui.theme.pad

@Composable
fun SheetHeader(
    selected: SheetContent,
    onSelected: (SheetContent) -> Unit,
    onExpandRequest: () -> Unit
) {
    DisableSelection {
        Column {
            FlowRow(
                verticalArrangement = Arrangement.spacedBy(.5f.pad),
                horizontalArrangement = Arrangement.spacedBy(1.pad)
            ) {
                listOf(
                    SheetContent.Pages,
                    SheetContent.Posts,
                    SheetContent.Events,
                    SheetContent.Groups,
                    SheetContent.People
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
                        Text(
                            when (button) {
                                SheetContent.Pages -> stringResource(R.string.sheet_content_things_to_do)
                                SheetContent.Posts -> stringResource(R.string.sheet_content_posts)
                                SheetContent.Events -> stringResource(R.string.sheet_content_events)
                                SheetContent.Groups -> stringResource(R.string.sheet_content_communities)
                                SheetContent.People -> stringResource(R.string.sheet_content_friends)
                            }
                        )
                        // todo load counts from server
                        //Text(" ${Random(button.name.hashCode()).nextInt(20)}", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                    }
                }
            }
        }
    }
}
