package com.queatz.ailaai.group

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.queatz.ailaai.R
import com.queatz.ailaai.extensions.contactPhoto
import com.queatz.ailaai.extensions.inList
import com.queatz.ailaai.extensions.rememberStateOf
import com.queatz.ailaai.nav
import com.queatz.ailaai.services.joins
import com.queatz.ailaai.ui.components.GroupPhoto
import com.queatz.ailaai.ui.theme.PaddingDefault
import com.queatz.db.JoinRequestAndPerson
import kotlinx.coroutines.launch

@Composable
fun GroupJoinRequest(joinRequest: JoinRequestAndPerson, onChange: () -> Unit) {
    var isLoading by rememberStateOf(false)
    val scope = rememberCoroutineScope()
    val nav = nav
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(PaddingDefault),
        modifier = Modifier
            .clip(MaterialTheme.shapes.large)
            .clickable {
                nav.navigate("profile/${joinRequest.joinRequest!!.person!!}")
            }
            .padding(PaddingDefault)
    ) {
        GroupPhoto(joinRequest.person!!.contactPhoto().inList(), padding = 0.dp)
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                joinRequest.person?.name ?: stringResource(R.string.someone),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            if (joinRequest.joinRequest?.message.isNullOrBlank().not()) {
                Text(
                    joinRequest.joinRequest?.message ?: "",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.secondary
                )
            }
        }
        OutlinedButton(
            onClick = {
                scope.launch {
                    isLoading = true
                    joins.delete(joinRequest.joinRequest!!.id!!)
                    onChange()
                    isLoading = false
                }
            },
            enabled = !isLoading
        ) {
            Text(stringResource(R.string.delete))
        }
        Button(
            onClick = {
                scope.launch {
                    isLoading = true
                    joins.accept(joinRequest.joinRequest!!.id!!)
                    onChange()
                    isLoading = false
                }
            },
            enabled = !isLoading
        ) {
            Text(stringResource(R.string.accept))
        }
    }
}
