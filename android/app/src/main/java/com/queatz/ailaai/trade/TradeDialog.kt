package com.queatz.ailaai.trade

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AddAPhoto
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import cancelTrade
import coil3.compose.AsyncImage
import com.queatz.ailaai.AppNav
import com.queatz.ailaai.R
import com.queatz.ailaai.api.uploadPhotosFromUris
import com.queatz.ailaai.data.api
import com.queatz.ailaai.extensions.appNavigate
import com.queatz.ailaai.extensions.inList
import com.queatz.ailaai.extensions.notEmpty
import com.queatz.ailaai.extensions.rememberStateOf
import com.queatz.ailaai.extensions.status
import com.queatz.ailaai.extensions.timeAgo
import com.queatz.ailaai.extensions.toast
import com.queatz.ailaai.group.SendTradeDialog
import com.queatz.ailaai.item.InventoryItemLayout
import com.queatz.ailaai.me
import com.queatz.ailaai.nav
import com.queatz.ailaai.services.push
import com.queatz.ailaai.ui.components.DialogBase
import com.queatz.ailaai.ui.components.DialogLayout
import com.queatz.ailaai.ui.components.Dropdown
import com.queatz.ailaai.ui.components.EmptyText
import com.queatz.ailaai.ui.components.Loading
import com.queatz.ailaai.ui.components.LoadingIcon
import com.queatz.ailaai.ui.dialogs.Alert
import com.queatz.ailaai.ui.dialogs.ChoosePhotoDialog
import com.queatz.ailaai.ui.dialogs.DialogCloseButton
import com.queatz.ailaai.ui.dialogs.Media
import com.queatz.ailaai.ui.dialogs.PeopleDialog
import com.queatz.ailaai.ui.dialogs.PhotoDialog
import com.queatz.ailaai.ui.dialogs.TextFieldDialog
import com.queatz.ailaai.ui.screens.seenText
import com.queatz.ailaai.ui.theme.pad
import com.queatz.db.InventoryItemExtended
import com.queatz.db.Person
import com.queatz.db.Trade
import com.queatz.db.TradeExtended
import com.queatz.db.TradeItem
import com.queatz.db.TradeMember
import com.queatz.db.TradePhoto
import com.queatz.push.TradePushData
import confirmTrade
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.launch
import kotlin.time.Clock
import trade
import unconfirmTrade
import updateTrade
import updateTradeItems

data class TradeMemberItem(
    val inventoryItem: InventoryItemExtended,
    val from: Person,
    val to: Person,
    val quantity: Double
)

data class TradeItemMember(
    val item: TradeItem,
    val from: TradeMember
)

data class TradeMemberState(
    val person: Person,
    val items: List<TradeMemberItem>,
    val confirmed: Boolean
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TradeDialog(
    onDismissRequest: () -> Unit,
    tradeId: String,
    onTradeUpdated: () -> Unit = {},
    onTradeCancelled: () -> Unit = {},
    onTradeCompleted: () -> Unit = {},
) {
    val scope = rememberCoroutineScope()
    var trade by rememberStateOf<TradeExtended?>(null)
    var isLoading by rememberStateOf(false)
    var isCompletedOrCancelled by rememberStateOf(false)
    var showCancelDialog by rememberStateOf(false)
    var showMenu by rememberStateOf(false)
    var showMembers by rememberStateOf(false)
    var editNote by rememberStateOf(false)
    var addPhotoDialog by rememberStateOf(false)
    var isGeneratingPhoto by rememberStateOf(false)
    var showSendDialog by rememberStateOf(false)
    var showPhotoDialog by rememberStateOf<String?>(null)
    var showDeletePhotoDialog by rememberStateOf<String?>(null)
    var editItemDialog by rememberStateOf<TradeMemberItem?>(null)
    var addItemDialog by rememberStateOf<TradeMemberItem?>(null)
    var addInventoryItemDialog by rememberStateOf<Person?>(null)
    val me = me ?: return
    val context = LocalContext.current
    val nav = nav

    val anyConfirmed = trade?.trade?.members?.any { it.confirmed == true } == true
    val confirmedByMe = trade?.trade?.members?.any { it.person == me.id!! && it.confirmed == true } == true
    val myTradeMember = trade?.trade?.members?.first { it.person == me.id }
    val enableConfirm = confirmedByMe || trade?.trade?.members?.any { it.items!!.isNotEmpty() } == true
    val mutable = !anyConfirmed && !isCompletedOrCancelled

    // todo reload on trade updates, cancelled, completed pushes

    val members = remember(trade) {
        trade?.let { trade ->
            val allItems = trade.trade!!.members!!
                .flatMap { member -> member.items!!.map { TradeItemMember(it, member) } }
                .map { item ->
                    TradeMemberItem(
                        inventoryItem = trade.inventoryItems!!.first { it.inventoryItem!!.id == item.item.inventoryItem!! },
                        from = trade.people!!.first { it.id == item.from.person },
                        to = trade.people!!.first { it.id == item.item.to },
                        quantity = item.item.quantity!!
                    )
                }
            trade.trade!!.members!!.map { member ->
                TradeMemberState(
                    person = trade.people!!.first { it.id == member.person },
                    items = allItems.filter {
                        it.to.id == member.person
                    },
                    confirmed = member.confirmed == true
                )
            }
        } ?: emptyList()
    }

    suspend fun reload() {
        api.trade(tradeId) {
            trade = it
        }
    }

    fun cancel() {
        scope.launch {
            api.cancelTrade(tradeId) {
                trade = it
            }
        }
    }

    suspend fun saveNote(note: String) {
        api.updateTrade(
            tradeId = tradeId,
            trade = Trade(note = note)
        ) {
            trade = it
            onTradeUpdated()
        }
    }

    suspend fun savePhotos(photos: List<TradePhoto>) {
        api.updateTrade(
            tradeId = tradeId,
            trade = Trade(photos = photos)
        ) {
            trade = it
            onTradeUpdated()
        }
    }

    fun confirmUnconfirm() {
        if (confirmedByMe) {
            scope.launch {
                api.unconfirmTrade(tradeId) {
                    trade = it
                    onTradeUpdated()
                }
            }
        } else {
            scope.launch {
                api.confirmTrade(
                    tradeId = tradeId,
                    trade = trade!!.trade!!,
                    onError = {
                        if (it.status == HttpStatusCode.BadRequest) {
                            reload()
                            context.toast(R.string.trade_updated)
                        }
                    }
                ) {
                    trade = it
                    onTradeUpdated()
                }
            }
        }
    }

    if (addPhotoDialog) {
        ChoosePhotoDialog(
            onDismissRequest = {
                addPhotoDialog = false
            },
            scope = scope,
            imagesOnly = true,
            onPhotos = { photos ->
                isGeneratingPhoto = true
                api.uploadPhotosFromUris(
                    context = context,
                    photos = photos,
                ) {
                    savePhotos(
                        trade?.trade?.photos.orEmpty() + it.urls.map { photo ->
                            TradePhoto(
                                person = me.id!!,
                                photo = photo,
                                addedAt = Clock.System.now()
                            )
                        }
                    )
                }
                isGeneratingPhoto = false
            },
            onIsGeneratingPhoto = {
                isGeneratingPhoto = it
            },
            onGeneratedPhoto = {
                savePhotos(
                    trade?.trade?.photos.orEmpty() + TradePhoto(
                        person = me.id!!,
                        photo = it,
                        addedAt = Clock.System.now()
                    )
                )
            }
        )
    }

    showDeletePhotoDialog?.let { url ->
        Alert(
            onDismissRequest = {
                showDeletePhotoDialog = null
            },
            title = stringResource(R.string.remove_photo),
            text = null,
            dismissButton = stringResource(R.string.cancel),
            confirmButton = stringResource(R.string.remove),
            confirmColor = MaterialTheme.colorScheme.error,
        ) {
            scope.launch {
                savePhotos(
                    trade?.trade?.photos.orEmpty().filter {
                        it.photo != url
                    }
                )
            }
            showDeletePhotoDialog = null
        }
    }

    showPhotoDialog?.let { url ->
        PhotoDialog(
            onDismissRequest = {
                showPhotoDialog = null
            },
            initialMedia = Media.Photo(url),
            medias = Media.Photo(url).inList()
        )
    }

    LaunchedEffect(tradeId) {
        trade = null
        isLoading = true
        reload()
        isCompletedOrCancelled = trade?.inProgress != true
        isLoading = false
    }

    LaunchedEffect(Unit) {
        push.events
            .mapNotNull { it as? TradePushData }
            .filter { it.trade.id == tradeId }
            .catch { it.printStackTrace() }
            .collectLatest {
                reload()
            }
    }

    LaunchedEffect(trade) {
        if (isCompletedOrCancelled) {
            return@LaunchedEffect
        }

        if (trade?.trade?.completedAt != null) {
            onTradeCompleted()
            context.toast(R.string.trade_completed)
            onDismissRequest()
        } else if (trade?.trade?.cancelledAt != null) {
            onTradeCancelled()
            context.toast(R.string.trade_cancelled)
            onDismissRequest()
        }
    }

    DialogBase(
        onDismissRequest = onDismissRequest
    ) {
        DialogLayout(
            scrollable = false,
            content = {
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .padding(bottom = 1.pad)
                            .weight(1f)
                    ) {
                        Text(
                            stringResource(R.string.trade),
                            style = MaterialTheme.typography.titleLarge
                        )
                        when {
                            trade?.trade?.completedAt != null -> {
                                Text(
                                    stringResource(R.string.completed) + " ${trade!!.trade!!.completedAt!!.timeAgo()}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                            }
                            trade?.trade?.cancelledAt != null -> {
                                Text(
                                    stringResource(R.string.cancelled) + " ${trade!!.trade!!.cancelledAt!!.timeAgo()}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                            }
                            else -> Unit
                        }
                    }

                    if (!isCompletedOrCancelled) {
                        IconButton(
                            onClick = {
                                addPhotoDialog = true
                            },
                            enabled = mutable,
                        ) {
                            if (isGeneratingPhoto) {
                                LoadingIcon()
                            } else {
                                Icon(
                                    imageVector = Icons.Outlined.AddAPhoto,
                                    contentDescription = null
                                )
                            }
                        }
                        IconButton(
                            onClick = {
                                editNote = true
                            },
                            enabled = mutable
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Edit,
                                contentDescription = null
                            )
                        }
                    }

                    IconButton(
                        onClick = {
                            showMenu = true
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.MoreVert,
                            contentDescription = null
                        )

                        Dropdown(
                            showMenu,
                            { showMenu = false }
                        ) {
                            DropdownMenuItem(
                                {
                                    Text(stringResource(R.string.members))
                                },
                                onClick = {
                                    showMenu = false
                                    showMembers = true
                                }
                            )
                            DropdownMenuItem(
                                {
                                    Text(stringResource(R.string.send))
                                },
                                onClick = {
                                    showMenu = false
                                    showSendDialog = true
                                }
                            )
                            if (trade?.inProgress == true) {
                                DropdownMenuItem(
                                    {
                                        Text(stringResource(R.string.cancel))
                                    },
                                    onClick = {
                                        showMenu = false
                                        showCancelDialog = true
                                    }
                                )
                            }
                        }
                    }
                }
                if (isLoading) {
                    Loading()
                } else {
                    trade?.let { trade ->
                        LazyVerticalGrid(
                            state = rememberLazyGridState(),
                            columns = GridCells.Adaptive(96.dp),
                            horizontalArrangement = Arrangement.spacedBy(1.pad),
                            verticalArrangement = Arrangement.spacedBy(1.pad),
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f, fill = false)
                                .padding(bottom = 1.pad)
                        ) {
                            if (!trade.trade?.note.isNullOrBlank()) {
                                item(span = { GridItemSpan(maxLineSpan) }) {
                                    OutlinedCard(
                                        onClick = {
                                            if (mutable) {
                                                editNote = true
                                            }
                                        },
                                        shape = MaterialTheme.shapes.large,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                    ) {
                                        Text(
                                            trade.trade?.note ?: "",
                                            modifier = Modifier
                                                .padding(horizontal = 1.5f.pad, vertical = 1.pad)
                                        )
                                    }
                                }
                            }
                            trade.trade?.photos?.notEmpty?.let { photos ->
                                item(span = { GridItemSpan(maxLineSpan) }) {
                                    FlowRow(
                                        horizontalArrangement = Arrangement.spacedBy(1.pad),
                                        verticalArrangement = Arrangement.spacedBy(1.pad),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 1.pad)
                                    ) {
                                        photos.forEach { photo ->
                                            AsyncImage(
                                                model = api.url(photo.photo!!),
                                                contentDescription = null,
                                                contentScale = ContentScale.Crop,
                                                alignment = Alignment.Center,
                                                modifier = Modifier
                                                    .requiredSize(64.dp)
                                                    .clip(MaterialTheme.shapes.large)
                                                    .combinedClickable(
                                                        onClick = {
                                                            showPhotoDialog = photo.photo!!
                                                        },
                                                        onLongClick = {
                                                            if (mutable) {
                                                                showDeletePhotoDialog = photo.photo!!
                                                            }
                                                        }
                                                    )
                                            )
                                        }
                                    }
                                }
                            }
                            members.forEach { member ->
                                item(span = { GridItemSpan(maxLineSpan) }) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = stringResource(R.string.x_gets, member.person.name ?: stringResource(R.string.someone))
                                        )
                                        if (anyConfirmed) {
                                            Text(" • ")
                                            if (member.confirmed) {
                                                Text(
                                                    stringResource(R.string.confirmed),
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                                Icon(
                                                    Icons.Outlined.Check,
                                                    stringResource(R.string.confirmed),
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier
                                                        .padding(start = .5f.pad)
                                                )
                                            } else {
                                                Text(stringResource(R.string.waiting))
                                            }
                                        }
                                    }
                                }

                                if ((member.person.id == me.id || !mutable) && member.items.isEmpty()) {
                                    item(span = { GridItemSpan(maxLineSpan) }) {
                                        OutlinedCard(
                                            shape = MaterialTheme.shapes.large,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                        ) {
                                            EmptyText(stringResource(R.string.no_items))
                                        }
                                    }
                                } else {
                                    items(member.items) { item ->
                                        InventoryItemLayout(item.inventoryItem, quantity = item.quantity) {
                                            editItemDialog = item
                                        }
                                    }

                                    if (member.person.id != me.id && mutable) {
                                        item {
                                            AddInventoryItemButton {
                                                addInventoryItemDialog = member.person
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            },
            actions = {
                DialogCloseButton(onDismissRequest)
                if (trade?.inProgress == true) {
                    if (confirmedByMe) {
                        OutlinedButton(
                            onClick = {
                                confirmUnconfirm()
                            },
                            enabled = true
                        ) {
                            Text(
                                stringResource(
                                    R.string.unconfirm
                                )
                            )
                        }
                    } else {
                        OutlinedButton(
                            {
                                confirmUnconfirm()
                            },
                            enabled = enableConfirm
                        ) {
                            Text(
                                stringResource(
                                    R.string.confirm
                                )
                            )
                        }
                    }
                }
            }
        )
    }

    if (editNote) {
        TextFieldDialog(
            onDismissRequest = {
                editNote = false
            },
            title = stringResource(R.string.note),
            initialValue = trade?.trade?.note ?: "",
            button = stringResource(R.string.update),
            showDismiss = true
        ) {
            saveNote(it)
            editNote = false
        }
    }

    fun maxQuantity(item: InventoryItemExtended, to: String? = null) =
        item.inventoryItem!!.quantity!! - trade!!.trade!!.members!!
            .flatMap { it.items!! }
            .filter { it.inventoryItem == item.inventoryItem!!.id!! && it.to != to }
            .sumOf { it.quantity!! }
            .coerceAtLeast(0.0)

    addInventoryItemDialog?.let { person ->
        AddInventoryItemDialog(
            {
                addInventoryItemDialog = null
            },
            omit = myTradeMember?.items?.filter { it.to == person.id }?.map { it.inventoryItem!! } ?: emptyList()
        ) { item ->
            val quantity = 1.0.coerceAtMost(maxQuantity(item))

            addItemDialog = TradeMemberItem(
                item,
                me,
                person,
                quantity
            )
            addInventoryItemDialog = null
        }
    }

    editItemDialog?.let { item ->
        TradeItemDialog(
            onDismissRequest = {
                editItemDialog = null
            },
            inventoryItem = item.inventoryItem,
            initialQuantity = item.quantity,
            maxQuantity = maxQuantity(item.inventoryItem, item.to.id!!),
            isMine = item.from.id == me.id,
            enabled = mutable,
            active = trade?.inProgress ?: true,
            onQuantity = { newQuantity ->
                val items = myTradeMember!!.items!!.mapNotNull {
                    if (it.inventoryItem == editItemDialog!!.inventoryItem.inventoryItem!!.id!! && it.to == editItemDialog!!.to.id!!) {
                        if (newQuantity > 0.0) {
                            it.copy(quantity = newQuantity)
                        } else {
                            null
                        }
                    } else {
                        it
                    }
                }
                editItemDialog = null
                scope.launch {
                    api.updateTradeItems(tradeId, items) {
                        trade = it
                    }
                }
            }
        )
    }

    addItemDialog?.let { item ->
        TradeItemDialog(
            onDismissRequest = {
                addItemDialog = null
            },
            inventoryItem = item.inventoryItem,
            initialQuantity = item.quantity,
            maxQuantity = maxQuantity(item.inventoryItem, item.to.id!!),
            isAdd = true,
            isMine = item.from.id == me.id,
            enabled = mutable,
            onQuantity = { newQuantity ->
                if (newQuantity > 0.0) {
                    val items = (myTradeMember!!.items!! + TradeItem(
                        inventoryItem = item.inventoryItem.inventoryItem!!.id!!,
                        quantity = newQuantity,
                        to = item.to.id!!
                    )).distinctBy { it.inventoryItem!! to it.to }
                    scope.launch {
                        api.updateTradeItems(tradeId, items) {
                            trade = it
                        }
                    }
                }
                addItemDialog = null
            }
        )
    }

    if (showMembers) {
        PeopleDialog(
            title = stringResource(R.string.members),
            onDismissRequest = {
                showMembers = false
            },
            people = trade!!.people!!,
            infoFormatter = { person ->
                person.seenText(context.getString(R.string.active))
            }
        ) {
            showMembers = false
            nav.appNavigate(AppNav.Profile(it.id!!))
        }
    }

    if (showCancelDialog) {
        AlertDialog(
            {
                showCancelDialog = false
            },
            title = {
                Text(stringResource(R.string.cancel_this_trade))
            },
            confirmButton = {
                TextButton(
                    {
                        cancel()
                        showCancelDialog = false
                    }
                ) {
                    Text(stringResource(R.string.cancel_trade))
                }
            },
            dismissButton = {
                DialogCloseButton {
                    showCancelDialog = false
                }
            }
        )
    }

    if (showSendDialog) {
        SendTradeDialog({ showSendDialog = false }, tradeId)
    }
}

val TradeExtended.inProgress get() = trade?.cancelledAt == null && trade?.completedAt == null
