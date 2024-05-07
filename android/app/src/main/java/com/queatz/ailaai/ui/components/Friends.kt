package com.queatz.ailaai.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.queatz.ailaai.extensions.ContactPhoto
import com.queatz.ailaai.extensions.inList
import com.queatz.ailaai.extensions.shortAgo
import com.queatz.db.GroupExtended
import com.queatz.db.Person
import kotlinx.datetime.Instant

@Composable
fun Friends(people: List<Person>, modifier: Modifier = Modifier, onLongClick: (Person) -> Unit = {}, onClick: (Person) -> Unit) {
    ButtonBar(
        items = people,
        onLongClick = onLongClick,
        onClick = onClick,
        modifier = modifier,
        photo = {
            GroupPhoto(
                ContactPhoto(it.name ?: "", it.photo, it.seen).inList(),
                padding = 0.dp,
                size = 54.dp
            )
        },
        title = {
            it.seen?.shortAgo() ?: ""
        }
    )
}

fun List<GroupExtended>.people(): List<Person> {
    return mapNotNull { it.members?.mapNotNull { it.person } }.flatten().distinctBy { it.id!! }
        .sortedByDescending { it.seen ?: Instant.DISTANT_PAST }
}
