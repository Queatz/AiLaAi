package app.game.editor

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import app.AppStyles
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.Text

/**
 * A reusable tab bar component for the game editor.
 * 
 * @param tabs List of tab names to display
 * @param initialSelectedIndex The initially selected tab index
 * @param onTabSelected Callback when a tab is selected
 */
@Composable
fun TabBar(
    tabs: List<String>,
    initialSelectedIndex: Int = 0,
    onTabSelected: (Int) -> Unit
) {
    var selectedTabIndex by remember { mutableStateOf(initialSelectedIndex) }
    
    Div({
        classes(AppStyles.editorTabContainer)
    }) {
        tabs.forEachIndexed { index, tabName ->
            Div({
                classes(AppStyles.editorTab)
                if (selectedTabIndex == index) {
                    classes(AppStyles.editorTabSelected)
                }
                onClick { 
                    selectedTabIndex = index
                    onTabSelected(index)
                }
            }) {
                Text(tabName)
            }
        }
    }
}
