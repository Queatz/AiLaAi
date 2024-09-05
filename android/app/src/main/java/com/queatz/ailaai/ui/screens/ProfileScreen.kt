package com.queatz.ailaai.ui.screens

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.ProgressIndicatorDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.boundsInParent
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.ailaai.api.activeCardsOfPerson
import app.ailaai.api.createGroup
import app.ailaai.api.createMember
import app.ailaai.api.equippedItems
import app.ailaai.api.profile
import app.ailaai.api.profileCards
import app.ailaai.api.subscribe
import app.ailaai.api.unsubscribe
import app.ailaai.api.updateMe
import app.ailaai.api.updateProfile
import coil.compose.AsyncImage
import com.queatz.ailaai.AppNav
import com.queatz.ailaai.R
import com.queatz.ailaai.api.updateMyPhotoFromUri
import com.queatz.ailaai.api.updateProfilePhotoFromUri
import com.queatz.ailaai.api.updateProfileVideoFromUri
import com.queatz.ailaai.api.uploadPhotosFromUris
import com.queatz.ailaai.data.api
import com.queatz.ailaai.extensions.ContactPhoto
import com.queatz.ailaai.extensions.appNavigate
import com.queatz.ailaai.extensions.copyToClipboard
import com.queatz.ailaai.extensions.fadingEdge
import com.queatz.ailaai.extensions.inList
import com.queatz.ailaai.extensions.isAtTop
import com.queatz.ailaai.extensions.isGroupLike
import com.queatz.ailaai.extensions.name
import com.queatz.ailaai.extensions.notBlank
import com.queatz.ailaai.extensions.profileUrl
import com.queatz.ailaai.extensions.rememberAutoplayIndex
import com.queatz.ailaai.extensions.rememberStateOf
import com.queatz.ailaai.extensions.shareAsUrl
import com.queatz.ailaai.extensions.timeAgo
import com.queatz.ailaai.extensions.toast
import com.queatz.ailaai.helpers.ResumeEffect
import com.queatz.ailaai.item.InventoryItems
import com.queatz.ailaai.me
import com.queatz.ailaai.nav
import com.queatz.ailaai.trade.TradeDialog
import com.queatz.ailaai.trade.TradeItemDialog
import com.queatz.ailaai.ui.card.CardContent
import com.queatz.ailaai.ui.components.CardLayout
import com.queatz.ailaai.ui.components.Dropdown
import com.queatz.ailaai.ui.components.EmptyText
import com.queatz.ailaai.ui.components.GroupPhoto
import com.queatz.ailaai.ui.components.LinkifyText
import com.queatz.ailaai.ui.components.SearchFieldAndAction
import com.queatz.ailaai.ui.components.Video
import com.queatz.ailaai.ui.dialogs.ChooseGroupDialog
import com.queatz.ailaai.ui.dialogs.ChoosePhotoDialog
import com.queatz.ailaai.ui.dialogs.ChoosePhotoDialogState
import com.queatz.ailaai.ui.dialogs.EditCardDialog
import com.queatz.ailaai.ui.dialogs.EditProfileAboutDialog
import com.queatz.ailaai.ui.dialogs.EditProfileNameDialog
import com.queatz.ailaai.ui.dialogs.Media
import com.queatz.ailaai.ui.dialogs.PhotoDialog
import com.queatz.ailaai.ui.dialogs.ProcessingVideoDialog
import com.queatz.ailaai.ui.dialogs.ProcessingVideoStage
import com.queatz.ailaai.ui.dialogs.ProfileSettingsDialog
import com.queatz.ailaai.ui.dialogs.QrCodeDialog
import com.queatz.ailaai.ui.dialogs.ReportDialog
import com.queatz.ailaai.ui.dialogs.TextFieldDialog
import com.queatz.ailaai.ui.dialogs.ViewSourceDialog
import com.queatz.ailaai.ui.dialogs.defaultConfirmFormatter
import com.queatz.ailaai.ui.profile.ProfileGroups
import com.queatz.ailaai.ui.profile.Stats
import com.queatz.ailaai.ui.state.jsonSaver
import com.queatz.ailaai.ui.story.StorySource
import com.queatz.ailaai.ui.theme.pad
import com.queatz.db.Card
import com.queatz.db.InventoryItemExtended
import com.queatz.db.Member
import com.queatz.db.Person
import com.queatz.db.Profile
import com.queatz.db.ProfileStats
import com.queatz.db.Trade
import createTrade
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.seconds

@Composable
fun ProfileScreen(personId: String) {
    val scope = rememberCoroutineScope()
    var cards by remember { mutableStateOf(emptyList<Card>()) }
    var items by remember { mutableStateOf(emptyList<InventoryItemExtended>()) }
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
    var showMenu by rememberStateOf(false)
    var showMyMenu by rememberStateOf(false)
    var showReportDialog by rememberStateOf(false)
    var showSourceDialog by rememberStateOf(false)
    var showInviteDialog by rememberStateOf(false)
    var showCoverPhotoDialog by rememberStateOf(false)
    var showProfilePhotoDialog by rememberStateOf(false)
    var showQrCodeDialog by rememberStateOf(false)
    var showProfileSettingDialog by rememberStateOf(false)
    var chooseBackground by rememberStateOf(false)
    var isLoadingBackground by rememberStateOf(false)
    var updateHintDialog by rememberStateOf(false)
    var uploadJob by remember { mutableStateOf<Job?>(null) }
    var isUploadingVideo by rememberStateOf(false)
    var showTradeDialog by rememberStateOf<Trade?>(null)
    var videoUploadStage by remember { mutableStateOf(ProcessingVideoStage.Processing) }
    var videoUploadProgress by remember { mutableStateOf(0f) }
    var showInventoryItemDialog by rememberStateOf<InventoryItemExtended?>(null)
    val me = me
    val nav = nav
    var showProfileLocation by rememberStateOf(false)

    val setPhotoState = remember(person == null) {
        ChoosePhotoDialogState(mutableStateOf(person?.name ?: ""))
    }

    val setBackgroundState = remember(person == null) {
        ChoosePhotoDialogState(mutableStateOf(person?.name ?: ""))
    }

    val context = LocalContext.current

    LaunchedEffect(profile) {
        while (true) {
            showProfileLocation = false
            delay(5.seconds)

            if (!profile?.location.isNullOrBlank()) {
                showProfileLocation = true
                delay(5.seconds)
            }
        }
    }

    if (showQrCodeDialog) {
        QrCodeDialog(
            {
                showQrCodeDialog = false
            },
            profileUrl(personId),
            person?.name
        )
    }

    suspend fun reloadProfile() {
        api.profile(personId, onError = { isError = true }) {
            person = it.person
            profile = it.profile
            stats = it.stats
            subscribed = it.subscription != null
        }
    }

    if (showProfileSettingDialog) {
        ProfileSettingsDialog(
            onDismissRequest = {
                showProfileSettingDialog = false
            },
            onUpdated = {
                scope.launch {
                    reloadProfile()
                }
            },
            profile = profile
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

    if (showSourceDialog) {
        ViewSourceDialog({ showSourceDialog = false }, profile?.content)
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

    suspend fun reloadItems() {
        api.equippedItems(personId) {
            items = it
        }
    }

    LaunchedEffect(search) {
        cards = emptyList()
        reloadCards()
    }
    
    LaunchedEffect(Unit) {
        reloadItems()
    }

    suspend fun reload() {
        listOf(
            scope.async {
                reloadCards()
            },
            scope.async {
                reloadProfile()
            },
            scope.async {
                reloadItems()
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

    if (chooseBackground) {
        ChoosePhotoDialog(
            scope = scope,
            state = setBackgroundState,
            onDismissRequest = { chooseBackground = false },
            multiple = false,
            imagesOnly = true,
            onPhotos = { photos ->
                scope.launch {
                    isLoadingBackground = true
                    api.uploadPhotosFromUris(context, photos) {
                        api.updateProfile(Profile(background = it.urls.first()))
                        reload()
                    }
                    isLoadingBackground = false
                }
            },
            onGeneratedPhoto = { photo ->
                scope.launch {
                    api.updateProfile(Profile(background = photo))
                    reload()
                }
            },
            onIsGeneratingPhoto = {
                isLoadingBackground = it
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
            nav.appNavigate(AppNav.Page(it.id!!))
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
            onDismissRequest = {
                showEditAbout = false
            },
            initialValue = profile?.about ?: "",
            onUpdated = {
                scope.launch {
                    reload()
                }
            }
        )
    }

    if (updateHintDialog) {
        TextFieldDialog(
            onDismissRequest = { updateHintDialog = false },
            title = stringResource(R.string.hint),
            button = stringResource(R.string.update),
            singleLine = true,
            initialValue = profile?.location.orEmpty()
        ) { value ->
            api.updateProfile(Profile(location = value.trim())) {
                reload()
                updateHintDialog = false
            }
        }
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

    showInventoryItemDialog?.let { item ->
        val quantity = item.inventoryItem?.quantity ?: 0.0
        TradeItemDialog(
            {
                showInventoryItemDialog = null
            },
            item,
            initialQuantity = quantity,
            maxQuantity = quantity,
            isMine = false,
            enabled = false,
            confirmButton = stringResource(R.string.close),
            onQuantity = {
                showInventoryItemDialog = null
            }
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
                                        showMyMenu = true
                                    },
                                    Modifier
                                        .size(42.dp)
                                ) {
                                    if(isLoadingBackground) {
                                        CircularProgressIndicator(
                                            strokeWidth = ProgressIndicatorDefaults.CircularStrokeWidth / 2,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    } else {
                                        Icon(
                                            Icons.Outlined.MoreVert,
                                            null,
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Dropdown(showMyMenu, { showMyMenu = false }) {
                                        DropdownMenuItem({
                                            Text(stringResource(R.string.background))
                                        }, {
                                            showMyMenu = false
                                            chooseBackground = true
                                        })
                                        DropdownMenuItem({
                                            Text(stringResource(R.string.hint))
                                        }, {
                                            showMyMenu = false
                                            updateHintDialog = true
                                        })
                                        DropdownMenuItem({
                                            Text(stringResource(R.string.qr_code))
                                        }, {
                                            showMyMenu = false
                                            showQrCodeDialog = true
                                        })
                                        DropdownMenuItem({
                                            Text(stringResource(R.string.profile_setting))
                                        }, {
                                            showMyMenu = false
                                            showProfileSettingDialog = true
                                        })
                                    }
                                }
                                IconButton(
                                    {
                                        nav.appNavigate(AppNav.Settings)
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
                                        Text(stringResource(R.string.view_source))
                                    }, {
                                        showMenu = false
                                        showSourceDialog = true
                                    })
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
                                Crossfade(
                                    showProfileLocation,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(bottom = .5f.pad)
                                ) {
                                    if (it) {
                                        Text(profile?.location.orEmpty(),
                                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 9.sp),
                                            color = MaterialTheme.colorScheme.secondary.copy(alpha = .5f),
                                            textAlign = TextAlign.Center,
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    } else {
                                        Text(person?.seen?.timeAgo()?.let { timeAgo ->
                                            "${context.getString(R.string.active)} ${timeAgo.lowercase()}"
                                        } ?: "",
                                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 9.sp),
                                            color = MaterialTheme.colorScheme.secondary.copy(alpha = .5f),
                                            textAlign = TextAlign.Center,
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }
                                }
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
                                                        nav.appNavigate(AppNav.Group(group.id!!))
                                                    }
                                                }
                                            }
                                        ) {
                                            Text(stringResource(R.string.message), maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        }
                                        OutlinedButton(
                                            {
                                                trade()
                                            }
                                        ) {
                                            Text(stringResource(R.string.trade), maxLines = 1, overflow = TextOverflow.Ellipsis)
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
                                                Text(stringResource(R.string.unsubscribe), maxLines = 1, overflow = TextOverflow.Ellipsis)
                                            } else {
                                                Text(stringResource(R.string.subscribe), maxLines = 1, overflow = TextOverflow.Ellipsis)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        if (search.isBlank()) {
                            stats?.let { stats ->
                                Stats(stats, person)
                            }
                            if (items.isNotEmpty()) {
                                val itemsState = rememberLazyGridState()
                                var viewport by remember { mutableStateOf(Size(0f, 0f)) }

                                InventoryItems(
                                    items = items,
                                    state = itemsState,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(max = 512.dp)
                                        .onPlaced { viewport = it.boundsInParent().size }
                                        .fadingEdge(viewport, itemsState)
                                ) {
                                    showInventoryItemDialog = it
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
                                    nav.appNavigate(AppNav.EditProfile(person!!.id!!))
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
                            if (profile?.config?.showGroups != false) {
                                ProfileGroups(it)
                            }
                        }
                    }
                }
            }

            if (search.isNotBlank() && cards.isEmpty()) {
                item {
                    EmptyText(stringResource(R.string.no_cards_to_show))
                }
            }

            if (cards.isNotEmpty()) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Text(
                        stringResource(
                            R.string.pages_of_x,
                            person?.name ?: stringResource(R.string.someone)
                        ),
                        modifier = Modifier.padding(
                            horizontal = 2.pad,
                            vertical = 1.pad,
                        )
                    )
                }
            }

            items(cards, key = { it.id!! }) { card ->
                CardLayout(
                    card = card,
                    showTitle = true,
                    onClick = {
                        nav.appNavigate(AppNav.Page(card.id!!))
                    },
                    scope = scope,
                    playVideo = card == playingVideo && !isAtTop,
                    hideCreators = personId.inList(),
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
                .align(Alignment.BottomCenter)
        )
    }

    showTradeDialog?.let {
        TradeDialog(
            {
                showTradeDialog = null
            },
            it.id!!
        )
    }
}
