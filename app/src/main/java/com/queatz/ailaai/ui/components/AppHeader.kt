package com.queatz.ailaai.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material.icons.outlined.PlayCircle
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.queatz.ailaai.Person
import com.queatz.ailaai.R
import com.queatz.ailaai.dataStore
import com.queatz.ailaai.extensions.ContactPhoto
import com.queatz.ailaai.extensions.rememberSavableStateOf
import com.queatz.ailaai.extensions.rememberStateOf
import com.queatz.ailaai.ui.theme.PaddingDefault
import com.queatz.ailaai.ui.tutorial.LearnMoreDialog
import com.queatz.ailaai.ui.tutorial.TutorialDialog
import com.queatz.ailaai.ui.tutorial.hideLearnMoreKey
import com.queatz.ailaai.ui.tutorial.tutorialCompleteKey

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
    val context = LocalContext.current
    var showTutorial by rememberSavableStateOf(false)
    var showTutorialButton by rememberStateOf(false)
    var showLearnMore by rememberStateOf(false)
    var showLearnMoreButton by rememberStateOf(false)

    LaunchedEffect(Unit) {
        context.dataStore.data.collect {
            showTutorialButton = it[tutorialCompleteKey] != true
            showLearnMoreButton = it[hideLearnMoreKey] != true
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
    if (showLearnMore) {
        LearnMoreDialog(
            {
                showLearnMore = false
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
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(PaddingDefault)
                ) {
                    if (showAppIcon) {
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
                    } else if (showLearnMoreButton) {
                        Button(
                            {
                                showLearnMore = true
                            },
                        ) {
                            Icon(
                                Icons.Outlined.Lightbulb,
                                null,
                                modifier = Modifier
                                    .padding(end = PaddingDefault / 2)
                            )
                            Text(stringResource(R.string.more_ideas))
                        }
                    }
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
