package com.queatz.ailaai.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.queatz.ailaai.R
import com.queatz.ailaai.data.api
import com.queatz.ailaai.extensions.ContactPhoto
import com.queatz.ailaai.ui.components.GroupPhoto
import com.queatz.ailaai.ui.theme.pad
import com.queatz.db.PersonProfile

@Composable
fun ProfileCard(person: PersonProfile, onClick: () -> Unit) {
    ElevatedCard(
        onClick = onClick,
        shape = MaterialTheme.shapes.large
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.aspectRatio(.75f)
        ) {
            person.profile.photo.let { coverPhoto ->
                Box(
                    modifier = Modifier.aspectRatio(2f)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    if (coverPhoto != null) {
                        AsyncImage(
                            model = coverPhoto.let(api::url),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxSize()
                        )
                    }

                    Box(
                        modifier = Modifier
                            .offset(y = 32.dp)
                            .align(Alignment.BottomCenter)
                    ) {
                        GroupPhoto(
                            listOf(
                                ContactPhoto(
                                    person.person.name.orEmpty(),
                                    person.person.photo,
                                    person.person.seen
                                )
                            ),
                            size = 64.dp,
                            padding = 0.dp,
                            border = true
                        )
                    }
                }
            }
            Text(
                text = person.person.name ?: stringResource(R.string.someone),
                style = MaterialTheme.typography.titleLarge,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .padding(top = 32.dp + .5f.pad)
                    .padding(horizontal = 1.pad)
            )
            person.profile.location?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 9.sp,
                        color = MaterialTheme.colorScheme.secondary.copy(alpha = .5f)
                    ),
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 1.pad)
                )
            }

            person.profile.about?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(1.pad)
                )
            }
        }
    }
}
