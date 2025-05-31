package app.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import app.dialog.dialog
import kotlinx.browser.window
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.w3c.dom.events.Event
import kotlin.coroutines.suspendCoroutine

// Shared flow to intercept navigation events
private val _navigateInterceptor = MutableSharedFlow<NavigationAttempt>()
val navigateInterceptor = _navigateInterceptor.asSharedFlow()

// Flag to track if there are active prevention handlers
private var hasActivePreventionHandlers = false

data class NavigationAttempt(
    val proceed: (Boolean) -> Unit
)

/**
 * Emits a navigation attempt and waits for confirmation
 * @return true if navigation should proceed, false otherwise
 */
suspend fun checkNavigationAllowed(): Boolean {
    if (!hasActivePreventionHandlers) {
        return true
    }

    return suspendCoroutine { continuation ->
        val navigationAttempt = NavigationAttempt { proceed ->
            continuation.resumeWith(Result.success(proceed))
        }

        // Use MainScope to launch a coroutine that can emit to the flow
        MainScope().launch {
            _navigateInterceptor.emit(navigationAttempt)
        }
    }
}

/**
 * A composable that prevents navigation away from a page when there are unsaved changes.
 * Similar to Android's BackHandler, this doesn't wrap content but intercepts navigation events.
 * 
 * @param enabled Whether navigation prevention is enabled (typically when there are unsaved changes)
 */
@Composable
fun PreventNavigation(
    enabled: Boolean
) {
    // Update the active prevention handlers flag
    DisposableEffect(enabled) {
        if (enabled) {
            hasActivePreventionHandlers = true
        }

        onDispose {
            if (enabled) {
                hasActivePreventionHandlers = false
            }
        }
    }

    // Intercept browser navigation
    DisposableEffect(enabled) {
        val originalBeforeUnload = window.onbeforeunload

        if (enabled) {
            window.onbeforeunload = {
                // Standard way to show a confirmation dialog when closing the page
                // In Kotlin/JS, returning a non-null string will trigger the confirmation dialog
                ""
            }
        } else {
            window.onbeforeunload = originalBeforeUnload
        }

        onDispose {
            window.onbeforeunload = originalBeforeUnload
        }
    }

    // Intercept in-app navigation
    LaunchedEffect(enabled) {
        if (enabled) {
            navigateInterceptor.collectLatest { attempt ->
                val confirmed = dialog(
                    title = "Discard your changes?",
                    confirmButton = "Discard changes",
                    cancelButton = "Cancel"
                ) == true

                attempt.proceed(confirmed)
            }
        }
    }
}
