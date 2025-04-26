package com.queatz.ailaai.schedule

import ReminderEvent
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Alarm
import androidx.compose.material.icons.outlined.AlarmOff
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material.icons.outlined.Clear
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.PersonAdd
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material.icons.outlined.ToggleOff
import androidx.compose.material.icons.outlined.ToggleOn
import androidx.compose.material.icons.outlined.Update
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.ailaai.api.deleteReminder
import app.ailaai.api.joinReminder
import app.ailaai.api.leaveReminder
import app.ailaai.api.reminder
import app.ailaai.api.reminderOccurrences
import app.ailaai.api.updateReminder
import at.bluesource.choicesdk.maps.common.LatLng
import coil3.compose.AsyncImage
import com.queatz.ailaai.AppNav
import com.queatz.ailaai.R
import com.queatz.ailaai.data.api
import com.queatz.ailaai.extensions.appNavigate
import com.queatz.ailaai.extensions.inList
import com.queatz.ailaai.extensions.notBlank
import com.queatz.ailaai.extensions.popBackStackOrFinish
import com.queatz.ailaai.extensions.rememberStateOf
import com.queatz.ailaai.extensions.toLatLng
import com.queatz.ailaai.extensions.toList
import com.queatz.ailaai.me
import com.queatz.ailaai.nav
import com.queatz.ailaai.services.authors
import com.queatz.ailaai.ui.card.CardContent
import com.queatz.ailaai.ui.components.AppBar
import com.queatz.ailaai.ui.components.BackButton
import com.queatz.ailaai.ui.components.DialogBase
import com.queatz.ailaai.ui.components.DialogLayout
import com.queatz.ailaai.ui.components.Dropdown
import com.queatz.ailaai.ui.components.Friends
import com.queatz.ailaai.ui.components.Loading
import com.queatz.ailaai.ui.components.Toolbar
import com.queatz.ailaai.ui.dialogs.Alert
import com.queatz.ailaai.ui.dialogs.ChooseCategoryDialog
import com.queatz.ailaai.ui.dialogs.ChoosePeopleDialog
import com.queatz.ailaai.ui.dialogs.ChoosePhotoDialog
import com.queatz.ailaai.ui.dialogs.ChoosePhotoDialogState
import com.queatz.ailaai.ui.dialogs.DialogCloseButton
import com.queatz.ailaai.ui.dialogs.DialogHeader
import com.queatz.ailaai.ui.dialogs.SetLocationDialog
import com.queatz.ailaai.ui.dialogs.TextFieldDialog
import com.queatz.ailaai.ui.dialogs.defaultConfirmFormatter
import com.queatz.ailaai.ui.story.StorySource
import com.queatz.ailaai.ui.theme.pad
import com.queatz.db.Reminder
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant.Companion.fromEpochMilliseconds
import toEvents
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes

@Composable
fun DurationDialog(
    onDismissRequest: () -> Unit,
    initialDuration: Long = 0L,
    onDuration: suspend (Long) -> Unit
) {
    val scope = rememberCoroutineScope()
    var isLoading by rememberStateOf(false)
    var duration by remember { mutableStateOf(initialDuration) }

    DialogBase(onDismissRequest) {
        DialogLayout(
            content = {
                var hours by remember { mutableStateOf(duration.milliseconds.inWholeHours) }
                var minutes by remember { mutableStateOf(duration.milliseconds.inWholeMinutes - hours.hours.inWholeMinutes) }

                var hoursText by remember { mutableStateOf(hours.toString()) }
                var minutesText by remember { mutableStateOf(minutes.toString()) }

                LaunchedEffect(hours, minutes) {
                    duration = hours.hours.inWholeMilliseconds + minutes.minutes.inWholeMilliseconds

                    if (hours == 24L) {
                        minutes = 0L
                        minutesText = ""
                    }
                }

                DialogHeader(stringResource(R.string.duration))

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 1.pad),
                    verticalArrangement = Arrangement.spacedBy(1.pad)
                ) {
                    OutlinedTextField(
                        value = hoursText,
                        onValueChange = { value ->
                            runCatching {
                                hours = (value.toLongOrNull() ?: 0L).coerceIn(0L..24L)
                                hoursText = hours.toString()
                            }
                        },
                        label = { Text(stringResource(R.string.hours)) },
                        shape = MaterialTheme.shapes.large,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = minutesText,
                        onValueChange = { value ->
                            runCatching {
                                minutes = (value.toLongOrNull() ?: 0L).coerceIn(0L..59L)
                                minutesText = minutes.toString()
                            }
                        },
                        label = { Text(stringResource(R.string.minutes)) },
                        shape = MaterialTheme.shapes.large,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            actions = {
                DialogCloseButton(onDismissRequest)
                Button(
                    onClick = {
                        isLoading = true
                        scope.launch {
                            onDuration(duration)
                            isLoading = false
                        }
                    },
                    enabled = !isLoading,
                ) {
                    Text(stringResource(R.string.update))
                }
            }
        )
    }
}

@Composable
fun ReminderScreen(reminderId: String) {
    val scope = rememberCoroutineScope()
    var isLoading by rememberStateOf(true)
    var showEditNote by rememberStateOf(false)
    var showAddPerson by rememberStateOf(false)
    var showLocationDialog by rememberStateOf(false)
    var showReschedule by rememberStateOf(false)
    var showEditTitle by rememberStateOf(false)
    var showCategory by rememberStateOf(false)
    var showDelete by rememberStateOf(false)
    var showLeave by rememberStateOf(false)
    var showPhotoDialog by rememberStateOf(false)
    var showDurationDialog by rememberStateOf(false)
    var isPhotoLoading by rememberStateOf(false)
    var reminder by rememberStateOf<Reminder?>(null)
    var events by rememberStateOf(emptyList<ReminderEvent>())
    val onExpand = remember {
        MutableSharedFlow<Unit>()
    }
    val nav = nav
    val me = me

    suspend fun reloadEvents() {
        if (reminder == null) {
            return
        }

        api.reminderOccurrences(
            id = reminderId,
            start = reminder!!.start!!,
            end = reminder!!.end ?: Clock.System.now()
        ) {
            events = it.toEvents().asReversed()
        }
    }

    suspend fun reload() {
        api.reminder(reminderId) {
            reminder = it
            reloadEvents()
        }
        isLoading = false
    }

    fun join() {
        scope.launch {
            api.joinReminder(
                id = reminderId,
            ) {
                reload()
            }
        }
    }
    fun toggleAlarm() {
        scope.launch {
            val alarm = reminder?.alarm != true
            api.updateReminder(
                id = reminderId,
                reminder = Reminder(alarm = alarm)
            ) {
                reminder = it
                reloadEvents()
            }
        }
    }

    fun togglePosted() {
        scope.launch {
            val open = reminder?.open == true
            api.updateReminder(
                id = reminderId,
                reminder = Reminder(open = !open)
            ) {
                reminder = it
                reloadEvents()
            }
        }
    }

    LaunchedEffect(Unit) {
        reload()
    }

    if (showCategory) {
        ChooseCategoryDialog(
            onDismissRequest = {
                showCategory = false
            },
            preselect = reminder?.categories?.firstOrNull(),
        ) {
            scope.launch {
                api.updateReminder(
                    reminderId, Reminder(
                        categories = it.inList()
                    )
                ) {
                    api.reminder(reminderId) {
                        reminder = it
                    }
                }
            }
        }
    }

    if (showLocationDialog) {
        SetLocationDialog(
            initialLocation = reminder?.geo?.toLatLng() ?: LatLng(0.0, 0.0),
            onDismissRequest = {
                showLocationDialog = false
            },
            onRemoveLocation = reminder?.geo?.let {
                {
                    scope.launch {
                        api.updateReminder(reminderId, Reminder(geo = emptyList())) {
                            reload()
                        }
                    }
                    showLocationDialog = false
                }
            }
        ) { geo ->
            scope.launch {
                api.updateReminder(reminderId, Reminder(geo = geo.toList())) {
                    reload()
                }
            }
            showLocationDialog = false
        }
    }

    if (showAddPerson) {
        val someone = stringResource(R.string.someone)
        ChoosePeopleDialog(
            onDismissRequest = {
                showAddPerson = false
            },
            title = stringResource(R.string.invite_someone),
            confirmFormatter = defaultConfirmFormatter(
                R.string.invite_someone,
                R.string.invite_person,
                R.string.invite_x_and_y,
                R.string.invite_x_people
            ) { it.name ?: someone },
            onPeopleSelected = { people ->
                api.updateReminder(
                    id = reminderId,
                    reminder = Reminder(
                        people = ((reminder!!.people ?: emptyList()) + people.map { it.id!! }).distinct()
                    )
                ) {
                    reload()
                }
            },
            omit = { it.id!! in (reminder?.people ?: emptyList()) + me?.id!! }
        )
    }

    if (showEditNote) {
        TextFieldDialog(
            onDismissRequest = {
                showEditNote = false
            },
            title = stringResource(R.string.edit_note),
            button = stringResource(R.string.update),
            showDismiss = true,
            dismissButtonText = stringResource(R.string.cancel),
            initialValue = reminder?.note.orEmpty()
        ) {
            api.updateReminder(reminderId, Reminder(note = it)) {
                reload()
                showEditNote = false
            }
        }
    }

    if (showReschedule && reminder != null) {
        ScheduleReminderDialog(
            onDismissRequest = {
                showReschedule = false
            },
            initialReminder = reminder!!
        ) {
            api.updateReminder(
                id = reminderId,
                reminder = Reminder(
                    start = it.start,
                    end = it.end,
                    schedule = it.schedule,
                    stickiness = it.stickiness
                )
            ) {
                reload()
                showReschedule = false
            }
        }
    }

    if (showEditTitle) {
        TextFieldDialog(
            onDismissRequest = {
                showEditTitle = false
            },
            title = stringResource(R.string.title),
            button = stringResource(R.string.update),
            showDismiss = true,
            dismissButtonText = stringResource(R.string.cancel),
            initialValue = reminder?.title ?: ""
        ) {
            api.updateReminder(
                id = reminderId,
                reminder = Reminder(title = it)
            ) {
                reload()
                showEditTitle = false
            }
        }
    }

    if (showDelete) {
        Alert(
            onDismissRequest = {
                showDelete = false
            },
            title = stringResource(R.string.delete_this_reminder),
            text = stringResource(R.string.you_cannot_undo_this_reminder),
            dismissButton = stringResource(R.string.cancel),
            confirmButton = stringResource(R.string.delete),
            confirmColor = MaterialTheme.colorScheme.error
        ) {
            scope.launch {
                api.deleteReminder(reminderId) {
                    nav.popBackStackOrFinish()
                }
            }
        }
    }

    if (showLeave) {
        Alert(
            onDismissRequest = {
                showLeave = false
            },
            title = stringResource(R.string.leave_reminder),
            text = null,
            dismissButton = stringResource(R.string.cancel),
            confirmButton = stringResource(R.string.yes),
            confirmColor = MaterialTheme.colorScheme.error
        ) {
            scope.launch {
                api.leaveReminder(reminderId) {
                    nav.popBackStackOrFinish()
                }
            }
        }
    }

    if (showDurationDialog && reminder != null) {
        DurationDialog(
            onDismissRequest = {
                showDurationDialog = false
            },
            initialDuration = reminder?.duration ?: 1.hours.inWholeMilliseconds,
            onDuration = { duration ->
                api.updateReminder(
                    id = reminderId,
                    reminder = Reminder(duration = duration)
                ) {
                    reload()
                    showDurationDialog = false
                }
            }
        )
    }

    if (showPhotoDialog) {
        ChoosePhotoDialog(
            state = remember { ChoosePhotoDialogState(mutableStateOf("")) },
            onDismissRequest = {
                showPhotoDialog = false
            },
            scope = scope,
            imagesOnly = true,
            onPhotos = { photos ->
                isPhotoLoading = true
                scope.launch {
                    api.updateReminder(
                        id = reminderId,
                        reminder = Reminder(photo = photos.firstOrNull()?.toString())
                    ) {
                        reload()
                        showPhotoDialog = false
                    }
                    isPhotoLoading = false
                }
            },
            onIsGeneratingPhoto = {
                isPhotoLoading = it
            },
            onGeneratedPhoto = { photo ->
                scope.launch {
                    api.updateReminder(
                        id = reminderId,
                        reminder = Reminder(photo = photo)
                    ) {
                        reload()
                        showPhotoDialog = false
                    }
                }
            }
        )
    }

    val isMine = reminder?.person == me?.id

    Column(
        verticalArrangement = Arrangement.Top,
        modifier = Modifier.fillMaxSize()
    ) {
        AppBar(
            title = {
                Column {
                    Text(reminder?.title ?: "", maxLines = 1, overflow = TextOverflow.Ellipsis)
                    reminder?.scheduleText?.notBlank?.let {
                        Text(
                            it,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.secondary,
                        )
                    }
                }
            },
            navigationIcon = {
                BackButton()
            },
            actions = {
                if (!isMine && me?.id in (reminder?.people ?: emptyList())) {
                    var showMenu by remember { mutableStateOf(false) }
                    IconButton(onClick = { showMenu = true }) {
                        Icon(
                            imageVector = Icons.Outlined.MoreVert,
                            contentDescription = null
                        )
                    }
                    Dropdown(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = {
                                Text(stringResource(R.string.leave))
                            },
                            onClick = {
                                showMenu = false
                                showLeave = true
                            }
                        )
                    }
                }
            }
        )

        if (isLoading) {
            Loading()
        } else {
            LazyColumn(
                contentPadding = PaddingValues(1.pad),
                modifier = Modifier
                    .fillMaxSize()
            ) {
                item {
                    if (isMine) {
                        Toolbar {
                            item(
                                icon = if (reminder?.open == true) Icons.Outlined.ToggleOn else Icons.Outlined.ToggleOff,
                                name = stringResource(if (reminder?.open == true) R.string.posted else R.string.not_posted),
                                selected = reminder?.open == true
                            ) {
                                togglePosted()
                            }
                            item(
                                icon = Icons.Outlined.PersonAdd,
                                name = stringResource(R.string.invite_someone)
                            ) {
                                showAddPerson = true
                            }
                            item(
                                icon = Icons.Outlined.EditNote,
                                name = stringResource(R.string.edit_note),
                                selected = !reminder?.note.isNullOrBlank()
                            ) {
                                showEditNote = true
                            }
                            item(
                                icon = Icons.Outlined.Update,
                                name = stringResource(R.string.reschedule)
                            ) {
                                showReschedule = true
                            }
                            item(
                                icon = Icons.Outlined.Edit,
                                name = stringResource(R.string.rename)
                            ) {
                                showEditTitle = true
                            }
                            item(
                                icon = Icons.Outlined.CameraAlt,
                                isLoading = isPhotoLoading,
                                name = stringResource(
                                    if (reminder?.photo.isNullOrBlank()) {
                                        R.string.add_photo
                                    } else {
                                        R.string.set_photo
                                    }
                                ),
                                selected = reminder?.photo != null
                            ) {
                                showPhotoDialog = true
                            }
                            item(
                                icon = Icons.Outlined.EditNote,
                                name = stringResource(R.string.edit_content),
                                selected = !reminder?.content.isNullOrBlank()
                            ) {
                                nav.appNavigate(AppNav.EditReminder(reminderId))
                            }
                            item(
                                icon = Icons.Outlined.Place,
                                name = stringResource(R.string.choose_location),
                                selected = reminder?.geo?.isNotEmpty() == true
                            ) {
                                showLocationDialog = true
                            }
                            item(
                                icon = if (reminder?.alarm == true) Icons.Outlined.Alarm else Icons.Outlined.AlarmOff,
                                name = if (reminder?.alarm == true) stringResource(R.string.alarm_on) else stringResource(
                                    R.string.alarm_off
                                ),
                                selected = reminder?.alarm == true
                            ) {
                                toggleAlarm()
                            }
                            val category = reminder?.categories?.firstOrNull()
                            item(
                                icon = Icons.Outlined.Category,
                                name = category ?: stringResource(R.string.set_category),
                                selected = category != null
                            ) {
                                showCategory = true
                            }
                            item(
                                icon = Icons.Outlined.Timer,
                                name = stringResource(R.string.set_duration),
                                selected = reminder?.duration != null && reminder?.duration!! > 0
                            ) {
                                showDurationDialog = true
                            }
                            if (reminder?.person == me?.id) {
                                item(
                                    icon = Icons.Outlined.Delete,
                                    name = stringResource(R.string.delete),
                                    color = MaterialTheme.colorScheme.error
                                ) {
                                    showDelete = true
                                }
                            } else if (me?.id in (reminder?.people ?: emptyList())) {
                                item(
                                    icon = Icons.Outlined.Clear,
                                    name = stringResource(R.string.leave),
                                    color = MaterialTheme.colorScheme.error
                                ) {
                                    showLeave = true
                                }
                            }
                        }
                    }
                    reminder?.photo?.notBlank?.let { photo ->
                        AsyncImage(
                            model = photo.let(api::url),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 1.pad)
                                .aspectRatio(1.5f)
                                .clip(MaterialTheme.shapes.medium)
                                .background(MaterialTheme.colorScheme.surfaceColorAtElevation(0.dp))
                        )
                    }
                    reminder?.note?.notBlank?.let {
                        OutlinedCard(
                            onClick = {
                                if (isMine) {
                                    showEditNote = true
                                }
                            },
                            shape = MaterialTheme.shapes.medium,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(1.pad)
                        ) {
                            Text(
                                it,
                                modifier = Modifier
                                    .padding(1.5f.pad)
                            )
                        }
                    }
                    reminder?.content?.notBlank?.let { content ->
                        Box(
                            modifier = Modifier
                                .heightIn(max = 2096.dp)
                        ) {
                            CardContent(
                                source = StorySource.Reminder(reminderId),
                                content = content
                            )
                        }
                    }
                    reminder?.let { reminder ->
                        (reminder.person!!.inList() + (reminder.people ?: emptyList()))
                            .distinct()
                            .mapNotNull { authors.get(it) }
                            .sortedByDescending { it.seen ?: fromEpochMilliseconds(0) }
                            .takeIf { it.size > 1 }
                            ?.let { people ->
                                Text(
                                    stringResource(R.string.people) + " (${people.size})",
                                    style = MaterialTheme.typography.titleLarge,
                                    modifier = Modifier
                                        .padding(horizontal = 1.pad)
                                )
                                Friends(
                                    people = people,
                                    modifier = Modifier
                                        .padding(vertical = 1.pad)
                                ) {
                                    nav.appNavigate(AppNav.Profile(it.id!!))
                                }
                            }
                    }
                    if (isMine) {
                        Text(
                            stringResource(R.string.history),
                            style = MaterialTheme.typography.titleLarge,
                            modifier = Modifier
                                .padding(horizontal = 1.pad)
                        )
                        if (events.isEmpty()) {
                            Text(
                                stringResource(R.string.none),
                                modifier = Modifier
                                    .padding(horizontal = 1.pad)
                            )
                        }
                    }
                }
                if (isMine) {
                    items(events) {
                        PeriodEvent(
                            view = ScheduleView.Yearly,
                            event = it,
                            showOpen = false,
                            showFullTime = true,
                            onExpand = onExpand,
                            onUpdated = {
                                scope.launch {
                                    reloadEvents()
                                }
                            }
                        )
                    }
                } else {
                    if (me?.id !in (reminder?.people ?: emptyList())) {
                        item {
                            Button(
                                onClick = {
                                    join()
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                            ) {
                                Text(stringResource(R.string.join))
                            }
                        }
                    }
                }
            }
        }
    }
}
