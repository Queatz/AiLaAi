package com.queatz.ailaai.ui.scripts

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.PersonSearch
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import app.ailaai.api.createScript
import app.ailaai.api.updateScript
import com.queatz.ailaai.R
import com.queatz.ailaai.data.api
import com.queatz.ailaai.extensions.bulletedString
import com.queatz.ailaai.extensions.inList
import com.queatz.ailaai.extensions.notBlank
import com.queatz.ailaai.extensions.rememberStateOf
import com.queatz.ailaai.me
import com.queatz.ailaai.ui.components.Toolbar
import com.queatz.ailaai.ui.components.DialogBase
import com.queatz.ailaai.ui.components.DialogLayout
import com.queatz.ailaai.ui.components.Dropdown
import com.queatz.ailaai.ui.dialogs.Alert
import com.queatz.ailaai.ui.dialogs.ChooseCategoryDialog
import com.queatz.ailaai.ui.dialogs.TextFieldDialog
import com.queatz.ailaai.ui.theme.pad
import com.queatz.db.Script
import kotlinx.coroutines.launch

private sealed class ScriptsDialogState {
    data object Select : ScriptsDialogState()
    data object Templates : ScriptsDialogState()
    data class EditScript(val id: String? = null) : ScriptsDialogState()
    data class Search(val onlyMine: Boolean) : ScriptsDialogState()
    data class Preview(val script: Script, val fromOnlyMine: Boolean) : ScriptsDialogState()
    data class AddData(val script: Script) : ScriptsDialogState()
}

private val templates by lazy {
    listOf(
        Script(
            name = "Call a JSON API",
            source = """
            @Serializable
            data class Stats(val activePeople24Hours: Int)
            
            val stats: Stats = http("https://api.ailaai.app/stats")
            
            render {
                text("There are currently ${"$"}{stats.activePeople24Hours} active people")
            }
        """.trimIndent()
        ),
        Script(
            name = "POST JSON to an API",
            source = """
            @Serializable
            data class Request(val channel: String)
            
            val ideas = http.post<List<String>, Request>("https://tagme.in/generate", Request("ideas"))
            
            render {
                section("Here are some ideas:")
            
                ideas.forEachIndexed { index, idea ->
                    text("${"$"}{index + 1}. ${"$"}idea")
                }
            }
        """.trimIndent()
        ),
        Script(
            name = "Import a library",
            source = """
            @file:Repository("https://jitpack.io")
            @file:DependsOn("com.github.cosinekitty:astronomy:v2.1.19")
            
            import io.github.cosinekitty.astronomy.*
            import java.lang.System.*
            import kotlin.math.*
            
            val now = currentTimeMillis()
            
            val percent = illumination(
                Body.Moon,
                Time.fromMillisecondsSince1970(now)
            ).phaseFraction.times(100f).roundToInt()
            
            val emoji = when {
                percent < 20 -> "🌑"
                percent < 40 -> "🌒"
                percent < 60 -> "🌓"
                percent < 80 -> "🌔"
                else -> "🌕️"
            }
            
            render {
                text("${"$"}emoji The moon is currently ${"$"}percent% illuminated")
            }
        """.trimIndent()
        ),
        Script(
            name = "Counting button",
            source = """
            val count = data?.toIntOrNull() ?: 0
            
            render {
                text("Count: ${"$"}count")
                button("+1", script = self, data = "${"$"}{count + 1}")
            }
        """.trimIndent()
        ),
        Script(
            name = "Roll a dice",
            source = """
            import kotlin.random.*
            
            render {
                text("${"$"}{Random.nextInt(6) + 1}")
                button("Roll again", self)
            }
        """.trimIndent()
        ),
        Script(
            name = "Text adventure",
            source = """
            data class Location(
                val name: String,
                val details: String,
                val destinations: List<String>,
            )
            
            val locations = listOf(
                Location("Beach", "The beach is peaceful and quiet. You see a cave in the distance. Tall reeds cling around the towering stones.", listOf("Cave", "Reeds")),
                Location("Cave", "A small wooden boat floats along the interior of the cave.", listOf("Boat", "Beach")),
                Location("Boat", "You lie on the boat face down and row with your hands. Do you continue or turn around?", listOf("Island", "Cave")),
                Location("Island", "There is nothing of interest on the small sandy island.", listOf("Boat")),
                Location("Reeds", "Hooray! You found the treasure!", listOf("Beach"))
            )
            
            val location = locations.firstOrNull { it.name == data } ?: locations.first()
            
            render {
                section("You are at the ${"$"}{location.name}")
                text(location.details)
                location.destinations.forEach { destination ->
                    button("Go to the ${"$"}destination", script = self, data = destination)
                }
            }
        """.trimIndent()
        ),
    )
}


enum class NewScriptDecision {
    AddData,
    None
}

enum class PreviewScriptAction {
    AddData,
    Edit,
    None
}

@Composable
fun ScriptsDialog(
    onDismissRequest: () -> Unit,
    previewScriptAction: PreviewScriptAction = PreviewScriptAction.None,
    onNewScript: suspend (Script) -> NewScriptDecision = { NewScriptDecision.None },
    onScriptWithData: suspend (script: Script, data: String) -> Unit = { _, _ -> },
) {
    val me = me
    val scope = rememberCoroutineScope()
    var state by rememberStateOf<ScriptsDialogState>(
        ScriptsDialogState.Select
    )
    var isLoading by rememberStateOf(false)
    var scriptName by rememberStateOf("")
    var scriptSource by rememberStateOf(
        """
            render {
            
            }
        """.trimIndent()
    )
    var scriptData by rememberStateOf("")
    var showHelp by rememberStateOf(false)
    val isMine = (state as? ScriptsDialogState.Preview)?.script?.person == me?.id

    fun createScript(name: String, source: String) {
        isLoading = true
        scope.launch {
            api.createScript(Script(name = name, source = source)) {
                when (onNewScript(it)) {
                    NewScriptDecision.AddData -> {
                        state = ScriptsDialogState.AddData(it)
                    }

                    else -> {
                        state = ScriptsDialogState.Select
                    }
                }
            }
            isLoading = false
        }
    }

    fun updateScript(scriptId: String, name: String, source: String) {
        isLoading = true
        scope.launch {
            api.updateScript(scriptId, Script(name = name, source = source)) {
                // Script updated
            }
            isLoading = false
        }
    }

    DialogBase(onDismissRequest) {
        DialogLayout(
            scrollable = false,
            content = {
                when (state) {
                    is ScriptsDialogState.Select -> {
                        Toolbar {
                            item(
                                Icons.Outlined.Add,
                                stringResource(R.string.new_script)
                            ) {
                                state = ScriptsDialogState.EditScript()
                            }
                            item(
                                Icons.Outlined.Description,
                                stringResource(R.string.templates)
                            ) {
                                state = ScriptsDialogState.Templates
                            }
                            item(
                                Icons.Outlined.PersonSearch,
                                stringResource(R.string.my_scripts)
                            ) {
                                state = ScriptsDialogState.Search(onlyMine = true)
                            }
                            item(
                                Icons.Outlined.Search,
                                stringResource(R.string.search)
                            ) {
                                state = ScriptsDialogState.Search(onlyMine = false)
                            }
                        }
                    }

                    is ScriptsDialogState.EditScript -> {
                        val focusRequester = remember { FocusRequester() }

                        LaunchedEffect(Unit) {
                            focusRequester.requestFocus()
                        }

                        OutlinedTextField(
                            scriptName,
                            onValueChange = { scriptName = it },
                            label = { Text(stringResource(R.string.script_name)) },
                            shape = MaterialTheme.shapes.large,
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                capitalization = KeyboardCapitalization.Sentences
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 1.pad)
                                .focusRequester(focusRequester)
                        )

                        OutlinedTextField(
                            value = scriptSource,
                            onValueChange = { scriptSource = it },
                            label = { Text(stringResource(R.string.script_source)) },
                            shape = MaterialTheme.shapes.large,
                            singleLine = false,
                            keyboardOptions = KeyboardOptions.Default.copy(
                                autoCorrectEnabled = false,
                                keyboardType = KeyboardType.Ascii,
                                capitalization = KeyboardCapitalization.None
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f, fill = false)
                                .padding(bottom = 1.pad)
                                .heightIn(min = 48.dp)
                        )
                    }

                    is ScriptsDialogState.Search -> {
                        SearchScriptsLayout(
                            onlyMine = (state as ScriptsDialogState.Search).onlyMine,
                            onScript = {
                                state = ScriptsDialogState.Preview(
                                    script = it,
                                    fromOnlyMine = (state as ScriptsDialogState.Search).onlyMine
                                )
                            }
                        )
                    }

                    is ScriptsDialogState.Templates -> {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(1.pad),
                            modifier = Modifier
                                .weight(1f, fill = false)
                        ) {
                            items(templates) {
                                Text(
                                    it.name?.notBlank ?: stringResource(R.string.new_script),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(MaterialTheme.shapes.large)
                                        .clickable {
                                            scriptName = it.name ?: ""
                                            scriptSource = it.source ?: ""
                                            state = ScriptsDialogState.EditScript()
                                        }
                                        .padding(1.pad)
                                )
                            }
                        }
                    }

                    is ScriptsDialogState.AddData -> {
                        OutlinedTextField(
                            value = scriptData,
                            onValueChange = { scriptData = it },
                            label = { Text(stringResource(R.string.script_data)) },
                            shape = MaterialTheme.shapes.large,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 1.pad)
                                .heightIn(min = 48.dp)
                        )
                    }

                    is ScriptsDialogState.Preview -> {
                        val script = (state as ScriptsDialogState.Preview).script
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .verticalScroll(rememberScrollState())
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier
                                        .padding(bottom = 1.pad)
                                ) {
                                    Text(
                                        text = script.name?.notBlank ?: stringResource(R.string.new_script),
                                        style = MaterialTheme.typography.titleLarge
                                    )
                                    val category = script.categories?.firstOrNull()
                                    if (!script.description.isNullOrBlank() || category != null) {
                                        Text(
                                            text = bulletedString(category, script.description),
                                            maxLines = 3,
                                            overflow = TextOverflow.Ellipsis,
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.secondary
                                        )
                                    }
                                }
                                if (isMine) {
                                    var showMenu by rememberStateOf(false)
                                    var showSetCategory by rememberStateOf(false)
                                    var showDescription by rememberStateOf(false)
                                    val fromOnlyMine = (state as ScriptsDialogState.Preview).fromOnlyMine

                                    if (showDescription) {
                                        TextFieldDialog(
                                            onDismissRequest = {
                                                showDescription = false
                                            },
                                            title = stringResource(R.string.description),
                                            initialValue = script.description.orEmpty(),
                                            button = stringResource(R.string.update),
                                            showDismiss = true,
                                            dismissButtonText = stringResource(R.string.cancel)
                                        ) { description ->
                                            scope.launch {
                                                api.updateScript(script.id!!, Script(description = description)) {
                                                    showDescription = false
                                                    state = ScriptsDialogState.Preview(it, fromOnlyMine = fromOnlyMine)
                                                }
                                            }
                                        }
                                    }

                                    if (showSetCategory) {
                                        ChooseCategoryDialog(
                                            onDismissRequest = {
                                                showSetCategory = false
                                            },
                                            preselect = script.categories?.firstOrNull(),
                                        ) {
                                            scope.launch {
                                                api.updateScript(script.id!!, Script(categories = it?.inList())) {
                                                    state = ScriptsDialogState.Preview(it, fromOnlyMine = fromOnlyMine)
                                                }
                                            }
                                        }
                                    }

                                    IconButton(
                                        onClick = {
                                            showMenu = true
                                        },
                                    ) {
                                        Icon(Icons.Outlined.MoreVert, null)

                                        Dropdown(
                                            expanded = showMenu,
                                            onDismissRequest = { showMenu = false }
                                        ) {
                                            DropdownMenuItem(
                                                text = {
                                                    Text(stringResource(R.string.description))
                                                },
                                                onClick = {
                                                    showMenu = false
                                                    showDescription = true
                                                }
                                            )
                                            DropdownMenuItem(
                                                text = {
                                                    Text(stringResource(R.string.set_category))
                                                },
                                                onClick = {
                                                    showMenu = false
                                                    showSetCategory = true
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                            SelectionContainer {
                                Text(script.source ?: "")
                            }
                        }
                    }
                }
            },
            actions = {
                if (state is ScriptsDialogState.EditScript) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                    ) {
                        IconButton(
                            {
                                showHelp = true
                            }
                        ) {
                            Icon(Icons.AutoMirrored.Outlined.HelpOutline, null)
                        }
                    }
                }
                when (state) {
                    is ScriptsDialogState.Preview -> {
                        TextButton(
                            {
                                state = ScriptsDialogState.Search((state as ScriptsDialogState.Preview).fromOnlyMine)
                            }
                        ) {
                            Text(stringResource(R.string.go_back))
                        }
                    }

                    is ScriptsDialogState.Templates -> {
                        TextButton(
                            {
                                state = ScriptsDialogState.Select
                            }
                        ) {
                            Text(stringResource(R.string.go_back))
                        }
                    }

                    is ScriptsDialogState.Search -> {
                        TextButton(
                            {
                                state = ScriptsDialogState.Select
                            }
                        ) {
                            Text(stringResource(R.string.go_back))
                        }
                    }

                    else -> {
                        TextButton(
                            {
                                onDismissRequest()
                            }
                        ) {
                            Text(stringResource(R.string.close))
                        }
                    }
                }
                when (state) {
                    is ScriptsDialogState.EditScript -> {
                        val scriptId = (state as ScriptsDialogState.EditScript).id
                        TextButton(
                            {
                                if (scriptId == null) {
                                    createScript(
                                        name = scriptName,
                                        source = scriptSource
                                    )
                                } else {
                                    updateScript(
                                        scriptId = scriptId,
                                        name = scriptName,
                                        source = scriptSource
                                    )
                                }
                            },
                            enabled = !isLoading
                        ) {
                            Text(
                                when (scriptId) {
                                    null -> stringResource(R.string.create_script)
                                    else -> stringResource(R.string.save)
                                }
                            )
                        }
                    }

                    is ScriptsDialogState.AddData -> {
                        TextButton(
                            onClick = {
                                isLoading = true
                                scope.launch {
                                    onScriptWithData(
                                        (state as ScriptsDialogState.AddData).script,
                                        scriptData
                                    )
                                    isLoading = false
                                }
                            },
                            enabled = !isLoading
                        ) {
                            Text(stringResource(R.string.choose_this_script))
                        }
                    }

                    is ScriptsDialogState.Preview -> {
                        TextButton(
                            {
                                scriptName = (state as ScriptsDialogState.Preview).script.name ?: ""
                                scriptSource = (state as ScriptsDialogState.Preview).script.source ?: ""
                                state = ScriptsDialogState.EditScript()
                            },
                            enabled = !isLoading
                        ) {
                            Text(stringResource(R.string.fork))
                        }
                        when (previewScriptAction) {
                            PreviewScriptAction.AddData -> {
                                TextButton(
                                    onClick = {
                                        state = ScriptsDialogState.AddData((state as ScriptsDialogState.Preview).script)
                                    },
                                    enabled = !isLoading
                                ) {
                                    Text(stringResource(R.string.next))
                                }
                            }

                            PreviewScriptAction.Edit -> {
                                if (isMine)
                                    TextButton(
                                        onClick = {
                                            scriptName = (state as ScriptsDialogState.Preview).script.name ?: ""
                                            scriptSource = (state as ScriptsDialogState.Preview).script.source ?: ""
                                            state =
                                                ScriptsDialogState.EditScript((state as ScriptsDialogState.Preview).script.id!!)
                                        },
                                        enabled = !isLoading
                                    ) {
                                        Text(stringResource(R.string.edit))
                                    }
                            }

                            else -> Unit
                        }
                    }

                    else -> Unit
                }
            }
        )
    }

    if (showHelp) {
        Alert(
            {
                showHelp = false
            },
            title = "Kotlin Scripts",
            text = """
                Scripts are written in Kotlin. You may want to use your favorite code editor to write scripts, as you will need to add all import statements. 
                
                The following variables are passed to into scripts:
                
                self: String // The id of this script
                me: Person? // The current user, if signed in, null if signed out
                data: String? // Data passed to the script, if any
                
                Person has the following fields: id, name, photo, language, utcOffset, seen
                
                The following are functions passed into scripts: render, http
                
                Rendering content:
                
                render {
                    section("<Title>")
                    text("<Text>")
                    button(
                        text = "<Button text>",
                        script = "<Script ID to run>",
                        data = "<Data passed to the script>",
                        // Optional
                        data = "<Data passed to the script>",
                        // Optional
                        style = ButtonStyle.Secondary // Default is Primary
                    )
                    photo("<Url>", <Aspect ratio>?) // Must start with /static/
                }
                
                Networking:
                
                Ktor's HttpClient is used for simple networking. Learn more at ktor.io.
                
                http<Any type here>("<url>")
                http.post<Response type here, Request type here>("<url>", body = <Any object here>)
                
                KotlinX Serialization is available. The recommendation is to use @Serializable data classes.
                
                @Serializable
                data class Request(val data: String)
                
                @Serializable
                data class Response(val data: String)
                
                val post = http.post<Response, Request>("<url>", body = Request(""))
                val get = http<Response>("<url>")
                val get: Response = http("<url>") // or this
                val get: Response = http("<url>", headers = mapOf("Authorization" to "<token>")) // set headers
                
                Other common response types are: String, HttpStatusCode, JsonObject
                
                Advanced:
                
                You can depend on packages from Maven Repositories.
                
                @file:Repository("<maven url>")
                @file:DependsOn("<package>")
                
                You can also depend on other scripts.
                                                
                @file:DependsOnScript("<script ID>")
                
                They will be available as script_<script ID> in your script.
                
                You can define the package of your script:
                
                package <package name>
                
                Which will make them available with your declared package name.
                
                Note that in imported scripts, no variables are passed in to the script.                
                
                Learn more at kotlinlang.org
            """.trimIndent(),
            dismissButton = null,
            confirmButton = stringResource(R.string.close),
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            showHelp = false
        }
    }
}
