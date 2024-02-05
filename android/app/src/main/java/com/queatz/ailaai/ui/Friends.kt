package com.queatz.ailaai.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.queatz.ailaai.extensions.ContactPhoto
import com.queatz.ailaai.extensions.inList
import com.queatz.ailaai.extensions.shortAgo
import com.queatz.ailaai.ui.components.GroupPhoto
import com.queatz.ailaai.ui.theme.pad
import com.queatz.db.GroupExtended
import com.queatz.db.Person
import kotlinx.datetime.Instant

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun Friends(people: List<Person>, onLongClick: (Person) -> Unit, onClick: (Person) -> Unit) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(2.pad),
        contentPadding = PaddingValues(1.pad)
    ) {
        items(people) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .clip(MaterialTheme.shapes.large)
                    .combinedClickable(
                        onLongClick = {
                            onLongClick(it)
                        },
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        onClick(it)
                    }
            ) {
                GroupPhoto(
                    ContactPhoto(it.name ?: "", it.photo, it.seen).inList(),
                    padding = 0.dp,
                    size = 54.dp
                )
                Text(
                    it.seen?.shortAgo() ?: "",
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier
                        .padding(top = 1.pad)
                )
            }
        }
    }
}


fun List<GroupExtended>.people(): List<Person> {
    return mapNotNull { it.members?.mapNotNull { it.person } }.flatten().distinctBy { it.id!! }
        .sortedByDescending { it.seen ?: Instant.DISTANT_PAST }
}
