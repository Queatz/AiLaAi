package com.queatz.ailaai.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.queatz.ailaai.data.Person
import com.queatz.ailaai.R
import com.queatz.ailaai.extensions.ContactPhoto
import com.queatz.ailaai.ui.theme.PaddingDefault

@Composable fun PersonMember(person: Person, selected: Boolean = false, infoFormatter: (Person) -> String? = { null }, onClick: () -> Unit) {
    GroupMember(
        listOf(ContactPhoto(person.name ?: "", person.photo)),
        person.name ?: stringResource(R.string.someone),
        infoFormatter(person),
        selected,
        onClick
    )
}

@Composable
fun GroupMember(photos: List<ContactPhoto>?, name: String, info: String?, selected: Boolean = false, onClick: () -> Unit) {
    val backgroundColor by animateColorAsState(
        if (selected) MaterialTheme.colorScheme.primary
        else MaterialTheme.colorScheme.surface
    )
    val contentColor by animateColorAsState(
        if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
    )
    val secondaryColor by animateColorAsState(
        if (selected) MaterialTheme.colorScheme.onPrimary.copy(alpha = .8f) else MaterialTheme.colorScheme.secondary
    )
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .background(backgroundColor)
            .clickable {
                onClick()
            }) {
        if (photos != null) {
            GroupPhoto(photos, size = 32.dp)
        }
        Column(
            modifier = Modifier
                .padding(
                    vertical = PaddingDefault,
                    horizontal = if (photos == null) PaddingDefault * 2 else PaddingDefault
                )
        ) {
            Text(
                name,
                color = contentColor
            )
            if (info != null) {
                Text(
                    info,
                    style = MaterialTheme.typography.labelMedium,
                    color = secondaryColor
                )
            }
        }
    }
}
