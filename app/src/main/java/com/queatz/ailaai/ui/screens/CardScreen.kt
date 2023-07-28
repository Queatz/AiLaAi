package com.queatz.ailaai.ui.screens

import android.app.Activity
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.navigation.NavController
import com.queatz.ailaai.R
import com.queatz.ailaai.api.*
import com.queatz.ailaai.data.*
import com.queatz.ailaai.extensions.*
import com.queatz.ailaai.services.SavedIcon
import com.queatz.ailaai.services.ToggleSaveResult
import com.queatz.ailaai.services.saves
import com.queatz.ailaai.ui.components.*
import com.queatz.ailaai.ui.dialogs.*
import com.queatz.ailaai.ui.state.jsonSaver
import com.queatz.ailaai.ui.theme.PaddingDefault
import io.ktor.http.*
import kotlinx.coroutines.*
import kotlinx.datetime.Instant.Companion.fromEpochMilliseconds
import kotlinx.serialization.encodeToString

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CardScreen(cardId: String, navController: NavController, me: () -> Person?) {
    var isLoading by rememberStateOf(false)
    var notFound by rememberStateOf(false)
    var showMenu by rememberStateOf(false)
    var showManageMenu by rememberStateOf(false)
    var openDeleteCard by rememberStateOf(false)
    var openLocationDialog by rememberStateOf(false)
    var showReportDialog by rememberStateOf(false)
    var openEditDialog by rememberStateOf(false)
    var openChangeOwner by rememberStateOf(false)
    var showQrCode by rememberSavableStateOf(false)
    var showSendDialog by rememberSavableStateOf(false)
    var openAddCollaboratorDialog by rememberSavableStateOf(false)
    var openRemoveCollaboratorsDialog by rememberSavableStateOf(false)
    var openCollaboratorsDialog by rememberSavableStateOf(false)
    var openLeaveCollaboratorsDialog by rememberSavableStateOf(false)
    var card by rememberSaveable(stateSaver = jsonSaver<Card?>()) { mutableStateOf(null) }
    var cards by rememberSaveable(stateSaver = jsonSaver<List<Card>>()) { mutableStateOf(emptyList()) }
    val scope = rememberCoroutineScope()
    val state = rememberLazyGridState()
    val stateLandscape = rememberLazyGridState()
    val context = LocalContext.current
    var uploadJob by remember { mutableStateOf<Job?>(null) }
    var isUploadingVideo by rememberStateOf(false)
    var videoUploadStage by remember { mutableStateOf(ProcessingVideoStage.Processing) }
    var videoUploadProgress by remember { mutableStateOf(0f) }
    var showSetCategory by rememberStateOf(false)

    if (isUploadingVideo) {
        ProcessingVideoDialog(
            onDismissRequest = { isUploadingVideo = false },
            onCancelRequest = { uploadJob?.cancel() },
            stage = videoUploadStage,
            progress = videoUploadProgress
        )
    }

    LaunchedEffect(Unit) {
        if (card != null) {
            return@LaunchedEffect
        }
        isLoading = true
        notFound = false

        api.card(cardId, onError = {
            if (it.status == HttpStatusCode.NotFound) {
                notFound = true
            }
        }) { card = it }
        api.cardsCards(cardId) { cards = it }
        isLoading = false
    }

    val isMine = me()?.id == card?.person
    val isMineOrIAmACollaborator = isMine || card?.collaborators?.contains(me()?.id) == true
    val recomposeScope = currentRecomposeScope

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

    if (showSetCategory) {
        ChooseCategoryDialog(
            {
                showSetCategory = false
            },
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

    if (showReportDialog) {
        ReportDialog("card/$cardId") {
            showReportDialog = false
        }
    }

    if (openLocationDialog) {
        EditCardLocationDialog(card!!, navController = navController, navController.context as Activity, {
            openLocationDialog = false
        }, {
            recomposeScope.invalidate()
        })
    }

    if (openEditDialog) {
        EditCardDialog(card!!, {
            openEditDialog = false
        }, {
            recomposeScope.invalidate()
        })
    }

    card?.let { card ->
        if (openDeleteCard) {
            DeleteCardDialog(card, {
                openDeleteCard = false
            }) {
                navController.popBackStackOrFinish()
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
            omit = { it.id == me()?.id },
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
            {
                showManageMenu = false
            }
        ) {
            menuItem(stringResource(if (card?.active == true) R.string.unpublish else R.string.publish)) {
                card?.let { card ->
                    scope.launch {
                        api.updateCard(
                            card.id!!,
                            Card(active = card.active?.not() ?: true)
                        ) {
                            card.active = it.active
                            context.toast(if (card.active == true) R.string.published else R.string.draft)
                        }
                    }
                }
                showManageMenu = false
            }
            menuItem(stringResource(R.string.change_owner)) {
                openChangeOwner = true
                showManageMenu = false
            }
            menuItem(stringResource(R.string.delete_card)) {
                openDeleteCard = true
                showManageMenu = false
            }
        }
    }

    Column(
        verticalArrangement = Arrangement.Top,
        modifier = Modifier.fillMaxSize()
    ) {
        TopAppBar(
            {
                Column {
                    Text(card?.name ?: "", maxLines = 1, overflow = TextOverflow.Ellipsis)

                    card?.location?.takeIf { it.isNotBlank() }?.let {
                        Text(
                            it,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                }
            },
            navigationIcon = {
                BackButton(navController)
            },
            actions = {
                card?.let { card ->
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

                IconButton({
                    showMenu = !showMenu
                }) {
                    Icon(Icons.Outlined.MoreVert, stringResource(R.string.more))
                }

                val cardString = stringResource(R.string.card)

                val launcher = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) {
                    if (it == null) return@rememberLauncherForActivityResult

                    uploadJob = scope.launch {
                        videoUploadProgress = 0f
                        if (it.isVideo(context)) {
                            isUploadingVideo = true
                            api.uploadCardVideo(
                                card!!.id!!,
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
                        } else if (it.isPhoto(context)) {
                            api.uploadCardPhoto(card!!.id!!, it)
                        }
                        api.card(cardId) { card = it }
                        uploadJob = null
                        isUploadingVideo = false
                    }
                }

                DropdownMenu(showMenu, { showMenu = false }) {
                    if (isMine) {
                        DropdownMenuItem({
                            Text(stringResource(R.string.edit))
                        }, {
                            openEditDialog = true
                            showMenu = false
                        })
                        DropdownMenuItem({
                            Text(stringResource(R.string.change_location))
                        }, {
                            openLocationDialog = true
                            showMenu = false
                        })
                        DropdownMenuItem({
                            Text(stringResource(R.string.set_category))
                        }, {
                            showSetCategory = true
                            showMenu = false
                        })
                        DropdownMenuItem({
                            Text(stringResource(R.string.set_photo))
                        }, {
                            launcher.launch(PickVisualMediaRequest())
                            showMenu = false
                        })
                        DropdownMenuItem({
                            Text(stringResource(R.string.manage))
                        }, {
                            showManageMenu = true
                            showMenu = false
                        })
                        if (!((card?.collaborators?.isNotEmpty() == true))) {
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
                    card?.let { card ->
                        DropdownMenuItem({
                            Text(stringResource(R.string.view_profile))
                        }, {
                            navController.navigate("profile/${card.person!!}")
                            showMenu = false
                        })
                        if (card.parent != null) {
                            DropdownMenuItem({
                                Text(stringResource(R.string.open_enclosing_card))
                            }, {
                                navController.navigate("card/${card.parent!!}")
                                showMenu = false
                            })
                        }
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
                                navController.context.startActivity(Intent.createChooser(intent, null))
                            }
                            showMenu = false
                        })
                    }
                    val textCopied = stringResource(R.string.copied)
                    if (card?.location.isNullOrBlank().not()) {
                        DropdownMenuItem({
                            Text(stringResource(R.string.copy_location))
                        }, {
                            card?.let { card ->
                                card.location?.copyToClipboard(context, card.name ?: cardString)
                                context.showDidntWork()
                            }
                            showMenu = false
                        })
                    }
                    DropdownMenuItem({
                        Text(stringResource(R.string.share))
                    }, {
                        cardUrl(cardId).shareAsUrl(context, card?.name ?: cardString)
                        showMenu = false
                    })
                    DropdownMenuItem({
                        Text(stringResource(R.string.copy_link))
                    }, {
                        cardUrl(cardId).copyToClipboard(context, card?.name ?: cardString)
                        context.toast(textCopied)
                        showMenu = false
                    })
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

        if (isLoading) {
            LinearProgressIndicator(
                color = MaterialTheme.colorScheme.tertiary,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = PaddingDefault * 2, vertical = PaddingDefault)
            )
        } else if (notFound) {
            Text(
                stringResource(R.string.card_not_found),
                color = MaterialTheme.colorScheme.secondary,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(PaddingDefault * 2)
            )
        } else {
            val isLandscape = LocalConfiguration.current.screenWidthDp > LocalConfiguration.current.screenHeightDp
            var verticalAspect by rememberStateOf(false)
            val headerAspect by animateFloatAsState(if (verticalAspect) .75f else 1.5f)
            var playingVideo by remember { mutableStateOf<Card?>(null) }
            val isAtTop by state.isAtTop()
            val autoplayIndex by state.rememberAutoplayIndex()
            LaunchedEffect(autoplayIndex, isLandscape) {
                playingVideo = cards.getOrNull(
                    (autoplayIndex - (if (isLandscape) 0 else 1)).coerceAtLeast(0)
                )
            }
            Box {
                Row(
                    modifier = Modifier.fillMaxSize()
                ) {
                    if (isLandscape) {
                        LazyVerticalGrid(
                            state = stateLandscape,
                            contentPadding = PaddingValues(PaddingDefault),
                            horizontalArrangement = Arrangement.spacedBy(PaddingDefault, Alignment.CenterHorizontally),
                            verticalArrangement = Arrangement.spacedBy(PaddingDefault, Alignment.Top),
                            columns = GridCells.Fixed(1),
                            modifier = Modifier
                                .width(240.dp)
                                .fillMaxHeight()
                        ) {
                            cardHeaderItem(
                                card,
                                isMine,
                                headerAspect,
                                onClick = { verticalAspect = !verticalAspect },
                                onChange = {
                                    if (card?.id == cardId) {
                                        reload()
                                    } else {
                                        reloadCards()
                                    }
                                },
                                scope,
                                navController,
                                elevation = 2,
                                playVideo = isAtTop
                            )
                        }
                    }

                    LazyVerticalGrid(
                        state = state,
                        contentPadding = PaddingValues(PaddingDefault),
                        horizontalArrangement = Arrangement.spacedBy(PaddingDefault, Alignment.CenterHorizontally),
                        verticalArrangement = Arrangement.spacedBy(PaddingDefault, Alignment.Top),
                        modifier = Modifier.fillMaxSize(),
                        columns = GridCells.Adaptive(240.dp)
                    ) {
                        if (!isLandscape) {
                            cardHeaderItem(
                                card,
                                isMine,
                                headerAspect,
                                onClick = { verticalAspect = !verticalAspect },
                                onChange = {
                                    if (card?.id == cardId) {
                                        reload()
                                    } else {
                                        reloadCards()
                                    }
                                },
                                scope,
                                navController,
                                playVideo = isAtTop
                            )
                        }
                        if (cards.isEmpty()) {
                            if (isLandscape && !isMine) {
                                item(span = { GridItemSpan(maxLineSpan) }) {
                                    Text(
                                        stringResource(R.string.no_cards),
                                        textAlign = TextAlign.Center,
                                        color = MaterialTheme.colorScheme.secondary,
                                        modifier = Modifier.padding(PaddingDefault * 2)
                                    )
                                }
                            }
                        } else {
                            items(cards, { it.id!! }) {
                                CardLayout(
                                    card = it,
                                    isMine = it.person == me()?.id,
                                    showTitle = true,
                                    onClick = {
                                        navController.navigate("card/${it.id!!}")
                                    },
                                    onChange = { reloadCards() },
                                    scope = scope,
                                    navController = navController,
                                    playVideo = playingVideo == it && !isAtTop,
                                )
                            }
                        }
                    }
                }
                if (isMineOrIAmACollaborator) {
                    FloatingActionButton(
                        onClick = {
                            scope.launch {
                                api.newCard(Card(parent = cardId, name = "")) {
                                    reloadCards()
                                    navController.navigate("card/${it.id}")
                                }
                            }
                        },
                        modifier = Modifier
                            .padding(PaddingDefault * 2)
                            .align(Alignment.BottomEnd)
                    ) {
                        Icon(Icons.Outlined.Add, stringResource(R.string.add_a_card))
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
            {
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
            omit = { it.id == me()?.id || card!!.collaborators?.contains(it.id) == true }
        )
    }

    if (openRemoveCollaboratorsDialog) {
        ChoosePeopleDialog(
            {
                openRemoveCollaboratorsDialog = false
            },
            title = stringResource(R.string.collaborators),
            confirmFormatter = defaultConfirmFormatter(
                R.string.remove,
                R.string.remove_person,
                R.string.remove_people,
                R.string.remove_x_people
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
            stringResource(R.string.collaborators),
            {
                openCollaboratorsDialog = false
            }, collaborators, infoFormatter = { person ->
                if (person.id == me()?.id) {
                    context.getString(R.string.leave)
                } else {
                    person.seen?.timeAgo()?.let { timeAgo ->
                        "${context.getString(R.string.active)} ${timeAgo.lowercase()}"
                    }
                }
            }) { person ->
            if (person.id == me()?.id) {
                openLeaveCollaboratorsDialog = true
                openCollaboratorsDialog = false
            } else {
                scope.launch {
                    api.createGroup(listOf(me()!!.id!!, person.id!!), reuse = true) {
                        navController.navigate("group/${it.id!!}")
                        openCollaboratorsDialog = false
                    }
                }
            }
        }
    }

    if (showSendDialog) {
        val sent = stringResource(R.string.sent)
        ChooseGroupDialog(
            {
                showSendDialog = false
            },
            title = stringResource(R.string.send_card),
            confirmFormatter = defaultConfirmFormatter(
                R.string.send_card,
                R.string.send_card_to_group,
                R.string.send_card_to_groups,
                R.string.send_card_to_x_groups
            ) { it.name(someone, emptyGroup, me()?.id?.let(::listOf) ?: emptyList()) },
            me = me()
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
            {
                showQrCode = false
            },
            cardUrl(cardId),
            card?.name
        )
    }
}

private fun LazyGridScope.cardHeaderItem(
    card: Card?,
    isMine: Boolean,
    aspect: Float,
    onClick: () -> Unit,
    onChange: () -> Unit,
    scope: CoroutineScope,
    navController: NavController,
    elevation: Int = 1,
    playVideo: Boolean = false
) {
    item(span = { GridItemSpan(maxLineSpan) }) {
        CardLayout(
            card = card,
            isMine = isMine,
            showTitle = false,
            aspect = aspect,
            onClick = onClick,
            onChange = onChange,
            scope = scope,
            navController = navController,
            elevation = elevation,
            playVideo = playVideo
        )
    }
}

