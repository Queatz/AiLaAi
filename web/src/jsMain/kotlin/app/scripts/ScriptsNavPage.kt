package app.scripts

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import api
import app.ailaai.api.myScripts
import app.nav.NavMenu
import app.nav.NavMenuItem
import app.nav.NavTopBar
import appString
import application
import bulletedString
import com.queatz.db.Script
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.collectLatest

sealed class ScriptsNav {
    data object None : ScriptsNav()
    data class Script(val script: com.queatz.db.Script) : ScriptsNav()
}

@Composable
fun ScriptsNavPage(
    updates: MutableSharedFlow<Script>,
    selected: ScriptsNav,
    onSelected: (ScriptsNav) -> Unit,
    onProfileClick: () -> Unit
) {
    var scripts by remember { mutableStateOf<List<Script>>(emptyList()) }

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

    NavTopBar(me, appString { platform }, onProfileClick = onProfileClick)

    NavMenu {
        scripts.forEach { script ->
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
