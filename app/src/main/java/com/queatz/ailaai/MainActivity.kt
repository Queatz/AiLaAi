package com.queatz.ailaai

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.zIndex
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.queatz.ailaai.ui.components.BasicCard
import com.queatz.ailaai.ui.components.ContactItem
import com.queatz.ailaai.ui.components.MessageItem
import com.queatz.ailaai.ui.theme.AiLaAiTheme
import com.queatz.ailaai.ui.theme.ElevationDefault
import com.queatz.ailaai.ui.theme.PaddingDefault

@OptIn(ExperimentalMaterial3Api::class)
class MainActivity : AppCompatActivity() {

    lateinit var navController: NavHostController

    @OptIn(ExperimentalComposeUiApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AiLaAiTheme {
                navController = rememberNavController()

                val showBottomBar = navController.currentBackStackEntryAsState()
                    .value
                    ?.destination
                    ?.route
                    ?.startsWith("messages/") != true

                Surface(color = MaterialTheme.colorScheme.background, modifier = Modifier.fillMaxSize()) {
                    Scaffold(
                        bottomBar = {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Bottom
                            ) {
                                AnimatedVisibility(showBottomBar) {
                                    NavigationBar {
                                        listOf(
                                            "Explore" to Icons.Outlined.Search,
                                            "Messages" to Icons.Outlined.Email,
                                            "Me" to Icons.Outlined.Person
                                        ).forEachIndexed { index, item ->
                                            NavigationBarItem(
                                                icon = { Icon(item.second, contentDescription = null) },
                                                label = { Text(item.first) },
                                                selected = navController.currentDestination?.route == item.first.lowercase(),
                                                onClick = {
                                                    navController.popBackStack()
                                                    navController.navigate(item.first.lowercase())
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        },
                    ) {
                        NavHost(navController, "explore", modifier = Modifier.padding(it).fillMaxSize()) {
                            composable("explore") {
                                var value by remember { mutableStateOf("") }

                                Box {
                                    LazyColumn(
                                        contentPadding = PaddingValues(
                                            PaddingDefault,
                                            PaddingDefault,
                                            PaddingDefault,
                                            PaddingDefault + 80.dp
                                        ),
                                        verticalArrangement = Arrangement.spacedBy(PaddingDefault, Alignment.Bottom),
                                        modifier = Modifier.fillMaxSize()
                                    ) {
                                        items(
                                            listOf(
                                                "Nate Ferrero" to "Detroit, MI",
                                                "Jacob Ferrero" to "Saigon, VN",
                                                "Aaron Dubois" to "Waco, TX",
                                                "Mai Ferrero" to "Saigon, VN",
                                                "Bun Seng" to "Dallas, TX",
                                                "Tracy Huynh" to "Dallas, TX"
                                            )
                                        ) {
                                            BasicCard(navController, it)
                                        }
                                    }
                                    Card(
                                        shape = MaterialTheme.shapes.large,
                                        colors = CardDefaults.elevatedCardColors(),
                                        elevation = CardDefaults.elevatedCardElevation(ElevationDefault / 2),
                                        modifier = Modifier
                                            .align(Alignment.BottomCenter)
                                            .padding(PaddingDefault * 2)
                                            .fillMaxWidth()
                                    ) {
                                        val keyboardController = LocalSoftwareKeyboardController.current!!
                                        OutlinedTextField(
                                            value,
                                            onValueChange = { value = it },
                                            placeholder = { Text("Search") },
                                            shape = MaterialTheme.shapes.large,
                                            singleLine = true,
                                            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Search),
                                            keyboardActions = KeyboardActions(onSearch = {
                                                keyboardController.hide()
                                            }),
                                            modifier = Modifier.fillMaxWidth()
                                                .background(MaterialTheme.colorScheme.surface)
                                        )
                                    }
                                }
                            }
                            composable("messages") {
                                LazyColumn(
                                    contentPadding = PaddingValues(PaddingDefault),
                                    verticalArrangement = Arrangement.spacedBy(PaddingDefault, Alignment.Bottom),
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    items(
                                        listOf(
                                            "Nate Ferrero" to "Where would we want to be",
                                            "Aaron Dubois" to "Bạn: Waco, for sure",
                                            "Mai Ferrero" to "Matchaa",
                                            "Bun Seng" to "Ok plan set"
                                        )
                                    ) {
                                        ContactItem(navController, it)
                                    }
                                }
                            }
                            composable("messages/{id}") {
                                var sendMessage by remember { mutableStateOf("") }

                                Column {
                                    SmallTopAppBar(
                                        {
                                            Column {
                                                Text("Nate Ferrero")
                                                Text(
                                                    "Detroit, MI, USA",
                                                    style = MaterialTheme.typography.labelMedium,
                                                    color = MaterialTheme.colorScheme.secondary
                                                )
                                            }
                                        },
                                        navigationIcon = {
                                            IconButton({
                                                navController.popBackStack()
                                            }) {
                                                Icon(Icons.Outlined.ArrowBack, Icons.Outlined.ArrowBack.name)
                                            }
                                        },
                                        actions = {
                                            var showMenu by remember { mutableStateOf(false) }

                                            IconButton({
                                                navController.popBackStack()
                                            }) {
                                                Icon(Icons.Outlined.AccountCircle, "View card")
                                            }

                                            IconButton({
                                                showMenu = !showMenu
                                            }) {
                                                Icon(Icons.Outlined.MoreVert, "More")
                                            }

                                            DropdownMenu(showMenu, { showMenu = false }) {
                                                DropdownMenuItem({ Text("Delete") }, { showMenu = false })
                                                DropdownMenuItem({ Text("Report") }, { showMenu = false })
                                            }
                                        },
                                        colors = TopAppBarDefaults.smallTopAppBarColors(
                                            containerColor = MaterialTheme.colorScheme.primaryContainer
                                        ),
                                        modifier = Modifier.shadow(ElevationDefault / 2).zIndex(1f)
                                    )
                                    LazyColumn(reverseLayout = true, modifier = Modifier.weight(1f)) {
                                        items(22) {
                                            MessageItem()
                                        }
                                    }
                                    OutlinedTextField(
                                        value = sendMessage,
                                        onValueChange = {
                                            sendMessage = it
                                        },
                                        trailingIcon = {
                                            Crossfade(targetState = sendMessage.isNotBlank()) { show ->
                                                when (show) {
                                                    true -> IconButton({
                                                        sendMessage = ""
                                                    }) {
                                                        Icon(
                                                            Icons.Default.Send,
                                                            Icons.Default.Send.name,
                                                            tint = MaterialTheme.colorScheme.primary
                                                        )
                                                    }
                                                    false -> {}
                                                }
                                            }
                                        },
                                        placeholder = {
                                            Text("Message")
                                        },
                                        keyboardOptions = KeyboardOptions(
                                            capitalization = KeyboardCapitalization.Sentences,
                                            imeAction = ImeAction.Send
                                        ),
                                        keyboardActions = KeyboardActions(onSend = {
                                            sendMessage = ""
                                        }),
                                        shape = MaterialTheme.shapes.large, modifier = Modifier
                                            .fillMaxWidth()
                                            .heightIn(max = 128.dp)
                                            .padding(PaddingDefault)
                                    )
                                }
                            }
                            composable("me") {
                                Column {
                                    SmallTopAppBar(
                                        {
                                            Text("My cards")
                                        },
                                        actions = {
                                            IconButton({
                                                navController.navigate("settings")
                                            }) {
                                                Icon(Icons.Outlined.Settings, Icons.Outlined.Settings.name)
                                            }
                                        }
                                    )
                                    LazyColumn(
                                        contentPadding = PaddingValues(PaddingDefault),
                                        verticalArrangement = Arrangement.spacedBy(PaddingDefault, Alignment.Bottom),
                                        modifier = Modifier.fillMaxWidth().weight(1f)
                                    ) {
                                        items(
                                            listOf(
                                                "Jacob Ferrero" to "Austin, TX",
                                                "Jacob Ferrero" to "Saigon, VN"
                                            )
                                        ) {
                                            BasicCard(navController, it)
                                        }
                                    }
                                }
                            }
                            composable("settings") {
                                Column {
                                    SmallTopAppBar(
                                        {
                                            Text("Settings")
                                        },
                                        navigationIcon = {
                                            IconButton({
                                                navController.popBackStack()
                                            }) {
                                                Icon(Icons.Outlined.ArrowBack, Icons.Outlined.ArrowBack.name)
                                            }
                                        }
                                    )
                                    DropdownMenuItem({
                                        Column(modifier = Modifier.padding(PaddingDefault)) {
                                            Text("Language", style = MaterialTheme.typography.titleMedium.copy(lineHeight = 2.5.em))
                                            Text(
                                                "English",
                                                style = MaterialTheme.typography.labelMedium,
                                                color = MaterialTheme.colorScheme.secondary
                                            )
                                        }
                                    }, {})
                                    DropdownMenuItem({
                                        Text("Sign out", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(PaddingDefault))
                                    }, {})
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

