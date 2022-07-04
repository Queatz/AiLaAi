package com.queatz.ailaai

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.zIndex
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import at.bluesource.choicesdk.location.factory.FusedLocationProviderFactory
import at.bluesource.choicesdk.maps.common.LatLng
import com.google.accompanist.permissions.*
import com.queatz.ailaai.ui.components.BasicCard
import com.queatz.ailaai.ui.components.ContactItem
import com.queatz.ailaai.ui.components.MessageItem
import com.queatz.ailaai.ui.theme.AiLaAiTheme
import com.queatz.ailaai.ui.theme.ElevationDefault
import com.queatz.ailaai.ui.theme.PaddingDefault
import io.ktor.client.plugins.*
import kotlinx.coroutines.launch


@OptIn(ExperimentalMaterial3Api::class)
class MainActivity : AppCompatActivity() {

    private lateinit var navController: NavHostController

    @OptIn(ExperimentalComposeUiApi::class, ExperimentalPermissionsApi::class)
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

                var known by remember { mutableStateOf(api.hasToken()) }

                if (!known) {
                    var codeValue by remember { mutableStateOf("") }
                    var codeValueEnabled by remember { mutableStateOf(true) }
                    val coroutineScope = rememberCoroutineScope()

                    fun signUp(code: String) {
                        coroutineScope.launch {
                            try {
                                val token = api.signUp(code).token
                                api.setToken(token)
                                known = true
                            } catch (ex: Exception) {
                                if (ex !is ResponseException) {
                                    ex.printStackTrace()
                                }
                            } finally {
                                codeValueEnabled = true
                            }
                        }
                    }

                    Column(
                        verticalArrangement = Arrangement.spacedBy(PaddingDefault * 4, Alignment.CenterVertically),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(
                                PaddingValues(
                                    top = PaddingDefault,
                                    start = PaddingDefault,
                                    end = PaddingDefault,
                                    bottom = PaddingDefault * 8
                                )
                            )
                    ) {
                        Text("Xin chào, bạn!", style = MaterialTheme.typography.headlineMedium)
                        OutlinedTextField(
                            codeValue,
                            onValueChange = {
                                codeValue = it.take(6)

                                if (it.length == 6) {
                                    codeValueEnabled = false
                                    signUp(it)
                                }
                            },
                            enabled = codeValueEnabled,
                            label = { Text("Enter invite code") },
                            shape = MaterialTheme.shapes.large,
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Number,
                                imeAction = ImeAction.Done
                            ),
                            keyboardActions = KeyboardActions(onDone = {
                                codeValueEnabled = false
                                signUp(codeValue)
                            }),
                        )
                    }
                } else Scaffold(
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
                            val locationClient = FusedLocationProviderFactory.getFusedLocationProviderClient(
                                navController.context as Activity
                            )
                            var value by remember { mutableStateOf("") }
                            var geo: LatLng? by remember { mutableStateOf(null) }
                            var isLoading by remember { mutableStateOf(true) }
                            var cards by remember { mutableStateOf(listOf<Card>()) }
                            val permissionState = rememberPermissionState(Manifest.permission.ACCESS_FINE_LOCATION)

                            if (!permissionState.status.isGranted) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(PaddingDefault * 2, Alignment.CenterVertically),
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(PaddingDefault)
                                ) {
                                    Button(
                                        {
                                        if (permissionState.status.shouldShowRationale) {
                                            permissionState.launchPermissionRequest()
                                        } else {
                                            val intent = Intent(
                                                Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                                Uri.parse("package:${navController.context.packageName}")
                                            )
                                            (navController.context as Activity).startActivity(intent)
                                        }
                                    }
                                    ) {
                                        Text(if (permissionState.status.shouldShowRationale) "Find my location" else "Open Settings")
                                    }

                                    if (permissionState.status.shouldShowRationale.not()) {
                                        Text(
                                            "The location permission is disabled in settings.",
                                            textAlign = TextAlign.Center,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .align(Alignment.CenterHorizontally)
                                        )
                                    }
                                }
                            } else if (geo == null) {
                                locationClient.getLastLocation()
                                    .addOnFailureListener(navController.context as Activity) {
                                        it.printStackTrace()
                                    }
                                    .addOnSuccessListener {
                                        if (it != null) {
                                            geo = LatLng(it.latitude, it.longitude)
                                        }
                                    }
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(PaddingDefault)
                                ) {
                                    Text("Finding your location...", color = MaterialTheme.colorScheme.secondary)
                                }
                            } else {
                                LaunchedEffect(value) {
                                    isLoading = true
                                    try {
                                        cards = api.cards(geo!!, value.takeIf { it.isNotBlank() })
                                    } catch (ex: Exception) {
                                        ex.printStackTrace()
                                    }
                                    isLoading = false
                                }

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
                                        if (isLoading) {
                                            item {
                                                LinearProgressIndicator(
                                                    color = MaterialTheme.colorScheme.tertiary,
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(horizontal = PaddingDefault)
                                                )
                                            }
                                        }
                                        items(cards) {
                                            BasicCard(
                                                {
                                                    navController.navigate("messages/${it.person}")
                                                },
                                                activity = navController.context as Activity,
                                                card = it
                                            )
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
                                            keyboardOptions = KeyboardOptions(
                                                capitalization = KeyboardCapitalization.Words,
                                                imeAction = ImeAction.Search
                                            ),
                                            keyboardActions = KeyboardActions(onSearch = {
                                                keyboardController.hide()
                                            }),
                                            modifier = Modifier.fillMaxWidth()
                                                .background(MaterialTheme.colorScheme.surface)
                                        )
                                    }
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
                            var myCards by remember { mutableStateOf(listOf<Card>()) }
                            var inviteDialog by remember { mutableStateOf(false) }
                            var isLoading by remember { mutableStateOf(true) }
                            val coroutineScope = rememberCoroutineScope()
                            var inviteCode by remember { mutableStateOf("") }

                            LaunchedEffect(inviteDialog) {
                                if (inviteDialog) {
                                    inviteCode = ""

                                    try {
                                        inviteCode = api.invite().code ?: ""
                                    } catch (ex: Exception) {
                                        ex.printStackTrace()
                                        inviteCode = "Error"
                                    }
                                }
                            }

                            if (inviteDialog) {
                                AlertDialog(
                                    {
                                        inviteDialog = false
                                    },
                                    {
                                        TextButton(
                                            {
                                                inviteDialog = false
                                            }
                                        ) {
                                            Text("Close")
                                        }
                                    },
                                    properties = DialogProperties(usePlatformDefaultWidth = false),
                                    title = { Text("Invite code") },
                                    text = {
                                        Column(
                                            verticalArrangement = Arrangement.spacedBy(PaddingDefault)
                                        ) {
                                            if (inviteCode.isBlank()) {
                                                CircularProgressIndicator()
                                            } else {
                                                Text(inviteCode, style = MaterialTheme.typography.displayMedium)
                                                Text(
                                                    "This code will be active for 48 hours.",
                                                    color = MaterialTheme.colorScheme.secondary
                                                )
                                            }
                                        }
                                    }
                                )
                            }

                            LaunchedEffect(true) {
                                myCards = api.myCards()
                                isLoading = false
                            }

                            Column {
                                SmallTopAppBar(
                                    {
                                        Text("My cards")
                                    },
                                    actions = {
                                        ElevatedButton(
                                            {
                                                inviteDialog = true
                                            },
                                            enabled = !inviteDialog
                                        ) {
                                            Icon(
                                                Icons.Outlined.Add,
                                                "Invite",
                                                modifier = Modifier.padding(end = PaddingDefault)
                                            )
                                            Text("Invite")
                                        }
                                        IconButton({
                                            navController.navigate("settings")
                                        }) {
                                            Icon(Icons.Outlined.Settings, Icons.Outlined.Settings.name)
                                        }
                                    }
                                )
                                LazyColumn(
                                    contentPadding = PaddingValues(PaddingDefault),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(PaddingDefault, Alignment.Bottom),
                                    modifier = Modifier.fillMaxWidth().weight(1f)
                                ) {
                                    items(myCards, key = { it.id!! }) {
                                        BasicCard({
                                            // todo upload a new image
                                        }, {
                                            coroutineScope.launch {
                                                try {
                                                    myCards = api.myCards()
                                                } catch (ex: Exception) {
                                                    ex.printStackTrace()
                                                }
                                            }
                                        }, navController.context as Activity, it, true)
                                    }

                                    if (myCards.isEmpty()) {
                                        if (isLoading) {
                                            item {
                                                LinearProgressIndicator(
                                                    color = MaterialTheme.colorScheme.tertiary,
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(horizontal = PaddingDefault)
                                                )
                                            }
                                        } else {
                                            item {
                                                Text(
                                                    "You currently have no cards.",
                                                    color = MaterialTheme.colorScheme.secondary,
                                                    modifier = Modifier.padding(PaddingDefault * 2)
                                                )
                                            }
                                        }
                                    }

                                    item {
                                        ElevatedButton(
                                            {
                                                coroutineScope.launch {
                                                    api.newCard()
                                                    myCards = api.myCards()
                                                }
                                            }
                                        ) {
                                            Text("Add a card")
                                        }
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
                                        Text(
                                            "Language",
                                            style = MaterialTheme.typography.titleMedium.copy(lineHeight = 2.5.em)
                                        )
                                        Text(
                                            "English",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.secondary
                                        )
                                    }
                                }, {})
                                DropdownMenuItem({
                                    Text(
                                        "Sign out",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.padding(PaddingDefault)
                                    )
                                }, {})
                            }
                        }
                    }
                }
            }
        }
    }
}

