package com.queatz.ailaai.reaction

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AddReaction
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextFieldDefaults.contentPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.queatz.ailaai.R
import com.queatz.ailaai.extensions.formatMini
import com.queatz.ailaai.ui.components.rememberLongClickInteractionSource
import com.queatz.ailaai.ui.theme.pad
import com.queatz.db.ReactionSummary
import kotlinx.coroutines.launch

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun Reactions(
    reactions: ReactionSummary?,
    modifier: Modifier = Modifier,
    selected: String? = null,
    alignment: Alignment.Horizontal = Alignment.Start,
    showReactCustom: Boolean = true,
    isSmall: Boolean = false,
    defaultReaction: String = "❤",
    onReact: (String) -> Unit,
    onReactCustom: (() -> Unit)? = null,
    onRemoveReaction: (String) -> Unit,
    onShowAllReactions: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val style = if (isSmall) MaterialTheme.typography.bodySmall else MaterialTheme.typography.bodyLarge
    val pad = if (isSmall) .25f.pad else .5f.pad
    val buttonModifier = if (isSmall) Modifier.height(36.dp) else Modifier
    val contentPadding = if (isSmall) PaddingValues(horizontal = 1.pad) else ButtonDefaults.ContentPadding

    FlowRow(
        verticalArrangement = Arrangement.spacedBy(1.pad, Alignment.CenterVertically),
        horizontalArrangement = Arrangement.spacedBy(1.pad, alignment = alignment),
        modifier = modifier
    ) {
        if (reactions?.all.isNullOrEmpty()) {
            if (showReactCustom) {
                OutlinedButton(
                    onClick = {
                        onReact(defaultReaction)
                    },
                    modifier = buttonModifier,
                    contentPadding = contentPadding
                ) {
                    Text(
                        text = defaultReaction,
                        style = style
                    )
                    Text(
                        text = stringResource(R.string.zero),
                        modifier = Modifier
                            .padding(start = pad)
                    )
                }
            }
        } else {
            reactions.all.forEach { reaction ->
                val mine = reactions.mine?.any {
                    it.reaction == reaction.reaction
                } == true

                key(reaction.reaction, reaction.count, mine) {
                    OutlinedButton(
                        onClick = {},
                        modifier = buttonModifier,
                        contentPadding = contentPadding,
                        colors = if (mine) ButtonDefaults.outlinedButtonColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer
                        ) else ButtonDefaults.outlinedButtonColors(),
                        border = if (selected == reaction.reaction) {
                            ButtonDefaults.outlinedButtonBorder.copy(
                                width = 2.dp,
                                brush = SolidColor(
                                    MaterialTheme.colorScheme.primary
                                )
                            )
                        } else {
                            ButtonDefaults.outlinedButtonBorder
                        },
                        interactionSource = rememberLongClickInteractionSource(
                            onClick = {
                                scope.launch {
                                    if (mine) {
                                        onShowAllReactions()
                                    } else {
                                        onReact(reaction.reaction)
                                    }
                                }
                            }
                        ) {
                            scope.launch {
                                if (mine) {
                                    onRemoveReaction(reaction.reaction)
                                } else {
                                    onShowAllReactions()
                                }
                            }
                        }
                    ) {
                        Text(
                            text = reaction.reaction,
                            style = style,
                            modifier = Modifier
                                .weight(1f, fill = false)
                        )
                        Text(
                            text = reaction.count.formatMini(),
                            style = LocalTextStyle.current.copy(fontWeight = FontWeight.Bold),
                            modifier = Modifier
                                .padding(start = pad)
                        )
                    }
                }
            }
        }

        if (showReactCustom) {
            OutlinedButton(
                onClick = {
                    onReactCustom?.invoke()
                },
                contentPadding = contentPadding,
                modifier = buttonModifier,
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            ) {
                Icon(
                    imageVector = Icons.Outlined.AddReaction,
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxHeight()
                )
            }
        }
    }
}
