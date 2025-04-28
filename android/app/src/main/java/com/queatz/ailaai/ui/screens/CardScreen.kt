package com.queatz.ailaai.ui.screens

import android.app.Activity
import android.app.PendingIntent
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.AddBox
import androidx.compose.material.icons.outlined.Adjust
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.Castle
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.Man
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material.icons.outlined.ToggleOff
import androidx.compose.material.icons.outlined.ToggleOn
import androidx.compose.material.icons.outlined.Wallpaper
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import androidx.core.net.toUri
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import app.ailaai.api.card
import app.ailaai.api.cardPeople
import app.ailaai.api.cardsCards
import app.ailaai.api.createGroup
import app.ailaai.api.generateCardPhoto
import app.ailaai.api.leaveCollaboration
import app.ailaai.api.sendMessage
import app.ailaai.api.updateCard
import com.queatz.ailaai.AppNav
import com.queatz.ailaai.MainActivity
import com.queatz.ailaai.R
import com.queatz.ailaai.api.uploadCardPhotoFromUri
import com.queatz.ailaai.api.uploadCardVideoFromUri
import com.queatz.ailaai.api.uploadPhotosFromUris
import com.queatz.ailaai.background
import com.queatz.ailaai.data.api
import com.queatz.ailaai.data.appDomain
import com.queatz.ailaai.data.json
import com.queatz.ailaai.dataStore
import com.queatz.ailaai.extensions.appNavigate
import com.queatz.ailaai.extensions.asOvalBitmap
import com.queatz.ailaai.extensions.bitmapResource
import com.queatz.ailaai.extensions.cardUrl
import com.queatz.ailaai.extensions.copyToClipboard
import com.queatz.ailaai.extensions.format
import com.queatz.ailaai.extensions.hint
import com.queatz.ailaai.extensions.idOrUrl
import com.queatz.ailaai.extensions.inList
import com.queatz.ailaai.extensions.isAtTop
import com.queatz.ailaai.extensions.isNumericTextInput
import com.queatz.ailaai.extensions.name
import com.queatz.ailaai.extensions.notBlank
import com.queatz.ailaai.extensions.popBackStackOrFinish
import com.queatz.ailaai.extensions.px
import com.queatz.ailaai.extensions.rememberAutoplayIndex
import com.queatz.ailaai.extensions.rememberSavableStateOf
import com.queatz.ailaai.extensions.rememberStateOf
import com.queatz.ailaai.extensions.shareAsUrl
import com.queatz.ailaai.extensions.showDidntWork
import com.queatz.ailaai.extensions.status
import com.queatz.ailaai.extensions.timeAgo
import com.queatz.ailaai.extensions.toast
import com.queatz.ailaai.helpers.ResumeEffect
import com.queatz.ailaai.me
import com.queatz.ailaai.nav
import com.queatz.ailaai.npc.NpcDialog
import com.queatz.ailaai.services.SavedIcon
import com.queatz.ailaai.services.ToggleSaveResult
import com.queatz.ailaai.services.saves
import com.queatz.ailaai.slideshow.slideshow
import com.queatz.ailaai.ui.card.CardDowngradeDialog
import com.queatz.ailaai.ui.card.CardUpgradeDialog
import com.queatz.ailaai.ui.card.PageStatisticsDialog
import com.queatz.ailaai.ui.components.AppBar
import com.queatz.ailaai.ui.components.BackButton
import com.queatz.ailaai.ui.components.CardLayout
import com.queatz.ailaai.ui.components.Dropdown
import com.queatz.ailaai.ui.components.Loading
import com.queatz.ailaai.ui.components.Toolbar
import com.queatz.ailaai.ui.dialogs.ChooseCategoryDialog
import com.queatz.ailaai.ui.dialogs.ChooseGroupDialog
import com.queatz.ailaai.ui.dialogs.ChoosePeopleDialog
import com.queatz.ailaai.ui.dialogs.ChoosePhotoDialog
import com.queatz.ailaai.ui.dialogs.ChoosePhotoDialogState
import com.queatz.ailaai.ui.dialogs.DeleteCardDialog
import com.queatz.ailaai.ui.dialogs.DialogCloseButton
import com.queatz.ailaai.ui.dialogs.EditCardDialog
import com.queatz.ailaai.ui.dialogs.EditCardLocationDialog
import com.queatz.ailaai.ui.dialogs.Menu
import com.queatz.ailaai.ui.dialogs.PayDialog
import com.queatz.ailaai.ui.dialogs.PeopleDialog
import com.queatz.ailaai.ui.dialogs.ProcessingVideoDialog
import com.queatz.ailaai.ui.dialogs.ProcessingVideoStage
import com.queatz.ailaai.ui.dialogs.QrCodeDialog
import com.queatz.ailaai.ui.dialogs.ReportDialog
import com.queatz.ailaai.ui.dialogs.TextFieldDialog
import com.queatz.ailaai.ui.dialogs.ViewSourceDialog
import com.queatz.ailaai.ui.dialogs.buildQrBitmap
import com.queatz.ailaai.ui.dialogs.defaultConfirmFormatter
import com.queatz.ailaai.ui.dialogs.menuItem
import com.queatz.ailaai.ui.state.jsonSaver
import com.queatz.ailaai.ui.theme.pad
import com.queatz.db.Card
import com.queatz.db.CardAttachment
import com.queatz.db.Message
import com.queatz.db.Person
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.datetime.Instant.Companion.fromEpochMilliseconds
import kotlin.time.Duration.Companion.seconds

private val showGeneratingMessage = booleanPreferencesKey("ui.showGeneratingMessage")
private val showMyCardTools = booleanPreferencesKey("ui.showMyCardTools")

@Composable
fun CardScreen(
    cardId: String,
    startInFullscreen: Boolean = false
) {
    var isLoading by rememberStateOf(false)
    var showTools by rememberStateOf(true)
    var notFound by rememberStateOf(false)
    var showMenu by rememberStateOf(false)
    var showManageMenu by rememberStateOf(false)
    var showStatisticsDialog by rememberStateOf(false)
    var openDeleteCard by rememberStateOf(false)
    var openLocationDialog by rememberStateOf(false)
    var showReportDialog by rememberStateOf(false)
    var openEditDialog by rememberStateOf(false)
    var openChangeOwner by rememberStateOf(false)
    var openChangeUrl by rememberStateOf(false)
    var showQrCode by rememberSavableStateOf(false)
    var showSendDialog by rememberSavableStateOf(false)
    var openAddCollaboratorDialog by rememberSavableStateOf(false)
    var openRemoveCollaboratorsDialog by rememberSavableStateOf(false)
    var openCollaboratorsDialog by rememberSavableStateOf(false)
    var openLeaveCollaboratorsDialog by rememberSavableStateOf(false)
    var showUpgradeDialog by rememberSavableStateOf(false)
    var showDowngradeDialog by rememberSavableStateOf(false)
    var showPageSizeDialog by rememberSavableStateOf(false)
    var card by rememberSaveable(stateSaver = jsonSaver<Card?>()) { mutableStateOf(null) }
    var cards by rememberSaveable(stateSaver = jsonSaver<List<Card>>(emptyList())) { mutableStateOf(emptyList()) }
    val scope = rememberCoroutineScope()
    val state = rememberLazyGridState()
    val stateLandscape = rememberLazyGridState()
    val context = LocalContext.current
    var uploadJob by remember { mutableStateOf<Job?>(null) }
    var isUploadingVideo by rememberStateOf(false)
    var videoUploadStage by remember { mutableStateOf(ProcessingVideoStage.Processing) }
    var videoUploadProgress by remember { mutableStateOf(0f) }
    var showSetCategory by rememberStateOf(false)
    var isGeneratingBackground by rememberStateOf(false)
    var showBackgroundDialog by rememberStateOf(false)
    var showPay by rememberStateOf(false)
    var showNpc by rememberStateOf(false)
    var showRegeneratePhotoDialog by rememberStateOf(false)
    var showGeneratingPhotoDialog by rememberStateOf(false)
    var isGeneratingPhoto by rememberStateOf(false)
    var isRegeneratingPhoto by rememberStateOf(false)
    var showPhotoDialog by rememberStateOf(false)
    var oldPhoto by rememberStateOf<String?>(null)
    val me = me
    val nav = nav
    val setPhotoState = remember(card?.name == null) {
        ChoosePhotoDialogState(mutableStateOf(card?.name ?: ""))
    }
    val setBackgroundState = remember(card?.name == null) {
        ChoosePhotoDialogState(mutableStateOf(card?.name ?: ""))
    }
    var showSourceDialog by rememberStateOf(false)
    val slideshowActive by slideshow.active.collectAsState()
    val userIsInactive by slideshow.userIsInactive.collectAsState()
    val fullscreen by slideshow.fullscreen.collectAsState()
    var showScanMe by rememberStateOf(false)

    val showInFullscreen = fullscreen || slideshowActive

    LaunchedEffect(startInFullscreen) {
        if (startInFullscreen) {
            slideshow.setFullscreen(true)
        }
    }

    BackHandler(fullscreen) {
        slideshow.setFullscreen(false)
    }

    DisposableEffect(Unit) {
        onDispose {
            if (fullscreen) {
                slideshow.setFullscreen(false)
            }
        }
    }

    fun reload() {
        scope.launch {
            api.card(cardId) { card = it }
        }
    }

    fun reloadCards() {
        scope.launch {
            api.cardsCards(cardId) { cards = it }
        }
    }

    LaunchedEffect(showScanMe) {
        if (showScanMe) {
            slideshow.cancelUserInteraction()
            delay(2.seconds)
            showScanMe = false
            slideshow.cancelUserInteraction()
        }
    }

    background(card?.background?.takeIf { showInFullscreen }?.let(api::url))

    if (openChangeUrl && card != null) {
        TextFieldDialog(
            onDismissRequest = { openChangeUrl = false },
            title = stringResource(R.string.page_url),
            button = stringResource(R.string.update),
            singleLine = true,
            initialValue = card?.url ?: "",
            bottomContent = { url ->
                if (url.isNotBlank()) {
                    Text(
                        text = "$appDomain/page/$url",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = MaterialTheme.colorScheme.secondary
                        )
                    )
                }
            }
        ) { value ->
            api.updateCard(
                card?.id!!,
                Card(url = value.trim()),
                onError = {
                    if (it.status == HttpStatusCode.Conflict) {
                        context.toast(R.string.url_already_in_use)
                    }
                }
            ) {
                openChangeUrl = false
                scope.launch {
                    reload()
                }
            }
        }
    }

    if (showStatisticsDialog && card != null) {
        PageStatisticsDialog(card!!) {
            showStatisticsDialog = false
        }
    }

    if (showPhotoDialog) {
        ChoosePhotoDialog(
            scope = scope,
            state = setPhotoState,
            onDismissRequest = { showPhotoDialog = false },
            multiple = false,
            onPhotos = { photos ->
                scope.launch {
                    api.uploadCardPhotoFromUri(context, card!!.id!!, photos.firstOrNull() ?: return@launch) {
                        api.card(cardId) { card = it }
                    }
                }
            },
            onVideos = { videos ->
                val it = videos.firstOrNull() ?: return@ChoosePhotoDialog
                uploadJob = scope.launch {
                    videoUploadProgress = 0f
                    isUploadingVideo = true
                    api.uploadCardVideoFromUri(
                        context = context,
                        id = card!!.id!!,
                        video = it,
                        contentType = context.contentResolver.getType(it) ?: "video/*",
                        filename = it.lastPathSegment ?: "video.${
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
                    api.card(cardId) { card = it }
                    uploadJob = null
                    isUploadingVideo = false
                }
            },
            onGeneratedPhoto = { photo ->
                scope.launch {
                    api.updateCard(card!!.id!!, Card(photo = photo)) {
                        api.card(cardId) { card = it }
                    }
                }
            },
            onIsGeneratingPhoto = {
                isGeneratingPhoto = it
            }
        )
    }

    if (showBackgroundDialog) {
        ChoosePhotoDialog(
            scope = scope,
            state = setBackgroundState,
            onDismissRequest = { showBackgroundDialog = false },
            multiple = false,
            imagesOnly = true,
            onPhotos = { photos ->
                scope.launch {
                    isGeneratingBackground = true
                    api.uploadPhotosFromUris(context, photos) {
                        api.updateCard(cardId, Card(background = it.urls.first())) {
                            api.card(cardId) { card = it }
                        }
                    }
                    isGeneratingBackground = false
                }
            },
            onGeneratedPhoto = { photo ->
                scope.launch {
                    api.updateCard(cardId, Card(background = photo)) {
                        api.card(cardId) { card = it }
                    }
                }
            },
            onIsGeneratingPhoto = {
                isGeneratingBackground = it
            }
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

    LaunchedEffect(cardId) {
        if (card != null) {
            return@LaunchedEffect
        }
        isLoading = true
        notFound = false

        slideshow.card.value?.takeIf { it.id!! == cardId }.let {
            if (it != null) {
                card = it
            } else {
                api.card(cardId, onError = {
                    if (it.status == HttpStatusCode.NotFound) {
                        notFound = true
                    }
                }) {
                    card = it
                }
            }
        }

        api.cardsCards(cardId) {
            cards = it
        }
        isLoading = false
    }

    val isMine = me?.id == card?.person && !showInFullscreen
    val isMineOrIAmACollaborator = (isMine || card?.collaborators?.contains(me?.id) == true) && !showInFullscreen

    fun generatePhoto() {
        isRegeneratingPhoto = true
        scope.launch {
            api.generateCardPhoto(cardId) {
                if (
                    context.dataStore.data.first().let {
                        it[showGeneratingMessage] != false
                    }
                ) {
                    showGeneratingPhotoDialog = true
                }
                oldPhoto = card?.photo ?: ""
            }
            isRegeneratingPhoto = false
        }
    }

    LaunchedEffect(Unit) {
        showTools = context.dataStore.data.first().let {
            it[showMyCardTools] ?: true
        }
    }

    fun toggleShowTools() {
        scope.launch {
            showTools = context.dataStore.data.first().let {
                it[showMyCardTools]?.not() ?: false
            }
            context.dataStore.edit {
                it[showMyCardTools] = showTools
            }
        }
    }

    fun regeneratePhoto() {
        card ?: return

        if (card!!.photo.isNullOrBlank()) {
            generatePhoto()
        } else {
            showRegeneratePhotoDialog = true
        }
    }

    fun addToHomescreen() {
        scope.launch {
            val pinShortcutInfo = ShortcutInfoCompat.Builder(context, "page/$cardId")
                .setIcon(
                    card?.photo?.let { api.url(it) }?.asOvalBitmap(context)?.let { IconCompat.createWithBitmap(it) }
                        ?: IconCompat.createWithResource(context, R.mipmap.ic_app)
                )
                .setShortLabel(card?.name ?: context.getString(R.string.app_name))
                .setIntent(Intent(context, MainActivity::class.java).apply {
                    action = Intent.ACTION_VIEW
                    data = cardUrl(card!!.id!!).let { "$it?fullscreen=true" }.toUri()
                })
                .build()
            val pinnedShortcutCallbackIntent =
                ShortcutManagerCompat.createShortcutResultIntent(context, pinShortcutInfo)
            val successCallback = PendingIntent.getBroadcast(
                context,
                0,
                pinnedShortcutCallbackIntent,
                PendingIntent.FLAG_IMMUTABLE
            )

            ShortcutManagerCompat.requestPinShortcut(
                context,
                pinShortcutInfo,
                successCallback.intentSender
            )
        }
    }

    ResumeEffect(skipFirst = true) {
        reload()
        reloadCards()
    }

    LaunchedEffect(oldPhoto) {
        if (oldPhoto == null) return@LaunchedEffect

        var tries = 0
        while (tries++ < 5 && oldPhoto != null) {
            delay(3.seconds)
            api.card(cardId) {
                if (if (oldPhoto.isNullOrBlank()) !it.photo.isNullOrBlank() else it.photo != oldPhoto) {
                    reload()
                    oldPhoto = null
                }
            }
        }
    }

    if (showRegeneratePhotoDialog) {
        AlertDialog(
            onDismissRequest = {
                showRegeneratePhotoDialog = false
            },
            title = {
                Text(stringResource(R.string.generate_a_new_photo))
            },
            text = {
                Text(stringResource(R.string.this_will_replace_the_current_photo))
            },
            confirmButton = {
                TextButton({
                    showRegeneratePhotoDialog = false
                    generatePhoto()
                }) {
                    Text(stringResource(R.string.yes))
                }
            }
        )
    }

    if (showGeneratingPhotoDialog) {
        AlertDialog(
            onDismissRequest = {
                showGeneratingPhotoDialog = false
            },
            title = {
                Text(stringResource(R.string.generating))
            },
            text = {
                Text(stringResource(R.string.generating_description))
            },
            dismissButton = {
                DialogCloseButton {
                    showGeneratingPhotoDialog = false
                }
            },
            confirmButton = {
                TextButton({
                    showGeneratingPhotoDialog = false
                    scope.launch {
                        context.dataStore.edit {
                            it[showGeneratingMessage] = false
                        }
                    }
                }) {
                    Text(stringResource(R.string.dont_show))
                }
            },
        )
    }

    if (showSetCategory) {
        ChooseCategoryDialog(
            {
                showSetCategory = false
            },
            preselect = card?.categories?.firstOrNull(),
            { category ->
                scope.launch {
                    api.updateCard(
                        card!!.id!!,
                        Card().apply {
                            categories = if (category == null) emptyList() else listOf(category)
                        }
                    ) {
                        reload()
                    }
                }
            }
        )
    }

    if (showPay) {
        PayDialog(
            onDismissRequest = {
                showPay = false
            },
            defaultPay = card?.pay?.pay,
            defaultFrequency = card?.pay?.frequency
        ) { pay ->
            api.updateCard(
                id = cardId,
                card = Card(pay = pay)
            ) {
                reload()
            }
        }
    }

    if (showNpc) {
        NpcDialog(
            npc = card?.npc,
            onDismissRequest = {
                showNpc = false
            }
        ) { npc ->
            api.updateCard(
                cardId,
                Card(npc = npc)
            ) {
                reload()
                showNpc = false
            }
        }
    }

    if (showReportDialog) {
        ReportDialog("card/$cardId") {
            showReportDialog = false
        }
    }

    if (showSourceDialog) {
        ViewSourceDialog({ showSourceDialog = false }, card?.content)
    }

    if (openLocationDialog) {
        EditCardLocationDialog(card!!, nav.context as Activity, {
            openLocationDialog = false
        }, {
            reload()
        })
    }

    if (openEditDialog) {
        EditCardDialog(card!!, {
            openEditDialog = false
        }) {
            reload()
        }
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
            reloadCards()
            nav.appNavigate(AppNav.Page(it.id!!))
        }
    }

    card?.let { card ->
        if (openDeleteCard) {
            DeleteCardDialog(card, {
                openDeleteCard = false
            }) {
                nav.popBackStackOrFinish()
            }
        }
    }

    if (openChangeOwner) {
        val someone = stringResource(R.string.someone)
        ChoosePeopleDialog(
            {
                openChangeOwner = false
            },
            title = stringResource(R.string.change_owner),
            confirmFormatter = defaultConfirmFormatter(
                R.string.give,
                R.string.give_to_person,
                R.string.give_to_people,
                R.string.give_to_x_people
            ) { it.name ?: someone },
            omit = { it.id == me?.id },
            multiple = false,
            onPeopleSelected = {
                if (it.size == 1) {
                    val newOwner = it.first().id
                    scope.launch {
                        card!!.person = newOwner
                        api.updateCard(card!!.id!!, Card().apply {
                            person = card!!.person
                        }) {
                            card = it
                        }
                    }
                }
            }
        )
    }

    if (showManageMenu) {
        Menu(
            onDismissRequest = {
                showManageMenu = false
            }
        ) {
            menuItem(stringResource(R.string.page_url)) {
                openChangeUrl = true
                showManageMenu = false
            }
            menuItem(stringResource(R.string.change_owner)) {
                openChangeOwner = true
                showManageMenu = false
            }
            if ((card?.level ?: 0) > 0) {
                menuItem(stringResource(R.string.downgrade)) {
                    showDowngradeDialog = true
                    showManageMenu = false
                }
            }
            menuItem(stringResource(R.string.delete_card)) {
                openDeleteCard = true
                showManageMenu = false
            }
        }
    }

    val userIsActive = !fullscreen && !userIsInactive

    Column(
        verticalArrangement = Arrangement.Top,
        modifier = Modifier.fillMaxSize()
    ) {
        val showAppBar = (!showInFullscreen || userIsActive) && !showScanMe

        AnimatedVisibility(showAppBar) {
            AppBar(
                title = {
                    if (!showInFullscreen) {
                        Column {
                            Text(
                                card?.name ?: "",
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )

                            card?.hint?.notBlank?.let {
                                Text(
                                    text = it,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                            }
                        }
                    }
                },
                navigationIcon = {
                    if (!showInFullscreen) {
                        BackButton()
                    }
                },
                actions = {
                    card?.let { card ->
                        if (!showInFullscreen) {
                            if (isMine) {
                                IconButton({
                                    toggleShowTools()
                                }) {
                                    Icon(
                                        if (showTools) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
                                        stringResource(R.string.tools)
                                    )
                                }
                            }
                            IconButton({
                                scope.launch {
                                    when (saves.toggleSave(card)) {
                                        ToggleSaveResult.Saved -> {
                                            context.toast(R.string.card_saved)
                                        }

                                        ToggleSaveResult.Unsaved -> {
                                            context.toast(R.string.card_unsaved)
                                        }

                                        else -> {
                                            context.showDidntWork()
                                        }
                                    }
                                }
                            }) {
                                SavedIcon(card)
                            }
                        }
                    }

                    IconButton({
                        showMenu = !showMenu
                    }) {
                        Icon(Icons.Outlined.MoreVert, stringResource(R.string.more))
                    }

                    val cardString = stringResource(R.string.card)

                    Dropdown(showMenu, { showMenu = false }) {
                        card?.let { card ->
                            DropdownMenuItem({
                                Text(stringResource(R.string.view_profile))
                            }, {
                                nav.appNavigate(AppNav.Profile(card.person!!))
                                showMenu = false
                            })
                            DropdownMenuItem({
                                Text(stringResource(R.string.toggle_fullscreen))
                            }, {
                                slideshow.setFullscreen(!fullscreen)
                                showMenu = false
                            })
                            if (showInFullscreen) {
                                DropdownMenuItem({
                                    Text(stringResource(R.string.stop_slideshow))
                                }, {
                                    slideshow.stop()
                                    showMenu = false
                                })
                            } else {
                                DropdownMenuItem({
                                    Text(stringResource(R.string.slideshow))
                                }, {
                                    slideshow.start(card.id!!)
                                    showMenu = false
                                }, enabled = cards.isNotEmpty())
                            }
                            if (card.parent != null) {
                                DropdownMenuItem({
                                    Text(stringResource(R.string.open_enclosing_card))
                                }, {
                                    nav.appNavigate(AppNav.Page(card.parent!!))
                                    showMenu = false
                                })
                            }
                        }
                        if (isMine) {
                            DropdownMenuItem({
                                Text(stringResource(R.string.manage))
                            }, {
                                showManageMenu = true
                                showMenu = false
                            })
                            DropdownMenuItem({
                                Text(stringResource(R.string.statistics))
                            }, {
                                showStatisticsDialog = true
                                showMenu = false
                            })
                            if (card?.collaborators?.isNotEmpty() != true) {
                                DropdownMenuItem({
                                    Text(stringResource(R.string.add_collaborators))
                                }, {
                                    openAddCollaboratorDialog = true
                                    showMenu = false
                                })
                            }
                        }
                        if (isMineOrIAmACollaborator && (card?.collaborators?.isNotEmpty() == true)) {
                            DropdownMenuItem({
                                Text(stringResource(R.string.collaborators))
                            }, {
                                if (isMine) {
                                    openRemoveCollaboratorsDialog = true
                                } else {
                                    openCollaboratorsDialog = true
                                }
                                showMenu = false
                            })
                        }
                        DropdownMenuItem({
                            Text(stringResource(R.string.send_card))
                        }, {
                            showSendDialog = true
                            showMenu = false
                        })
                        DropdownMenuItem({
                            Text(stringResource(R.string.qr_code))
                        }, {
                            showQrCode = true
                            showMenu = false
                        })
                        if (card?.geo?.size == 2) {
                            DropdownMenuItem({
                                Text(stringResource(R.string.show_on_map))
                            }, {
                                card?.let { card ->
                                    val uri = Uri.parse(
                                        "geo:${card.geo!![0]},${card.geo!![1]}?q=${card.geo!![0]},${card.geo!![1]}(${
                                            Uri.encode(card.name ?: cardString)
                                        })"
                                    )
                                    val intent = Intent(Intent.ACTION_VIEW, uri)
                                    nav.context.startActivity(Intent.createChooser(intent, null))
                                }
                                showMenu = false
                            })
                        }
                        val textCopied = stringResource(R.string.copied)
                        DropdownMenuItem({
                            Text(stringResource(R.string.share))
                        }, {
                            card!!.idOrUrl.shareAsUrl(context, card?.name ?: cardString)
                            showMenu = false
                        })
                        DropdownMenuItem({
                            Text(stringResource(R.string.copy_link))
                        }, {
                            card!!.idOrUrl.copyToClipboard(context, card?.name ?: cardString)
                            context.toast(textCopied)
                            showMenu = false
                        })
                        DropdownMenuItem({
                            Text(stringResource(R.string.view_source))
                        }, {
                            showMenu = false
                            showSourceDialog = true
                        })
                        if (ShortcutManagerCompat.isRequestPinShortcutSupported(context)) {
                            DropdownMenuItem({
                                Text(stringResource(R.string.add_to_homescreen))
                            }, {
                                showMenu = false
                                addToHomescreen()
                            })
                        }
                        DropdownMenuItem({
                            Text(stringResource(R.string.report))
                        }, {
                            showReportDialog = true
                            showMenu = false
                        })
                    }
                },
                modifier = Modifier
                    .zIndex(1f)
            )
        }

        fun LazyGridScope.cardHeaderItem(
            card: Card?,
            isMine: Boolean,
            aspect: Float,
            scope: CoroutineScope,
            elevation: Int = 1,
            playVideo: Boolean = false,
            onVideoClick: () -> Unit
        ) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Column(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (card != null && isMine) {
                        var active by remember { mutableStateOf(card.active ?: false) }
                        var activeCommitted by remember { mutableStateOf(active) }

                        AnimatedVisibility(showTools) {
                            Toolbar(
                                modifier = Modifier.padding(bottom = 1.pad)
                            ) {
                                item(
                                    icon = if (active) Icons.Outlined.ToggleOn else Icons.Outlined.ToggleOff,
                                    name = if (activeCommitted) stringResource(R.string.posted) else stringResource(R.string.not_posted),
                                    selected = active
                                ) {
                                    active = !active
                                    scope.launch {
                                        api.updateCard(card.id!!, Card(active = active)) {
                                            card.active = it.active
                                            activeCommitted = it.active ?: false
                                            active = activeCommitted
                                        }
                                    }
                                }

                                item(
                                    icon = Icons.Outlined.Place,
                                    name = when {
                                        card.parent != null -> stringResource(R.string.inside_another_card)
                                        card.group != null -> stringResource(R.string.in_a_group)
                                        card.equipped == true -> stringResource(R.string.on_profile)
                                        card.geo != null -> stringResource(R.string.at_a_location)
                                        card.offline == true -> stringResource(R.string.offline)
                                        else -> stringResource(R.string.none)
                                    },
                                    selected = when {
                                        card.parent != null -> true
                                        card.group != null -> true
                                        card.equipped == true -> true
                                        card.geo != null -> true
                                        card.offline == true -> true
                                        else -> false
                                    }
                                ) {
                                    openLocationDialog = true
                                }

                                item(
                                    icon = Icons.Outlined.Edit,
                                    name = stringResource(R.string.edit)
                                ) {
                                    openEditDialog = true
                                }

                                item(
                                    icon = Icons.Outlined.CameraAlt,
                                    name = stringResource(R.string.set_photo),
                                    isLoading = isGeneratingPhoto
                                ) {
                                    showPhotoDialog = true
                                }

                                if (card.photo.isNullOrEmpty() && card.video.isNullOrEmpty()) {
                                    item(
                                        icon = Icons.Outlined.AutoAwesome,
                                        name = stringResource(R.string.generate_photo),
                                        isLoading = isRegeneratingPhoto
                                    ) {
                                        regeneratePhoto()
                                        showMenu = false
                                    }
                                }

                                val category = card.categories?.firstOrNull()
                                item(
                                    icon = Icons.Outlined.Category,
                                    name = category ?: stringResource(R.string.set_category),
                                    selected = category != null
                                ) {
                                    showSetCategory = true
                                    showMenu = false
                                }

                                item(
                                    icon = Icons.Outlined.Wallpaper,
                                    name = stringResource(R.string.background),
                                    selected = !card.background.isNullOrBlank(),
                                    isLoading = isGeneratingBackground
                                ) {
                                    showBackgroundDialog = true
                                }

                                item(
                                    icon = Icons.Outlined.Payments,
                                    name = stringResource(if (card.pay == null) R.string.add_pay else R.string.change_pay),
                                    selected = card.pay != null
                                ) {
                                    showPay = true
                                    showMenu = false
                                }

                                item(
                                    icon = Icons.Outlined.Man,
                                    name = stringResource(if (card.npc == null) R.string.add_npc else R.string.npc),
                                    selected = card.npc != null
                                ) {
                                    showNpc = true
                                    showMenu = false
                                }

                                item(
                                    icon = Icons.Outlined.AddBox,
                                    name = stringResource(if (card.content?.notBlank == null) R.string.add_content else R.string.content),
                                    selected = card.content?.notBlank != null
                                ) {
                                    nav.appNavigate(AppNav.EditCard(card.id!!))
                                }

                                val level = card.level ?: 0

                                item(
                                    icon = Icons.Outlined.Castle,
                                    name = if (level == 0) stringResource(R.string.upgrade) else pluralStringResource(
                                        id = R.plurals.level_x,
                                        count = level,
                                        level
                                    ),
                                    selected = level > 0
                                ) {
                                    showUpgradeDialog = true
                                }

                                val sizeInKm = card.size ?: 0.0

                                item(
                                    icon = Icons.Outlined.Adjust,
                                    name = if (sizeInKm == 0.0) stringResource(R.string.size) else {
                                        pluralStringResource(
                                            id = R.plurals.x_km,
                                            count = sizeInKm.toInt(),
                                            sizeInKm.format()
                                        )
                                    },
                                    selected = sizeInKm > 0.0
                                ) {
                                    showPageSizeDialog = true
                                }
                            }
                        }
                    }
                    CardLayout(
                        card = card,
                        showTitle = showInFullscreen,
                        largeTitle = showInFullscreen,
                        showAuthors = !showInFullscreen,
                        aspect = aspect,
                        scope = scope,
                        elevation = elevation,
                        playVideo = playVideo,
                        onClick = {
                            if (!card?.video.isNullOrBlank()) {
                                onVideoClick()
                            }
                        },
                        onReply = if (showInFullscreen) {
                            { conversation ->
                                showScanMe = true
                            }
                        } else {
                            null
                        }
                    )
                }
            }
        }

        if (isLoading) {
            Loading(
                modifier = Modifier.padding(vertical = 1.pad)
            )
        } else if (notFound) {
            Text(
                stringResource(R.string.card_not_found),
                color = MaterialTheme.colorScheme.secondary,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(2.pad)
            )
        } else {
            val isLandscape = LocalConfiguration.current.screenWidthDp > LocalConfiguration.current.screenHeightDp
            var playingVideo by remember(card) { mutableStateOf(card) }
            val isAtTop by state.isAtTop()
            val autoplayIndex by state.rememberAutoplayIndex()

            LaunchedEffect(autoplayIndex, isLandscape) {
                playingVideo = cards.getOrNull(
                    (autoplayIndex - (if (isLandscape) 0 else 1)).coerceAtLeast(0)
                )
            }

            LaunchedEffect(card, isAtTop) {
                if (isAtTop) {
                    playingVideo = card
                }
            }

            Box {
                Row(
                    modifier = Modifier.fillMaxSize()
                ) {
                    if (isLandscape) {
                        LazyVerticalGrid(
                            state = stateLandscape,
                            contentPadding = PaddingValues(1.pad),
                            horizontalArrangement = Arrangement.spacedBy(1.pad, Alignment.CenterHorizontally),
                            verticalArrangement = Arrangement.spacedBy(1.pad, Alignment.Top),
                            columns = GridCells.Fixed(1),
                            modifier = Modifier
                                .width(240.dp)
                                .fillMaxHeight()
                        ) {
                            cardHeaderItem(
                                card = card,
                                isMine = isMine,
                                aspect = 1.5f,
                                scope = scope,
                                elevation = 2,
                                playVideo = playingVideo == card && isAtTop,
                                onVideoClick = {
                                    playingVideo = null
                                }
                            )
                        }
                    }

                    LazyVerticalGrid(
                        state = state,
                        contentPadding = PaddingValues(1.pad),
                        horizontalArrangement = Arrangement.spacedBy(1.pad, Alignment.CenterHorizontally),
                        verticalArrangement = Arrangement.spacedBy(1.pad, Alignment.Top),
                        modifier = Modifier.fillMaxSize(),
                        columns = GridCells.Adaptive(240.dp)
                    ) {
                        if (!isLandscape) {
                            cardHeaderItem(
                                card = card,
                                isMine = isMine,
                                aspect = 1.5f,
                                scope = scope,
                                playVideo = playingVideo == card && isAtTop,
                                onVideoClick = {
                                    playingVideo = null
                                }
                            )
                        }
                        if (cards.isNotEmpty()) {
                            items(cards, key = { it.id!! }) {
                                CardLayout(
                                    card = it,
                                    showTitle = true,
                                    hideCreators = card?.person?.inList()
                                        ?.let { it + (card?.collaborators ?: emptyList()) },
                                    onClick = {
                                        nav.appNavigate(AppNav.Page(it.id!!))
                                    },
                                    scope = scope,
                                    playVideo = playingVideo == it && !isAtTop,
                                )
                            }
                        }
                    }
                }
                if (isMineOrIAmACollaborator) {
                    FloatingActionButton(
                        onClick = {
                            newCard = Card(parent = cardId)
                        },
                        modifier = Modifier
                            .padding(2.pad)
                            .align(Alignment.BottomEnd)
                    ) {
                        Icon(Icons.Outlined.Add, stringResource(R.string.add_a_card))
                    }
                }

                if (showInFullscreen) {
                    val logo = bitmapResource(R.drawable.ic_notification)
                    val size = 220.dp.px
                    val qrCode = remember(cardId) {
                        cardUrl(cardId).buildQrBitmap(logo, size)
                    }

                    Crossfade(
                        targetState = userIsActive || showScanMe,
                        modifier = Modifier
                            .align(if (isLandscape) Alignment.BottomEnd else Alignment.BottomStart)
                            .padding(1.pad)
                    ) { state ->
                        if (state) {
                            Box(
                                modifier = Modifier
                                    .shadow(16.dp, MaterialTheme.shapes.medium)
                                    .clip(MaterialTheme.shapes.medium)
                                    .background(Color.White)
                                    .padding(.5f.pad)
                            ) {
                                Image(
                                    qrCode.asImageBitmap(),
                                    contentDescription = null
                                )

                                if (showScanMe) {
                                    Text(
                                        text = stringResource(R.string.scan_me),
                                        style = MaterialTheme.typography.titleLarge.copy(
                                            color = MaterialTheme.colorScheme.primary,
                                            fontWeight = FontWeight.Black
                                        ),
                                        modifier = Modifier
                                            .align(Alignment.Center)
                                            .rotate(-45f)
                                            .clip(MaterialTheme.shapes.medium)
                                            .background(Color.White)
                                            .padding(.5f.pad)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    val someone = stringResource(R.string.someone)
    val emptyGroup = stringResource(R.string.empty_group_name)

    if (openLeaveCollaboratorsDialog) {
        AlertDialog(
            onDismissRequest = {
                openLeaveCollaboratorsDialog = false
            },
            title = {
                Text(stringResource(R.string.leave_card))
            },
            confirmButton = {
                TextButton({
                    scope.launch {
                        api.leaveCollaboration(cardId)
                        api.card(cardId) { card = it }
                        api.cardsCards(cardId) { cards = it }
                        openLeaveCollaboratorsDialog = false
                    }
                }) {
                    Text(stringResource(R.string.leave))
                }
            },
            dismissButton = {
                TextButton({
                    openLeaveCollaboratorsDialog = false
                }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }

    if (openAddCollaboratorDialog) {
        ChoosePeopleDialog(
            onDismissRequest = {
                openAddCollaboratorDialog = false
            },
            title = stringResource(R.string.add_collaborators),
            confirmFormatter = defaultConfirmFormatter(
                R.string.add,
                R.string.add_person,
                R.string.add_people,
                R.string.add_x_people
            ) { it.name ?: someone },
            onPeopleSelected = { people ->
                card!!.collaborators = (card?.collaborators ?: emptyList()) + people.map { it.id!! }
                api.updateCard(card!!.id!!, Card().apply {
                    collaborators = card!!.collaborators
                }) {
                    card = it
                }
            },
            omit = { it.id == me?.id || card!!.collaborators?.contains(it.id) == true }
        )
    }

    if (openRemoveCollaboratorsDialog) {
        ChoosePeopleDialog(
            {
                openRemoveCollaboratorsDialog = false
            },
            title = stringResource(R.string.collaborators),
            confirmFormatter = defaultConfirmFormatter(
                none = R.string.remove,
                one = R.string.remove_person,
                two = R.string.remove_people,
                many = R.string.remove_x_people
            ) { it.name ?: someone },
            extraButtons = {
                TextButton(
                    {
                        openRemoveCollaboratorsDialog = false
                        openAddCollaboratorDialog = true
                    }
                ) {
                    Text(stringResource(R.string.add))
                }
            },
            onPeopleSelected = { people ->
                card!!.collaborators = (card?.collaborators ?: emptyList()) - people.map { it.id!! }.toSet()
                api.updateCard(card!!.id!!, Card().apply {
                    collaborators = card!!.collaborators
                }) {
                    card = it
                }
            },
            omit = { it.id !in (card!!.collaborators ?: emptyList()) }
        )
    }

    var collaborators by remember { mutableStateOf(emptyList<Person>()) }

    LaunchedEffect(openCollaboratorsDialog) {
        if (openCollaboratorsDialog) {
            api.cardPeople(cardId) {
                collaborators = it.sortedByDescending { it.seen ?: fromEpochMilliseconds(0) }
            }
        }
    }

    if (openCollaboratorsDialog && collaborators.isNotEmpty()) {
        PeopleDialog(
            title = stringResource(R.string.collaborators),
            onDismissRequest = {
                openCollaboratorsDialog = false
            },
            people = collaborators,
            infoFormatter = { person ->
                if (person.id == me?.id) {
                    context.getString(R.string.leave)
                } else {
                    person.seen?.timeAgo()?.let { timeAgo ->
                        "${context.getString(R.string.active)} ${timeAgo.lowercase()}"
                    }
                }
            }
        ) { person ->
            if (person.id == me?.id) {
                openLeaveCollaboratorsDialog = true
                openCollaboratorsDialog = false
            } else {
                scope.launch {
                    // todo open conversations dialog
                    api.createGroup(listOf(me!!.id!!, person.id!!), reuse = true) {
                        nav.appNavigate(AppNav.Group(it.id!!))
                        openCollaboratorsDialog = false
                    }
                }
            }
        }
    }

    if (showSendDialog) {
        val sent = stringResource(R.string.sent)
        ChooseGroupDialog(
            onDismissRequest = {
                showSendDialog = false
            },
            title = stringResource(R.string.send_card),
            confirmFormatter = defaultConfirmFormatter(
                R.string.send_card,
                R.string.send_card_to_group,
                R.string.send_card_to_groups,
                R.string.send_card_to_x_groups
            ) { it.name(someone, emptyGroup, me?.id?.let(::listOf) ?: emptyList()) }
        ) { groups ->
            coroutineScope {
                var sendSuccess = false
                groups.map { group ->
                    async {
                        api.sendMessage(
                            group.id!!,
                            Message(attachment = json.encodeToString(CardAttachment(cardId)))
                        ) {
                            sendSuccess = true
                        }
                    }
                }.awaitAll()
                if (sendSuccess) {
                    context.toast(sent)
                }
            }
        }
    }

    if (showQrCode) {
        QrCodeDialog(
            onDismissRequest = {
                showQrCode = false
            },
            url = cardUrl(cardId),
            name = card?.name
        )
    }

    if (showDowngradeDialog) {
        CardDowngradeDialog(
            onDismissRequest = { showDowngradeDialog = false },
            cardId = cardId,
            currentLevel = card?.level ?: 0
        ) {
            reload()
        }
    }

    if (showUpgradeDialog) {
        CardUpgradeDialog(
            onDismissRequest = { showUpgradeDialog = false },
            cardId = cardId,
            currentLevel = card?.level ?: 0
        ) {
            reload()
        }
    }

    if (showPageSizeDialog) {
        TextFieldDialog(
            onDismissRequest = {
                showPageSizeDialog = false
            },
            title = stringResource(R.string.page_size),
            button = stringResource(R.string.update),
            showDismiss = true,
            initialValue = card?.size?.format().orEmpty(),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Decimal
            ),
            valueFormatter = {
                if (it.isNumericTextInput()) it else null
            },
            extraContent = {
                Text(
                    text = stringResource(R.string.page_size_description),
                    modifier = Modifier.padding(bottom = 1.pad)
                )
            }
        ) { size ->
            api.updateCard(
                id = card?.id ?: return@TextFieldDialog,
                card = Card(
                    size = size.toDoubleOrNull() ?: 0.0
                )
            ) {
                reload()
                showPageSizeDialog = false
            }
        }
    }
}
