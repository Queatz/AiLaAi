package com.queatz.ailaai.ui.components

import android.annotation.SuppressLint
import android.app.Activity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.AnimationConstants.DefaultDurationMillis
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MailOutline
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidViewBinding
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.view.doOnAttach
import androidx.core.view.doOnDetach
import at.bluesource.choicesdk.location.factory.FusedLocationProviderFactory
import at.bluesource.choicesdk.maps.common.*
import at.bluesource.choicesdk.maps.common.listener.OnMarkerDragListener
import at.bluesource.choicesdk.maps.common.options.MarkerOptions
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.google.accompanist.permissions.*
import com.queatz.ailaai.Card
import com.queatz.ailaai.R
import com.queatz.ailaai.api
import com.queatz.ailaai.databinding.LayoutMapBinding
import com.queatz.ailaai.gson
import com.queatz.ailaai.ui.theme.PaddingDefault
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.seconds

@SuppressLint("UnrememberedMutableState")
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun BasicCard(
    onClick: () -> Unit,
    onChange: () -> Unit = {},
    activity: Activity,
    card: Card,
    edit: Boolean = false,
    isMine: Boolean = false
) {
    Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.elevatedCardColors(),
        elevation = CardDefaults.elevatedCardElevation(),
    ) {
        var hideContent by remember { mutableStateOf(false) }
        val alpha by animateFloatAsState(if (!hideContent) 1f else 0f, tween())
        val scale by animateFloatAsState(if (!hideContent) 1f else 1.125f, tween(DefaultDurationMillis * 2))

        LaunchedEffect(hideContent) {
            if (hideContent) {
                delay(2.seconds)
                hideContent = false
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(.75f)
                .combinedClickable(
                    onClick = {
                        onClick()
                    },
                    onLongClick = {
                        hideContent = true
                    }
                ),
            contentAlignment = Alignment.BottomCenter
        ) {
            card.photo?.also {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(api.url(it))
                        .crossfade(true)
                        .build(),
                    contentDescription = "",
                    contentScale = ContentScale.Crop,
                    alignment = Alignment.Center,
                    modifier = Modifier.matchParentSize().scale(scale)
                )
            }

            Column(
                modifier = Modifier
                    .alpha(alpha)
                    .background(MaterialTheme.colorScheme.background.copy(alpha = .8f))
                    .padding(PaddingDefault * 2)
            ) {
                val conversation = gson.fromJson(card.conversation ?: "{}", ConversationItem::class.java)
                var current by remember { mutableStateOf(conversation) }
                val stack = remember { mutableListOf<ConversationItem>() }

                Text(
                    text = buildAnnotatedString {
                        withStyle(
                            MaterialTheme.typography.titleMedium.toSpanStyle().copy(fontWeight = FontWeight.Bold)
                        ) {
                            append(card.name ?: stringResource(R.string.someone))
                        }

                        append("  ")

                        withStyle(
                            MaterialTheme.typography.titleSmall.toSpanStyle()
                                .copy(color = MaterialTheme.colorScheme.secondary)
                        ) {
                            append(card.location ?: stringResource(R.string.somewhere))
                        }
                    },
                    style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurface),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = PaddingDefault)
                )

                Text(
                    text = current.message,
                    style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurface),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = PaddingDefault * 2)
                )

                current.items.forEach {
                    Button({
                        stack.add(current)
                        current = it
                    }) {
                        Text(it.title, overflow = TextOverflow.Ellipsis, maxLines = 1)
                    }
                }

                if (current.items.isEmpty()) {
                    Button({
                        onClick()
                    }, enabled = !isMine) {
                        Icon(Icons.Filled.MailOutline, "", modifier = Modifier.padding(end = PaddingDefault))
                        Text(
                            stringResource(R.string.send_a_message),
                            overflow = TextOverflow.Ellipsis,
                            maxLines = 1
                        )
                    }
                }

                AnimatedVisibility(
                    stack.isNotEmpty(),
                    modifier = Modifier.align(Alignment.Start)
                ) {
                    TextButton({
                        if (stack.isNotEmpty()) {
                            current = stack.removeLast()
                        }
                    }) {
                        Icon(Icons.Outlined.ArrowBack, stringResource(R.string.go_back))
                        Text(stringResource(R.string.go_back), modifier = Modifier.padding(start = PaddingDefault))
                    }
                }

                if (isMine) showToolbar(activity, onChange, card, edit)
            }
        }
    }
}

@SuppressLint("MissingPermission", "UnrememberedMutableState")
@OptIn(ExperimentalPermissionsApi::class, ExperimentalComposeUiApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun ColumnScope.showToolbar(activity: Activity, onChange: () -> Unit, card: Card, edit: Boolean) {
    var openDeleteDialog by remember { mutableStateOf(false) }
    var openEditDialog by remember { mutableStateOf(false) }
    var openLocationDialog by remember { mutableStateOf(edit) }
    val keyboardController = LocalSoftwareKeyboardController.current!!

    Row(
        modifier = Modifier
            .background(Color.Transparent)
            .align(Alignment.End)
            .padding(PaddingValues(top = PaddingDefault))
    ) {
        var active by remember { mutableStateOf(card.active ?: false) }
        var activeCommitted by remember { mutableStateOf(active) }
        val coroutineScope = rememberCoroutineScope()

        Switch(active, {
            active = it
            coroutineScope.launch {
                try {
                    val update = api.updateCard(card.id!!, Card(active = active))
                    card.active = update.active
                    activeCommitted = update.active ?: false
                } catch (ex: Exception) {
                    ex.printStackTrace()
                }
            }
        })
        Text(
            if (activeCommitted) stringResource(R.string.card_active) else stringResource(R.string.card_inactive),
            style = MaterialTheme.typography.labelMedium,
            color = if (activeCommitted) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.secondary,
            modifier = Modifier
                .align(Alignment.CenterVertically)
                .padding(start = PaddingDefault)
        )
        Box(modifier = Modifier.weight(1f))
        IconButton({
            openLocationDialog = true
        }) {
            Icon(Icons.Outlined.Place, "")
        }
        IconButton({
            openEditDialog = true
        }) {
            Icon(Icons.Outlined.Edit, "")
        }
        IconButton({
            openDeleteDialog = true
        }) {
            Icon(Icons.Outlined.Delete, "", tint = MaterialTheme.colorScheme.error)
        }
    }

    if (openLocationDialog) {
        val locationClient = FusedLocationProviderFactory.getFusedLocationProviderClient(
            activity
        )

        var locationName by remember { mutableStateOf(card.location ?: "") }
        var position by remember { mutableStateOf(LatLng(card.geo?.get(0) ?: 0.0, card.geo?.get(1) ?: 0.0)) }
        val coroutineScope = rememberCoroutineScope()
        val permissionState = rememberPermissionState(android.Manifest.permission.ACCESS_FINE_LOCATION)

        when (permissionState.status) {
            is PermissionStatus.Denied -> {
                if (!permissionState.status.shouldShowRationale) {
                    LaunchedEffect(permissionState) {
                        permissionState.launchPermissionRequest()
                    }
                }
            }

            else -> {}
        }

        if (position.toList().sum() == 0.0) {
            locationClient.getLastLocation()
                .addOnFailureListener(activity) {
                    it.printStackTrace()
                }
                .addOnSuccessListener {
                    if (it != null) {
                        position = LatLng(it.latitude, it.longitude)
                    }
                }
        }

        Dialog({
            openLocationDialog = false
        }) {
            Surface(
                shape = MaterialTheme.shapes.extraLarge
            ) {
                Column(
                    modifier = Modifier
                        .padding(PaddingDefault * 3)
                ) {
                    Text(
                        stringResource(R.string.card_location),
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.padding(bottom = PaddingDefault)
                    )
                    OutlinedTextField(
                        locationName,
                        onValueChange = {
                            locationName = it
                        },
                        label = {
                            Text(stringResource(R.string.location_name))
                        },
                        shape = MaterialTheme.shapes.large,
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Words,
                            imeAction = ImeAction.Next
                        ),
                        keyboardActions = KeyboardActions(onSearch = {
                            keyboardController.hide()
                        }),
                        modifier = Modifier
                            .fillMaxWidth()
                    )
                    Text(
                        stringResource(R.string.location_name_description),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.padding(PaddingValues(top = PaddingDefault * 2))
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(1f)
                            .padding(PaddingValues(vertical = PaddingDefault * 2))
                            .clip(MaterialTheme.shapes.large)
                            .background(MaterialTheme.colorScheme.primaryContainer)
                            .border(1.dp, MaterialTheme.colorScheme.outline, MaterialTheme.shapes.large)
                    ) {
                        var composed by remember { mutableStateOf(false) }
                        var marker: Marker? by remember { mutableStateOf(null) }
                        var map: at.bluesource.choicesdk.maps.common.Map? by remember { mutableStateOf(null) }

                        AndroidViewBinding(
                            LayoutMapBinding::inflate,
                            modifier = Modifier
                                .fillMaxSize()
                        ) {

                            if (composed) {
                                if (marker != null) {
                                    marker?.position = position

                                    map?.animateCamera(
                                        CameraUpdateFactory.get().newCameraPosition(
                                            CameraPosition.Builder()
                                                .setTarget(position)
                                                .setZoom(14f)
                                                .build()
                                        )
                                    )
                                }
                                return@AndroidViewBinding
                            } else composed = true

                            mapFragmentContainerView.doOnAttach { it.doOnDetach { mapFragmentContainerView.removeAllViews() } }

                            val mapFragment = mapFragmentContainerView.getFragment<MapFragment>()

                            mapFragment.getMapObservable().subscribe {
                                map = it
                                map?.clear()

                                map?.getUiSettings()?.isMapToolbarEnabled = true
                                map?.getUiSettings()?.isMyLocationButtonEnabled = true

                                marker = map?.addMarker(
                                    MarkerOptions
                                        .create()
                                        .position(position)
                                        .draggable(true)
                                )!!

                                map?.setOnMapClickListener {
                                    position = it
                                }

                                map?.setOnMarkerClickListener { true }
                                map?.setOnMarkerDragListener(object : OnMarkerDragListener {
                                    override fun onMarkerDrag(marker: Marker) {}

                                    override fun onMarkerDragEnd(marker: Marker) {
                                        position = marker.position
                                    }

                                    override fun onMarkerDragStart(marker: Marker) {}
                                })

                                map?.moveCamera(
                                    CameraUpdateFactory.get().newCameraPosition(
                                        CameraPosition.Builder()
                                            .setTarget(position)
                                            .setZoom(14f)
                                            .build()
                                    )
                                )
                            }
                        }
                    }
                    Text(
                        stringResource(R.string.map_description),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.padding(PaddingValues(bottom = PaddingDefault))
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(PaddingDefault, Alignment.End),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        var disableSaveButton by remember { mutableStateOf(false) }

                        TextButton(
                            {
                                openLocationDialog = false
                            }
                        ) {
                            Text(stringResource(R.string.cancel))
                        }
                        TextButton(
                            {
                                disableSaveButton = true

                                coroutineScope.launch {
                                    try {
                                        val update = api.updateCard(
                                            card.id!!,
                                            Card(location = locationName.trim(), geo = position.toList())
                                        )

                                        card.location = update.location
                                        card.geo = update.geo

                                        openLocationDialog = false
                                        onChange()
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                    } finally {
                                        disableSaveButton = false
                                    }

                                }
                            },
                            enabled = !disableSaveButton
                        ) {
                            Text(stringResource(R.string.save))
                        }
                    }
                }
            }
        }
    }

    if (openEditDialog) {
        val conversation = remember {
            card.conversation?.let {
                gson.fromJson(it, ConversationItem::class.java)
            } ?: ConversationItem()
        }

        var cardName by remember { mutableStateOf(card.name ?: "") }
        val backstack = remember { mutableListOf<ConversationItem>() }
        var cardConversation by remember { mutableStateOf(conversation) }
        val coroutineScope = rememberCoroutineScope()

        Dialog({
            openEditDialog = false
        }, DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false)) {
            Surface(
                shape = MaterialTheme.shapes.extraLarge,
                modifier = Modifier
                    .padding(PaddingDefault * 2)
                    .fillMaxHeight(.9f)
            ) {
                val scrollState = rememberScrollState()
                val currentRecomposeScope = currentRecomposeScope
                fun invalidate() {
                    currentRecomposeScope.invalidate()
                }

                Column(
                    modifier = Modifier.padding(PaddingDefault * 3)
                ) {
                    Text(
                        stringResource(R.string.card_conversation),
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.padding(bottom = PaddingDefault)
                    )
                    OutlinedTextField(
                        cardName,
                        onValueChange = {
                            cardName = it
                        },
                        label = {
                            Text(stringResource(R.string.your_name))
                        },
                        shape = MaterialTheme.shapes.large,
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Words,
                            imeAction = ImeAction.Next
                        ),
                        keyboardActions = KeyboardActions(onSearch = {
                            keyboardController.hide()
                        }),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Column(
                        verticalArrangement = Arrangement.spacedBy(PaddingDefault),
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(scrollState)
                    ) {
                        if (backstack.isNotEmpty()) {
                            TextButton(
                                {
                                    cardConversation = backstack.removeLast()
                                    invalidate()
                                },
                                modifier = Modifier.padding(PaddingValues(top = PaddingDefault * 2))
                            ) {
                                Icon(
                                    Icons.Outlined.ArrowBack,
                                    stringResource(R.string.go_back),
                                    modifier = Modifier.padding(end = PaddingDefault)
                                )
                                Text(
                                    backstack.last().message.takeIf { it.isNotBlank() }
                                        ?: stringResource(R.string.go_back),
                                    overflow = TextOverflow.Ellipsis,
                                    maxLines = 1
                                )
                            }
                        }

                        var messageState by mutableStateOf(cardConversation.message)

                        OutlinedTextField(
                            messageState,
                            {
                                messageState = it
                                cardConversation.message = it
                            },
                            shape = MaterialTheme.shapes.large,
                            label = {
                                Text(
                                    if (backstack.isEmpty()) stringResource(R.string.your_message) else stringResource(
                                        R.string.your_reply
                                    )
                                )
                            },
                            keyboardOptions = KeyboardOptions(
                                capitalization = KeyboardCapitalization.Sentences,
                                imeAction = ImeAction.Next
                            ),
                            keyboardActions = KeyboardActions(onSearch = {
                                keyboardController.hide()
                            }),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Text(
                            if (backstack.isEmpty()) stringResource(R.string.card_message_description) else stringResource(
                                R.string.card_reply_description,
                                cardConversation.title
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.padding(PaddingValues(bottom = PaddingDefault))
                        )

                        cardConversation.items.forEach {
                            var titleState by mutableStateOf(it.title)

                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedTextField(
                                    titleState,
                                    { value ->
                                        titleState = value
                                        it.title = value
                                    },
                                    placeholder = {
                                        Text(stringResource(R.string.option))
                                    },
                                    shape = MaterialTheme.shapes.large,
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(
                                        capitalization = KeyboardCapitalization.Sentences,
                                        imeAction = ImeAction.Next
                                    ),
                                    keyboardActions = KeyboardActions(onSearch = {
                                        keyboardController.hide()
                                    }),
                                    modifier = Modifier
                                        .weight(1f)
                                        .onKeyEvent { keyEvent ->
                                            if (it.title.isEmpty() && keyEvent.key == Key.Backspace) {
                                                cardConversation.items.remove(it)
                                                invalidate()
                                                true
                                            } else false
                                        }
                                )
                                if (titleState.isNotBlank()) {
                                    IconButton(
                                        {
                                            backstack.add(cardConversation)
                                            cardConversation = it
                                            invalidate()
                                        },
                                        colors = IconButtonDefaults.iconButtonColors(contentColor = MaterialTheme.colorScheme.primary)
                                    ) {
                                        Icon(
                                            Icons.Outlined.ArrowForward,
                                            stringResource(R.string.continue_conversation)
                                        )
                                    }
                                }
                            }
                        }
                        if (cardConversation.items.size < 4) {
                            TextButton(
                                {
                                    cardConversation.items.add(ConversationItem())
                                    invalidate()
                                }
                            ) {
                                Icon(
                                    Icons.Outlined.Add,
                                    stringResource(R.string.add_an_option),
                                    modifier = Modifier.padding(end = PaddingDefault)
                                )
                                Text(stringResource(R.string.add_an_option))
                            }
                        }
                    }
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(PaddingDefault, Alignment.End),
                        verticalAlignment = Alignment.Bottom,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        var disableSaveButton by remember { mutableStateOf(false) }

                        TextButton(
                            {
                                openEditDialog = false
                            }
                        ) {
                            Text(stringResource(R.string.cancel))
                        }
                        TextButton(
                            {
                                disableSaveButton = true

                                fun trim(it: ConversationItem) {
                                    it.title.trim()
                                    it.message.trim()
                                    it.items.forEach { trim(it) }
                                }

                                trim(conversation)

                                coroutineScope.launch {
                                    try {
                                        val update = api.updateCard(
                                            card.id!!,
                                            Card(name = cardName.trim(), conversation = gson.toJson(conversation))
                                        )

                                        card.name = update.name
                                        card.conversation = update.conversation

                                        openEditDialog = false
                                        onChange()
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                    } finally {
                                        disableSaveButton = false
                                    }
                                }
                            },
                            enabled = !disableSaveButton
                        ) {
                            Text(stringResource(R.string.save))
                        }
                    }
                }
            }
        }
    }

    if (openDeleteDialog) {
        var disableSubmit by remember { mutableStateOf(false) }
        val coroutineScope = rememberCoroutineScope()

        AlertDialog(
            {
                openDeleteDialog = false
            },
            confirmButton = {
                TextButton(
                    {
                        disableSubmit = true

                        coroutineScope.launch {
                            try {
                                api.deleteCard(card.id!!)
                                onChange()
                            } catch (ex: Exception) {
                                ex.printStackTrace()
                            } finally {
                                disableSubmit = false
                                openDeleteDialog = false
                            }
                        }
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                    enabled = !disableSubmit
                ) {
                    Text(stringResource(R.string.delete_card))
                }
            },
            dismissButton = {
                TextButton({
                    openDeleteDialog = false
                }) {
                    Text(stringResource(R.string.cancel))
                }
            },
            title = {
                Text(stringResource(R.string.delete_this_card_q))
            },
            text = {
                Text(stringResource(R.string.you_cannot_undo_this))
            })
    }
}

data class ConversationItem(
    var title: String = "",
    var message: String = "",
    var items: MutableList<ConversationItem> = mutableListOf()
)

fun LatLng.toList() = listOf(latitude, longitude)
