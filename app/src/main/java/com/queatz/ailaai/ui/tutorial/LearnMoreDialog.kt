package com.queatz.ailaai.ui.tutorial

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.navigation.NavController
import com.queatz.ailaai.R
import com.queatz.ailaai.dataStore
import com.queatz.ailaai.ui.components.DialogBase
import com.queatz.ailaai.ui.theme.PaddingDefault
import kotlinx.coroutines.launch

val hideLearnMoreKey = booleanPreferencesKey("tutorial.more.hide")

class TutorialIdea(
    val name: String,
    val description: String,
    val content: @Composable () -> Unit,
)

class StepsBuilder {
    var count = 0
}

@Composable
fun steps(builder: @Composable StepsBuilder.() -> Unit) {
    Column(
        verticalArrangement = Arrangement.spacedBy(PaddingDefault)
    ) {
        with(StepsBuilder()) {
            builder()
        }
    }
}


@Composable
fun StepsBuilder.step(text: String) {
    Text(
        buildAnnotatedString {
            withStyle(SpanStyle(fontSize = 18.sp,fontWeight = FontWeight.ExtraBold)) {
                append("${++count}. ")
            }
            append(text)
        }
    )
}



@Composable
fun LearnMoreDialog(
    onDismissRequest: () -> Unit,
    navController: NavController,
) {
    val scope = rememberCoroutineScope()
    var present by remember { mutableStateOf<TutorialIdea?>(null) }

    val ideas = listOf(
        TutorialIdea(
            "Create a local website for your business",
            "Promote your business with a mini website and connect with your customers."
        ) {
            steps {
                step("Create an Ai Là Ai card using your business name")
                step("Tap on the card to open it")
                step("Add additional cards describing your business's products and services")
                step("Use the menu to copy the link")
            }
        },
        TutorialIdea(
            "Become a local guide",
            "Show travellers around your community."
        ) {
            steps {
                step("Create an Ai Là Ai card using your name")
                step("Tap on the card to open it")
                step("Add additional cards describing the guided tours you offer")
            }
        },
        TutorialIdea(
            "Become a local spiritual leader",
            "Lead people down the path to understanding and internal fulfillment."
        ) {
            steps {
                step("Create an Ai Là Ai card using your name")
                step("Tap on the card to open it")
                step("Add additional cards describing the topics you cover")
            }
        },
        TutorialIdea(
            "Promote local ideas to improve your community",
            "Show off and socialize your ideas."
        ) {
            steps {
                step("Create an Ai Là Ai card using your idea's name")
                step("Tap on the card to open it")
                step("Add additional cards to further shed light on your ideas")

            }
        },
        TutorialIdea(
            "Become a local teacher",
            "Teach music, language, or something else."
        ) {
            steps {
                step("Create an Ai Là Ai card describing what you teach")
            }
        },
        TutorialIdea(
            "Start a local business",
            "Instantly start selling products and services."
        ) {
            steps {
                step("Create an Ai Là Ai card using your desired business name")
                step("Tap on the card to open it")
                step("Add an additional cards describing the products and services you offer")
            }
        },
        TutorialIdea(
            "Start a local activity group",
            "Be the one to introduce fun in your local community."
        ) {
            steps {
                step("Create a new group on the 'Friends' tab")
                step("Rename the group to your desired name")
                step("Create an Ai Là Ai card using your group name")
                step("Add an additional conversation step to your card called 'Join group'")
                step("Add people that reply to your card to the group")
            }
        },
        TutorialIdea(
            "Start a local discussion club",
            "Manage a topic, locations, and grow a local movement."
        ) {
            steps {
                step("Create a new group on the 'Friends' tab")
                step("Rename the group to your desired club name")
                step("Add an additional conversation step to your card called 'Join club'")
                step("Add people that reply to your card to the group")
            }
        },
        TutorialIdea(
            "Start a local project",
            "Promote your concept and find teammates."
        ) {
            steps {
                step("Create a new group on the 'Friends' tab")
                step("Rename the group to your project name")
                step("Create an Ai Là Ai card using your project's name")
                step("Add an additional conversation step to your card called 'Join project'")
                step("Add people that reply to your card to the group")
            }
        },
        TutorialIdea(
            "Make international connections",
            "Promote yourself and connect with people from around the world."
        ) {
            steps {
                step("Create an Ai Là Ai card using a catchy name")
                step("Set the location of the card to your city of choice")
            }
        },
        TutorialIdea(
            "Promote yourself in local places",
            "Reach your further into your local community via local parks and cafes."
        ) {
            steps {
                step("Create an Ai Là Ai card using the name of the place you want to reach people")
                step("Set the location of the card to offline")
                step("Open the card by tapping on it")
                step("Using the menu, print the QR code and add some details to the printed paper")
                step("Put the printed paper with the QR code in the place your want to reach people")
            }
        }
    )

    DialogBase(onDismissRequest) {
        Column(
            modifier = Modifier
                .padding(PaddingDefault * 3)
        ) {
            Text(
                present?.name ?: stringResource(R.string.more_ideas),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(bottom = PaddingDefault)
            )
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(PaddingDefault),
                modifier = Modifier
                    .weight(1f, fill = false)
            ) {
                if (present != null) {
                    item {
                        present?.content?.invoke()
                    }
                } else {
                    items(ideas) { idea ->
                        DropdownMenuItem(
                            {
                                Row(
                                    verticalAlignment = Alignment.Top,
                                    horizontalArrangement = Arrangement.spacedBy(PaddingDefault)
                                ) {
                                    Icon(
                                        Icons.Outlined.Lightbulb,
                                        null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Column(
                                        verticalArrangement = Arrangement.spacedBy(PaddingDefault / 2)
                                    ) {
                                        Text(
                                            idea.name,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(idea.description, style = MaterialTheme.typography.bodyMedium)
                                    }
                                }
                            }, {
                                present = idea
                            },
                            contentPadding = PaddingValues(PaddingDefault),
                            modifier = Modifier
                                .clip(MaterialTheme.shapes.large)
                        )
                    }
                }
            }
            val context = LocalContext.current
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom,
                modifier = Modifier.fillMaxWidth().wrapContentHeight()
            ) {
                TextButton(
                    {
                        scope.launch {
                            context.dataStore.edit {
                                it[hideLearnMoreKey] = true
                            }
                            onDismissRequest()
                        }
                    }
                ) {
                    Text(stringResource(R.string.hide), color = MaterialTheme.colorScheme.secondary.copy(alpha = .5f))
                }
                if (present != null) {
                    TextButton(
                        {
                            present = null
                        }
                    ) {
                        Text(stringResource(R.string.go_back))
                    }
                } else {
                    TextButton(
                        {
                            onDismissRequest()
                        }
                    ) {
                        Text(stringResource(R.string.close))
                    }
                }
            }
        }
    }
}
