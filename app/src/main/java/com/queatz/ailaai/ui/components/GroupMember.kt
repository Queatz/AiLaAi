package com.queatz.ailaai.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import com.queatz.ailaai.Person
import com.queatz.ailaai.R
import com.queatz.ailaai.api
import com.queatz.ailaai.ui.theme.PaddingDefault

@Composable fun PersonMember(person: Person, selected: Boolean = false, onClick: () -> Unit) {
    GroupMember(
        listOf(person.photo?.let { api.url(it) } ?: ""),
        person.name ?: stringResource(R.string.someone),
        selected,
        onClick
    )
}

@Composable
fun GroupMember(photos: List<String>, name: String, selected: Boolean = false, onClick: () -> Unit) {
    val backgroundColor by animateColorAsState(
        if (selected) MaterialTheme.colorScheme.primary
        else MaterialTheme.colorScheme.surface
    )
    val contentColor by animateColorAsState(
        if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
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
        GroupPhoto(photos, 32.dp)
        Text(
            name,
            color = contentColor,
            modifier = Modifier
                .padding(PaddingDefault)
        )
    }
}
