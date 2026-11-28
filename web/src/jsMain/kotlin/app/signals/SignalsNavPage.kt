package app.signals

import Styles
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import app.components.Spacer
import app.menu.Menu
import app.nav.NavMenu
import app.nav.NavMenuItem
import app.nav.NavSectionHeader
import app.nav.NavTopBar
import app.nav.NavSearchInput
import application
import components.IconButton
import kotlinx.coroutines.launch
import org.jetbrains.compose.web.css.DisplayStyle
import org.jetbrains.compose.web.css.FlexDirection
import org.jetbrains.compose.web.css.display
import org.jetbrains.compose.web.css.flex
import org.jetbrains.compose.web.css.flexDirection
import org.jetbrains.compose.web.css.overflowY
import org.jetbrains.compose.web.css.padding
import org.jetbrains.compose.web.dom.Div
import org.w3c.dom.DOMRect
import org.w3c.dom.HTMLElement
import r

@Composable
fun SignalsNavPage(
    onProfileClick: () -> Unit,
) {
    val me by application.me.collectAsState()
    val activeSignals by signals.activeSignals.collectAsState()
    val personSignals by signals.personSignals.collectAsState()
    val catalog by signals.catalog.collectAsState()
    val navFilter by signals.navFilter.collectAsState()
    val selectedCategory by signals.selectedCategory.collectAsState()
    val searchText by signals.searchText.collectAsState()

    val scope = rememberCoroutineScope()
    var showSearch by remember { mutableStateOf(false) }
    var filterMenuTarget by remember { mutableStateOf<DOMRect?>(null) }

    val categories = remember(catalog) {
        catalog.flatMap { it.categories.orEmpty() }.distinct().sorted()
    }

    LaunchedEffect(Unit) {
        signals.reloadActive()
        signals.reloadMySignals()
        if (catalog.isEmpty()) {
            signals.loadCatalog(reset = true)
        }
    }

    if (filterMenuTarget != null) {
        Menu(
            onDismissRequest = { filterMenuTarget = null },
            target = filterMenuTarget!!,
        ) {
            categories.forEach { category ->
                item(
                    title = category,
                    icon = if (category == selectedCategory) "check" else null,
                ) {
                    signals.setSelectedCategory(if (selectedCategory == category) null else category)
                }
            }
        }
    }

    NavTopBar(
        me = me,
        title = "Signals",
        onProfileClick = onProfileClick,
    ) {
        IconButton(
            name = "search",
            title = application.appString { search },
        ) {
            showSearch = !showSearch
            if (!showSearch) {
                signals.setSearchText("")
            }
        }

        if (categories.isNotEmpty()) {
            IconButton(
                name = "filter_list",
                title = application.appString { filter },
                count = if (selectedCategory != null) 1 else 0,
            ) {
                filterMenuTarget = if (filterMenuTarget == null) {
                    (it.target as HTMLElement).getBoundingClientRect()
                } else {
                    null
                }
            }
        }

        IconButton(
            name = "add",
            title = "Create Signal",
            styles = {
                padding(0.r, .25.r)
            },
        ) {
            scope.launch {
                val created = createSignalDialog(existingCategories = categories)
                if (created != null) {
                    signals.addCreatedSignal(created)
                }
            }
        }
    }

    if (showSearch) {
        NavSearchInput(
            value = searchText,
            onChange = { signals.setSearchText(it) },
            onDismissRequest = {
                signals.setSearchText("")
                showSearch = false
            },
        )
    }

    val myTurnedOnIds = remember(personSignals) {
        personSignals.filter { it.turnedOn == true }.mapNotNull { it.signal }.toSet()
    }
    val myTurnedOnSignals = remember(catalog, myTurnedOnIds) {
        catalog.filter { it.id in myTurnedOnIds }
    }
    val activeCount = activeSignals?.let { it.mine.size + it.others.size } ?: 0

    Div({
        style {
            flex(1)
            display(DisplayStyle.Flex)
            flexDirection(FlexDirection.Column)
            overflowY("auto")
        }
    }) {
        NavMenu {
            NavMenuItem(
                icon = "sensors",
                title = "All Signals",
                selected = navFilter == SignalsNavFilter.All,
            ) {
                signals.setNavFilter(SignalsNavFilter.All)
            }

            NavMenuItem(
                icon = "bolt",
                title = "My Signals",
                description = "${myTurnedOnIds.size} turned on",
                selected = navFilter == SignalsNavFilter.MineOn,
            ) {
                signals.setNavFilter(SignalsNavFilter.MineOn)
            }

            NavMenuItem(
                icon = "wifi_tethering",
                title = "Active Now",
                description = if (activeCount == 1) "1 active signal" else "$activeCount active signals",
                selected = navFilter == SignalsNavFilter.Active,
            ) {
                signals.setNavFilter(SignalsNavFilter.Active)
            }
        }

        val myActive = activeSignals?.mine.orEmpty()
        if (myActive.isNotEmpty()) {
            NavSectionHeader("Your Active Signals")
            NavMenu {
                myActive.forEach { send ->
                    val replyCount = send.replies?.size ?: 0
                    NavMenuItem(
                        icon = send.signal?.emoji ?: "👋",
                        textIcon = true,
                        title = send.signal?.name ?: "Signal",
                        description = if (replyCount == 1) "1 reply" else "$replyCount replies",
                    ) {
                        scope.launch {
                            when (val res = signalRepliesDialog(send)) {
                                is SignalRepliesResult.Cancelled -> signals.reloadActive()
                                is SignalRepliesResult.CreatedGroup -> signals.reloadActive()
                                null -> {}
                            }
                        }
                    }
                }
            }
        }

        if (myTurnedOnSignals.isNotEmpty()) {
            NavSectionHeader("Turned On")
            NavMenu {
                myTurnedOnSignals.forEach { sig ->
                    NavMenuItem(
                        icon = sig.emoji ?: "👋",
                        textIcon = true,
                        title = sig.name.orEmpty(),
                        description = sig.categories?.joinToString(", ").orEmpty(),
                        iconColor = Styles.colors.primary,
                    ) {
                        signals.setNavFilter(SignalsNavFilter.MineOn)
                    }
                }
            }
        }

        Spacer()
    }
}
