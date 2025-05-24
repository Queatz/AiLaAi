package app.compose

import androidx.compose.runtime.*
import kotlinx.browser.window

@Composable
fun rememberMobileMode(key: Any = Unit): Boolean {
    val isMobileMode = remember { mutableStateOf(false) }

    DisposableEffect(key) {
        // Check if the screen width is mobile-sized (max-width: 640px)
        val mobileModeMediaQuery = window.matchMedia("(max-width: 640px)")
        isMobileMode.value = mobileModeMediaQuery.matches

        // Listen for changes in the screen size
        val listener: (dynamic) -> Unit = { event ->
            isMobileMode.value = event.matches
        }
        mobileModeMediaQuery.addListener(listener)

        // Clean up the listener when the composable is disposed
        onDispose {
            mobileModeMediaQuery.removeListener(listener)
        }
    }

    return isMobileMode.value
}
