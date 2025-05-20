package app.game.editor

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import app.AppStyles
import components.Icon
import org.jetbrains.compose.web.css.marginLeft
import org.jetbrains.compose.web.css.px
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.Span
import org.jetbrains.compose.web.dom.Text

/**
 * Data class representing a tab with a name, optional icon, and content.
 */
data class TabInfo(
    val name: String,
    val icon: String? = null,
    val content: @Composable () -> Unit
)

/**
 * A reusable tab bar component for the game editor.
 * 
 * @param tabs List of TabInfo objects to display
 * @param initialSelectedIndex The initially selected tab index
 * @param onTabSelected Callback when a tab is selected
 */
@Composable
fun TabBar(
    tabs: List<TabInfo>,
    initialSelectedIndex: Int = 0,
    onTabSelected: (Int) -> Unit
) {
    var selectedTabIndex by remember { mutableStateOf(initialSelectedIndex) }

    Div({
        classes(AppStyles.editorTabContainer)
    }) {
        tabs.forEachIndexed { index, tabInfo ->
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
                if (tabInfo.icon != null) {
                    Icon(
                        name = tabInfo.icon,
                        title = tabInfo.name
                    )
                    Span({
                        style {
                            marginLeft(4.px)
                        }
                    }) {
                        Text(tabInfo.name)
                    }
                } else {
                    Text(tabInfo.name)
                }
            }
        }
    }
}
