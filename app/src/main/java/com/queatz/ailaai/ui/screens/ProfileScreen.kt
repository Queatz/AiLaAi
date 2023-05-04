package com.queatz.ailaai.ui.screens

import android.app.Activity
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
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
import com.queatz.ailaai.ui.components.Video
import com.queatz.ailaai.ui.dialogs.*
import com.queatz.ailaai.ui.state.jsonSaver
import com.queatz.ailaai.ui.theme.PaddingDefault
import kotlinx.coroutines.*
import kotlinx.serialization.encodeToString

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
    var showJoined by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }
    var showInviteDialog by remember { mutableStateOf(false) }
    var uploadJob by remember { mutableStateOf<Job?>(null) }
    var isUploadingVideo by remember { mutableStateOf(false) }
    var videoUploadProgress by remember { mutableStateOf(0f) }


    if (isUploadingVideo) {
        ProcessingVideoDialog(
            onDismissRequest = { isUploadingVideo = false },
            onCancelRequest = { uploadJob?.cancel() },
            progress = videoUploadProgress
        )
    }

    if (showInviteDialog) {
        val inviteString = stringResource(R.string.invite)
        val didntWork = stringResource(R.string.didnt_work)
        val someone = stringResource(R.string.someone)
        val emptyGroup = stringResource(R.string.empty_group_name)
        ChooseGroupDialog(
            {
                showInviteDialog = false
            },
            title = inviteString,
            confirmFormatter = defaultConfirmFormatter(
                R.string.invite,
                R.string.invite_to_group,
                R.string.invite_to_groups,
                R.string.invite_to_x_groups
            ) { it.name(someone, emptyGroup, me()?.id?.let(::listOf) ?: emptyList()) },
            me = me(),
            filter = {
                it.group?.name?.isNotBlank() == true && it.members?.none { it.person?.id == person?.id } == true
            },
            onGroupsSelected = { groups ->
                try {
                    coroutineScope {
                        groups.map { group ->
                            async {
                                api.createMember(Member().apply {
                                    from = person!!.id!!
                                    to = group.id!!
                                })
                            }
                        }.awaitAll()
                    }
                    Toast.makeText(
                        context,
                        context.getString(R.string.person_invited, person?.name ?: someone),
                        Toast.LENGTH_SHORT
                    ).show()
                } catch (ex: Exception) {
                    Toast.makeText(context, didntWork, Toast.LENGTH_SHORT).show()
                    ex.printStackTrace()
                }
            }
        )
    }

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

    val photoLauncher = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) {
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

    val profilePhotoLauncher = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) {
        if (it == null) return@rememberLauncherForActivityResult

        uploadJob = scope.launch {
            videoUploadProgress = 0f
            try {
                if (it.isVideo(context)) {
                    isUploadingVideo = true
                    api.updateProfileVideo(
                        it,
                        context.contentResolver.getType(it) ?: "video/*",
                        it.lastPathSegment ?: "video.${
                            context.contentResolver.getType(it)?.split("/")?.lastOrNull() ?: ""
                        }"
                    ) {
                        videoUploadProgress = it
                    }
                } else if (it.isPhoto(context)) {
                    api.updateProfilePhoto(it)
                }
                reload()
            } catch (ex: Exception) {
                ex.printStackTrace()
            } finally {
                isUploadingVideo = false
                uploadJob = null
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

    val state = rememberLazyGridState()
    val isAtTop by state.isAtTop()
    var playingVideo by remember { mutableStateOf<Card?>(null) }
    val autoplayIndex by state.rememberAutoplayIndex()
    LaunchedEffect(autoplayIndex) {
        playingVideo = cards.getOrNull(
            (autoplayIndex - 1).coerceAtLeast(0)
        )
    }

    LazyVerticalGrid(
        state = state,
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
            val isLandscape = LocalConfiguration.current.screenWidthDp > LocalConfiguration.current.screenHeightDp
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .padding(bottom = PaddingDefault)
            ) {
                Box {
                    val bottomPadding = 128.dp / 3
                    val video = profile?.video
                    if (video != null) {
                        Box(
                            modifier = Modifier
                                .aspectRatio(if (isLandscape) 2f else 1.5f)
                                .fillMaxWidth()
                                .padding(bottom = bottomPadding)
                                .clip(
                                    RoundedCornerShape(
                                        MaterialTheme.shapes.large.topStart,
                                        MaterialTheme.shapes.large.topEnd,
                                        CornerSize(0.dp),
                                        CornerSize(0.dp)
                                    )
                                )
                                .background(MaterialTheme.colorScheme.secondaryContainer)
                                .clickable {
                                    if (isMe) {
                                        profilePhotoLauncher.launch(PickVisualMediaRequest())
                                    } else {
                                        showPhoto = profile?.photo
                                    }
                                }
                        ) {
                            Video(
                                video.let(api::url),
                                isPlaying = isAtTop,
                                modifier = Modifier
                                    .fillMaxSize()
                            )
                        }
                    } else {
                        AsyncImage(
                            model = profile?.photo?.let(api::url),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .aspectRatio(if (isLandscape) 2f else 1.5f)
                                .fillMaxWidth()
                                .padding(bottom = bottomPadding)
                                .clip(
                                    RoundedCornerShape(
                                        MaterialTheme.shapes.large.topStart,
                                        MaterialTheme.shapes.large.topEnd,
                                        CornerSize(0.dp),
                                        CornerSize(0.dp)
                                    )
                                )
                                .background(MaterialTheme.colorScheme.secondaryContainer)
                                .clickable {
                                    if (isMe) {
                                        profilePhotoLauncher.launch(PickVisualMediaRequest())
                                    } else {
                                        showPhoto = profile?.photo
                                    }
                                }
                        )
                    }
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
                    } else {
                        IconButton(
                            {
                                showMenu = true
                            },
                            colors = colors,
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(PaddingDefault)
                        ) {
                            Icon(Icons.Outlined.MoreVert, null)
                            DropdownMenu(showMenu, { showMenu = false }) {
                                DropdownMenuItem({
                                    Text(stringResource(R.string.invite))
                                }, {
                                    showInviteDialog = true
                                    showMenu = false
                                })
                            }
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
                                        photoLauncher.launch(PickVisualMediaRequest(mediaType = ActivityResultContracts.PickVisualMedia.ImageOnly))
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
                            if (!isMe) {
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
                                    .padding(PaddingDefault * 2)
                            ) {
                                Text(
                                    stats.friendsCount.toString(),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    fontWeight = FontWeight.ExtraBold,
                                    style = MaterialTheme.typography.bodyLarge
                                )
                                Text(
                                    pluralStringResource(R.plurals.friends_plural, stats.friendsCount),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
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
                                    .padding(PaddingDefault * 2)
                            ) {
                                Text(
                                    stats.cardCount.toString(),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    fontWeight = FontWeight.ExtraBold,
                                    style = MaterialTheme.typography.bodyLarge
                                )
                                Text(
                                    pluralStringResource(R.plurals.cards_plural, stats.cardCount),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    color = MaterialTheme.colorScheme.secondary,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .border(1.dp, MaterialTheme.colorScheme.outline, MaterialTheme.shapes.large)
                                    .clip(MaterialTheme.shapes.large)
                                    .clickable {
                                        showJoined = true
                                    }
                                    .weight(1f)
                                    .padding(PaddingDefault * 2)
                            ) {
                                Text(
                                    person?.createdAt?.monthYear() ?: "?",
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    fontWeight = FontWeight.ExtraBold,
                                    style = MaterialTheme.typography.bodyLarge
                                )
                                Text(
                                    stringResource(R.string.joined),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
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
                isMineToolbar = false,
                playVideo = !isAtTop && card == playingVideo
            )
        }
    }

    if (showJoined) {
        AlertDialog(
            {
                showJoined = false
            },
            title = {
                Text(stringResource(R.string.joined))
            },
            text = {
                Text(person?.createdAt?.dayMonthYear() ?: "?")
            },
            confirmButton = {
                TextButton(
                    {
                        showJoined = false
                    }
                ) {
                    Text(stringResource(R.string.close))
                }
            }
        )
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
