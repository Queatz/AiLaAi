package app.scripts

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import api
import app.ailaai.api.createScript
import app.ailaai.api.myScripts
import app.components.Spacer
import app.dialog.inputDialog
import app.menu.Menu
import app.nav.NavMenu
import app.nav.NavMenuItem
import app.nav.NavSearchInput
import app.nav.NavTopBar
import appString
import application
import bulletedString
import com.queatz.db.Script
import components.IconButton
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.jetbrains.compose.web.css.marginRight
import org.w3c.dom.DOMRect
import org.w3c.dom.HTMLElement
import r
import sortedDistinct

sealed class ScriptsNav {
    data object None : ScriptsNav()
    data class Script(val script: com.queatz.db.Script) : ScriptsNav()
    data object Explore: ScriptsNav()
}

@Composable
fun ScriptsNavPage(
    updates: MutableSharedFlow<Script>,
    selected: ScriptsNav,
    onSelected: (ScriptsNav) -> Unit,
    onCreated: (Script) -> Unit,
    onProfileClick: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    var scripts by remember { mutableStateOf<List<Script>>(emptyList()) }
    var showSearch by remember { mutableStateOf(false) }
    var searchQuery by remember(showSearch) { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf<String?>(null) }

    val categories by remember(scripts) {
        mutableStateOf(scripts.mapNotNull { it.categories }.flatten().sortedDistinct())
    }

    var filterMenuTarget by remember {
        mutableStateOf<DOMRect?>(null)
    }

    LaunchedEffect(Unit) {
        api.myScripts {
            scripts = it
        }

        updates.collectLatest {
            api.myScripts {
                scripts = it
            }
        }
    }

    LaunchedEffect(scripts) {
        (selected as? ScriptsNav.Script)?.script?.id?.let { scriptId ->
            scripts.firstOrNull { it.id == scriptId }?.let { script ->
                onSelected(ScriptsNav.Script(script))
            }
        }
    }

    val me by application.me.collectAsState()

    val title = appString { title }
    val create = appString { create }

    if (filterMenuTarget != null) {
        Menu(
            onDismissRequest = { filterMenuTarget = null },
            target = filterMenuTarget!!
        ) {
            categories.forEach { category ->
                item(category, icon = if (category == selectedCategory) "check" else null) {
                    selectedCategory = if (category == selectedCategory) {
                        null
                    } else {
                        category
                    }
                }
            }
        }
    }

    NavTopBar(me, appString { this.scripts }, onProfileClick = onProfileClick) {
        IconButton(
            name = "search",
            title = appString { this.search },
        ) {
            showSearch = !showSearch
        }
        IconButton(
            name = "filter_list",
            title = appString { this.filter },
            count = selectedCategory?.let { 1 } ?: 0,
        ) {
            filterMenuTarget = if (filterMenuTarget == null) {
                (it.target as HTMLElement).getBoundingClientRect()
            } else {
                null
            }
        }
        IconButton(
            name = "add",
            // todo: create a script
            title = "Create a script",
            styles = {
                marginRight(.5.r)
            }
        ) {
            scope.launch {
                val result = inputDialog(
                    title = application.appString { newScript },
                    placeholder = title,
                    confirmButton = create
                )

                if (result == null) return@launch

                api.createScript(Script(name = result)) {
                    onCreated(it)
                    onSelected(ScriptsNav.Script(it))
                }
            }
        }
    }

    if (showSearch) {
        NavSearchInput(searchQuery, { searchQuery = it }, onDismissRequest = {
            searchQuery = ""
            showSearch = false
        })
    }

    NavMenu {
        NavMenuItem(
            icon = "explore",
            title = appString { explore },
            selected = selected is ScriptsNav.Explore
        ) {
            onSelected(ScriptsNav.Explore)
        }

        Spacer()

        scripts.filter {
            (searchQuery.isBlank() || it.name.orEmpty().contains(searchQuery, ignoreCase = true)) &&
                    (selectedCategory.isNullOrBlank() || it.categories.orEmpty().contains(selectedCategory))
        }.forEach { script ->
            NavMenuItem(
                icon = null,
                title = script.name.orEmpty(),
                description = bulletedString(
                    script.categories?.firstOrNull(),
                    script.description
                ),
                selected = (selected as? ScriptsNav.Script)?.script?.id == script.id
            ) {
                onSelected(ScriptsNav.Script(script = script))
            }
        }
    }
}
