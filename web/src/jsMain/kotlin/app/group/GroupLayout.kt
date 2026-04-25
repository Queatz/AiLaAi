import androidx.compose.runtime.*
import app.ailaai.api.group
import app.group.GroupSidePanel
import app.group.GroupTopBar
import com.queatz.db.GroupExtended
import org.jetbrains.compose.web.css.*
import org.jetbrains.compose.web.dom.Div
import androidx.compose.runtime.movableContentOf

@Composable
fun GroupLayout(
    group: GroupExtended,
    onGroupUpdated: () -> Unit,
    onGroupGone: () -> Unit
) {
     var showCards by remember(group.group?.id) {
        mutableStateOf(false)
    }
    var showSearch by remember(group.group?.id) { mutableStateOf(false) }
    var sidePanelOnLeft by remember { mutableStateOf(false) }
    var showSidePanel by remember(group.group?.id) { mutableStateOf(!group.group?.content.isNullOrBlank()) }

    val currentOnSwap by rememberUpdatedState { sidePanelOnLeft = !sidePanelOnLeft }
    val currentOnClose by rememberUpdatedState { showSidePanel = false }
    val currentOnGroupUpdated by rememberUpdatedState(onGroupUpdated)

    val sidePanel = remember {
        movableContentOf { group: GroupExtended, isSwapped: Boolean ->
            GroupSidePanel(
                group,
                isSwapped = isSwapped,
                onSwap = { currentOnSwap() },
                onClose = { currentOnClose() }
            ) {
                currentOnGroupUpdated()
            }
        }
    }

    application.background(
        group.group?.background?.let { "$baseUrl$it" },
        group.group?.config?.backgroundOpacity ?: 1f
    )
    application.effects(group.group?.config?.effects?.let { json.decodeFromString(it) })

    LaunchedEffect(group.group?.id) {
        group.group?.id?.let { groupId ->
            // Mark group as read
            api.group(groupId) {}
        }
    }

    Div({
        style {
            display(DisplayStyle.Flex)
            flexDirection(FlexDirection.Row)
            flex(1)
            height(100.percent)
            width(100.percent)
        }
    }) {
        if (showSidePanel && sidePanelOnLeft) {
            sidePanel(group, true)
        }

        Div({
            if (showSidePanel && sidePanelOnLeft) {
                classes(Styles.sidePane)
            }
            style {
                display(DisplayStyle.Flex)
                flexDirection(FlexDirection.ColumnReverse)
                if (!showSidePanel || !sidePanelOnLeft) {
                    flex(1)
                    width(0.r)
                }
                height(100.percent)
            }
        }) {
            if (showCards) {
                GroupCards(group = group)
            } else {
                GroupMessages(
                    group = group,
                    showSearch = showSearch,
                    onShowSearch = {
                        showSearch = it
                    },
                    onGroupUpdated = onGroupUpdated,
                    onShowSidePanel = {
                        showSidePanel = true
                    }
                )
            }

            GroupTopBar(
                group = group,
                onGroupUpdated = onGroupUpdated,
                onGroupGone = onGroupGone,
                showCards = showCards,
                onShowCards = {
                    showCards = !showCards
                },
                onSearch = {
                    showSearch = !showSearch
                },
                showSidePanel = showSidePanel,
                onShowSidePanel = {
                    showSidePanel = !showSidePanel
                }
            )
        }

        if (showSidePanel && !sidePanelOnLeft) {
            sidePanel(group, false)
        }
    }
}
