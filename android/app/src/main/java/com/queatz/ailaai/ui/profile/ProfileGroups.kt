package com.queatz.ailaai.ui.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import app.ailaai.api.groupsOfPerson
import com.queatz.ailaai.R
import com.queatz.ailaai.data.api
import com.queatz.ailaai.extensions.rememberStateOf
import com.queatz.ailaai.ui.components.ContactItem
import com.queatz.ailaai.ui.components.GroupInfo
import com.queatz.ailaai.ui.components.SearchResult
import com.queatz.ailaai.ui.theme.pad
import com.queatz.db.GroupExtended
import com.queatz.db.Person

@Composable
fun ProfileGroups(person: Person?) {
    var showAll by rememberStateOf(false)
    var groups by remember {
        mutableStateOf<List<GroupExtended>>(emptyList())
    }

    LaunchedEffect(person?.id) {
        api.groupsOfPerson(person?.id ?: return@LaunchedEffect) {
            groups = it
        }
    }

    if (groups.isNotEmpty()) {
        Column(
            modifier = Modifier.padding(1.pad),
            verticalArrangement = Arrangement.spacedBy(1.pad),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                stringResource(
                    R.string.x_is_a_member,
                    person?.name ?: stringResource(R.string.someone)
                ),
                modifier = Modifier
                    .padding(bottom = 1.pad)
                    .fillMaxWidth()
            )

            groups.let {
                if (showAll) {
                    it
                } else {
                    it.take(5)
                }
            }.forEach { group ->
                ContactItem(
                    SearchResult.Group(group),
                    onChange = {},
                    coverPhoto = true,
                    info = GroupInfo.Members
                )
            }

            if (!showAll && groups.size > 5) {
                OutlinedButton(
                    {
                        showAll = true
                    }
                ) {
                    Text(stringResource(R.string.show_more))
                }
            }
        }
    }
}
