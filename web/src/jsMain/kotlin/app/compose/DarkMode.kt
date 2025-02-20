package app.compose

import androidx.compose.runtime.*
import kotlinx.browser.window

@Composable
fun rememberDarkMode(key: Any = Unit): Boolean {
    val isDarkMode = remember { mutableStateOf(false) }

    DisposableEffect(key) {
        // Check the current dark mode preference
        val darkModeMediaQuery = window.matchMedia("(prefers-color-scheme: dark)")
        isDarkMode.value = darkModeMediaQuery.matches

        // Listen for changes in the dark mode preference
        val listener: (dynamic) -> Unit = { event ->
            isDarkMode.value = event.matches
        }
        darkModeMediaQuery.addListener(listener)

        // Clean up the listener when the composable is disposed
        onDispose {
            darkModeMediaQuery.removeListener(listener)
        }
    }

    return isDarkMode.value
}
