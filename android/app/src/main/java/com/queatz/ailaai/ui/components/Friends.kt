package com.queatz.ailaai.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.core.graphics.toColorInt
import com.queatz.ailaai.extensions.ContactPhoto
import com.queatz.ailaai.extensions.inList
import com.queatz.ailaai.extensions.shortAgo
import com.queatz.db.Person
import com.queatz.db.PersonStatus

@Composable
fun Friends(
    people: List<Person>,
    statuses: Map<String, PersonStatus?> = emptyMap(),
    title: (Person) -> String? = { null },
    modifier: Modifier = Modifier,
    onLongClick: (Person) -> Unit = {},
    onClick: (Person) -> Unit
) {
    ButtonBar(
        items = people,
        onLongClick = onLongClick,
        onClick = onClick,
        modifier = modifier,
        photo = { person ->
            val status = statuses[person.id!!]

            Status(
                text = status?.note,
                color = status?.statusInfo?.color?.toColorInt()?.let { Color(it) },
                seen = person.seen
            ) {
                GroupPhoto(
                    photos = ContactPhoto(person.name ?: "", person.photo, person.seen).inList(),
                    padding = 0.dp,
                    size = 54.dp
                )
            }
        },
        title = {
            title(it) ?: it.seen?.shortAgo() ?: ""
        }
    )
}
