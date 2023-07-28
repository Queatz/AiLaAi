package com.queatz.ailaai.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.queatz.ailaai.data.Person
import com.queatz.ailaai.data.api
import com.queatz.ailaai.extensions.nullIfBlank

@Composable
fun ProfileImage(person: Person?, padding: PaddingValues, onClick: (Person) -> Unit) {
    if (person?.photo?.nullIfBlank == null) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .padding(padding)
                .requiredSize(32.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.secondaryContainer)
                .clickable {
                    person?.let(onClick)
                }
        ) {
            Text(
                person?.name?.take(1) ?: "",
                style = MaterialTheme.typography.titleMedium
            )
        }
    } else {
        AsyncImage(
            model = person.photo?.let { api.url(it) } ?: "",
            contentDescription = "",
            contentScale = ContentScale.Crop,
            alignment = Alignment.Center,
            modifier = Modifier
                .padding(padding)
                .requiredSize(32.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.secondaryContainer)
                .clickable {
                    onClick(person)
                }
        )
    }
}
