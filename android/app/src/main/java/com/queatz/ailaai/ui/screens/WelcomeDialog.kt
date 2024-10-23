package com.queatz.ailaai.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import app.ailaai.api.me
import app.ailaai.api.updateMe
import app.ailaai.api.updateProfile
import com.queatz.ailaai.R
import com.queatz.ailaai.api.updateMyPhotoFromUri
import com.queatz.ailaai.data.api
import com.queatz.ailaai.extensions.ContactPhoto
import com.queatz.ailaai.extensions.rememberStateOf
import com.queatz.ailaai.me
import com.queatz.ailaai.ui.components.DialogBase
import com.queatz.ailaai.ui.components.DialogLayout
import com.queatz.ailaai.ui.components.GroupPhoto
import com.queatz.ailaai.ui.components.Loading
import com.queatz.ailaai.ui.dialogs.ChoosePhotoDialog
import com.queatz.ailaai.ui.dialogs.ChoosePhotoDialogState
import com.queatz.ailaai.ui.theme.pad
import com.queatz.db.Person
import com.queatz.db.Profile
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.seconds

enum class WelcomeStep {
    SetName,
    SetPhoto,
    SetIntroduction,
    Intro
}

// todo: translate
@Composable
fun WelcomeDialog(onDismissRequest: () -> Unit, onProfileUpdate: () -> Unit) {
    val context = LocalContext.current
    val me = me
    var step by rememberStateOf(WelcomeStep.SetName)
    var name by rememberStateOf("")
    var about by rememberStateOf("")
    var isLoadingPhoto by rememberStateOf(false)
    val scope = rememberCoroutineScope()
    var showProfilePhotoDialog by rememberStateOf(false)
    val setPhotoState = remember {
        ChoosePhotoDialogState(mutableStateOf(""))
    }

    LaunchedEffect(name) {
        delay(1.seconds)
        api.updateMe(Person(name = name))
        onProfileUpdate()
    }

    LaunchedEffect(about) {
        delay(1.seconds)
        api.updateProfile(Profile(about = about))
        onProfileUpdate()
    }

    if (showProfilePhotoDialog) {
        ChoosePhotoDialog(
            scope = scope,
            state = setPhotoState,
            onDismissRequest = { showProfilePhotoDialog = false },
            multiple = false,
            imagesOnly = true,
            onPhotos = { photos ->
                isLoadingPhoto = true
                scope.launch {
                    api.updateMyPhotoFromUri(context, photos.first()) {}
                    isLoadingPhoto = false
                    api.me {
                        onProfileUpdate()
                    }
                }
            },
            onGeneratedPhoto = { photo ->
                scope.launch {
                    api.updateMe(Person(photo = photo)) {
                        onProfileUpdate()
                    }
                }
            },
            onIsGeneratingPhoto = {
                isLoadingPhoto = it
            }
        )
    }

    DialogBase(
        onDismissRequest,
        dismissable = false,
        dismissOnBackPress = true
    ) {
        DialogLayout(
            content = {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 1.pad)
                ) {
                    Text(
                        when (step) {
                            WelcomeStep.SetName -> stringResource(R.string.hello)
                            WelcomeStep.SetPhoto -> stringResource(R.string.add_photo) + " \uD83D\uDCF8"
                            WelcomeStep.SetIntroduction -> stringResource(R.string.introduce_yourself) + " \uD83D\uDE0D"
                            WelcomeStep.Intro -> stringResource(R.string.welcome_to_town) + " \uD83C\uDF32\uD83D\uDC92\uD83C\uDF32"
                        },
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 1.pad)
                    )
                    when (step) {
                        WelcomeStep.SetName -> {
                            Text(
                                stringResource(R.string.what_shall_people_call_you),
                                modifier = Modifier
                                    .fillMaxWidth()
                            )
                            OutlinedTextField(
                                name,
                                { name = it },
                                label = {
                                    Text(stringResource(R.string.your_name))
                                },
                                shape = MaterialTheme.shapes.large,
                                singleLine = true,
                                keyboardOptions = KeyboardOptions.Default.copy(
                                    capitalization = KeyboardCapitalization.Words
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 1.pad)
                            )
                        }

                        WelcomeStep.SetPhoto -> {
                            Text(
                                // todo: translate
                                "Let people recognize you out in town.",
                                modifier = Modifier
                                    .fillMaxWidth()
                            )
                            GroupPhoto(
                                listOf(
                                    ContactPhoto(
                                        me?.name ?: "+",
                                        me?.photo,
                                        me?.seen
                                    )
                                ),
                                size = 128.dp,
                                modifier = Modifier
                                    .clickable {
                                        showProfilePhotoDialog = true
                                    }
                            )
                            if (isLoadingPhoto) {
                                Loading()
                            }
                        }

                        WelcomeStep.SetIntroduction -> {
                            Text(
                                // todo: translate
                                "Share anything else you'd like to say.",
                                modifier = Modifier
                                    .fillMaxWidth()
                            )
                            OutlinedTextField(
                                about,
                                { about = it },
                                label = {
                                    Text(stringResource(R.string.say_hi))
                                },
                                shape = MaterialTheme.shapes.large,
                                singleLine = false,
                                keyboardOptions = KeyboardOptions.Default.copy(
                                    capitalization = KeyboardCapitalization.Sentences
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 1.pad)
                            )
                        }

                        WelcomeStep.Intro -> {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                            ) {
                                // todo: translate
                                Text("Here's what you can do in town.")
                                Spacer(Modifier.height(1.pad))
                                Text("Create groups", style = MaterialTheme.typography.titleMedium)
                                Text("Gather friends and make things happen! Or open your group to new friends.")
                                Spacer(Modifier.height(1.pad))
                                Text("Write posts", style = MaterialTheme.typography.titleMedium)
                                Text("Express your true self! Keep the town interesting and lively.")
                                Spacer(Modifier.height(1.pad))
                                Text("Create pages", style = MaterialTheme.typography.titleMedium)
                                Text("Let people get to know you better with pages covering all of your ambitions.")
                                Spacer(Modifier.height(1.pad))
                                Text(
                                    "Reminders, trading, scripts, oh my!",
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Text("There's so much more for the adventurous!")
                            }
                        }
                    }
                }
            }
        ) {
            val steps = WelcomeStep.entries.toTypedArray()
            val index = steps.indexOfFirst { it == step }

            if (index != 0) {
                TextButton(
                    {
                        step = steps[index - 1]
                    }
                ) {
                    Text(stringResource(R.string.go_back))
                }
            }

            Button(
                {
                    if (index == steps.lastIndex) {
                        onDismissRequest()
                    } else {
                        step = steps[index + 1]
                    }
                }
            ) {
                Text(stringResource(if (index == steps.lastIndex) R.string.lets_go else R.string.next))
            }
        }
    }
}
