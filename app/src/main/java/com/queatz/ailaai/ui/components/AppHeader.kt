package com.queatz.ailaai.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.queatz.ailaai.extensions.ContactPhoto
import com.queatz.ailaai.me
import com.queatz.ailaai.nav
import com.queatz.ailaai.ui.theme.PaddingDefault

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppHeader(
    title: String,
    onTitleClick: () -> Unit,
    actions: @Composable (RowScope.() -> Unit) = {}
) {
    val nav = nav

    Column {
        TopAppBar(
            {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(PaddingDefault)
                ) {
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
                }
            },
            actions = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .padding(start = PaddingDefault / 2)
                ) {
                    actions()
                    me?.let { me ->
                        if (me.name?.isNotBlank() == true || me.photo?.isNotBlank() == true) {
                            val nav = nav
                            GroupPhoto(
                                listOf(ContactPhoto(me.name ?: "", me.photo, me.seen)),
                                size = 40.dp,
                                modifier = Modifier
                                    .clickable {
                                        nav.navigate("profile/${me.id}")
                                    }
                            )
                        } else {
                            IconButton({
                                nav.navigate("profile/${me.id}")
                            }) {
                                Icon(Icons.Outlined.AccountCircle, Icons.Outlined.Settings.name)
                            }
                        }
                    }
                }
            }
        )
    }
}
