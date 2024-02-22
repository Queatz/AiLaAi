package com.queatz.ailaai.ui.screens

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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.ailaai.api.*
import coil.compose.AsyncImage
import com.queatz.ailaai.R
import com.queatz.ailaai.api.updateMyPhotoFromUri
import com.queatz.ailaai.api.updateProfilePhotoFromUri
import com.queatz.ailaai.api.updateProfileVideoFromUri
import com.queatz.ailaai.data.api
import com.queatz.ailaai.extensions.*
import com.queatz.ailaai.helpers.ResumeEffect
import com.queatz.ailaai.me
import com.queatz.ailaai.nav
import com.queatz.ailaai.trade.TradeDialog
import com.queatz.ailaai.ui.card.CardContent
import com.queatz.ailaai.ui.components.*
import com.queatz.ailaai.ui.dialogs.*
import com.queatz.ailaai.ui.profile.ProfileGroups
import com.queatz.ailaai.ui.state.jsonSaver
import com.queatz.ailaai.ui.story.StorySource
import com.queatz.ailaai.ui.theme.pad
import com.queatz.db.*
import createTrade
import kotlinx.coroutines.*

@Composable
fun ProfileScreen(personId: String) {
    val scope = rememberCoroutineScope()
    var cards by remember { mutableStateOf(emptyList<Card>()) }
    var person by rememberSaveable(stateSaver = jsonSaver()) { mutableStateOf<Person?>(null) }
    var profile by rememberSaveable(stateSaver = jsonSaver()) { mutableStateOf<Profile?>(null) }
    var stats by rememberSaveable(stateSaver = jsonSaver()) { mutableStateOf<ProfileStats?>(null) }
    var subscribed by rememberStateOf(false)
    var showMedia by remember { mutableStateOf<Media?>(null) }
    var isLoading by rememberStateOf(true)
    var isSubscribing by rememberStateOf(false)
    var search by rememberStateOf("")
    var isError by rememberStateOf(false)
    var showEditName by rememberStateOf(false)
    var showEditAbout by rememberStateOf(false)
    var showJoined by rememberStateOf(false)
    var showMenu by rememberStateOf(false)
    var showReportDialog by rememberStateOf(false)
    var showInviteDialog by rememberStateOf(false)
    var showCoverPhotoDialog by rememberStateOf(false)
    var showProfilePhotoDialog by rememberStateOf(false)
    var showQrCodeDialog by rememberStateOf(false)
    var uploadJob by remember { mutableStateOf<Job?>(null) }
    var isUploadingVideo by rememberStateOf(false)
    var showTradeDialog by rememberStateOf<Trade?>(null)
    var videoUploadStage by remember { mutableStateOf(ProcessingVideoStage.Processing) }
    var videoUploadProgress by remember { mutableStateOf(0f) }
    val me = me
    val nav = nav

    val setPhotoState = remember(person == null) {
        ChoosePhotoDialogState(mutableStateOf(person?.name ?: ""))
    }

    val context = LocalContext.current

    if (showQrCodeDialog) {
        QrCodeDialog(
            {
                showQrCodeDialog = false
            },
            profileUrl(personId),
            person?.name
        )
    }

    if (isUploadingVideo) {
        ProcessingVideoDialog(
            onDismissRequest = { isUploadingVideo = false },
            onCancelRequest = { uploadJob?.cancel() },
            stage = videoUploadStage,
            progress = videoUploadProgress
        )
    }

    if (showInviteDialog) {
        val inviteString = stringResource(R.string.invite_someone)
        val someone = stringResource(R.string.someone)
        val emptyGroup = stringResource(R.string.empty_group_name)
        ChooseGroupDialog(
            {
                showInviteDialog = false
            },
            title = inviteString,
            confirmFormatter = defaultConfirmFormatter(
                R.string.invite_someone,
                R.string.invite_to_group,
                R.string.invite_to_groups,
                R.string.invite_to_x_groups
            ) { it.name(someone, emptyGroup, me?.id?.let(::listOf) ?: emptyList()) },
            filter = {
                it.isGroupLike(person)
            }
        ) { groups ->
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
            context.toast(context.getString(R.string.person_invited, person?.name ?: someone))
        }
    }

    if (showReportDialog) {
        ReportDialog("person/$personId") {
            showReportDialog = false
        }
    }

    fun trade() {
        scope.launch {
            api.createTrade(
                Trade().apply {
                    people = listOf(me!!.id!!, personId)
                }
            ) {
                showTradeDialog = it
            }
        }
    }

    suspend fun reloadCards() {
        if (search.isBlank()) {
            api.profileCards(personId) {
                cards = it
            }
        } else {
            api.activeCardsOfPerson(personId, search) {
                cards = it
            }
        }
    }

    LaunchedEffect(search) {
        cards = emptyList()
        reloadCards()
    }

    suspend fun reload() {
        listOf(
            scope.async {
                reloadCards()
            },
            scope.async {
                api.profile(personId, onError = { isError = true }) {
                    person = it.person
                    profile = it.profile
                    stats = it.stats
                    subscribed = it.subscription != null
                }
            }
        ).awaitAll()
    }

    if (showProfilePhotoDialog) {
        ChoosePhotoDialog(
            scope = scope,
            state = setPhotoState,
            onDismissRequest = { showProfilePhotoDialog = false },
            multiple = false,
            imagesOnly = true,
            onPhotos = { photos ->
                scope.launch {
                    api.updateMyPhotoFromUri(context, photos.first()) {
                        reload()
                    }
                }
            },
            onGeneratedPhoto = { photo ->
                scope.launch {
                    api.updateMe(Person(photo = photo))
                    reload()
                }
            }
        )
    }

    if (showCoverPhotoDialog) {
        ChoosePhotoDialog(
            scope = scope,
            state = setPhotoState,
            onDismissRequest = { showCoverPhotoDialog = false },
            multiple = false,
            onPhotos = { photos ->
                scope.launch {
                    api.updateProfilePhotoFromUri(context, photos.first())
                    reload()
                }
            },
            onVideos = { videos ->
                val it = videos.first()
                uploadJob = scope.launch {
                    videoUploadProgress = 0f
                    isUploadingVideo = true
                    api.updateProfileVideoFromUri(
                        context,
                        it,
                        context.contentResolver.getType(it) ?: "video/*",
                        it.lastPathSegment ?: "video.${
                            context.contentResolver.getType(it)?.split("/")?.lastOrNull() ?: ""
                        }",
                        processingCallback = {
                            videoUploadStage = ProcessingVideoStage.Processing
                            videoUploadProgress = it
                        },
                        uploadCallback = {
                            videoUploadStage = ProcessingVideoStage.Uploading
                            videoUploadProgress = it
                        }
                    )
                    reload()
                    isUploadingVideo = false
                    uploadJob = null
                }
            },
            onGeneratedPhoto = { photo ->
                scope.launch {
                    api.updateProfile(Profile(photo = photo))
                    reload()
                }
            }
        )
    }

    ResumeEffect {
        if (cards.isEmpty() || person == null || profile == null) {
            isLoading = true
        }

        reload()
        isLoading = false
    }

    // todo: this needs to be reusable, which video plays
    val state = rememberLazyGridState()
    val isAtTop by state.isAtTop()
    var playingVideo by remember { mutableStateOf<Card?>(null) }
    val autoplayIndex by state.rememberAutoplayIndex()

    LaunchedEffect(autoplayIndex) {
        playingVideo = cards.getOrNull(
            (autoplayIndex - 1).coerceAtLeast(0)
        )
    }

    var newCard by rememberStateOf<Card?>(null)

    if (newCard != null) {
        EditCardDialog(
            newCard!!,
            {
                newCard = null
            },
            create = true
        ) {
            nav.navigate("card/${it.id!!}")
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

    if (showMedia != null) {
        PhotoDialog(
            {
                showMedia = null
            },
            showMedia!!,
            listOf(showMedia!!)
        )
    }

    val isMe = me?.id == personId

    Box {
        LazyVerticalGrid(
            state = state,
            contentPadding = PaddingValues(
                bottom = 1.pad + 80.dp
            ),
            horizontalArrangement = Arrangement.spacedBy(1.pad, Alignment.Start),
            verticalArrangement = Arrangement.spacedBy(1.pad, Alignment.Top),
            modifier = Modifier.fillMaxSize(),
            columns = GridCells.Adaptive(240.dp)
        ) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                val isLandscape = LocalConfiguration.current.screenWidthDp > LocalConfiguration.current.screenHeightDp
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .padding(bottom = 1.pad)
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
                                            showCoverPhotoDialog = true
                                        } else {
                                            showMedia = Media.Video(video)
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
                                            showCoverPhotoDialog = true
                                        } else {
                                            showMedia = profile?.photo?.let { Media.Photo(it) }
                                        }
                                    }
                            )
                        }
                        if (isMe) {
                            Icon(
                                Icons.Outlined.Edit,
                                null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier
                                    .padding(bottom = bottomPadding)
                                    .padding(1.pad)
                                    .scale(.85f)
                                    .align(Alignment.BottomEnd)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.background)
                                    .padding(1.pad)
                            )
                        }
                        val containerColor = MaterialTheme.colorScheme.background.copy(alpha = .8f)
                        val colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = containerColor
                        )
                        IconButton(
                            {
                                nav.popBackStack()
                            },
                            colors = colors,
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .padding(1.pad)
                                .clip(CircleShape)
                        ) {
                            Icon(
                                Icons.Outlined.ArrowBack,
                                stringResource(R.string.go_back),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (isMe) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(1.pad + 2.dp)
                                    .clip(MaterialTheme.shapes.extraLarge)
                                    .background(containerColor)
                            ) {
                                IconButton(
                                    {
                                        showQrCodeDialog = true
                                    },
                                    Modifier
                                        .size(42.dp)
                                ) {
                                    Icon(
                                        Icons.Outlined.QrCode2,
                                        stringResource(R.string.qr_code),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                IconButton(
                                    {
                                        nav.navigate("settings")
                                    },
                                    Modifier
                                        .size(42.dp)
                                ) {
                                    Icon(
                                        Icons.Outlined.Settings,
                                        stringResource(R.string.settings),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        } else {
                            IconButton(
                                {
                                    showMenu = true
                                },
                                colors = colors,
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(1.pad)
                            ) {
                                Icon(Icons.Outlined.MoreVert, null)
                                Dropdown(showMenu, { showMenu = false }) {
                                    DropdownMenuItem({
                                        Text(stringResource(R.string.invite_into_group))
                                    }, {
                                        showInviteDialog = true
                                        showMenu = false
                                    })
                                    DropdownMenuItem({
                                        Text(stringResource(R.string.qr_code))
                                    }, {
                                        showMenu = false
                                        showQrCodeDialog = true
                                    })
                                    person?.let { person ->
                                        val someoneString = stringResource(R.string.someone)
                                        DropdownMenuItem({
                                            Text(stringResource(R.string.share))
                                        }, {
                                            profileUrl(person.id!!).shareAsUrl(context, person.name ?: someoneString)
                                            showMenu = false
                                        })
                                    }
                                    DropdownMenuItem({
                                        Text(stringResource(R.string.report))
                                    }, {
                                        showMenu = false
                                        showReportDialog = true
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
                                        person?.photo,
                                        person?.seen
                                    )
                                ),
                                size = 128.dp,
                                padding = 0.dp,
                                border = true,
                                modifier = Modifier
                                    .clickable {
                                        if (isMe) {
                                            showProfilePhotoDialog = true
                                        } else {
                                            showMedia = person?.photo?.let { Media.Photo(it) }
                                        }
                                    }
                            )
                            if (isMe) {
                                Icon(
                                    Icons.Outlined.Edit,
                                    null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(.5f.pad)
                                        .scale(.85f)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.background)
                                        .padding(1.pad)
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
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ) {
                                    if (isMe) {
                                        showEditName = true
                                    } else {
                                        person?.name?.copyToClipboard(context)
                                        context.toast(copiedString)
                                    }
                                }
                        ) {
                            Column(
                                verticalArrangement = Arrangement.Center,
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .padding(horizontal = 1.pad)
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
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(1.pad, Alignment.CenterHorizontally),
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        OutlinedButton(
                                            {
                                                scope.launch {
                                                    api.createGroup(
                                                        listOf(me!!.id!!, personId),
                                                        reuse = true
                                                    ) { group ->
                                                        nav.navigate("group/${group.id!!}")
                                                    }
                                                }
                                            }
                                        ) {
                                            Text(stringResource(R.string.message))
                                        }
                                        OutlinedButton(
                                            {
                                                trade()
                                            }
                                        ) {
                                            Text(stringResource(R.string.trade))
                                        }
                                        OutlinedButton(
                                            {
                                                scope.launch {
                                                    isSubscribing = true
                                                    if (subscribed) {
                                                        api.unsubscribe(person!!.id!!) {
                                                            reload()
                                                        }
                                                    } else {
                                                        api.subscribe(person!!.id!!) {
                                                            reload()
                                                        }
                                                    }
                                                    isSubscribing = false
                                                }
                                            },
                                            enabled = !isSubscribing
                                        ) {
                                            if (subscribed) {
                                                Text(stringResource(R.string.unsubscribe))
                                            } else {
                                                Text(stringResource(R.string.subscribe))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        stats?.let { stats ->
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(
                                    2.pad,
                                    Alignment.CenterHorizontally
                                ),
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .padding(1.pad)
                                    .widthIn(max = 360.dp) // todo what size
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier
                                        .border(1.dp, MaterialTheme.colorScheme.outline, MaterialTheme.shapes.large)
                                        .clip(MaterialTheme.shapes.large)
//                                .clickable {  }
                                        .weight(1f)
                                        .padding(2.pad)
                                ) {
                                    Text(
                                        stats.friendsCount.toString(),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        fontWeight = FontWeight.ExtraBold,
                                        style = MaterialTheme.typography.bodyLarge
                                    )
                                    Text(
                                        pluralStringResource(
                                            R.plurals.friends_plural,
                                            stats.friendsCount,
                                            stats.friendsCount
                                        ),
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
                                        .padding(2.pad)
                                ) {
                                    Text(
                                        stats.cardCount.toString(),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        fontWeight = FontWeight.ExtraBold,
                                        style = MaterialTheme.typography.bodyLarge
                                    )
                                    Text(
                                        pluralStringResource(R.plurals.cards_plural, stats.cardCount, stats.cardCount),
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
                                        .padding(2.pad)
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
                        if (search.isBlank()) {
                            Box(
                                modifier = Modifier
                                    .clip(MaterialTheme.shapes.large)
                                    .clickable {
                                        if (isMe) {
                                            showEditAbout = true
                                        } else {
                                            profile?.about?.copyToClipboard(context)
                                            context.toast(copiedString)
                                        }
                                    }
                                    .padding(1.pad)
                            ) {
                                if (isMe || profile?.about?.isBlank() == false) {
                                    LinkifyText(
                                        profile?.about
                                            ?: (if (isMe) stringResource(R.string.introduce_yourself) else ""),
                                        color = if (isMe && profile?.about?.isBlank() != false) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onBackground,
                                        style = MaterialTheme.typography.bodyMedium,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                    )
                                }
                            }
                        }
                    }

                    if (search.isBlank()) {
                        profile?.content?.notBlank?.let { content ->
                            Box(
                                modifier = Modifier
                                    .padding(top = 1.pad)
                                    .heightIn(max = 2096.dp)
                            ) {
                                CardContent(
                                    StorySource.Profile(person!!.id!!),
                                    content
                                )
                            }
                        }

                        if (isMe) {
                            OutlinedButton(
                                onClick = {
                                    nav.navigate("profile/${person!!.id!!}/edit")
                                }
                            ) {
                                Text(
                                    stringResource(
                                        if (profile?.content.isNullOrBlank()) {
                                            R.string.add_content
                                        } else {
                                            R.string.edit_content
                                        }
                                    )
                                )
                            }
                        }

                        person?.let {
                            ProfileGroups(it)
                        }
                    }
                }
            }

            if (search.isNotBlank() && cards.isEmpty()) {
                item {
                    EmptyText(stringResource(R.string.no_cards_to_show))
                }
            }

            items(cards, key = { it.id!! }) { card ->
                CardLayout(
                    card = card,
                    showTitle = true,
                    onClick = {
                        nav.navigate("card/${card.id!!}")
                    },
                    scope = scope,
                    playVideo = card == playingVideo && !isAtTop,
                    modifier = Modifier.padding(horizontal = 1.pad)
                )
            }
        }

        SearchFieldAndAction(
            value = search,
            valueChange = { search = it },
            placeholder = stringResource(R.string.search_cards_of_x, person?.name ?: stringResource(R.string.someone)),
            action = if (isMe) {
                {
                    Icon(Icons.Outlined.Add, stringResource(R.string.add_a_card))
                }
            } else {
                null
            },
            onAction = {
                newCard = Card(equipped = true)

            },
            modifier = Modifier
                .padding(bottom = 2.pad)
                .align(Alignment.BottomEnd)
        )
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

    showTradeDialog?.let {
        TradeDialog(
            {
                showTradeDialog = null
            },
            it.id!!,
            onTradeCancelled = {},
            onTradeCompleted = {},
        )
    }
}
