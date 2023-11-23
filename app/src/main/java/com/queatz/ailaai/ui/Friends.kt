package com.queatz.ailaai.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.queatz.ailaai.extensions.ContactPhoto
import com.queatz.ailaai.extensions.inList
import com.queatz.ailaai.extensions.shortAgo
import com.queatz.ailaai.ui.components.GroupPhoto
import com.queatz.ailaai.ui.theme.PaddingDefault
import com.queatz.db.GroupExtended
import com.queatz.db.Person
import kotlinx.datetime.Instant

@Composable
fun Friends(people: List<Person>, onPerson: (Person) -> Unit) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(PaddingDefault / 4f),
        contentPadding = PaddingValues(PaddingDefault)
    ) {
        items(people) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .clip(MaterialTheme.shapes.large)
                    .clickable {
                        onPerson(it)
                    }
            ) {
                GroupPhoto(
                    ContactPhoto(it.name ?: "", it.photo, it.seen).inList(),
                    size = 54.dp
                )
                Text(
                    it.seen?.shortAgo() ?: "",
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier
                        .padding(
                            bottom = PaddingDefault / 2f,
                            start = PaddingDefault / 2f,
                            end = PaddingDefault / 2f
                        )
                )
            }
        }
    }
}


fun List<GroupExtended>.people(): List<Person> {
    return mapNotNull { it.members?.mapNotNull { it.person } }.flatten().distinctBy { it.id!! }
        .sortedByDescending { it.seen ?: Instant.DISTANT_PAST }
}
