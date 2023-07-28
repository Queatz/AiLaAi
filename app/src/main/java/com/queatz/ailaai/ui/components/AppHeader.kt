package com.queatz.ailaai.ui.components

import androidx.compose.foundation.Image
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.queatz.ailaai.data.Person
import com.queatz.ailaai.R
import com.queatz.ailaai.extensions.ContactPhoto
import com.queatz.ailaai.ui.theme.PaddingDefault

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppHeader(
    navController: NavController,
    title: String,
    onTitleClick: () -> Unit,
    me: () -> Person?,
    showAppIcon: Boolean = false,
    actions: @Composable RowScope.() -> Unit = {}
) {
    Column {
        TopAppBar(
            {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(PaddingDefault)
                ) {
                    if (false && showAppIcon) {
                        Image(
                            painterResource(R.mipmap.ic_app),
                            null,
                            modifier = Modifier
                                .requiredSize(40.dp)
                        )
                    }
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
                    me()?.let { me ->
                        if (me.name?.isNotBlank() == true || me.photo?.isNotBlank() == true) {
                            GroupPhoto(
                                listOf(ContactPhoto(me.name ?: "", me.photo)),
                                size = 40.dp,
                                modifier = Modifier
                                    .clickable {
                                        navController.navigate("profile/${me.id}")
                                    }
                            )
                        } else {
                            IconButton({
                                navController.navigate("profile/${me.id}")
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
