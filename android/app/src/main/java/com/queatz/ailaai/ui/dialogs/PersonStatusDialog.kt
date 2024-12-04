package com.queatz.ailaai.ui.dialogs

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import com.queatz.ailaai.R
import com.queatz.ailaai.extensions.contactPhoto
import com.queatz.ailaai.extensions.inList
import com.queatz.ailaai.ui.components.DialogBase
import com.queatz.ailaai.ui.components.DialogLayout
import com.queatz.ailaai.ui.components.GroupPhoto
import com.queatz.db.Person
import com.queatz.db.PersonStatus

@Composable
fun PersonStatusDialog(
    onDismissRequest: () -> Unit,
    person: Person,
    personStatus: PersonStatus?,
    onMessageClick: () -> Unit,
    onProfileClick: () -> Unit,
) {
    DialogBase(onDismissRequest) {
        DialogLayout(
            scrollable = false,
            content = {
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
                    Statuses(
                        personId = person.id!!,
                        initialValue = personStatus.inList(),
                        modifier = Modifier.weight(1f, fill = false)
                    ) {
                        // todo delete, set as my status
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
                TextButton(onDismissRequest) {
                    Text(stringResource(R.string.close))
                }
            }
        )
    }
}
