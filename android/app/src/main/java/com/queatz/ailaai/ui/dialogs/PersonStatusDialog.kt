package com.queatz.ailaai.ui.dialogs

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Message
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.queatz.ailaai.R
import com.queatz.ailaai.extensions.contactPhoto
import com.queatz.ailaai.extensions.inList
import com.queatz.ailaai.extensions.rememberStateOf
import com.queatz.ailaai.ui.components.DialogBase
import com.queatz.ailaai.ui.components.DialogLayout
import com.queatz.ailaai.ui.components.GroupPhoto
import com.queatz.ailaai.ui.theme.pad
import com.queatz.db.Person
import com.queatz.db.PersonStatus
import com.queatz.db.Signal

@Composable
fun PersonStatusDialog(
    onDismissRequest: () -> Unit,
    person: Person,
    personStatus: PersonStatus?,
    onMessageClick: () -> Unit,
    onProfileClick: () -> Unit,
    onUseStatus: (PersonStatus) -> Unit,
    affinitySignals: List<Signal>? = null,
) {
    DialogBase(onDismissRequest) {
        DialogLayout(
            scrollable = false,
            content = {
                var showStatusMenu by rememberStateOf<PersonStatus?>(null)

                showStatusMenu?.let { status ->
                    Menu(
                        onDismissRequest = {
                            showStatusMenu = null
                        }
                    ) {
                        menuItem(
                            title = stringResource(R.string.use_status)
                        ) {
                            onUseStatus(status)
                            showStatusMenu = null
                        }
                    }
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false)
                ) {
                    GroupPhoto(
                        photos = person.contactPhoto().inList(),
                        modifier = Modifier.clickable {
                            onProfileClick()
                        }
                    )
                    Text(
                        text = person.name ?: stringResource(R.string.someone),
                        style = MaterialTheme.typography.titleMedium,
                        textAlign = TextAlign.Center
                    )
                    affinitySignals?.takeIf { it.isNotEmpty() }?.let { signals ->
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(1.pad, Alignment.CenterHorizontally),
                            verticalArrangement = Arrangement.spacedBy(1.pad, Alignment.CenterVertically),
                            modifier = Modifier
                                .padding(vertical = 2.pad)
                                .fillMaxWidth()
                        ) {
                            signals.forEach { signal ->
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.padding(horizontal = 0.5f.pad)
                                ) {
                                    Text(
                                        text = signal.emoji ?: "👋",
                                        style = MaterialTheme.typography.titleLarge
                                    )
                                    Text(
                                        text = signal.name ?: "",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.secondary
                                    )
                                }
                            }
                        }
                    }
                    Statuses(
                        personId = person.id!!,
                        initialValue = personStatus.inList(),
                        modifier = Modifier
                            .weight(1f, fill = false)
                            .heightIn(max = 240.dp)
                    ) {
                        showStatusMenu = it
                    }
                }
            },
            actions = {
                IconButton(onClick = onMessageClick) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.Message,
                        contentDescription = stringResource(R.string.message),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                Spacer(Modifier.weight(1f))
                DialogCloseButton(onDismissRequest)
            }
        )
    }
}
