package com.queatz.ailaai.ui.components

import android.annotation.SuppressLint
import android.app.Activity
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.ParagraphStyle
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
import com.queatz.ailaai.api
import com.queatz.ailaai.databinding.LayoutMapBinding
import com.queatz.ailaai.gson
import com.queatz.ailaai.ui.theme.PaddingDefault
import kotlinx.coroutines.launch

@SuppressLint("UnrememberedMutableState")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BasicCard(
    onClick: () -> Unit,
    onChange: () -> Unit = {},
    activity: Activity,
    card: Card,
    isMine: Boolean = false
) {
    Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.elevatedCardColors(),
        elevation = CardDefaults.elevatedCardElevation(),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(.75f)
                .clickable {
                    onClick()
                },
            contentAlignment = Alignment.BottomCenter
        ) {
            card.photo?.also {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(api.url(it))
                        .crossfade(true)
                        .build(),
                    contentDescription = "Image",
                    contentScale = ContentScale.Crop,
                    alignment = Alignment.TopCenter,
                    modifier = Modifier.matchParentSize()
                )
            }

            Column(
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.background.copy(alpha = .8f))
                    .padding(PaddingDefault * 2)
            ) {
                val conversation = gson.fromJson(card.conversation ?: "{}", ConversationItem::class.java)
                var current by remember { mutableStateOf(conversation) }

                Text(
                    text = buildAnnotatedString {
                        withStyle(
                            MaterialTheme.typography.titleMedium.toSpanStyle().copy(fontWeight = FontWeight.Bold)
                        ) {
                            append(card.name ?: "Someone")
                        }
                        append("  ")
                        withStyle(
                            MaterialTheme.typography.titleSmall.toSpanStyle()
                                .copy(color = MaterialTheme.colorScheme.secondary)
                        ) {
                            append(card.location ?: "Somewhere")
                        }
                        append("\n")
                        append(current.message)
                    },
                    style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurface),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = PaddingDefault)
                )

                current.items.forEach {
                    Button({
                        current = it
                    }) {
                        Text(it.title, overflow = TextOverflow.Ellipsis, maxLines = 1)
                    }
                }

                if (current.items.isEmpty()) {
                    Button({
                        onClick()
                    }) {
                        Icon(Icons.Filled.MailOutline, "", modifier = Modifier.padding(end = PaddingDefault))
                        Text("Send a message", overflow = TextOverflow.Ellipsis, maxLines = 1)
                    }
                }

                AnimatedVisibility(
                    current.title.isNotBlank(),
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                ) {
                    IconButton({
                        current = conversation
                    }) {
                        Icon(Icons.Outlined.Refresh, "Refresh", tint = MaterialTheme.colorScheme.tertiary)
                    }
                }

                if (isMine) showToolbar(activity, onChange, card)
            }
        }
    }
}

@OptIn(ExperimentalPermissionsApi::class, ExperimentalComposeUiApi::class)
@Composable
private fun ColumnScope.showToolbar(activity: Activity, onChange: () -> Unit, card: Card) {
    var openDeleteDialog by remember { mutableStateOf(false) }
    var openEditDialog by remember { mutableStateOf(false) }
    var openLocationDialog by remember { mutableStateOf(false) }
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
                val update = api.updateCard(card.id!!, Card(active = active))
                card.active = update.active
                activeCommitted = update.active ?: false
            }
        })
        Text(
            if (activeCommitted) "Card is active" else "Card is inactive",
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
                        "Card location",
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.padding(bottom = PaddingDefault)
                    )
                    OutlinedTextField(
                        locationName,
                        onValueChange = {
                            locationName = it
                        },
                        label = {
                            Text("Location name")
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
                        "The location name is shown on the card to help others quickly understand where you are generally located.",
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

                                    map?.moveCamera(
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

                                map?.mapType = at.bluesource.choicesdk.maps.common.Map.MAP_TYPE_HYBRID

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
                        "Tap on the map to set the location of this card.",
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
                            Text("Cancel")
                        }
                        TextButton(
                            {
                                disableSaveButton = true

                                coroutineScope.launch {
                                    try {
                                        val update = api.updateCard(
                                            card.id!!,
                                            Card(location = locationName, geo = position.toList())
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
                            Text("Save")
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

                Column(
                    modifier = Modifier.padding(PaddingDefault * 3)
                ) {
                    Text(
                        "Card conversation",
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.padding(bottom = PaddingDefault)
                    )
                    OutlinedTextField(
                        cardName,
                        onValueChange = {
                            cardName = it
                        },
                        label = {
                            Text("Your name")
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
                                    onChange()
                                },
                                modifier = Modifier.padding(PaddingValues(top = PaddingDefault * 2))
                            ) {
                                Icon(
                                    Icons.Outlined.ArrowBack,
                                    "Go back",
                                    modifier = Modifier.padding(end = PaddingDefault)
                                )
                                Text(
                                    backstack.last().message.takeIf { it.isNotBlank() } ?: "Go back",
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
                            label = { Text(if (backstack.isEmpty()) "Your message" else "Your reply") },
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
                            "This ${if (backstack.isEmpty()) "message" else "reply"} is shown on the card ${if (backstack.isEmpty()) "under your name" else "when someone chooses \"${cardConversation.title}\""}.",
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
                                        Text("Option")
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
                                                onChange()
                                                true
                                            } else false
                                        }
                                )
                                if (titleState.isNotBlank()) {
                                    IconButton(
                                        {
                                            backstack.add(cardConversation)
                                            cardConversation = it
                                            onChange()
                                        },
                                        colors = IconButtonDefaults.iconButtonColors(contentColor = MaterialTheme.colorScheme.primary)
                                    ) {
                                        Icon(Icons.Outlined.ArrowForward, "Continue conversation")
                                    }
                                }
                            }
                        }
                        if (cardConversation.items.size < 4) {
                            TextButton(
                                {
                                    cardConversation.items.add(ConversationItem())
                                    onChange()
                                }
                            ) {
                                Icon(
                                    Icons.Outlined.Add,
                                    "Add an option",
                                    modifier = Modifier.padding(end = PaddingDefault)
                                )
                                Text("Add an option")
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
                            Text("Cancel")
                        }
                        TextButton(
                            {
                                disableSaveButton = true

                                coroutineScope.launch {
                                    try {
                                        val update = api.updateCard(
                                            card.id!!,
                                            Card(name = cardName, conversation = gson.toJson(conversation))
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
                            Text("Save")
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
                    Text("Delete card")
                }
            },
            dismissButton = {
                TextButton({
                    openDeleteDialog = false
                }) {
                    Text("Cancel")
                }
            },
            title = {
                Text("Delete this card?")
            },
            text = {
                Text("You cannot undo this.")
            })
    }
}

data class ConversationItem(
    var title: String = "",
    var message: String = "",
    var items: MutableList<ConversationItem> = mutableListOf()
)

fun LatLng.toList() = listOf(latitude, longitude)
