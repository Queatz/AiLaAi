package app.theme

import StyleManager
import kotlinx.browser.localStorage
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

// Base interface for theme colors
interface ThemeColors {
    val background: String?
    val primary: String?
    val secondary: String?
    val tertiary: String?
    val red: String?
    val green: String?
    val outline: String?
    val surface: String?
    val white: String?
    val black: String?
    val gray: String?
    val lightgray: String?
    val darkgray: String?
}

// Light theme implementation
@Serializable
data class LightTheme(
    val name: String,
    override val background: String? = null,
    override val primary: String? = null,
    override val secondary: String? = null,
    override val tertiary: String? = null,
    override val red: String? = null,
    override val green: String? = null,
    override val outline: String? = null,
    override val surface: String? = null,
    override val white: String? = null,
    override val black: String? = null,
    override val gray: String? = null,
    override val lightgray: String? = null,
    override val darkgray: String? = null
) : ThemeColors

// Dark theme implementation that can fall back to light theme
@Serializable
data class DarkTheme(
    override val background: String? = null,
    override val primary: String? = null,
    override val secondary: String? = null,
    override val tertiary: String? = null,
    override val red: String? = null,
    override val green: String? = null,
    override val outline: String? = null,
    override val surface: String? = null,
    override val white: String? = null,
    override val black: String? = null,
    override val gray: String? = null,
    override val lightgray: String? = null,
    override val darkgray: String? = null
) : ThemeColors

// Complete user theme with both light and dark variants
@Serializable
data class UserTheme(
    val name: String,
    val light: LightTheme,
    val dark: DarkTheme
) {
    // Constructor that creates a theme with just light colors
    constructor(lightTheme: LightTheme) : this(
        name = lightTheme.name,
        light = lightTheme,
        dark = DarkTheme()
    )
}

object ThemeManager {
    private const val CURRENT_THEME_KEY = "current_theme"
    private const val USER_THEMES_KEY = "user_themes"

    private val json = Json {
        encodeDefaults = true
        isLenient = true
        ignoreUnknownKeys = true
        explicitNulls = false
    }

    fun saveTheme(theme: UserTheme) {
        // Save to localStorage as JSON
        val themesList = getAllThemes().toMutableList()
        val existingIndex = themesList.indexOfFirst { it.name == theme.name }

        if (existingIndex >= 0) {
            themesList[existingIndex] = theme
        } else {
            themesList.add(theme)
        }

        localStorage.setItem(USER_THEMES_KEY, json.encodeToString(themesList))
    }

    fun deleteTheme(themeName: String) {
        val themesList = getAllThemes().toMutableList()
        val currentTheme = getCurrentTheme()

        // Remove the theme from the list
        val filteredList = themesList.filter { it.name != themeName }.toMutableList()

        // Update localStorage
        localStorage.setItem(USER_THEMES_KEY, json.encodeToString(filteredList))

        // If the deleted theme was the current theme, reset to default
        if (currentTheme?.name == themeName) {
            setCurrentTheme(null)
            StyleManager.setTheme(null)
        }
    }

    fun getCurrentTheme(): UserTheme? {
        val themeName = localStorage.getItem(CURRENT_THEME_KEY) ?: return null
        return getAllThemes().find { it.name == themeName }
    }

    fun setCurrentTheme(theme: UserTheme?) {
        if (theme == null) {
            localStorage.removeItem(CURRENT_THEME_KEY)
        } else {
            localStorage.setItem(CURRENT_THEME_KEY, theme.name)
        }
    }

    fun getAllThemes(): List<UserTheme> {
        val themesJson = localStorage.getItem(USER_THEMES_KEY) ?: return emptyList()
        return try {
            json.decodeFromString<List<UserTheme>>(themesJson)
        } catch (e: Exception) {
            emptyList()
        }
    }
}
