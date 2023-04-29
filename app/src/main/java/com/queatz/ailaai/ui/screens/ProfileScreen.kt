package com.queatz.ailaai.ui.screens

import android.app.Activity
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Message
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.queatz.ailaai.*
import com.queatz.ailaai.R
import com.queatz.ailaai.extensions.*
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

@Composable
fun ProfileScreen(personId: String, navController: NavController, me: () -> Person?) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var cards by rememberSaveable(stateSaver = jsonSaver()) { mutableStateOf(listOf<Card>()) }
    var person by rememberSaveable(stateSaver = jsonSaver()) { mutableStateOf<Person?>(null) }
    var profile by rememberSaveable(stateSaver = jsonSaver()) { mutableStateOf<Profile?>(null) }
    var stats by rememberSaveable(stateSaver = jsonSaver()) { mutableStateOf<ProfileStats?>(null) }
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
                    stats = it.stats
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
                    .padding(bottom = PaddingDefault)
            ) {
                Box {
                    val bottomPadding = 128.dp / 3
                    AsyncImage(
                        model = profile?.photo?.let(api::url),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .aspectRatio(1.5f)
                            .fillMaxWidth()
                            .clip(MaterialTheme.shapes.large)
                            .padding(bottom = bottomPadding)
                            .background(MaterialTheme.colorScheme.secondaryContainer)
                            .clickable {
                                if (isMe) {
                                    profilePhotoLauncher.launch("image/*")
                                } else {
                                    showPhoto = profile?.photo
                                }
                            }
                    )
                    if (isMe) {
                        Icon(
                            Icons.Outlined.Edit,
                            null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .padding(bottom = bottomPadding)
                                .padding(PaddingDefault)
                                .scale(.85f)
                                .align(Alignment.BottomEnd)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.background)
                                .padding(PaddingDefault)
                        )
                    }
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
                        Icon(Icons.Outlined.ArrowBack, stringResource(R.string.go_back))
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
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                    ) {
                        GroupPhoto(
                            listOf(
                                ContactPhoto(
                                    person?.name ?: "",
                                    person?.photo
                                )
                            ),
                            size = 128.dp,
                            padding = 0.dp,
                            border = true,
                            modifier = Modifier
                                .clickable {
                                    if (isMe) {
                                        photoLauncher.launch("image/*")
                                    } else {
                                        showPhoto = person?.photo
                                    }
                                }
                        )
                        if (isMe) {
                            Icon(
                                Icons.Outlined.Edit,
                                null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(PaddingDefault / 2)
                                    .scale(.85f)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.background)
                                    .padding(PaddingDefault)
                            )
                        }
                    }
                }
                if (!isLoading && !isError) {
                    val copiedString = stringResource(R.string.copied)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
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
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .padding(horizontal = PaddingDefault)
                                .align(Alignment.Center)
                        ) {
                            Text(
                                person?.name
                                    ?: (if (isMe) stringResource(R.string.add_your_name) else stringResource(R.string.someone)),
                                color = if (isMe && person?.name?.isBlank() != false) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onBackground,
                                style = MaterialTheme.typography.titleLarge,
                                textAlign = TextAlign.Center
                            )
                            IconButton(
                                {
                                scope.launch {
                                    try {
                                        val group = api.createGroup(listOf(me()!!.id!!, personId), reuse = true)
                                        navController.navigate("group/${group.id!!}")
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                    }
                                }
                            },
                                colors = IconButtonDefaults.outlinedIconButtonColors(
                                    contentColor = MaterialTheme.colorScheme.primary
                                ),
                                enabled = !isMe
                            ) {
                                Icon(Icons.Outlined.Message, "")
                            }
                        }
                        if (isMe) {
                            Icon(
                                Icons.Outlined.Edit,
                                null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier
                                    .scale(.85f)
                                    .align(Alignment.CenterEnd)
                                    .padding(
                                        vertical = PaddingDefault,
                                        horizontal = PaddingDefault * 2
                                    )
                            )
                        }
                    }
//                    Button({
//                        scope.launch {
//                            try {
//                                val group = api.createGroup(listOf(me()!!.id!!, personId), reuse = true)
//                                navController.navigate("group/${group.id!!}")
//                            } catch (e: Exception) {
//                                e.printStackTrace()
//                            }
//                        }
//                    }, enabled = !isMe, modifier = Modifier.padding(top = PaddingDefault)) {
//                        Icon(Icons.Outlined.Message, "", modifier = Modifier.padding(end = PaddingDefault))
//                        Text(
//                            stringResource(R.string.message),
//                            overflow = TextOverflow.Ellipsis,
//                            maxLines = 1
//                        )
//                    }
                    stats?.let { stats ->
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(
                                PaddingDefault * 2,
                                Alignment.CenterHorizontally
                            ),
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .padding(PaddingDefault)
                                .widthIn(max = 360.dp) // todo what size
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .border(1.dp, MaterialTheme.colorScheme.outline, MaterialTheme.shapes.large)
                                    .clip(MaterialTheme.shapes.large)
//                                .clickable {  }
                                    .weight(1f)
                                    .padding(vertical = PaddingDefault * 2, horizontal = PaddingDefault * 2)
                            ) {
                                Text(
                                    stats.friendsCount.toString(),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    fontWeight = FontWeight.ExtraBold,
                                    style = MaterialTheme.typography.bodyLarge
                                )
                                Text(
                                    stringResource(R.string.friends),
                                    color = MaterialTheme.colorScheme.secondary,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .border(1.dp, MaterialTheme.colorScheme.outline, MaterialTheme.shapes.large)
                                    .clip(MaterialTheme.shapes.large)
//                                .clickable {  }
                                    .weight(1f)
                                    .padding(vertical = PaddingDefault * 2, horizontal = PaddingDefault * 2)
                            ) {
                                Text(
                                    stats.cardCount.toString(),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    fontWeight = FontWeight.ExtraBold,
                                    style = MaterialTheme.typography.bodyLarge
                                )
                                Text(
                                    stringResource(R.string.cards),
                                    color = MaterialTheme.colorScheme.secondary,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .border(1.dp, MaterialTheme.colorScheme.outline, MaterialTheme.shapes.large)
                                    .clip(MaterialTheme.shapes.large)
//                                .clickable {  }
                                    .weight(1f)
                                    .padding(vertical = PaddingDefault * 2, horizontal = PaddingDefault * 2)
                            ) {
                                Text(
                                    person?.createdAt?.monthYear() ?: "",
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    fontWeight = FontWeight.ExtraBold,
                                    style = MaterialTheme.typography.bodyLarge
                                )
                                Text(
                                    stringResource(R.string.joined),
                                    color = MaterialTheme.colorScheme.secondary,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }
                    Box(
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
                    ) {
                        if (isMe || profile?.about?.isBlank() == false) {
                            LinkifyText(
                                profile?.about ?: (if (isMe) stringResource(R.string.add_about_you) else ""),
                                color = if (isMe && profile?.about?.isBlank() != false) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onBackground,
                                style = MaterialTheme.typography.bodyMedium,
                                textAlign = TextAlign.Center,
                                modifier = Modifier
                                    .fillMaxWidth()
                            )
                        }
                        if (isMe) {
                            Icon(
                                Icons.Outlined.Edit,
                                null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier
                                    .align(Alignment.CenterEnd)
                                    .scale(.85f)
                                    .padding(
                                        horizontal = PaddingDefault
                                    )
                            )
                        }
                    }
                }

                Box {

                }
            }
        }

        items(cards, key = { it.id!! }) { card ->
            BasicCard(
                {
                    navController.navigate("card/${card.id!!}")
                },
                onCategoryClick = {
                    exploreInitialCategory = it
                    navController.navigate("explore")
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
                isMine = card.person == me()?.id,
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
