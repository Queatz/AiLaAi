package com.queatz.ailaai.ui.screens

import android.app.Activity
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.huawei.hms.analytics.db
import com.queatz.ailaai.*
import com.queatz.ailaai.R
import com.queatz.ailaai.extensions.ContactPhoto
import com.queatz.ailaai.extensions.copyToClipboard
import com.queatz.ailaai.extensions.popBackStackOrFinish
import com.queatz.ailaai.extensions.reply
import com.queatz.ailaai.ui.components.BasicCard
import com.queatz.ailaai.ui.components.GroupPhoto
import com.queatz.ailaai.ui.dialogs.EditProfileAboutDialog
import com.queatz.ailaai.ui.dialogs.EditProfileNameDialog
import com.queatz.ailaai.ui.dialogs.PhotoDialog
import com.queatz.ailaai.ui.state.jsonSaver
import com.queatz.ailaai.ui.theme.PaddingDefault
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch
import kotlin.coroutines.cancellation.CancellationException

@Composable
fun ProfileScreen(personId: String, navController: NavController, me: () -> Person?) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var cards by rememberSaveable(stateSaver = jsonSaver()) { mutableStateOf(listOf<Card>()) }
    var person by rememberSaveable(stateSaver = jsonSaver()) { mutableStateOf<Person?>(null) }
    var profile by rememberSaveable(stateSaver = jsonSaver()) { mutableStateOf<Profile?>(null) }
    var showPhoto by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(cards.isEmpty() || person == null || profile == null) }
    var isError by remember { mutableStateOf(false) }
    var showEditName by remember { mutableStateOf(false) }
    var showEditAbout by remember { mutableStateOf(false) }

    suspend fun reload() {
        listOf(
            scope.async {
                cards = api.profileCards(personId)
            },
            scope.async {
                api.profile(personId).let {
                    person = it.person
                    profile = it.profile
                }
            }
        ).awaitAll()
    }

    val photoLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) {
        if (it == null) return@rememberLauncherForActivityResult

        scope.launch {
            try {
                api.updateMyPhoto(it)
                reload()
            } catch (ex: Exception) {
                ex.printStackTrace()
            }
        }
    }

    val profilePhotoLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) {
        if (it == null) return@rememberLauncherForActivityResult

        scope.launch {
            try {
                api.updateProfilePhoto(it)
                reload()
            } catch (ex: Exception) {
                ex.printStackTrace()
            }
        }
    }

    LaunchedEffect(Unit) {
        try {
            if (cards.isEmpty() || person == null || profile == null) {
                isLoading = true
            }

            reload()
            isError = false
        } catch (ex: Exception) {
            isError = true
            ex.printStackTrace()
        } finally {
            isLoading = false
        }
    }

    LazyVerticalGrid(
        contentPadding = PaddingValues(
            bottom = PaddingDefault
        ),
        horizontalArrangement = Arrangement.spacedBy(PaddingDefault, Alignment.Start),
        verticalArrangement = Arrangement.spacedBy(PaddingDefault, Alignment.Top),
        modifier = Modifier.fillMaxSize(),
        columns = GridCells.Adaptive(240.dp)
    ) {
        val isMe = me()?.id == personId

        item(span = { GridItemSpan(maxLineSpan) }) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .padding(bottom = PaddingDefault * 2)
            ) {
                Box {
                    AsyncImage(
                        model = profile?.photo?.let(api::url),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .aspectRatio(1.5f)
                            .fillMaxWidth()
                            .clip(MaterialTheme.shapes.large)
                            .padding(bottom = 128.dp / 3)
                            .background(MaterialTheme.colorScheme.secondaryContainer)
                            .clickable {
                                if (isMe) {
                                    profilePhotoLauncher.launch("image/*")
                                } else {
                                    showPhoto = profile?.photo
                                }
                            }
                    )
                    val colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.background.copy(alpha = .8f)
                    )
                    IconButton(
                        {
                            navController.popBackStack()
                        },
                        colors = colors,
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(PaddingDefault)
                            .clip(CircleShape)
                    ) {
                        Icon(Icons.Outlined.ArrowBack, stringResource(R.string.settings))
                    }
                    if (isMe) {
                        IconButton(
                            {
                                navController.navigate("settings")
                            },
                            colors = colors,
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(PaddingDefault)
                        ) {
                            Icon(Icons.Outlined.Settings, stringResource(R.string.settings))
                        }
                    }
                    GroupPhoto(
                        listOf(
                            ContactPhoto(
                                person?.name ?: "",
                                person?.photo
                            )
                        ),
                        size = 128.dp,
                        border = true,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .clickable {
                                if (isMe) {
                                    photoLauncher.launch("image/*")
                                } else {
                                    showPhoto = person?.photo
                                }
                            }
                    )
                }
                val copiedString = stringResource(R.string.copied)
                Text(
                    person?.name ?: (if (isMe) stringResource(R.string.add_your_name) else stringResource(R.string.someone)),
                    color = if (isMe && person?.name?.isBlank() != false) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onBackground,
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier
                        .clickable(
                            interactionSource = MutableInteractionSource(),
                            indication = null
                        ) {
                            if (isMe) {
                                showEditName = true
                            } else {
                                person?.name?.copyToClipboard(context)
                                Toast.makeText(context, copiedString, Toast.LENGTH_SHORT).show()
                            }
                        }
                )
                if (isMe || profile?.about?.isBlank() == false) {
                    Text(
                        profile?.about ?: (if (isMe) stringResource(R.string.add_about_you) else ""),
                        color = if (isMe && profile?.about?.isBlank() != false) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onBackground,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier
                            .clip(MaterialTheme.shapes.large)
                            .clickable {
                                if (isMe) {
                                    showEditAbout = true
                                } else {
                                    profile?.about?.copyToClipboard(context)
                                    Toast.makeText(context, copiedString, Toast.LENGTH_SHORT).show()
                                }
                            }
                            .padding(PaddingDefault)
                    )
                }
            }
        }

        items(cards, key = { it.id!! }) { card ->
            BasicCard(
                {
                    navController.navigate("card/${card.id!!}")
                },
                onReply = { conversation ->
                    scope.launch {
                        card.reply(conversation) { groupId ->
                            navController.navigate("group/${groupId}")
                        }
                    }
                },
                card = card,
                activity = navController.context as Activity,
                isMine = false,
                isMineToolbar = false
            )
        }
    }

    if (showEditName) {
        EditProfileNameDialog(
            {
                showEditName = false
            },
            person?.name ?: "",
            {
                scope.launch {
                    reload()
                }
            }
        )
    }

    if (showEditAbout) {
        EditProfileAboutDialog(
            {
                showEditAbout = false
            },
            profile?.about ?: "",
            {
                scope.launch {
                    reload()
                }
            }
        )
    }

    if (showPhoto != null) {
        PhotoDialog(
            {
                showPhoto = null
            },
            showPhoto!!,
            listOf(showPhoto!!)
        )
    }
}
