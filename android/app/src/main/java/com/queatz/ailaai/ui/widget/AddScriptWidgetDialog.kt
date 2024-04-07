package com.queatz.ailaai.ui.widget

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Search
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import app.ailaai.api.createScript
import app.ailaai.api.scripts
import com.queatz.ailaai.R
import com.queatz.ailaai.data.api
import com.queatz.ailaai.data.json
import com.queatz.ailaai.extensions.notBlank
import com.queatz.ailaai.extensions.rememberStateOf
import com.queatz.ailaai.ui.components.CardToolbar
import com.queatz.ailaai.ui.components.DialogBase
import com.queatz.ailaai.ui.components.DialogLayout
import com.queatz.ailaai.ui.dialogs.Alert
import com.queatz.ailaai.ui.theme.pad
import com.queatz.db.Script
import com.queatz.db.Widget
import com.queatz.widgets.Widgets
import com.queatz.widgets.widgets.ScriptData
import createWidget
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString

private sealed class AddScriptWidgetDialogState {
    data object Select : AddScriptWidgetDialogState()
    data object Script : AddScriptWidgetDialogState()
    data object Templates : AddScriptWidgetDialogState()
    data object Search : AddScriptWidgetDialogState()
    data class AddData(val script: com.queatz.db.Script) : AddScriptWidgetDialogState()
}

private val templates = listOf(
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

@Composable
fun AddScriptWidgetDialog(
    onDismissRequest: () -> Unit,
    onWidget: (widget: Widget) -> Unit
) {
    val scope = rememberCoroutineScope()
    var state by rememberStateOf<AddScriptWidgetDialogState>(
        AddScriptWidgetDialogState.Select
    )
    var isLoading by rememberStateOf(false)
    var newScriptName by rememberStateOf("")
    var newScriptSource by rememberStateOf(
        """
            render {
            
            }
        """.trimIndent()
    )
    var scriptData by rememberStateOf("")
    var showHelp by rememberStateOf(false)

    fun createScriptWidget(script: Script, data: String?) {
        isLoading = true
        scope.launch {
            api.createWidget(
                Widgets.Script,
                data = json.encodeToString(ScriptData(script.id!!, data))
            ) {
                onWidget(it)
            }
            isLoading = false
        }
    }

    fun createScript(name: String, source: String) {
        isLoading = true
        scope.launch {
            api.createScript(Script(name = name, source = source)) {
                state = AddScriptWidgetDialogState.AddData(it)
            }
            isLoading = false
        }
    }

    DialogBase(onDismissRequest) {
        DialogLayout(
            scrollable = false,
            content = {
                when (state) {
                    is AddScriptWidgetDialogState.Select -> {
                        CardToolbar {
                            item(
                                Icons.Outlined.Add,
                                stringResource(R.string.new_script)
                            ) {
                                state = AddScriptWidgetDialogState.Script
                            }
                            item(
                                Icons.Outlined.Description,
                                stringResource(R.string.templates)
                            ) {
                                state = AddScriptWidgetDialogState.Templates
                            }
                            item(
                                Icons.Outlined.Search,
                                stringResource(R.string.scripts)
                            ) {
                                state = AddScriptWidgetDialogState.Search
                            }
                        }
                    }
                    is AddScriptWidgetDialogState.Script -> {
                        val focusRequester = remember { FocusRequester() }

                        LaunchedEffect(Unit) {
                            focusRequester.requestFocus()
                        }

                        OutlinedTextField(
                            newScriptName,
                            onValueChange = { newScriptName = it },
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
                            newScriptSource,
                            onValueChange = { newScriptSource = it },
                            label = { Text(stringResource(R.string.script_source)) },
                            shape = MaterialTheme.shapes.large,
                            singleLine = false,
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f, fill = false)
                                .padding(bottom = 1.pad)
                                .heightIn(min = 48.dp)
                        )
                    }
                    is AddScriptWidgetDialogState.Search -> {
                        val focusRequester = remember { FocusRequester() }
                        var search by rememberStateOf("")
                        var scripts by rememberStateOf(emptyList<Script>())

                        LaunchedEffect(Unit) {
                            focusRequester.requestFocus()
                        }

                        LaunchedEffect(Unit) {
                            api.scripts(search.notBlank) {
                                scripts = it
                            }
                        }

                        OutlinedTextField(
                            search,
                            onValueChange = { search = it },
                            label = { Text(stringResource(R.string.search_scripts)) },
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

                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(1.pad),
                            modifier = Modifier
                                .weight(1f)
                        ) {
                            items(scripts) {
                                Text(
                                    it.name ?: stringResource(R.string.new_script),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(MaterialTheme.shapes.large)
                                        .clickable {
                                            state = AddScriptWidgetDialogState.AddData(it)
                                        }
                                        .padding(1.pad)
                                )
                            }
                        }
                    }
                    is AddScriptWidgetDialogState.Templates -> {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(1.pad),
                            modifier = Modifier
                                .weight(1f, fill = false)
                        ) {
                            items(templates) {
                                Text(
                                    it.name ?: stringResource(R.string.new_script),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(MaterialTheme.shapes.large)
                                        .clickable {
                                            newScriptName = it.name ?: ""
                                            newScriptSource = it.source ?: ""
                                            state = AddScriptWidgetDialogState.Script
                                        }
                                        .padding(1.pad)
                                )
                            }
                        }
                    }
                    is AddScriptWidgetDialogState.AddData -> {
                        OutlinedTextField(
                            scriptData,
                            onValueChange = { scriptData = it },
                            label = { Text(stringResource(R.string.script_data)) },
                            shape = MaterialTheme.shapes.large,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 1.pad)
                                .heightIn(min = 48.dp)
                        )
                    }
                }
            },
            actions = {
                if (state is AddScriptWidgetDialogState.Script) {
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
                TextButton(
                    {
                        onDismissRequest()
                    }
                ) {
                    Text(stringResource(R.string.close))
                }
                when (state) {
                    is AddScriptWidgetDialogState.Script -> {
                        TextButton(
                            {
                                createScript(
                                    newScriptName,
                                    newScriptSource
                                )
                            },
                            enabled = !isLoading
                        ) {
                            Text(stringResource(R.string.create_script))
                        }
                    }
                    is AddScriptWidgetDialogState.AddData -> {
                        TextButton(
                            {
                                createScriptWidget(
                                    (state as AddScriptWidgetDialogState.AddData).script,
                                    scriptData
                                )
                            },
                            enabled = !isLoading
                        ) {
                            Text(stringResource(R.string.choose_this_script))
                        }
                    }
                    else -> {}
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
                
                Person has the following fields: id, name, photo
                
                The following are functions passed into scripts: render, http
                
                Rendering content:
                
                render {
                    section("<Title>")
                    text("<Text>")
                    button(
                        text = "<Button text>",
                        script = "<Script ID to run>",
                        data = "<Data passed to the script>",
                    )
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
                
                val post = http.post<Response>("<url>", body = Request(""))
                val get = http<Response>("<url>")
                val get: Response = http("<url>") // or this
                
                Other common response types are: String, HttpStatusCode, JsonObject
                
                Advanced:
                
                You can depend on packages from Maven Repositories.
                
                @file:Repository("<maven url>")
                @file:DependsOn("<package>")
                
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
