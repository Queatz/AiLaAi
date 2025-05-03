package app.scripts

import Styles
import aiScript
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import api
import app.AppStyles
import app.FullPageLayout
import app.PageTopBar
import app.ailaai.api.categories
import app.ailaai.api.createScript
import app.ailaai.api.deleteScript
import app.ailaai.api.newCard
import app.ailaai.api.prompts
import app.ailaai.api.runScript
import app.ailaai.api.scriptData
import app.ailaai.api.scripts
import app.ailaai.api.updateScript
import app.ailaai.api.updateScriptData
import app.dialog.rememberChoosePhotoDialog
import app.ailaai.shared.resources.ScriptsResources
import app.components.TopBarSearch
import app.dialog.categoryDialog
import app.dialog.dialog
import app.dialog.inputDialog
import app.dialog.inputSelectDialog
import components.SearchField
import app.menu.Menu
import app.messaages.inList
import app.nav.appFeedbackDialog
import appString
import appText
import application
import bulletedString
import com.queatz.db.AiScriptRequest
import com.queatz.db.AppFeedback
import com.queatz.db.AppFeedbackType
import com.queatz.db.Card
import com.queatz.db.CardOptions
import com.queatz.db.PromptContext
import com.queatz.db.RunScriptBody
import com.queatz.db.Script
import com.queatz.db.ScriptData
import com.queatz.db.ScriptResult
import com.queatz.db.StoryContent
import com.queatz.db.asGeo
import com.queatz.db.toJsonStoryPart
import com.queatz.widgets.Widgets
import components.IconButton
import components.LinkifyText
import components.Loading
import createWidget
import json
import kotlinx.browser.window
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import notBlank
import org.jetbrains.compose.web.attributes.disabled
import org.jetbrains.compose.web.css.AlignItems
import org.jetbrains.compose.web.css.DisplayStyle
import org.jetbrains.compose.web.css.FlexDirection
import org.jetbrains.compose.web.css.JustifyContent
import org.jetbrains.compose.web.css.Position
import org.jetbrains.compose.web.css.Position.Companion.Absolute
import org.jetbrains.compose.web.css.alignItems
import org.jetbrains.compose.web.css.bottom
import org.jetbrains.compose.web.css.display
import org.jetbrains.compose.web.css.flex
import org.jetbrains.compose.web.css.flexDirection
import org.jetbrains.compose.web.css.flexGrow
import org.jetbrains.compose.web.css.flexShrink
import org.jetbrains.compose.web.css.fontSize
import org.jetbrains.compose.web.css.fontWeight
import org.jetbrains.compose.web.css.gap
import org.jetbrains.compose.web.css.height
import org.jetbrains.compose.web.css.justifyContent
import org.jetbrains.compose.web.css.margin
import org.jetbrains.compose.web.css.marginBottom
import org.jetbrains.compose.web.css.marginLeft
import org.jetbrains.compose.web.css.marginTop
import org.jetbrains.compose.web.css.opacity
import org.jetbrains.compose.web.css.overflow
import org.jetbrains.compose.web.css.overflowX
import org.jetbrains.compose.web.css.overflowY
import org.jetbrains.compose.web.css.padding
import org.jetbrains.compose.web.css.paddingBottom
import org.jetbrains.compose.web.css.paddingLeft
import org.jetbrains.compose.web.css.paddingRight
import org.jetbrains.compose.web.css.paddingTop
import org.jetbrains.compose.web.css.percent
import org.jetbrains.compose.web.css.position
import org.jetbrains.compose.web.css.px
import org.jetbrains.compose.web.css.right
import org.jetbrains.compose.web.css.top
import org.jetbrains.compose.web.css.whiteSpace
import org.jetbrains.compose.web.css.width
import org.jetbrains.compose.web.dom.Button
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.Text
import org.w3c.dom.DOMRect
import org.w3c.dom.HTMLElement
import r
import sortedDistinct
import stories.StoryContents

@Composable
fun ResultPanel(
    isRunningScript: Boolean,
    scriptResult: ScriptResult?,
    runScript: suspend (String, String?, Map<String, String?>, Boolean?) -> Unit,
    isOnLeft: Boolean
) {
    Div({
        style {
            if (isOnLeft) {
                flex(1)
                width(0.r)
            } else {
                flexShrink(0)
                width(24.r)
            }
            display(DisplayStyle.Flex)
            flexDirection(FlexDirection.Column)
            overflowX("hidden")
            overflowY("auto")
            height(100.percent)
        }
    }) {
        if (isRunningScript) {
            Loading()
        } else {
            scriptResult?.content?.let {
                Div({
                    classes(Styles.cardContent)
                }) {
                    StoryContents(
                        content = it,
                        onButtonClick = { script, data, input ->
                            runScript(
                                script,
                                data,
                                input,
                                true
                            )
                        },
                    )
                }
            }
        }
    }
}

suspend fun scriptSecretDialog(secret: String) = inputDialog(
    title = application.appString { this.secret },
    defaultValue = secret,
    confirmButton = application.appString { save },
    singleLine = false
)

suspend fun selectScriptDialog(
    scope: kotlinx.coroutines.CoroutineScope,
    state: MonacoEditorState,
    editedScript: String?,
    script: Script
) {
    dialog(
        title = application.appString { dependOnScript },
        confirmButton = null,
        cancelButton = application.appString { cancel },
        content = { resolve ->
            var scripts by remember { mutableStateOf<List<Script>>(emptyList()) }
            var isLoading by remember { mutableStateOf(true) }
            var search by remember { mutableStateOf("") }

            var hasMore by remember { mutableStateOf(true) }
            var isLoadingMore by remember { mutableStateOf(false) }
            var offset by remember { mutableStateOf(0) }
            val limit = 20

            fun loadMore() {
                if (!hasMore || isLoadingMore) return
                isLoadingMore = true
                scope.launch {
                    api.scripts(
                        search = search.notBlank,
                        offset = offset + limit,
                        limit = limit
                    ) {
                        val newScripts = it.filter { it.id != script.id }
                        if (offset == 0) {
                            scripts = newScripts
                        } else {
                            scripts = (scripts + newScripts).distinctBy { it.id }
                            offset += limit
                        }
                        hasMore = newScripts.size >= limit
                        isLoadingMore = false
                        isLoading = false
                    }
                }
            }

            LaunchedEffect(search) {
                offset = 0
                isLoading = true
                loadMore()
            }

            Div({
                style {
                    display(DisplayStyle.Flex)
                    flexDirection(FlexDirection.Column)
                    gap(1.r)
                    width(32.r)
                    height(24.r)
                }
            }) {
                SearchField(
                    value = search,
                    placeholder = appString { this.search },
                    shadow = false,
                    styles = {
                        margin(.5.r)
                    },
                    onValue = {
                        search = it
                    }
                )

                if (isLoading) {
                    Loading()
                } else if (scripts.isNotEmpty()) {
                    Div({
                        style {
                            display(DisplayStyle.Flex)
                            flexDirection(FlexDirection.Column)
                            gap(.5.r)
                            padding(0.r, .5.r)
                            property("overflow-y", "auto")
                            flex(1)
                        }
                    }) {
                        scripts.forEach { selectedScript ->
                            Div({
                                classes(AppStyles.scriptItem)

                                onClick {
                                    scope.launch {
                                        val currentSource = editedScript ?: script.source.orEmpty()
                                        val description = if (selectedScript.description.isNullOrBlank()) {
                                            ""
                                        } else {
                                            "\n/* ${selectedScript.description.orEmpty()}\n */\n"
                                        }
                                        val dependsOnLine = "$description@file:DependsOnScript(\"${selectedScript.id}\") // ${selectedScript.name}"

                                        // Check if the script already has a @file:DependsOnScript annotation
                                        val newSource = if (currentSource.contains("@file:DependsOnScript")) {
                                            // Insert the new annotation after the last @file:DependsOnScript annotation
                                            val lines = currentSource.lines()
                                            val lastAnnotationIndex = lines.indexOfLast { it.contains("@file:DependsOnScript") }

                                            if (lastAnnotationIndex >= 0) {
                                                val updatedLines = lines.toMutableList()
                                                updatedLines.add(lastAnnotationIndex + 1, dependsOnLine)
                                                updatedLines.joinToString("\n")
                                            } else {
                                                "$dependsOnLine\n$currentSource"
                                            }
                                        } else {
                                            "$dependsOnLine\n$currentSource"
                                        }

                                        state.setValue(newSource)
                                        resolve(false)
                                    }
                                }
                            }) {
                                Div({
                                    style {
                                        fontSize(18.px)
                                        fontWeight("bold")
                                        marginBottom(.5.r)
                                    }
                                }) {
                                    Text(selectedScript.name?.notBlank ?: appString { newScript })
                                }
                                Div({
                                    style {
                                        opacity(.5)
                                    }
                                }) {
                                    Text(
                                        bulletedString(
                                            selectedScript.author?.name?.let { "By $it" },
                                            selectedScript.categories?.firstOrNull(),
                                            selectedScript.id!!
                                        )
                                    )
                                }
                                selectedScript.description?.notBlank?.let { description ->
                                    Div({
                                        style {
                                            marginTop(.5.r)
                                            overflow("auto")
                                        }
                                    }) {
                                        LinkifyText(description)
                                    }
                                }
                            }
                        }
                    }
                } else if (search.isNotBlank()) {
                    Div({
                        style {
                            display(DisplayStyle.Flex)
                            alignItems(AlignItems.Center)
                            justifyContent(JustifyContent.Center)
                            opacity(.5)
                            padding(1.r)
                        }
                    }) {
                        appText { noScripts }
                    }
                } else {
                    Div({
                        style {
                            display(DisplayStyle.Flex)
                            alignItems(AlignItems.Center)
                            justifyContent(JustifyContent.Center)
                            opacity(.5)
                            padding(1.r)
                        }
                    }) {
                        appText { noScripts }
                    }
                }
            }
        }
    )
}

@Composable
fun ScriptsPage(
    nav: ScriptsNav,
    onUpdate: (Script) -> Unit,
    onScriptDeleted: (Script) -> Unit,
    onScriptCreated: (Script) -> Unit = {},
) {
    val scope = rememberCoroutineScope()
    val choosePhoto = rememberChoosePhotoDialog(showUpload = true)

    when (nav) {
        is ScriptsNav.None -> Unit
        is ScriptsNav.Explore -> {
            var scripts by remember { mutableStateOf<List<Script>>(emptyList()) }
            var isLoading by remember { mutableStateOf(true) }
            var search by remember { mutableStateOf("") }
            var selectedCategory by remember { mutableStateOf<String?>(null) }
            var categoriesCache by remember { mutableStateOf(emptyList<String>()) }
            var offset by remember { mutableStateOf(0) }
            val pageSize = 20
            val categories = remember(scripts) {
                if (selectedCategory == null) {
                    scripts
                        .mapNotNull { it.categories }
                        .flatten()
                        .sortedDistinct().also {
                            categoriesCache = it
                        }
                } else {
                    categoriesCache
                }
            }

            LaunchedEffect(search, selectedCategory) {
                offset = 0
                api.scripts(
                    search = selectedCategory?.notBlank ?: search.notBlank,
                    offset = offset,
                    limit = pageSize
                ) {
                    scripts = it
                }
                isLoading = false
            }

            FullPageLayout {
                Div({
                    style {
                        display(DisplayStyle.Flex)
                        flexDirection(FlexDirection.Column)
                        paddingLeft(1.r)
                        paddingRight(1.r)
                        paddingBottom(1.r)
                    }
                }) {
                    if (isLoading) {
                        Loading()
                    } else if (scripts.isNotEmpty()) {
                        TopBarSearch(
                            value = search,
                            onValue = { search = it },
                            styles = {
                                margin(1.r, 0.r)
                            }
                        )
                        if (categories.isNotEmpty()) {
                            Div({
                                style {
                                    display(DisplayStyle.Flex)
                                    gap(.5.r)
                                    overflowY("auto")
                                    paddingTop(.5.r)
                                    paddingBottom(1.5.r)
                                    position(Position.Relative)
                                }
                            }) {
                                categories.forEach { category ->
                                    Button({
                                        classes(Styles.floatingButton)

                                        if (selectedCategory == category) {
                                            classes(Styles.floatingButtonSelected)
                                        }

                                        style {
                                            flexShrink(0)
                                        }

                                        onClick {
                                            selectedCategory = if (selectedCategory == category) null else category
                                        }
                                    }) {
                                        Text(category)
                                    }
                                }
                            }
                        }
                        Div({
                            style {
                                display(DisplayStyle.Flex)
                                flexDirection(FlexDirection.Column)
                            }
                        }) {
                            scripts.forEach { script ->
                                Div({
                                    classes(AppStyles.scriptItem)

                                    style {
                                        marginBottom(1.r)
                                    }

                                    onClick {
                                        onScriptCreated(script)
                                    }
                                }) {
                                    Div({
                                        style {
                                            fontSize(18.px)
                                            fontWeight("bold")
                                            marginBottom(.5.r)
                                        }
                                    }) {
                                        Text(script.name?.notBlank ?: appString { newScript })
                                    }
                                    Div({
                                        style {
                                            opacity(.5)
                                        }
                                    }) {
                                        Text(
                                            bulletedString(
                                                script.author?.name?.let { "By $it" },
                                                script.categories?.firstOrNull(),
                                                script.id!!
                                            )
                                        )
                                    }
                                    script.description?.notBlank?.let { description ->
                                        Div({
                                            style {
                                                marginTop(.5.r)
                                                overflow("auto")
                                            }
                                        }) {
                                            LinkifyText(description)
                                        }
                                    }
                                }
                            }
                        }
                    } else if (search.isNotBlank()) {
                        Div({
                            style {
                                display(DisplayStyle.Flex)
                                alignItems(AlignItems.Center)
                                justifyContent(JustifyContent.Center)
                                opacity(.5)
                                padding(1.r)
                            }
                        }) {
                            appText { noScripts }
                        }
                    } else {
                        Div({
                            style {
                                display(DisplayStyle.Flex)
                                alignItems(AlignItems.Center)
                                justifyContent(JustifyContent.Center)
                                opacity(.5)
                                padding(1.r)
                            }
                        }) {
                            appText { noScripts }
                        }
                    }
                }
            }
        }

        is ScriptsNav.Script -> {
            val script by remember(nav.script) { mutableStateOf(nav.script) }
            var editedScript by remember(script) { mutableStateOf<String?>(null) }
            var edited by remember(script) { mutableStateOf(false) }
            var isSaving by remember(script) { mutableStateOf(false) }
            var showResultPanel by remember(script) { mutableStateOf(false) }
            var isRunningScript by remember(script) { mutableStateOf(false) }
            var runScriptData by remember(script) { mutableStateOf<String?>(null) }
            var scriptResult by remember(script) { mutableStateOf<ScriptResult?>(null) }
            var isAiScriptGenerating by remember(script) { mutableStateOf(false) }
            var undoAiScript by remember(script) { mutableStateOf<String?>(null) }
            var isEditorOnRight by remember { mutableStateOf(false) }

            var isLoading by remember { mutableStateOf(false) }
            var menuTarget by remember(script) { mutableStateOf<DOMRect?>(null) }
            var aiJob by remember { mutableStateOf<Job?>(null) }
            var isBackgroundLoading by remember { mutableStateOf(false) }
            val state = remember { MonacoEditorState() }

            LaunchedEffect(script) {
                aiJob?.cancel()
            }

            // Check if the current user is the script owner
            val me = application.me.value
            val isCurrentUserOwner = script.person == me?.id

            menuTarget?.let {
                Menu(
                    onDismissRequest = { menuTarget = null },
                    target = it,
                ) {
                    item(
                        title = appString { openInNewTab },
                        icon = "open_in_new",
                        onClick = {
                            menuTarget = null
                            window.open("/script/${script.id}", "_blank")
                        }
                    )

                    if (isCurrentUserOwner) {
                        // Show all options for the script owner
                        item(
                            title = appString { rename },
                            onClick = {
                                menuTarget = null
                                scope.launch {
                                    val name = inputDialog(
                                        // todo: translate
                                        title = "Script name",
                                        confirmButton = application.appString { update },
                                        defaultValue = script.name.orEmpty()
                                    )

                                    if (name != null) {
                                        api.updateScript(
                                            id = script.id!!,
                                            script = Script(name = name)
                                        ) {
                                            onUpdate(it)
                                        }
                                    }
                                }
                            }
                        )
                        item(
                            title = appString { category },
                            onClick = {
                                menuTarget = null
                                scope.launch {
                                    api.categories(
                                        // todo: might need to not require geo
                                        geo = application.me.value?.geo?.asGeo() ?: return@launch
                                    ) { categories ->
                                        val category = categoryDialog(
                                            categories = categories
                                        )

                                        if (category != null) {
                                            api.updateScript(
                                                id = script.id!!,
                                                script = Script(categories = category.inList())
                                            ) {
                                                onUpdate(it)
                                            }
                                        }
                                    }
                                }
                            }
                        )
                        val title = appString { description }
                        item(
                            title = title,
                            onClick = {
                                menuTarget = null
                                scope.launch {
                                    val description = inputDialog(
                                        title = title,
                                        confirmButton = application.appString { update },
                                        defaultValue = script.description.orEmpty(),
                                        singleLine = false
                                    )

                                    if (description != null) {
                                        api.updateScript(
                                            id = script.id!!,
                                            script = Script(description = description)
                                        ) {
                                            onUpdate(it)
                                        }
                                    }
                                }
                            }
                        )
                        item(
                            title = appString { secret },
                            onClick = {
                                menuTarget = null
                                scope.launch {
                                    api.scriptData(script.id!!) { scriptData ->
                                        val result = scriptSecretDialog(scriptData.secret ?: "")

                                        if (result != null) {
                                            api.updateScriptData(
                                                id = script.id!!,
                                                scriptData = ScriptData(secret = result),
                                                onSuccess = {}
                                            )
                                        }
                                    }
                                }
                            }
                        )
                        item(
                            // todo: translate
                            title = "Background",
                            onClick = {
                                menuTarget = null
                                scope.launch {
                                    try {
                                        choosePhoto.launch { photo ->
                                            isBackgroundLoading = true
                                            api.updateScript(
                                                id = script.id!!,
                                                script = Script(background = photo)
                                            ) {
                                                onUpdate(it)
                                                isBackgroundLoading = false
                                            }
                                        }
                                    } catch (e: Exception) {
                                        isBackgroundLoading = false
                                    }
                                }
                            }
                        )
                        item(
                            title = appString { delete },
                            onClick = {
                                menuTarget = null
                                scope.launch {
                                    val proceed = dialog(
                                        title = application.appString { deleteThisScript },
                                        confirmButton = application.appString { yesDelete }
                                    )
                                    if (proceed == true) {
                                        api.deleteScript(script.id!!) {
                                            onScriptDeleted(script)
                                        }
                                    }
                                }
                            }
                        )
                    }

                    item(
                        title = appString { swapEditorPosition },
                        icon = "swap_horiz",
                        onClick = {
                            menuTarget = null
                            isEditorOnRight = !isEditorOnRight
                            showResultPanel = true
                        }
                    )

                    script.person?.let { person ->
                        item(
                            title = appString { goToAuthorProfile },
                            icon = "open_in_new",
                            onClick = {
                                menuTarget = null
                                window.open("/profile/$person", "_blank")
                            }
                        )
                    }

                    item(
                        title = appString { openScriptInNewPage },
                        icon = "note_add",
                        onClick = {
                            menuTarget = null
                            scope.launch {
                                api.createWidget(
                                    widget = Widgets.Script,
                                    data = json.encodeToString(
                                        com.queatz.widgets.widgets.ScriptData(
                                            script = script.id!!
                                        )
                                    )
                                ) { widget ->
                                    api.newCard(
                                        card = Card(
                                            name = script.name,
                                            options = json.encodeToString(CardOptions(enableReplies = false, enableAnonymousReplies = false)),
                                            content = json.encodeToString(
                                                listOf(
                                                    StoryContent.Widget(
                                                        Widgets.Script,
                                                        widget.id!!
                                                    ).toJsonStoryPart(json)
                                                )
                                            )
                                        ),
                                        onSuccess = { card ->
                                            window.open("/page/${card.id}", "_blank")
                                        }
                                    )
                                }
                            }
                        }
                    )

                    // Always show Fork option for all users
                    item(
                        title = appString { fork },
                        icon = "call_split",
                        onClick = {
                            menuTarget = null
                            scope.launch {
                                api.createScript(
                                    Script(
                                        name = script.name,
                                        categories = script.categories,
                                        description = script.description,
                                        source = editedScript?.notBlank ?: script.source
                                    )
                                ) { newScript ->
                                    onScriptCreated(newScript)
                                }
                            }
                        }
                    )
                }
            }

            val runScript: suspend (String, String?, Map<String, String?>, Boolean?) -> Unit = { scriptId, data, input, useCache ->
                api.runScript(
                    id = scriptId,
                    data = RunScriptBody(
                        data = data,
                        input = input,
                        useCache = useCache
                    )
                ) {
                    scriptResult = it
                    isRunningScript = false
                }
            }

            if (isLoading) {
                Loading()
            } else {
                Div({
                    style {
                        display(DisplayStyle.Flex)
                        flex(1)
                        height(0.r)
                    }
                }) {
                    // Conditionally arrange the editor and result panel based on isEditorOnRight
                    if (isEditorOnRight && showResultPanel) {
                        // Result Panel on the left
                        ResultPanel(
                            isRunningScript = isRunningScript,
                            scriptResult = scriptResult,
                            runScript = { scriptId, data, input, useCache ->
                                runScript(scriptId, data, input, useCache)
                            },
                            isOnLeft = true
                        )
                    }

                    // Editor (left by default, right when isEditorOnRight is true)
                    Div({
                        style {
                            if (isEditorOnRight && showResultPanel) {
                                flexShrink(0)
                                width(32.r)
                            } else {
                                flex(1)
                                width(0.r)
                            }
                            height(100.percent)
                            display(DisplayStyle.Flex)
                            flexDirection(FlexDirection.Column)
                            overflowX("hidden")
                            overflowY("auto")
                        }
                    }) {
                        MonacoEditor(
                            state = state,
                            initialValue = script.source.orEmpty(),
                            onValueChange = {
                                editedScript = it
                            },
                            onIsEdited = {
                                edited = it
                            },
                            styles = {
                                margin(0.r, 1.r, 1.r, 1.r)
                                flexGrow(1)
                            }
                        )
                    }

                    // Result Panel on the right (when not isEditorOnRight)
                    if (!isEditorOnRight && showResultPanel) {
                        ResultPanel(
                            isRunningScript = isRunningScript,
                            scriptResult = scriptResult,
                            runScript = { scriptId, data, input, useCache ->
                                runScript(scriptId, data, input, useCache)
                            },
                            isOnLeft = false
                        )
                    }
                }
                PageTopBar(
                    title = script.name?.notBlank ?: appString { newScript },
                    useMinHeight = true,
                    description = bulletedString(
                        script.categories?.firstOrNull(),
                        script.description,
                        script.id
                    ),
                    onTitleClick = {
                        if (isCurrentUserOwner) {
                            scope.launch {
                                val newName = inputDialog(
                                    title = application.appString { renameScript },
                                    defaultValue = script.name?.notBlank ?: application.appString { newScript },
                                    placeholder = application.appString { scriptName }
                                )

                                if (newName != null) {
                                    api.updateScript(
                                        id = script.id!!,
                                        script = Script(name = newName)
                                    ) {
                                        onUpdate(it)
                                    }
                                }
                            }
                        }
                    },
                    actions = {
                        if (undoAiScript != null) {
                            IconButton(
                                name = "undo",
                                title = appString { undoAiChanges },
                                styles = {
                                    marginLeft(.5.r)
                                }
                            ) {
                                scope.launch {
                                    state.setValue(undoAiScript!!)
                                    undoAiScript = null
                                }
                            }
                        }
                        if (edited) {
                            Button(
                                attrs = {
                                    classes(Styles.button)

                                    style {
                                        marginLeft(.5.r)
                                    }

                                    onClick {
                                        if (!isCurrentUserOwner) {
                                            scope.launch {
                                                api.createScript(
                                                    Script(
                                                        name = script.name,
                                                        categories = script.categories,
                                                        description = script.description,
                                                        source = editedScript?.notBlank ?: script.source
                                                    )
                                                ) { newScript ->
                                                    onScriptCreated(newScript)
                                                }
                                            }
                                        } else {
                                            scope.launch {
                                                isSaving = true
                                                api.updateScript(
                                                    id = script.id!!,
                                                    script = Script(source = editedScript)
                                                ) {
                                                    onUpdate(it)
                                                }
                                                isSaving = false
                                            }
                                        }
                                    }

                                    if (isSaving) {
                                        disabled()
                                    }
                                }
                            ) {
                                if (isCurrentUserOwner) {
                                    appText { save }
                                } else {
                                    appText { fork }
                                }
                            }
                        } else {
                            IconButton(
                                name = "play_arrow",
                                title = appString { runScriptHoldShift },
                                styles = {
                                    marginLeft(.5.r)
                                }
                            ) {
                                showResultPanel = true
                                runScriptData = null
                                isRunningScript = true
                                scope.launch {
                                    runScript(script.id!!, null, emptyMap(), !it.shiftKey)
                                }
                            }
                            IconButton(
                                name = "auto_awesome",
                                title = appString { codeWithAi },
                                isLoading = isAiScriptGenerating,
                                styles = {
                                    marginLeft(.5.r)
                                }
                            ) {
                                if (isAiScriptGenerating) {
                                    scope.launch {
                                        val result = dialog(
                                            title = application.appString { stopScriptGeneration },
                                            confirmButton = application.appString { yesStop }
                                        )

                                        if (result == true) {
                                            aiJob?.cancel()
                                            isAiScriptGenerating = false
                                        }
                                    }
                                } else {
                                    aiJob = scope.launch {
                                        val prompt = inputDialog(
                                            title = application.appString { aiPrompt },
                                            confirmButton = application.appString { send },
                                            singleLine = false,
                                            placeholder = if (
                                                (editedScript ?: script.source.orEmpty()).isBlank()
                                            ) application.appString { createScriptThat } else application.appString { modifyThisScriptTo },
                                            inputAction = { resolve, value, onValue ->
                                                IconButton(
                                                    name = "expand_more",
                                                    title = appString { history },
                                                    styles = {
                                                        if (value.isNotBlank()) {
                                                            opacity(0)
                                                            property("pointer-events", "none")
                                                        }

                                                        position(Absolute)
                                                        right(0.5.r)
                                                        top(1.r)
                                                        bottom(1.r)
                                                    }
                                                ) {
                                                    scope.launch {
                                                        // Get prompts with Scripts context
                                                        api.prompts(context = PromptContext.Scripts) { previousPrompts ->
                                                            if (previousPrompts.isNotEmpty()) {
                                                                inputSelectDialog(
                                                                    confirmButton = application.appString { choose },
                                                                    items = previousPrompts.map { it.prompt!! }
                                                                ) { selectedPrompt ->
                                                                    onValue(selectedPrompt)
                                                                }
                                                            } else {
                                                                dialog(
                                                                    title = application.appString { noPromptHistory },
                                                                    cancelButton = null
                                                                )
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        )

                                        if (prompt != null) {
                                            isAiScriptGenerating = true
                                            api.aiScript(
                                                request = AiScriptRequest(
                                                    prompt = prompt,
                                                    script = editedScript ?: script.source.orEmpty()
                                                ),
                                                onError = { error ->
                                                    if (error !is CancellationException) {
                                                        scope.launch {
                                                            dialog(
                                                                title = application.appString { somethingWentWrong },
                                                                cancelButton = null,
                                                                content = {
                                                                    appText { pleaseTryAgain }
                                                                }
                                                            )
                                                        }
                                                    }
                                                }
                                            ) {
                                                undoAiScript = editedScript ?: script.source.orEmpty()
                                                state.setValue(it.code)
                                            }
                                            isAiScriptGenerating = false
                                        }

                                        aiJob = null
                                    }
                                }
                            }
                        }

                        IconButton(
                            name = "post_add",
                            title = appString { dependOnScript },
                            styles = {
                                marginLeft(.5.r)
                            }
                        ) {
                            scope.launch {
                                selectScriptDialog(scope, state, editedScript, script)
                            }
                        }

                        IconButton(
                            name = "help_outline",
                            title = appString { help },
                            styles = {
                                marginLeft(.5.r)
                            }
                        ) {
                            scope.launch {
                                dialog(
                                    title = application.appString { kotlinScripts },
                                    cancelButton = null,
                                    actions = {
                                        Button(
                                            attrs = {
                                                classes(Styles.button)

                                                onClick {
                                                    scope.launch {
                                                        appFeedbackDialog(
                                                            AppFeedbackType.Suggestion,
                                                            prefix = "Scripts request:\n\n"
                                                        )
                                                    }
                                                }
                                            }
                                        ) {
                                            Text("Request a feature")
                                        }
                                    },
                                    content = {
                                        Div(
                                            {
                                                style {
                                                    whiteSpace("pre-wrap")
                                                }
                                            }
                                        ) {
                                            LinkifyText(ScriptsResources.documentation)
                                        }
                                    }
                                )
                            }
                        }
                        IconButton(
                            name = "more_vert",
                            title = appString { menu },
                            styles = {
                                marginLeft(.5.r)
                            },
                            isLoading = choosePhoto.isGenerating.collectAsState().value || isBackgroundLoading
                        ) {
                            menuTarget =
                                if (menuTarget == null) (it.target as HTMLElement).getBoundingClientRect() else null
                        }
                    }
                )
            }
        }
    }
}
