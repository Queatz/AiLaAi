package com.queatz.ailaai.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.core.os.LocaleListCompat
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.queatz.ailaai.Person
import com.queatz.ailaai.api
import com.queatz.ailaai.ui.theme.PaddingDefault
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(navController: NavController, me: () -> Person?, updateMe: () -> Unit) {
    val coroutineScope = rememberCoroutineScope()
    var myName by remember { mutableStateOf(me()?.name ?: "") }
    var myNameUnsaved by remember { mutableStateOf(false) }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) {
        if (it == null) return@rememberLauncherForActivityResult

        coroutineScope.launch {
            try {
                api.updateMyPhoto(it)
                updateMe()
            } catch (ex: Exception) {
                ex.printStackTrace()
            }
        }
    }

    Column {
        SmallTopAppBar(
            {
                Text("Settings")
            },
            navigationIcon = {
                IconButton({
                    navController.popBackStack()
                }) {
                    Icon(Icons.Outlined.ArrowBack, Icons.Outlined.ArrowBack.name)
                }
            }
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier =  Modifier
                .padding(horizontal = PaddingDefault)
        ) {
            AsyncImage(
                model = me()?.photo?.let { api.url(it) } ?: "",
                contentDescription = "Image",
                contentScale = ContentScale.Crop,
                alignment = Alignment.TopCenter,
                modifier = Modifier
                    .padding(PaddingDefault)
                    .requiredSize(84.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.secondaryContainer)
                    .clickable {
                        launcher.launch("image/*")
                    }
            )

            Column {
                OutlinedTextField(
                    myName,
                    {
                        myName = it
                        myNameUnsaved = true
                    },
                    label = {
                        Text("Your name")
                    },
                    shape = MaterialTheme.shapes.large,
                    modifier = Modifier
                        .padding(horizontal = PaddingDefault)
                )

                AnimatedVisibility(myNameUnsaved) {
                    var saving by remember { mutableStateOf(false) }
                    Button(
                        {
                            coroutineScope.launch {
                                try {
                                    saving = true
                                    api.updateMe(Person(name = myName.trim()))
                                    myNameUnsaved = false
                                    updateMe()
                                } catch (ex: Exception) {
                                    ex.printStackTrace()
                                } finally {
                                    saving = false
                                }
                            }
                        },
                        enabled = !saving,
                        modifier = Modifier
                            .padding(PaddingDefault)
                    ) {
                        Text("Update your name")
                    }
                }
            }
        }

        Text(
            "Your name and photo are used for messaging. You can use a different name on each of your cards.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.secondary,
            modifier = Modifier
                .padding(vertical = PaddingDefault, horizontal = PaddingDefault * 2)
        )

        DropdownMenuItem({
            Column(modifier = Modifier.padding(PaddingDefault)) {
                Text(
                    "Language",
                    style = MaterialTheme.typography.titleMedium.copy(lineHeight = 2.5.em)
                )
                Text(
                    "English",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.secondary
                )
            }
        }, {
            val appLocale: LocaleListCompat = LocaleListCompat.forLanguageTags("en,vn")
            AppCompatDelegate.setApplicationLocales(appLocale)

        })

        DropdownMenuItem({
            Text(
                "Sign out",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(PaddingDefault)
            )
        }, {})
    }
}
