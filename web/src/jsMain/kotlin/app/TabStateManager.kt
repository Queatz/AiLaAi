package app

import json
import kotlinx.browser.localStorage
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import org.w3c.dom.get
import org.w3c.dom.set

/**
 * Singleton instance of TabStateManager
 */
val tabStateManager = TabStateManager()

/**
 * Manages tab-specific state in localStorage
 */
class TabStateManager {
    private val TAB_STATE_PREFIX = "app.tab.state."

    /**
     * Generates a unique ID for a tab
     */
    fun generateTabId(): String = (0 until 8).joinToString("") { 
        kotlin.random.Random.nextInt(35).toString(36) 
    }

    /**
     * Saves state for a specific tab ID
     */
    fun saveState(tabId: String, state: AppPageState) {
        localStorage[TAB_STATE_PREFIX + tabId] = json.encodeToString(state)
    }

    /**
     * Loads state for a specific tab ID
     */
    fun loadState(tabId: String): AppPageState? {
        val stateJson = localStorage[TAB_STATE_PREFIX + tabId] ?: return null
        return try {
            json.decodeFromString<AppPageState>(stateJson)
        } catch (e: Exception) {
            console.error("Failed to parse tab state", e)
            null
        }
    }

    /**
     * Cleans up old tab states (can be called periodically)
     */
    fun cleanupOldStates(maxAgeMs: Long = 7 * 24 * 60 * 60 * 1000) { // Default: 7 days
        val currentTime = kotlin.js.Date.now().toLong()
        val keysToRemove = mutableListOf<String>()

        for (i in 0 until localStorage.length) {
            val key = localStorage.key(i) ?: continue
            if (key.startsWith(TAB_STATE_PREFIX)) {
                try {
                    val state = json.decodeFromString<AppPageState>(localStorage[key] ?: continue)
                    if (currentTime - state.lastUpdated > maxAgeMs) {
                        keysToRemove.add(key)
                    }
                } catch (e: Exception) {
                    // If we can't parse it, it's probably corrupted, so remove it
                    keysToRemove.add(key)
                }
            }
        }

        keysToRemove.forEach { localStorage.removeItem(it) }
    }
}
