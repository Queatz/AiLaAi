package com.queatz.ailaai.ui.tutorial

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.navigation.NavController
import com.queatz.ailaai.R
import com.queatz.ailaai.api
import com.queatz.ailaai.dataStore
import com.queatz.ailaai.json
import com.queatz.ailaai.ui.components.CardParentSelector
import com.queatz.ailaai.ui.components.CardParentType
import com.queatz.ailaai.ui.components.ConversationItem
import com.queatz.ailaai.ui.components.DialogBase
import com.queatz.ailaai.ui.theme.PaddingDefault
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString

data class TutorialStep(
    val title: String,
    val button: String,
    val enabled: () -> Boolean = { true },
    val onContinue: (suspend () -> Unit)? = null,
    val content: @Composable () -> Unit,
)

val tutorialCompleteKey = booleanPreferencesKey("tutorial.complete")

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun TutorialDialog(onDismissRequest: () -> Unit, navController: NavController) {
    val keyboardController = LocalSoftwareKeyboardController.current!!
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isInProgress by remember { mutableStateOf(false) }
    var tutorialStep by rememberSaveable { mutableStateOf(0) }
    var cardName by rememberSaveable { mutableStateOf("") }
    var cardMessage by rememberSaveable { mutableStateOf("") }
    var cardParent by rememberSaveable { mutableStateOf<CardParentType?>(null) }
    var cardPhoto by rememberSaveable { mutableStateOf<Uri?>(null) }
    var cardPublished by rememberSaveable { mutableStateOf(false) }
    var cardCreated by rememberSaveable { mutableStateOf(false) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) {
        if (it == null) return@rememberLauncherForActivityResult
        cardPhoto = it
    }
    val tutorialSteps = listOf(
        TutorialStep(
            "Welcome!",
            "Next",
        ) {
            Text("Ai Là Ai is a creative way to introduce yourself in your local area and form meaningful connections.")
        },
        TutorialStep(
            "What's a card?",
            "Okay",
        ) {
            Text("You introduce yourself by creating Ai Là Ai cards expressing what you're all about.")
        },
        TutorialStep(
            "How it works",
            "Great!",
        ) {
            Text("Other people then see your cards and are able to initiate a conversation with you.")
        },
        TutorialStep(
            "Let's learn",
            "Let's start!",
        ) {
            Text("In this tutorial, you will create your first Ai Là Ai card.")
        },
        TutorialStep(
            "Your first card",
            "Continue",
            { cardName.isNotEmpty() }
        ) {
            Text("Think of a name for your card. It can be something like 'Nature lover', 'New in town', or even just your name.")

            OutlinedTextField(
                cardName,
                onValueChange = {
                    cardName = it
                },
                label = {
                    Text(stringResource(R.string.card_name))
                },
                shape = MaterialTheme.shapes.large,
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences,
                    imeAction = ImeAction.Next
                ),
                keyboardActions = KeyboardActions(onNext = {
                    keyboardController.hide()
                }),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = PaddingDefault)
            )
        },
        TutorialStep(
            "Great!",
            "Keep going",
            { cardMessage.isNotEmpty() }
        ) {
            Text("Now tell people what you're all about in a little more detail.  Help them understand why they might want to reach out to you.")
            OutlinedTextField(
                cardMessage,
                {
                    cardMessage = it
                },
                shape = MaterialTheme.shapes.large,
                label = {
                    Text(stringResource(R.string.your_message))
                },
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences,
                    imeAction = ImeAction.Next
                ),
                keyboardActions = KeyboardActions(onNext = {
                    keyboardController.hide()
                }),
                modifier = Modifier.fillMaxWidth()
                    .padding(vertical = PaddingDefault)
            )
        },
        TutorialStep(
            "Where is the card?",
            "Continue"
        ) {
            Text("You can place a card on your profile, at a location, or even inside another card.")
        },
        TutorialStep(
            "The location is important",
            "Got it"
        ) {
            Text("People nearest to the location you choose will see your card.")
        },
        TutorialStep(
            "Place your card",
            "Continue",
            { cardParent == CardParentType.Person }
        ) {
            Text("Choose the first option to put your card on your profile.")
            CardParentSelector(
                cardParent,
                modifier = Modifier
                    .padding(top = PaddingDefault)
            ) {
                cardParent = if (it == cardParent) {
                    null
                } else {
                    it
                }
            }
            Text(
                when (cardParent) {
                    CardParentType.Map -> stringResource(R.string.at_a_location)
                    CardParentType.Card -> stringResource(R.string.inside_another_card)
                    CardParentType.Person -> stringResource(R.string.on_profile)
                    else -> stringResource(R.string.offline)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = PaddingDefault),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.secondary
            )
        },
        TutorialStep(
            "Super!",
            "Continue"
        ) {
            Text("Let's finish up by adding a pretty photo.")
        },
        TutorialStep(
            "Add a photo",
            "Continue",
            {
                cardPhoto != null
            }
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    {
                        launcher.launch("image/*")
                    },
                    colors = ButtonDefaults.textButtonColors(
                        containerColor = MaterialTheme.colorScheme.background.copy(alpha = .8f)
                    ),
                    modifier = Modifier
                ) {
                    Icon(Icons.Outlined.Edit, "")
                    Text(
                        stringResource(R.string.set_photo),
                        modifier = Modifier.padding(start = PaddingDefault)
                    )
                }
                if (cardPhoto != null) {
                    Icon(
                        Icons.Outlined.Check,
                        null,
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier
                            .padding(start = PaddingDefault)
                    )
                }
            }
        },
        TutorialStep(
            "Amazing!",
            "Continue"
        ) {
            Text("There's only one more step.")
        },
        TutorialStep(
            "Publish your card",
            if (cardPublished) "Publish it" else "Skip",
            onContinue = {
                if (!cardCreated) {
                    try {
                        val card = api.newCard(
                            com.queatz.ailaai.Card(
                                name = cardName.trim(),
                                conversation = json.encodeToString(
                                    ConversationItem(
                                        message = cardMessage
                                    )
                                ),
                                equipped = true
                            )
                        )
                        api.uploadCardPhoto(card.id!!, cardPhoto!!)

                        if (cardPublished) {
                            api.updateCard(
                                card.id!!, com.queatz.ailaai.Card(
                                    active = true
                                )
                            )
                        }

                        cardCreated = true
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        ) {
            Column {
                Text("If you're ready, use the toggle to publish your card.\n\nWhen a card is published, other people are able see it and initiate a conversation with you.")
                Row(
                    modifier = Modifier
                        .padding(top = PaddingDefault)
                ) {
                    Switch(
                        cardPublished,
                        {
                            cardPublished = it
                        }
                    )
                    Text(
                        if (cardPublished) stringResource(R.string.published) else stringResource(R.string.draft),
                        style = MaterialTheme.typography.labelMedium,
                        color = if (cardPublished) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.secondary,
                        modifier = Modifier
                            .align(Alignment.CenterVertically)
                            .padding(start = PaddingDefault)
                    )
                }
            }
        },
        TutorialStep(
            "Congratulations!",
            "Finish",
            onContinue = {
                context.dataStore.edit {
                    it[tutorialCompleteKey] = true
                }
                navController.navigate("me")
            }
        ) {
            Text("You have completed the tutorial.\n\nYou can see your new card and create more like it in the 'Me' tab.")
        }
    )

    DialogBase(
        onDismissRequest,
        dismissable = false,
        dismissOnBackPress = true,
        modifier = Modifier.wrapContentSize()
    ) {
        val scrollState = rememberScrollState()
        Column(
            modifier = Modifier
                .padding(PaddingDefault * 3)
                .verticalScroll(scrollState)
        ) {
            Text(
                tutorialSteps[tutorialStep].title,
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(bottom = PaddingDefault)
            )
            Column(
                modifier = Modifier
            ) {
                tutorialSteps[tutorialStep].content()
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(PaddingDefault, Alignment.End),
                verticalAlignment = Alignment.Bottom,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (tutorialStep > 0) {
                    TextButton(
                        {
                            tutorialStep -= 1
                        }
                    ) {
                        Text(
                            stringResource(R.string.go_back),
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .alpha(.5f)
                        )
                    }
                }
                TextButton(
                    {
                        scope.launch {
                            isInProgress = true

                            try {
                                tutorialSteps[tutorialStep].onContinue?.invoke()
                            } finally {
                                isInProgress = false
                            }

                            if (tutorialStep < tutorialSteps.lastIndex) {
                                tutorialStep += 1
                            } else {
                                onDismissRequest()
                            }
                        }
                    },
                    enabled = tutorialSteps[tutorialStep].enabled() && !isInProgress,
                    modifier = Modifier.align(Alignment.CenterVertically)
                ) {
                    Text(
                        tutorialSteps[tutorialStep].button,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
