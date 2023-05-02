package com.queatz.ailaai.ui.screens

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.widget.Toast
import android.widget.Toast.LENGTH_SHORT
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.queatz.ailaai.*
import com.queatz.ailaai.R
import com.queatz.ailaai.extensions.*
import com.queatz.ailaai.ui.components.BasicCard
import com.queatz.ailaai.ui.components.CardConversation
import com.queatz.ailaai.ui.components.EditCard
import com.queatz.ailaai.ui.components.Video
import com.queatz.ailaai.ui.dialogs.*
import com.queatz.ailaai.ui.state.jsonSaver
import com.queatz.ailaai.ui.theme.ElevationDefault
import com.queatz.ailaai.ui.theme.PaddingDefault
import kotlinx.coroutines.*
import kotlinx.serialization.encodeToString

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CardScreen(navBackStackEntry: NavBackStackEntry, navController: NavController, me: () -> Person?) {
    val cardId = navBackStackEntry.arguments!!.getString("id")!!
    var addedCardId by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var notFound by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }
    var showManageMenu by remember { mutableStateOf(false) }
    var openDeleteCard by remember { mutableStateOf(false) }
    var openLocationDialog by remember { mutableStateOf(false) }
    var openEditDialog by remember { mutableStateOf(false) }
    var openChangeOwner by remember { mutableStateOf(false) }
    var showQrCode by rememberSaveable { mutableStateOf(false) }
    var showSendDialog by rememberSaveable { mutableStateOf(false) }
    var openAddCollaboratorDialog by rememberSaveable { mutableStateOf(false) }
    var openRemoveCollaboratorsDialog by rememberSaveable { mutableStateOf(false) }
    var openCollaboratorsDialog by rememberSaveable { mutableStateOf(false) }
    var openLeaveCollaboratorsDialog by rememberSaveable { mutableStateOf(false) }
    var showSetCategory by remember { mutableStateOf(false) }
    var card by rememberSaveable(stateSaver = jsonSaver<Card?>()) { mutableStateOf(null) }
    var cards by rememberSaveable(stateSaver = jsonSaver<List<Card>>()) { mutableStateOf(emptyList()) }
    val scope = rememberCoroutineScope()
    val state = rememberLazyGridState()
    val stateLandscape = rememberLazyGridState()
    val context = LocalContext.current
    val didntWork = stringResource(R.string.didnt_work)
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

    LaunchedEffect(Unit) {
        if (card != null) {
            return@LaunchedEffect
        }
        isLoading = true
        notFound = false

        try {
            card = api.card(cardId)
            cards = api.cardsCards(cardId)
        } catch (ex: Exception) {
            ex.printStackTrace()
            notFound = true
        } finally {
            isLoading = false
        }
    }

    val isMine = me()?.id == card?.person
    val isMineOrIAmACollaborator = isMine || card?.collaborators?.contains(me()?.id) == true
    val recomposeScope = currentRecomposeScope

    fun reload() {
        scope.launch {
            try {
                card = api.card(cardId)
            } catch (ex: Exception) {
                ex.printStackTrace()
            }
        }
    }

    if (showSetCategory) {
        ChooseCategoryDialog(
            {
                showSetCategory = false
            },
            { category ->
                scope.launch {
                    try {
                        api.updateCard(
                            card!!.id!!,
                            Card().apply {
                                categories = if (category == null) emptyList() else listOf(category)
                            }
                        )
                        reload()
                    } catch (ex: Exception) {
                        ex.printStackTrace()
                    }
                }
            }
        )
    }


    if (openLocationDialog) {
        EditCardLocationDialog(card!!, navController.context as Activity, {
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
                        try {
                            card!!.person = newOwner
                            val updatedCard = api.updateCard(card!!.id!!, Card().apply {
                                person = card!!.person
                            })
                            card = updatedCard
                        } catch (ex: Exception) {
                            Toast.makeText(context, didntWork, LENGTH_SHORT).show()
                            ex.printStackTrace()
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
            if (card?.photo != null || card?.video != null) {
                item(stringResource(if (card?.active == true) R.string.unpublish else R.string.publish)) {
                    card?.let { card ->
                        scope.launch {
                            try {
                                val update = api.updateCard(
                                    card.id!!,
                                    Card(active = card.active?.not() ?: true)
                                )
                                card.active = update.active
                                Toast.makeText(
                                    context,
                                    context.getString(if (card.active == true) R.string.published else R.string.draft),
                                    LENGTH_SHORT
                                ).show()
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }
                    }
                    showManageMenu = false
                }
            }
            item(stringResource(R.string.change_owner)) {
                openChangeOwner = true
                showManageMenu = false
            }
            item(stringResource(R.string.delete_card)) {
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
                IconButton({
                    navController.popBackStackOrFinish()
                }) {
                    Icon(Icons.Outlined.ArrowBack, Icons.Outlined.ArrowBack.name)
                }
            },
            actions = {
                card?.let { card ->
                    IconButton({
                        scope.launch {
                            when (saves.toggleSave(card)) {
                                ToggleSaveResult.Saved -> {
                                    Toast.makeText(context, context.getString(R.string.card_saved), LENGTH_SHORT)
                                        .show()
                                }

                                ToggleSaveResult.Unsaved -> {
                                    Toast.makeText(
                                        context,
                                        context.getString(R.string.card_unsaved),
                                        LENGTH_SHORT
                                    ).show()
                                }

                                else -> {
                                    Toast.makeText(context, context.getString(R.string.didnt_work), LENGTH_SHORT)
                                        .show()
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
                        try {
                            if (it.isVideo(context)) {
                                isUploadingVideo = true
                                api.uploadCardVideo(
                                    card!!.id!!,
                                    it,
                                    context.contentResolver.getType(it) ?: "video/*",
                                    it.lastPathSegment ?: "video.${context.contentResolver.getType(it)?.split("/")?.lastOrNull() ?: ""}"
                                ) {
                                    videoUploadProgress = it
                                }
                            } else if (it.isPhoto(context)) {
                                api.uploadCardPhoto(card!!.id!!, it)
                            }
                            card = api.card(cardId)
                        } catch (ex: Exception) {
                            ex.printStackTrace()
                        } finally {
                            uploadJob = null
                            isUploadingVideo = false
                        }
                    }
                }

                DropdownMenu(showMenu, { showMenu = false }) {
                    if (isMine) {
                        DropdownMenuItem({
                            Text(stringResource(R.string.edit_card))
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
                    DropdownMenuItem({
                        Text(stringResource(R.string.view_profile))
                    }, {
                        navController.navigate("profile/${card!!.person!!}")
                        showMenu = false
                    })
                    if (card?.parent != null) {
                        DropdownMenuItem({
                            Text(stringResource(R.string.open_enclosing_card))
                        }, {
                            navController.navigate("card/${card!!.parent!!}")
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
                                Toast.makeText(context, textCopied, LENGTH_SHORT).show()
                            }
                            showMenu = false
                        })
                    }
                    DropdownMenuItem({
                        Text(stringResource(R.string.share))
                    }, {
                        cardUrl(cardId).shareAsUrl(context, card?.name)
                        showMenu = false
                    })
                    DropdownMenuItem({
                        Text(stringResource(R.string.copy_link))
                    }, {
                        cardUrl(cardId).copyToClipboard(context, card?.name ?: cardString)
                        Toast.makeText(context, textCopied, LENGTH_SHORT).show()
                        showMenu = false
                    })
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            ),
            modifier = Modifier
                .shadow(ElevationDefault / 2)
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
            var verticalAspect by remember { mutableStateOf(false) }
            val headerAspect by animateFloatAsState(if (verticalAspect) .75f else 1.5f)
            var playingVideo by remember { mutableStateOf<Card?>(null) }
            val isAtTop by state.isAtTop()
            val autoplayIndex by state.rememberAutoplayIndex()
            LaunchedEffect(autoplayIndex, isLandscape) {
                playingVideo = cards.getOrNull(
                    (autoplayIndex - (if (isLandscape) 0 else 1)).coerceAtLeast(0)
                )
            }
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
                            .shadow(ElevationDefault)
                            .background(MaterialTheme.colorScheme.surfaceColorAtElevation(ElevationDefault))
                    ) {
                        cardHeaderItem(
                            card,
                            isMine,
                            headerAspect,
                            onsetCategoryClick = { showSetCategory = true },
                            toggleAspect = { verticalAspect = !verticalAspect },
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
                            onsetCategoryClick = { showSetCategory = true },
                            toggleAspect = { verticalAspect = !verticalAspect },
                            scope,
                            navController,
                            playVideo = isAtTop
                        )
                    }
                    if (cards.isEmpty()) {
//                        item(span = { GridItemSpan(maxLineSpan) }) {
//                            Text(
//                                stringResource(R.string.no_cards),
//                                textAlign = TextAlign.Center,
//                                color = MaterialTheme.colorScheme.secondary,
//                                modifier = Modifier.padding(PaddingDefault * 2)
//                            )
//                        }
                    } else {
                        items(cards, { it.id!! }) {
                            BasicCard(
                                {
                                    navController.navigate("card/${it.id!!}")
                                },
                                onReply = { conversation ->
                                    scope.launch {
                                        it.reply(conversation) { groupId ->
                                            navController.navigate("group/${groupId}")
                                        }
                                    }
                                },
                                onChange = {
                                    scope.launch {
                                        isLoading = true
                                        try {
                                            cards = api.cardsCards(cardId)
                                        } catch (ex: Exception) {
                                            ex.printStackTrace()
                                        } finally {
                                            isLoading = false
                                        }
                                    }
                                },
                                activity = navController.context as Activity,
                                card = it,
                                edit = if (it.id == addedCardId) EditCard.Conversation else null,
                                onCategoryClick = {
                                    exploreInitialCategory = it
                                    navController.navigate("explore")
                                },
                                isMine = it.person == me()?.id,
                                playVideo = playingVideo == it && !isAtTop
                            )
                            if (it.id == addedCardId) {
                                addedCardId = null
                            }
                        }
                    }
                    if (isMineOrIAmACollaborator) {
                        item(span = { GridItemSpan(maxLineSpan) }) {
                            Box(contentAlignment = Alignment.Center) {
                                ElevatedButton(
                                    {
                                        scope.launch {
                                            try {
                                                addedCardId = api.newCard(Card(parent = cardId, name = "")).id
                                                cards = api.cardsCards(cardId)
                                                delay(100)

                                                if (state.firstVisibleItemIndex > 2) {
                                                    state.scrollToItem(2)
                                                }

                                                state.animateScrollToItem(0)
                                            } catch (ex: Exception) {
                                                ex.printStackTrace()
                                            }
                                        }
                                    }
                                ) {
                                    Text(stringResource(R.string.add_a_card))
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
                        try {
                            api.leaveCollaboration(cardId)
                            card = api.card(cardId)
                            cards = api.cardsCards(cardId)
                            openLeaveCollaboratorsDialog = false
                        } catch (ex: Exception) {
                            Toast.makeText(context, didntWork, LENGTH_SHORT).show()
                            ex.printStackTrace()
                        }
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
                try {
                    card!!.collaborators = (card?.collaborators ?: emptyList()) + people.map { it.id!! }
                    val updatedCard = api.updateCard(card!!.id!!, Card().apply {
                        collaborators = card!!.collaborators
                    })
                    card = updatedCard
                } catch (ex: Exception) {
                    Toast.makeText(context, didntWork, LENGTH_SHORT).show()
                    ex.printStackTrace()
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
                try {
                    card!!.collaborators = (card?.collaborators ?: emptyList()) - people.map { it.id!! }.toSet()
                    val updatedCard = api.updateCard(card!!.id!!, Card().apply {
                        collaborators = card!!.collaborators
                    })
                    card = updatedCard
                } catch (ex: Exception) {
                    Toast.makeText(context, didntWork, LENGTH_SHORT).show()
                    ex.printStackTrace()
                }
            },
            omit = { it.id !in (card!!.collaborators ?: emptyList()) }
        )
    }

    var collaborators by remember { mutableStateOf(emptyList<Person>()) }

    LaunchedEffect(openCollaboratorsDialog) {
        if (openCollaboratorsDialog) {
            collaborators = api.cardPeople(cardId)
        }
    }

    if (openCollaboratorsDialog && collaborators.isNotEmpty()) {
        GroupMembersDialog({
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
                    val group = api.createGroup(listOf(me()!!.id!!, person.id!!), reuse = true)
                    navController.navigate("group/${group.id!!}")
                }
                openCollaboratorsDialog = false
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
            me = me(),
            onGroupsSelected = { groups ->
                try {
                    coroutineScope {
                        groups.map { group ->
                            async {
                                api.sendMessage(
                                    group.id!!,
                                    Message(attachment = json.encodeToString(CardAttachment(cardId)))
                                )
                            }
                        }.awaitAll()
                    }
                    Toast.makeText(context, sent, LENGTH_SHORT).show()
                } catch (ex: Exception) {
                    Toast.makeText(context, didntWork, LENGTH_SHORT).show()
                    ex.printStackTrace()
                }
            }
        )
    }

    if (showQrCode) {
        ShareCardQrCodeDialog({
            showQrCode = false
        }, cardUrl(cardId), card?.name)
    }
}

private fun LazyGridScope.cardHeaderItem(
    card: Card?,
    isMine: Boolean,
    aspect: Float,
    onsetCategoryClick: () -> Unit,
    toggleAspect: () -> Unit,
    scope: CoroutineScope,
    navController: NavController,
    elevation: Int = 1,
    playVideo: Boolean = false
) {
    item(span = { GridItemSpan(maxLineSpan) }) {
        Column {
            val video = card?.video
            if (video != null) {
                Video(
                    video.let(api::url),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(MaterialTheme.shapes.large)
                        .aspectRatio(aspect)
                        .background(MaterialTheme.colorScheme.surfaceColorAtElevation(ElevationDefault * elevation))
                        .clickable {
                            toggleAspect()
                        },
                    isPlaying = playVideo
                )
            } else {
                card?.photo?.also {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(api.url(it))
                            .crossfade(true)
                            .build(),
                        contentDescription = "",
                        contentScale = ContentScale.Crop,
                        alignment = Alignment.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(MaterialTheme.shapes.large)
                            .aspectRatio(aspect)
                            .background(MaterialTheme.colorScheme.surfaceColorAtElevation(ElevationDefault * elevation))
                            .clickable {
                                toggleAspect()
                            }
                    )
                }
            }
            card?.let {
                CardConversation(
                    card,
                    interactable = true,
                    showTitle = false,
                    isMine = isMine,
                    onCategoryClick = {
                        if (isMine) {
                            onsetCategoryClick()
                        } else {
                            exploreInitialCategory = it
                            navController.navigate("explore")
                        }
                    },
                    onSetCategoryClick = {
                        onsetCategoryClick()
                    },
                    onReply = { conversation ->
                        scope.launch {
                            it.reply(conversation) { groupId ->
                                navController.navigate("group/${groupId}")
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(PaddingDefault)
                )
            }
        }
    }
}
