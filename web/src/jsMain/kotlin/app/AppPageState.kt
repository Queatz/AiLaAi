package app

import app.nav.*
import app.page.ScheduleView
import app.page.ScheduleViewType
import app.platform.PlatformNav
import app.scripts.ScriptsNav
import com.queatz.db.Reminder
import kotlinx.serialization.Serializable

/**
 * Comprehensive serializable state class for AppPage
 */
@Serializable
data class AppPageState(
    // Navigation state
    val nav: NavPage = NavPage.Groups,

    // Navigation states for different sections
    val groupNav: GroupNav = GroupNav.None,
    val cardNav: CardNav = CardNav.Map,
    val storyNav: StoryNav = StoryNav.Friends,
    val platformNav: PlatformNav = PlatformNav.None,
    val scriptsNav: ScriptsNav = ScriptsNav.None,
    val sceneNav: SceneNav = SceneNav.None,

    // Schedule related states
    val scheduleView: ScheduleView = ScheduleView.Daily,
    val scheduleViewType: ScheduleViewType = ScheduleViewType.Schedule,
    val reminderSearch: String? = null,
    val reminder: Reminder? = null,

    // UI state
    val sideLayoutVisible: Boolean = true,

    // Metadata
    val lastUpdated: Long = kotlin.js.Date.now().toLong()
)
