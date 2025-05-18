package app.game.editor

import Styles
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import app.nav.NavSearchInput
import components.IconButton
import components.Loading
import org.jetbrains.compose.web.css.marginBottom
import org.jetbrains.compose.web.css.marginTop
import org.jetbrains.compose.web.css.padding
import org.jetbrains.compose.web.css.percent
import org.jetbrains.compose.web.css.width
import org.jetbrains.compose.web.dom.Button
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.Text
import r

/**
 * A generic section for displaying and managing assets (tiles, objects, etc.)
 * 
 * @param T The type of asset to display, must implement the Asset interface
 * @param title The title of the section
 * @param icon The icon to display next to the title
 * @param assets The list of assets to display
 * @param isLoading Whether the assets are currently loading
 * @param isCreating Whether an asset is currently being created
 * @param selectedAssetId The ID of the currently selected asset
 * @param onAssetSelected Callback when an asset is selected
 * @param onCreateAsset Callback when the create button is clicked
 * @param assetToTool Function to convert an asset to a Tool for display
 * @param searchFilter Function to filter assets based on a search query
 * @param createButtonText Text to display on the create button
 * @param emptyText Text to display when there are no assets
 * @param loadingText Text to display when assets are loading
 * @param processingText Text to display when an asset is being created
 * @param customContent Optional custom content to display instead of the default ToolGrid
 */
@Composable
fun <T : Asset> AssetSection(
    title: String,
    icon: String,
    assets: List<T>,
    isLoading: Boolean,
    isCreating: Boolean,
    selectedAssetId: String?,
    onAssetSelected: (T?) -> Unit,
    onCreateAsset: () -> Unit,
    assetToTool: (T) -> Tool,
    searchFilter: (T, String) -> Boolean,
    createButtonText: String = "Create New Asset",
    emptyText: String = "No assets yet. Create your first asset!",
    loadingText: String = "Loading assets...",
    processingText: String = "Processing asset...",
    customContent: (@Composable (List<T>) -> Unit)? = null
) {
    // Search functionality
    var showSearch by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }

    PanelSection(
        title = title,
        icon = icon,
        initiallyExpanded = true,
        closeOtherPanels = true
    ) {
        // Search and button container
        Div({
            style {
                marginBottom(1.r)
            }
        }) {
            // Search icon and input
            Div({
                style {
                    property("display", "flex")
                    property("align-items", "center")
                    marginBottom(1.r)
                }
            }) {
                if (showSearch) {
                    NavSearchInput(
                        value = searchQuery,
                        onChange = { searchQuery = it },
                        onDismissRequest = {
                            showSearch = false
                            searchQuery = ""
                        }
                    )
                } else {
                    IconButton(
                        name = "search",
                        title = "Search $title"
                    ) {
                        showSearch = true
                    }
                }
            }

            // Button to create a new asset
            Button({
                classes(Styles.button)

                style {
                    width(100.percent)
                    marginBottom(1.r)
                }
                onClick {
                    onCreateAsset()
                }
            }) {
                Text(createButtonText)
            }

            // Show loading indicator when creating an asset
            if (isCreating) {
                Div({
                    style {
                        padding(.5.r)
                        marginBottom(1.r)
                        property("text-align", "center")
                    }
                }) {
                    Loading()
                    Div({
                        style {
                            marginTop(.5.r)
                        }
                    }) {
                        Text(processingText)
                    }
                }
            }

            // Display assets in a grid
            if (isLoading) {
                Div({
                    style {
                        padding(1.r)
                        property("text-align", "center")
                    }
                }) {
                    Text(loadingText)
                }
            } else if (assets.isEmpty()) {
                Div({
                    style {
                        padding(1.r)
                        property("text-align", "center")
                    }
                }) {
                    Text(emptyText)
                }
            } else {
                // Filter assets based on search query
                val filteredAssets = if (searchQuery.isNotEmpty()) {
                    assets.filter { asset -> searchFilter(asset, searchQuery) }
                } else {
                    assets
                }

                // If custom content is provided, use it
                if (customContent != null) {
                    customContent(filteredAssets)
                } else {
                    // Otherwise, use the default ToolGrid
                    val assetTools = filteredAssets.map { asset -> assetToTool(asset) }

                    Toolbox {
                        ToolGrid(
                            tools = assetTools,
                            selectedToolId = selectedAssetId,
                            onToolSelected = { tool ->
                                // Find the selected asset and pass it to the callback
                                val selectedAsset = filteredAssets.find { it.id == tool.id }
                                onAssetSelected(selectedAsset)
                            }
                        )
                    }
                }
            }
        }
    }
}
