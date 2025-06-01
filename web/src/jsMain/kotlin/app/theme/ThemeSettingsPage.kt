package app.theme

import Styles
import androidx.compose.runtime.*
import app.FullPageLayout
import app.components.Spacer
import org.jetbrains.compose.web.css.*
import org.jetbrains.compose.web.dom.*
import r

/**
 * A composable function that displays the theme settings page.
 */
@Composable
fun ThemeSettingsPage() {
    var showEditor by remember { mutableStateOf(false) }
    var themeToEdit by remember { mutableStateOf<UserTheme?>(null) }

    FullPageLayout(useVh = true) {
        H2 {
            Text("Theme Settings")
        }

        P {
            Text("Customize the appearance of the application by selecting or creating a theme.")
        }

        if (showEditor) {
            ThemeEditor(
                initialTheme = themeToEdit,
                onDismiss = {
                    showEditor = false
                    themeToEdit = null
                }
            ) { newTheme ->
                // Update the themes list after saving
                showEditor = false
                themeToEdit = null
            }
        } else {
            ThemeSelector(
                onEditTheme = { theme ->
                    themeToEdit = theme
                    showEditor = true
                }
            )

            // Create new theme button
            Button({
                classes(Styles.button)
                style {
                    marginTop(1.r)
                }
                onClick {
                    showEditor = true
                    themeToEdit = null
                }
            }) {
                Span({
                    classes("material-symbols-outlined")
                }) {
                    Text("add")
                }
                Text(" Create New Theme")
            }
        }

        Spacer()
    }
}
