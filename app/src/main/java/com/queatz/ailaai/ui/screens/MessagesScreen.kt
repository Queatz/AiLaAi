package com.queatz.ailaai.ui.screens

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.queatz.ailaai.GroupExtended
import com.queatz.ailaai.Person
import com.queatz.ailaai.R
import com.queatz.ailaai.api
import com.queatz.ailaai.ui.components.ContactItem
import com.queatz.ailaai.ui.components.SearchField
import com.queatz.ailaai.ui.dialogs.ChoosePeopleDialog
import com.queatz.ailaai.ui.dialogs.defaultConfirmFormatter
import com.queatz.ailaai.ui.state.gsonSaver
import com.queatz.ailaai.ui.theme.PaddingDefault

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MessagesScreen(navController: NavController, me: () -> Person?) {
    var isLoading by remember { mutableStateOf(false) }
    var searchText by rememberSaveable { mutableStateOf("") }
    var allGroups by rememberSaveable(stateSaver = gsonSaver<List<GroupExtended>>()) { mutableStateOf(listOf()) }
    var groups by rememberSaveable(stateSaver = gsonSaver<List<GroupExtended>>()) { mutableStateOf(listOf()) }
    var showCreateGroup by remember { mutableStateOf(false) }
    var hasInitialGroups by remember { mutableStateOf(allGroups.isNotEmpty()) }

    // The LaunchedEffect below could have lag and allow isLoading to initially be false
    if (!hasInitialGroups) {
        isLoading = allGroups.isEmpty()
    }

    LaunchedEffect(Unit) {
        if (hasInitialGroups) {
            hasInitialGroups = false

            if (allGroups.isNotEmpty()) {
                return@LaunchedEffect
            }
        }

        isLoading = true
        try {
            allGroups = api.groups()
        } catch (ex: Exception) {
            ex.printStackTrace()
        }
        isLoading = false
    }

    LaunchedEffect(searchText) {
        // todo search server, set allGroups
    }

    LaunchedEffect(allGroups, searchText) {
        groups = if (searchText.isBlank()) allGroups else allGroups.filter {
            (it.group?.name?.contains(searchText, true) ?: false) ||
                    it.members?.any { it.person?.name?.contains(searchText, true) ?: false } ?: false
        }
    }

    Column {
        TopAppBar(
            {
                Text(stringResource(R.string.conversations), maxLines = 1, overflow = TextOverflow.Ellipsis)
            },
            actions = {
                ElevatedButton(
                    {
                        showCreateGroup = true
                    },
                    modifier = Modifier.padding(horizontal = PaddingDefault)
                ) {
                    Icon(
                        Icons.Outlined.Add,
                        stringResource(R.string.new_group),
                        modifier = Modifier.padding(end = PaddingDefault)
                    )
                    Text(stringResource(R.string.new_group))
                }
            }
        )
        Box(contentAlignment = Alignment.BottomCenter, modifier = Modifier.fillMaxSize()) {
            LazyColumn(
                contentPadding = PaddingValues(
                    PaddingDefault,
                    PaddingDefault,
                    PaddingDefault,
                    PaddingDefault + 80.dp
                ),
                verticalArrangement = Arrangement.spacedBy(PaddingDefault, Alignment.Bottom),
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxSize(),
                reverseLayout = true
            ) {
                if (isLoading) {
                    item {
                        LinearProgressIndicator(
                            color = MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = PaddingDefault)
                        )
                    }
                } else if (groups.isEmpty()) {
                    item {
                        Text(
                            stringResource(if (searchText.isBlank()) R.string.you_have_no_conversations else R.string.no_conversations_to_show),
                            color = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.padding(PaddingDefault * 2)
                        )
                    }
                } else {
                    items(groups, key = { it.group!!.id!! }) {
                        ContactItem(navController, it, me())
                    }
                }
            }
            SearchField(
                searchText, { searchText = it }, modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(PaddingDefault * 2)
            )
        }
    }

    if (showCreateGroup) {
        val context = LocalContext.current
        val didntWork = stringResource(R.string.didnt_work)
        val someone = stringResource(R.string.someone)
        ChoosePeopleDialog(
            {
                showCreateGroup = false
            },
            title = stringResource(R.string.new_group),
            confirmFormatter = defaultConfirmFormatter(
                R.string.new_group,
                R.string.new_group_with_person,
                R.string.new_group_with_people,
                R.string.new_group_with_x_people
            ) { it.name ?: someone },
            { people ->
                try {
                    val group = api.createGroup(people.map { it.id!! })
                    navController.navigate("group/${group.id!!}")
                } catch (ex: Exception) {
                    Toast.makeText(context, didntWork, Toast.LENGTH_SHORT).show()
                    ex.printStackTrace()
                }
            },
            me()?.let(::listOf) ?: emptyList()
        )
    }
}
