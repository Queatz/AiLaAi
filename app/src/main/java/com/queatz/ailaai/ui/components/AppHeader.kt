package com.queatz.ailaai.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.PlayCircle
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.queatz.ailaai.Person
import com.queatz.ailaai.dataStore
import com.queatz.ailaai.extensions.ContactPhoto
import com.queatz.ailaai.ui.theme.PaddingDefault
import com.queatz.ailaai.ui.tutorial.TutorialDialog
import com.queatz.ailaai.ui.tutorial.tutorialCompleteKey
import kotlinx.coroutines.flow.map

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppHeader(navController: NavController, title: String, onTitleClick: () -> Unit, me: () -> Person?) {
    val context = LocalContext.current
    var showTutorial by rememberSaveable { mutableStateOf(false) }
    var showTutorialButton by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        context.dataStore.data.map { it[tutorialCompleteKey] != true }.collect {
            showTutorialButton = it
        }
    }

    if (showTutorial) {
        TutorialDialog(
            {
                showTutorial = false
            },
            navController
        )
    }

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
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(PaddingDefault / 2),
                    modifier = Modifier
                        .padding(start = PaddingDefault / 2)
                ) {
                    if (showTutorialButton) {
                        Button(
                            {
                                showTutorial = true
                            },
                        ) {
                            Icon(
                                Icons.Outlined.PlayCircle,
                                null,
                                modifier = Modifier
                                    .padding(end = PaddingDefault / 2)
                            )
                            Text("Start tutorial")
                        }
                    }
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
            }
        )
    }
}
