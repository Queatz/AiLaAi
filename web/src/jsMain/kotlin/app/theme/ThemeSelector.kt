package app.theme

import StyleManager
import Styles
import androidx.compose.runtime.*
import aiJson
import app.dialog.dialog
import api
import app.dialog.inputDialog
import com.queatz.db.AiJsonRequest
import com.queatz.db.AiJsonResponse
import components.IconButton
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonObjectBuilder
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject
import org.jetbrains.compose.web.attributes.InputType
import org.jetbrains.compose.web.attributes.placeholder
import org.jetbrains.compose.web.css.*
import org.jetbrains.compose.web.dom.*
import r

/**
 * A composable function that displays a list of available themes and allows the user to select one.
 */
@Composable
fun ThemeSelector(
    onEditTheme: (UserTheme) -> Unit = {}
) {
    val themes = remember { mutableStateOf(ThemeManager.getAllThemes()) }
    val currentTheme = remember { mutableStateOf(ThemeManager.getCurrentTheme()) }
    var showDeleteConfirmation by remember { mutableStateOf<String?>(null) }

    // Handle theme deletion confirmation
    showDeleteConfirmation?.let { themeName ->
        val scope = rememberCoroutineScope()
        LaunchedEffect(themeName) {
            scope.launch {
                val result = dialog(
                    title = "Delete Theme",
                    confirmButton = "Delete",
                    cancelButton = "Cancel",
                    maxWidth = 400.px
                ) { resolve ->
                    P {
                        Text("Are you sure you want to delete the theme \"$themeName\"?")
                    }
                }

                if (result == true) {
                    ThemeManager.deleteTheme(themeName)
                    themes.value = ThemeManager.getAllThemes()
                }
                showDeleteConfirmation = null
            }
        }
    }

    Div({
        style {
            display(DisplayStyle.Flex)
            flexDirection(FlexDirection.Column)
            gap(1.r)
        }
    }) {
        H3 {
            Text("Select Theme")
        }

        // Default theme option
        Div({
            classes(Styles.outlineButton)
            style {
                cursor("pointer")
                display(DisplayStyle.Flex)
                alignItems(AlignItems.Center)
            }
            onClick {
                currentTheme.value = null
                StyleManager.setTheme(null)
                ThemeManager.setCurrentTheme(null)
            }
        }) {
            Div({
                style {
                    width(24.px)
                    height(24.px)
                    border(1.px, LineStyle.Dashed, Styles.colors.primary)
                    borderRadius(4.px)
                    marginRight(1.r)
                }
            })
            Span({
                style {
                    flex(1)
                }
            }) {
                Text("Default Theme")
            }
            if (currentTheme.value == null) {
                Span({
                    classes("material-symbols-outlined")
                    style {
                        color(Styles.colors.primary)
                    }
                }) {
                    Text("check")
                }
            }
        }

        // List of available themes
        themes.value.forEach { theme ->
            Div({
                classes(Styles.outlineButton)
                style {
                    cursor("pointer")
                    display(DisplayStyle.Flex)
                    alignItems(AlignItems.Center)
                }
                onClick {
                    currentTheme.value = theme
                    StyleManager.setTheme(theme)
                    ThemeManager.setCurrentTheme(theme)
                }
            }) {
                Div({
                    style {
                        width(24.px)
                        height(24.px)
                        backgroundColor(theme.light.primary?.let { Color(it) } ?: Styles.colors.primary)
                        borderRadius(4.px)
                        marginRight(1.r)
                    }
                })
                Span({
                    style {
                        flex(1)
                    }
                }) {
                    Text(theme.name)
                }
                if (currentTheme.value?.name == theme.name) {
                    Span({
                        classes("material-symbols-outlined")
                        style {
                            color(Styles.colors.primary)
                        }
                    }) {
                        Text("check")
                    }
                }

                // Edit button
                Span({
                    classes("material-symbols-outlined")
                    style {
                        cursor("pointer")
                        color(Styles.colors.primary)
                    }
                    onClick { event ->
                        event.stopPropagation()
                        onEditTheme(theme)
                    }
                }) {
                    Text("edit")
                }

                // Delete button
                Span({
                    classes("material-symbols-outlined")
                    style {
                        cursor("pointer")
                        color(Styles.colors.red)
                        marginRight(0.r)
                    }
                    onClick { event ->
                        event.stopPropagation()
                        showDeleteConfirmation = theme.name
                    }
                }) {
                    Text("delete")
                }
            }
        }
    }
}

/**
 * A composable function that allows the user to create or edit a theme.
 */
@Composable
fun ThemeEditor(
    initialTheme: UserTheme? = null,
    onDismiss: () -> Unit = {},
    onSave: (UserTheme) -> Unit = {}
) {
    var themeName by remember { mutableStateOf(initialTheme?.name ?: "My Theme") }

    // Light theme colors
    var lightBackground by remember { mutableStateOf(initialTheme?.light?.background ?: "") }
    var lightPrimary by remember { mutableStateOf(initialTheme?.light?.primary ?: "") }
    var lightSecondary by remember { mutableStateOf(initialTheme?.light?.secondary ?: "") }
    var lightTertiary by remember { mutableStateOf(initialTheme?.light?.tertiary ?: "") }
    var lightRed by remember { mutableStateOf(initialTheme?.light?.red ?: "") }
    var lightGreen by remember { mutableStateOf(initialTheme?.light?.green ?: "") }
    var lightOutline by remember { mutableStateOf(initialTheme?.light?.outline ?: "") }
    var lightSurface by remember { mutableStateOf(initialTheme?.light?.surface ?: "") }
    var lightWhite by remember { mutableStateOf(initialTheme?.light?.white ?: "") }
    var lightBlack by remember { mutableStateOf(initialTheme?.light?.black ?: "") }
    var lightGray by remember { mutableStateOf(initialTheme?.light?.gray ?: "") }
    var lightLightgray by remember { mutableStateOf(initialTheme?.light?.lightgray ?: "") }
    var lightDarkgray by remember { mutableStateOf(initialTheme?.light?.darkgray ?: "") }

    // Dark theme colors
    var darkBackground by remember { mutableStateOf(initialTheme?.dark?.background ?: "") }
    var darkSurface by remember { mutableStateOf(initialTheme?.dark?.surface ?: "") }
    var darkPrimary by remember { mutableStateOf(initialTheme?.dark?.primary ?: "") }
    var darkSecondary by remember { mutableStateOf(initialTheme?.dark?.secondary ?: "") }
    var darkTertiary by remember { mutableStateOf(initialTheme?.dark?.tertiary ?: "") }
    var darkRed by remember { mutableStateOf(initialTheme?.dark?.red ?: "") }
    var darkGreen by remember { mutableStateOf(initialTheme?.dark?.green ?: "") }
    var darkOutline by remember { mutableStateOf(initialTheme?.dark?.outline ?: "") }
    var darkWhite by remember { mutableStateOf(initialTheme?.dark?.white ?: "") }
    var darkBlack by remember { mutableStateOf(initialTheme?.dark?.black ?: "") }
    var darkGray by remember { mutableStateOf(initialTheme?.dark?.gray ?: "") }
    var darkLightgray by remember { mutableStateOf(initialTheme?.dark?.lightgray ?: "") }
    var darkDarkgray by remember { mutableStateOf(initialTheme?.dark?.darkgray ?: "") }

    // UI state for tab selection
    var selectedTab by remember { mutableStateOf("light") }

    Div({
        style {
            display(DisplayStyle.Flex)
            flexDirection(FlexDirection.Column)
            gap(1.r)
        }
    }) {
        H3 {
            Text("Theme Editor")
        }

        // Theme name input
        Div({
            style {
                marginBottom(1.r)
            }
        }) {
            Label {
                Text("Theme ID")
            }

            Input(InputType.Text) {
                classes(Styles.textarea)
                value(themeName)
                onInput { event ->
                    themeName = event.value
                }
                style {
                    display(DisplayStyle.Block)
                    width(100.percent)
                    marginTop(0.5.r)
                }
            }
        }

        // Tab selector
        Div({
            style {
                display(DisplayStyle.Flex)
                marginBottom(1.r)
            }
        }) {
            Button({
                classes(if (selectedTab == "light") Styles.button else Styles.outlineButton)
                onClick {
                    selectedTab = "light"
                }
            }) {
                Text("Light Theme")
            }
            Button({
                classes(if (selectedTab == "dark") Styles.button else Styles.outlineButton)
                style {
                    marginLeft(0.5.r)
                }
                onClick {
                    selectedTab = "dark"
                }
            }) {
                Text("Dark Theme")
            }

            // AI button for theme generation
            var isGeneratingTheme by remember { mutableStateOf(false) }
            val scope = rememberCoroutineScope()

            IconButton(
                name = "auto_fix_high",
                title = "Generate theme with AI",
                isLoading = isGeneratingTheme,
                styles = {
                    marginLeft(0.5.r)
                },
                circular = true,
                onClick = {
                    scope.launch {
                        generateThemeWithAi(
                            isLoading = { loading -> isGeneratingTheme = loading },
                            defaultValue = "",
                            onThemeGenerated = { lightTheme: LightTheme, darkTheme: DarkTheme ->
                                // Update theme colors
                                lightBackground = lightTheme.background ?: ""
                                lightPrimary = lightTheme.primary ?: ""
                                lightSecondary = lightTheme.secondary ?: ""
                                lightTertiary = lightTheme.tertiary ?: ""
                                lightRed = lightTheme.red ?: ""
                                lightGreen = lightTheme.green ?: ""
                                lightOutline = lightTheme.outline ?: ""
                                lightSurface = lightTheme.surface ?: ""
                                lightWhite = lightTheme.white ?: ""
                                lightBlack = lightTheme.black ?: ""
                                lightGray = lightTheme.gray ?: ""
                                lightLightgray = lightTheme.lightgray ?: ""
                                lightDarkgray = lightTheme.darkgray ?: ""

                                darkBackground = darkTheme.background ?: ""
                                darkPrimary = darkTheme.primary ?: ""
                                darkSecondary = darkTheme.secondary ?: ""
                                darkTertiary = darkTheme.tertiary ?: ""
                                darkRed = darkTheme.red ?: ""
                                darkGreen = darkTheme.green ?: ""
                                darkOutline = darkTheme.outline ?: ""
                                darkSurface = darkTheme.surface ?: ""
                                darkWhite = darkTheme.white ?: ""
                                darkBlack = darkTheme.black ?: ""
                                darkGray = darkTheme.gray ?: ""
                                darkLightgray = darkTheme.lightgray ?: ""
                                darkDarkgray = darkTheme.darkgray ?: ""
                            }
                        )
                    }
                }
            )
        }

        // Light theme editor
        if (selectedTab == "light") {
            Div {
                H4 {
                    Text("Light Theme Colors")
                }
                P {
                    Text("Leave fields empty to use default colors")
                }

                ColorPicker("Background Color", lightBackground) { lightBackground = it }
                ColorPicker("Surface Color", lightSurface) { lightSurface = it }
                ColorPicker("Primary Color", lightPrimary) { lightPrimary = it }
                ColorPicker("Secondary Color", lightSecondary) { lightSecondary = it }
                ColorPicker("Tertiary Color", lightTertiary) { lightTertiary = it }
                ColorPicker("Red Color", lightRed) { lightRed = it }
                ColorPicker("Green Color", lightGreen) { lightGreen = it }
                ColorPicker("Outline Color", lightOutline) { lightOutline = it }
                ColorPicker("White Color", lightWhite) { lightWhite = it }
                ColorPicker("Black Color", lightBlack) { lightBlack = it }
                ColorPicker("Gray Color", lightGray) { lightGray = it }
                ColorPicker("Light Gray Color", lightLightgray) { lightLightgray = it }
                ColorPicker("Dark Gray Color", lightDarkgray) { lightDarkgray = it }
            }
        }

        // Dark theme editor
        else {
            Div {
                H4 {
                    Text("Dark Theme Colors")
                }
                P {
                    Text("Leave fields empty to use light theme or default colors")
                }

                ColorPicker("Background Color", darkBackground) { darkBackground = it }
                ColorPicker("Surface Color", darkSurface) { darkSurface = it }
                ColorPicker("Primary Color", darkPrimary) { darkPrimary = it }
                ColorPicker("Secondary Color", darkSecondary) { darkSecondary = it }
                ColorPicker("Tertiary Color", darkTertiary) { darkTertiary = it }
                ColorPicker("Red Color", darkRed) { darkRed = it }
                ColorPicker("Green Color", darkGreen) { darkGreen = it }
                ColorPicker("Outline Color", darkOutline) { darkOutline = it }
                ColorPicker("White Color", darkWhite) { darkWhite = it }
                ColorPicker("Black Color", darkBlack) { darkBlack = it }
                ColorPicker("Gray Color", darkGray) { darkGray = it }
                ColorPicker("Light Gray Color", darkLightgray) { darkLightgray = it }
                ColorPicker("Dark Gray Color", darkDarkgray) { darkDarkgray = it }
            }
        }

        // Save button
        Button({
            classes(Styles.button)
            style {
                marginTop(1.r)
            }
            onClick {
                val lightTheme = LightTheme(
                    name = themeName,
                    background = lightBackground.takeIf { it.isNotEmpty() },
                    primary = lightPrimary.takeIf { it.isNotEmpty() },
                    secondary = lightSecondary.takeIf { it.isNotEmpty() },
                    tertiary = lightTertiary.takeIf { it.isNotEmpty() },
                    red = lightRed.takeIf { it.isNotEmpty() },
                    green = lightGreen.takeIf { it.isNotEmpty() },
                    outline = lightOutline.takeIf { it.isNotEmpty() },
                    surface = lightSurface.takeIf { it.isNotEmpty() },
                    white = lightWhite.takeIf { it.isNotEmpty() },
                    black = lightBlack.takeIf { it.isNotEmpty() },
                    gray = lightGray.takeIf { it.isNotEmpty() },
                    lightgray = lightLightgray.takeIf { it.isNotEmpty() },
                    darkgray = lightDarkgray.takeIf { it.isNotEmpty() }
                )

                val darkTheme = DarkTheme(
                    background = darkBackground.takeIf { it.isNotEmpty() },
                    surface = darkSurface.takeIf { it.isNotEmpty() },
                    primary = darkPrimary.takeIf { it.isNotEmpty() },
                    secondary = darkSecondary.takeIf { it.isNotEmpty() },
                    tertiary = darkTertiary.takeIf { it.isNotEmpty() },
                    red = darkRed.takeIf { it.isNotEmpty() },
                    green = darkGreen.takeIf { it.isNotEmpty() },
                    outline = darkOutline.takeIf { it.isNotEmpty() },
                    white = darkWhite.takeIf { it.isNotEmpty() },
                    black = darkBlack.takeIf { it.isNotEmpty() },
                    gray = darkGray.takeIf { it.isNotEmpty() },
                    lightgray = darkLightgray.takeIf { it.isNotEmpty() },
                    darkgray = darkDarkgray.takeIf { it.isNotEmpty() }
                )

                val newTheme = UserTheme(
                    name = themeName,
                    light = lightTheme,
                    dark = darkTheme
                )

                ThemeManager.saveTheme(newTheme)
                StyleManager.setTheme(newTheme)
                ThemeManager.setCurrentTheme(newTheme)
                onSave(newTheme)
            }
        }) {
            Text("Save Theme")
        }

        // Back button
        Button({
            classes(Styles.outlineButton)
            style {
                marginTop(1.r)
            }
            onClick {
                onDismiss()
            }
        }) {
            Span({
                classes("material-symbols-outlined")
            }) {
                Text("arrow_back")
            }
            Text(" Back to Themes")
        }
    }
}

/**
 * Data class for AI-generated theme colors
 */
@Serializable
data class AiGeneratedTheme(
    val light: AiGeneratedLightTheme,
    val dark: AiGeneratedDarkTheme
)

@Serializable
data class AiGeneratedLightTheme(
    val background: String? = null,
    val primary: String? = null,
    val secondary: String? = null,
    val tertiary: String? = null,
    val red: String? = null,
    val green: String? = null,
    val outline: String? = null,
    val surface: String? = null,
    val white: String? = null,
    val black: String? = null,
    val gray: String? = null,
    val lightgray: String? = null,
    val darkgray: String? = null
)

@Serializable
data class AiGeneratedDarkTheme(
    val background: String? = null,
    val primary: String? = null,
    val secondary: String? = null,
    val tertiary: String? = null,
    val red: String? = null,
    val green: String? = null,
    val outline: String? = null,
    val surface: String? = null,
    val white: String? = null,
    val black: String? = null,
    val gray: String? = null,
    val lightgray: String? = null,
    val darkgray: String? = null
)

/**
 * Function to generate theme colors using AI
 */
suspend fun generateThemeWithAi(
    isLoading: (Boolean) -> Unit,
    defaultValue: String = "",
    onThemeGenerated: (LightTheme, DarkTheme) -> Unit
) {
    // Show prompt dialog
    val prompt = inputDialog(
        title = "Generate Theme with AI",
        placeholder = "Describe your theme...",
        defaultValue = defaultValue
    ) ?: return

    // Set loading state
    isLoading(true)

    try {
        // Create JSON schema for theme colors
        val schema = buildJsonObject {
            putJsonObject("format") {
                put("type", "json_schema")
                put("name", "theme")
                put("description", "Theme colors for light and dark mode")
                put("strict", true)
                putJsonObject("schema") {
                    put("type", "object")
                    putJsonObject("properties") {
                        putJsonObject("light") {
                            put("type", "object")
                            putJsonObject("properties") {
                                putJsonObject("background") { put("type", "string") }
                                putJsonObject("primary") { put("type", "string") }
                                putJsonObject("secondary") { put("type", "string") }
                                putJsonObject("tertiary") { put("type", "string") }
                                putJsonObject("red") { put("type", "string") }
                                putJsonObject("green") { put("type", "string") }
                                putJsonObject("outline") { put("type", "string") }
                                putJsonObject("surface") { put("type", "string") }
                                putJsonObject("white") { put("type", "string") }
                                putJsonObject("black") { put("type", "string") }
                                putJsonObject("gray") { put("type", "string") }
                                putJsonObject("lightgray") { put("type", "string") }
                                putJsonObject("darkgray") { put("type", "string") }
                            }
                            putJsonArray("required") {
                                add(JsonPrimitive("background"))
                                add(JsonPrimitive("primary"))
                                add(JsonPrimitive("secondary"))
                                add(JsonPrimitive("tertiary"))
                                add(JsonPrimitive("red"))
                                add(JsonPrimitive("green"))
                                add(JsonPrimitive("outline"))
                                add(JsonPrimitive("surface"))
                                add(JsonPrimitive("white"))
                                add(JsonPrimitive("black"))
                                add(JsonPrimitive("gray"))
                                add(JsonPrimitive("lightgray"))
                                add(JsonPrimitive("darkgray"))
                            }
                            put("additionalProperties", false)
                        }
                        putJsonObject("dark") {
                            put("type", "object")
                            putJsonObject("properties") {
                                putJsonObject("background") { put("type", "string") }
                                putJsonObject("primary") { put("type", "string") }
                                putJsonObject("secondary") { put("type", "string") }
                                putJsonObject("tertiary") { put("type", "string") }
                                putJsonObject("red") { put("type", "string") }
                                putJsonObject("green") { put("type", "string") }
                                putJsonObject("outline") { put("type", "string") }
                                putJsonObject("surface") { put("type", "string") }
                                putJsonObject("white") { put("type", "string") }
                                putJsonObject("black") { put("type", "string") }
                                putJsonObject("gray") { put("type", "string") }
                                putJsonObject("lightgray") { put("type", "string") }
                                putJsonObject("darkgray") { put("type", "string") }
                            }
                            putJsonArray("required") {
                                add(JsonPrimitive("background"))
                                add(JsonPrimitive("primary"))
                                add(JsonPrimitive("secondary"))
                                add(JsonPrimitive("tertiary"))
                                add(JsonPrimitive("red"))
                                add(JsonPrimitive("green"))
                                add(JsonPrimitive("outline"))
                                add(JsonPrimitive("surface"))
                                add(JsonPrimitive("white"))
                                add(JsonPrimitive("black"))
                                add(JsonPrimitive("gray"))
                                add(JsonPrimitive("lightgray"))
                                add(JsonPrimitive("darkgray"))
                            }
                            put("additionalProperties", false)
                        }
                    }
                    putJsonArray("required") {
                        add(JsonPrimitive("light"))
                        add(JsonPrimitive("dark"))
                    }
                    put("additionalProperties", false)
                }
            }
        }

        // Call AI endpoint
        api.aiJson(
            request = AiJsonRequest(
                prompt = "Generate a color theme with the following requirements: $prompt. " +
                        "All colors should be valid CSS hex colors (e.g., #FF5733). " +
                        "For the light theme, use lighter colors that work well together. " +
                        "For the dark theme, use darker colors that complement the light theme.",
                schema = schema
            ),
            onError = {
                isLoading(false)
                dialog(
                    title = "Error",
                    confirmButton = "OK"
                ) {
                    P {
                        Text("Failed to generate theme")
                    }
                }
            },
            onSuccess = { response ->
                isLoading(false)
                try {
                    // Parse the JSON response
                    val themeData = Json.decodeFromString<AiGeneratedTheme>(response.json)

                    // Create light and dark themes
                    val lightTheme = LightTheme(
                        name = "AI Generated",
                        background = themeData.light.background,
                        primary = themeData.light.primary,
                        secondary = themeData.light.secondary,
                        tertiary = themeData.light.tertiary,
                        red = themeData.light.red,
                        green = themeData.light.green,
                        outline = themeData.light.outline,
                        surface = themeData.light.surface,
                        white = themeData.light.white,
                        black = themeData.light.black,
                        gray = themeData.light.gray,
                        lightgray = themeData.light.lightgray,
                        darkgray = themeData.light.darkgray
                    )

                    val darkTheme = DarkTheme(
                        background = themeData.dark.background,
                        primary = themeData.dark.primary,
                        secondary = themeData.dark.secondary,
                        tertiary = themeData.dark.tertiary,
                        red = themeData.dark.red,
                        green = themeData.dark.green,
                        outline = themeData.dark.outline,
                        surface = themeData.dark.surface,
                        white = themeData.dark.white,
                        black = themeData.dark.black,
                        gray = themeData.dark.gray,
                        lightgray = themeData.dark.lightgray,
                        darkgray = themeData.dark.darkgray
                    )

                    // Call the callback with the generated themes
                    onThemeGenerated(lightTheme, darkTheme)
                } catch (e: Exception) {
                    dialog(
                        title = "Error",
                        confirmButton = "OK"
                    ) {
                        P {
                            Text("Failed to parse theme data")
                        }
                    }
                }
            }
        )
    } catch (e: Exception) {
        isLoading(false)
        dialog(
            title = "Error",
            confirmButton = "OK"
        ) {
            P {
                Text("An error occurred")
            }
        }
    }
}

/**
 * A composable function that displays a color picker.
 */
@Composable
fun ColorPicker(label: String, color: String, onColorChanged: (String) -> Unit) {
    Div({
        style {
            display(DisplayStyle.Flex)
            alignItems(AlignItems.Center)
            marginBottom(0.5.r)
        }
    }) {
        // Label
        Div({
            style {
                width(12.r)
                fontWeight("bold")
                flexShrink(0)
            }
        }) {
            Text(label)
        }

        // Color picker input
        Input(InputType.Color) {
            if (color.isNotEmpty()) {
                value(color)
            }
            style {
                marginRight(1.r)
                width(40.px)
                height(40.px)
                padding(0.px)
                border(1.px, LineStyle.Solid, Styles.colors.lightgray)
                borderRadius(4.px)
            }
            onInput { event ->
                onColorChanged(event.value)
            }
        }

        // Color input field
        Input(InputType.Text) {
            classes(Styles.textarea)
            value(color)
            placeholder("Default")
            onInput { event ->
                onColorChanged(event.value)
            }
            style {
                flex(1)
            }
        }
    }
}
