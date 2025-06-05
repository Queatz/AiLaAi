package app

import app.nav.*
import app.page.ScheduleView
import app.page.ScheduleViewType
import app.platform.PlatformNav
import app.scripts.ScriptsNav
import com.queatz.db.Reminder
import kotlinx.browser.window
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.serialization.Serializable

/**
 * Manages the state of AppPage and handles saving/loading from localStorage
 */
object AppPageStateManager {
    private val _state = MutableStateFlow(AppPageState())
    val state: StateFlow<AppPageState> = _state.asStateFlow()

    /**
     * Updates the state with the given values
     */
    fun updateState(update: (AppPageState) -> AppPageState) {
        _state.update(update)
    }

    /**
     * Creates an AppPageState from the individual state variables
     */
    fun createState(
        nav: NavPage,
        group: GroupNav,
        card: CardNav,
        story: StoryNav,
        platform: PlatformNav,
        script: ScriptsNav,
        scene: SceneNav,
        scheduleView: ScheduleView,
        scheduleViewType: ScheduleViewType,
        reminderSearch: String?,
        reminder: Reminder?,
        sideLayoutVisible: Boolean
    ): AppPageState {
        return AppPageState(
            nav = nav,
            groupNav = group,
            cardNav = card,
            storyNav = story,
            platformNav = platform,
            scriptsNav = script,
            sceneNav = scene,
            scheduleView = scheduleView,
            scheduleViewType = scheduleViewType,
            reminderSearch = reminderSearch,
            reminder = reminder,
            sideLayoutVisible = sideLayoutVisible
        )
    }

    /**
     * Saves the given state to localStorage for the given tab ID
     */
    fun saveState(tabId: String, state: AppPageState) {
        tabStateManager.saveState(tabId, state)
    }

    /**
     * Loads the state from localStorage for the given tab ID
     */
    fun loadState(tabId: String): AppPageState? {
        return tabStateManager.loadState(tabId)
    }

    /**
     * Sets up a beforeunload event listener to save state when the tab is closed
     * Returns the handler function so it can be removed later if needed
     */
    fun setupBeforeUnloadHandler(tabId: String, getState: () -> AppPageState): (dynamic) -> dynamic {
        val beforeUnloadHandler: (dynamic) -> dynamic = { event ->
            // Save state one final time before unloading
            saveState(tabId, getState())

            // Allow the page to unload normally
            undefined
        }

        window.addEventListener("beforeunload", beforeUnloadHandler)

        return beforeUnloadHandler
    }
}
