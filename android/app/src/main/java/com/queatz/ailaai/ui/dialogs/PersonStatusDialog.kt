package com.queatz.ailaai.ui.dialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Message
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.core.graphics.toColorInt
import com.queatz.ailaai.R
import com.queatz.ailaai.extensions.contactPhoto
import com.queatz.ailaai.extensions.inList
import com.queatz.ailaai.extensions.px
import com.queatz.ailaai.extensions.timeAgo
import com.queatz.ailaai.ui.components.DialogBase
import com.queatz.ailaai.ui.components.DialogLayout
import com.queatz.ailaai.ui.components.GroupPhoto
import com.queatz.ailaai.ui.theme.pad
import com.queatz.db.Person
import com.queatz.db.PersonStatus

@Composable
fun PersonStatusDialog(
    onDismissRequest: () -> Unit,
    person: Person,
    personStatus: PersonStatus?,
    onMessageClick: () -> Unit,
    onProfileClick: () -> Unit
) {
    DialogBase(onDismissRequest) {
        DialogLayout(
            content = {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    GroupPhoto(person.contactPhoto().inList(), modifier = Modifier.clickable {
                        onProfileClick()
                    })
                    Text(
                        text = person.name ?: stringResource(R.string.someone),
                        style = MaterialTheme.typography.titleMedium,
                        textAlign = TextAlign.Center
                    )
                    personStatus?.let { status ->
                        status.statusInfo?.let { info ->
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(.5f.pad, Alignment.CenterHorizontally),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .padding(.25f.pad)
                                        .size(12.dp)
                                        .shadow(3.dp, CircleShape)
                                        .clip(CircleShape)
                                        .background(info.color?.toColorInt()?.let { Color(it) } ?: MaterialTheme.colorScheme.background)
                                        .background(
                                            brush = Brush.radialGradient(
                                                colors = listOf(
                                                    Color.White.copy(alpha = .5f),
                                                    Color.White.copy(alpha = 0f)
                                                ),
                                                center = Offset(
                                                    4.5f.dp.px.toFloat(),
                                                    4.5f.dp.px.toFloat()
                                                ),
                                                radius = 9.dp.px.toFloat()
                                            ),
                                            shape = CircleShape
                                        )
                                        .zIndex(1f)
                                )
                                Text(
                                    text = info.name.orEmpty(),
                                    style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onBackground)
                                )
                            }
                        }
                        SelectionContainer {
                            status.note?.let { note ->
                                Text(
                                    text = note,
                                    style = MaterialTheme.typography.bodyLarge,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier
                                        .padding(top = 2.pad)
                                )
                            }
                        }
                        Text(
                            text = status.createdAt!!.timeAgo(),
                            style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.secondary),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(vertical = .5f.pad)
                        )
                    }
                }
            },
            actions = {
                IconButton(onClick = onMessageClick) {
                    Icon(
                        Icons.AutoMirrored.Outlined.Message,
                        stringResource(R.string.message),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                Spacer(Modifier.weight(1f))
                TextButton(onDismissRequest) {
                    Text(stringResource(R.string.close))
                }
            }
        )
    }
}
