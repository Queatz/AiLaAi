package com.queatz.ailaai.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.queatz.ailaai.R
import com.queatz.ailaai.api.myCards
import com.queatz.ailaai.data.Card
import com.queatz.ailaai.data.Person
import com.queatz.ailaai.data.api
import com.queatz.ailaai.extensions.day
import com.queatz.ailaai.extensions.nameOfDayOfWeek
import com.queatz.ailaai.extensions.rememberStateOf
import com.queatz.ailaai.schedule.ScheduleItemActions
import com.queatz.ailaai.ui.components.AppHeader
import com.queatz.ailaai.ui.components.Loading
import com.queatz.ailaai.ui.components.ScanQrCodeButton
import com.queatz.ailaai.ui.theme.PaddingDefault
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlin.random.Random
import kotlin.random.Random.Default.nextInt
import kotlin.time.Duration.Companion.days

data class MonthSchedule(
    val name: String,
    val count: Int = nextInt(12)
)

@Composable
fun ScheduleScreen(navController: NavController, me: () -> Person?) {
    var myCards by remember { mutableStateOf(emptyList<Card>()) }
    val onExpand = remember { MutableSharedFlow<Unit>() }
    val scope = rememberCoroutineScope()
    var isLoading by rememberStateOf(true)

    LaunchedEffect(Unit) {
        api.myCards {
            myCards = it
        }
        isLoading = false
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            AppHeader(
                navController,
                stringResource(R.string.schedule),
                {
                    // scroll to top
                },
                me
            ) {
                ScanQrCodeButton(navController)
            }
            if (isLoading) {
                Loading()
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(PaddingDefault),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(PaddingDefault)
                ) {
                    items(
                        listOf(
                            "August",
                            "September",
                            "October",
                            "November",
                            "December",
                            "January, 2023",
                            "February"
                        ).map { MonthSchedule(it) }
                    ) {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(PaddingDefault),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                it.name,
                                color = MaterialTheme.colorScheme.secondary,
                                style = MaterialTheme.typography.labelLarge,
                                modifier = Modifier
                                    .padding(
                                        horizontal = PaddingDefault
                                    )
                            )
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .shadow(1.dp, MaterialTheme.shapes.large)
                                    .clip(MaterialTheme.shapes.large)
                                    .background(MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp))
                                    .padding(PaddingDefault / 2f)
                            ) {
                                if (it.count == 0) {
                                    // Month is empty
                                    Row {
                                        Text(
                                            "", modifier = Modifier.width(60.dp)
                                                .padding(
                                                    start = PaddingDefault,
                                                    top = PaddingDefault,
                                                    bottom = PaddingDefault
                                                ),
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Text(
                                            stringResource(R.string.nothing_scheduled),
                                            color = MaterialTheme.colorScheme.secondary.copy(alpha = .5f),
                                            modifier = Modifier
                                                .padding(PaddingDefault)
                                                .weight(1f)
                                        )
                                    }
                                } else {
                                    // Month has reminders
                                    val r = Random(it.name.hashCode())
                                    val days = (0 until it.count).mapNotNull { item ->
                                        myCards.shuffled(r).getOrNull(item)
                                    }.map { it to r.nextInt(4) }
                                    days.forEachIndexed { index, (it, day) ->
                                        Row(
                                            verticalAlignment = Alignment.Top
                                        ) {
                                            Column(
                                                horizontalAlignment = Alignment.CenterHorizontally,
                                                modifier = Modifier.width(60.dp)
                                                    .padding(
                                                        vertical = PaddingDefault,
                                                    )
                                            ) {
                                                if (index == 0 || days[index - 1].second != day) {
                                                    Text(
                                                        "${Clock.System.now().plus(day.days).day()}",
                                                        color = MaterialTheme.colorScheme.primary,
                                                        style = MaterialTheme.typography.titleMedium
                                                    )
                                                    Text(
                                                        Clock.System.now().plus(day.days).nameOfDayOfWeek(),
                                                        color = MaterialTheme.colorScheme.secondary,
                                                        style = MaterialTheme.typography.labelSmall
                                                    )
                                                }
                                            }
                                            var expanded by rememberStateOf(false)
                                            var done by rememberStateOf(it.location.isNullOrBlank().not())

                                            LaunchedEffect(Unit) {
                                                onExpand.collect {
                                                    expanded = false
                                                }
                                            }

                                            Column(
                                                verticalArrangement = Arrangement.Top,
                                                modifier = Modifier
                                                    .clip(MaterialTheme.shapes.large)
                                                    .clickable {
                                                        scope.launch {
                                                            if (!expanded) {
                                                                onExpand.emit(Unit)
                                                            }
                                                            expanded = !expanded
                                                        }
                                                    }
                                                    .padding(PaddingDefault)
                                                    .weight(1f)
                                            ) {
                                                Text(
                                                    it.name ?: "",
                                                    style = MaterialTheme.typography.bodyMedium.let {
                                                        if (done) {
                                                            it.copy(textDecoration = TextDecoration.LineThrough)
                                                        } else {
                                                            it
                                                        }
                                                    },
                                                    color = MaterialTheme.colorScheme.onSurface.let {
                                                        if (done) {
                                                            it.copy(alpha = .5f)
                                                        } else {
                                                            it
                                                        }
                                                    }
                                                )
                                                if (r.nextBoolean()) {
                                                    Text(
                                                        "11:30am",
                                                        style = MaterialTheme.typography.labelSmall.let {
                                                            if (done) {
                                                                it.copy(textDecoration = TextDecoration.LineThrough)
                                                            } else {
                                                                it
                                                            }
                                                        },
                                                        color = MaterialTheme.colorScheme.secondary.let {
                                                            if (done) {
                                                                it.copy(alpha = .5f)
                                                            } else {
                                                                it
                                                            }
                                                        }
                                                    )
                                                }
                                                AnimatedVisibility(expanded) {
                                                    ScheduleItemActions(
                                                        {
                                                            expanded = false
                                                        },
                                                        onDone = {
                                                            done = !done
                                                        },
                                                        onOpen = {
                                                            navController.navigate("card/${it.id!!}")
                                                        },
                                                        onEdit = {

                                                        },
                                                        onRemove = {

                                                        }
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        FloatingActionButton(
            onClick = {
            },
            modifier = Modifier
                .padding(
                    end = PaddingDefault * 2,
                    bottom = PaddingDefault * 2
                )
                .align(Alignment.BottomEnd)
        ) {
            Icon(Icons.Outlined.Add, stringResource(R.string.add_reminder))
        }
    }
}
