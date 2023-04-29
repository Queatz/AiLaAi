package com.queatz.ailaai.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.queatz.ailaai.Person
import com.queatz.ailaai.extensions.ContactPhoto
import com.queatz.ailaai.ui.theme.ElevationDefault
import com.queatz.ailaai.ui.theme.PaddingDefault

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppHeader(navController: NavController, title: String, onTitleClick: () -> Unit, me: () -> Person?) {
    Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.elevatedCardColors(),
        elevation = CardDefaults.elevatedCardElevation(),
        modifier = Modifier
            .padding(PaddingDefault)
    ) {
        TopAppBar(
            {
                Text(
                    title,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .clickable(
                            interactionSource = MutableInteractionSource(),
                            indication = null
                        ) {
                            onTitleClick()
                        }
                )
            },
            actions = {
                if (me()?.name?.isNotBlank() == true || me()?.photo?.isNotBlank() == true) {
                    GroupPhoto(
                        listOf(ContactPhoto(me()?.name ?: "", me()?.photo)),
                        size = 40.dp,
                        modifier = Modifier
                            .clickable {
                                navController.navigate("profile/${me()?.id}")
                            }
                    )
                } else {
                    IconButton({
                        navController.navigate("profile/${me()?.id}")
                    }) {
                        Icon(Icons.Outlined.AccountCircle, Icons.Outlined.Settings.name)
                    }
                }
            }
        )
    }
}
