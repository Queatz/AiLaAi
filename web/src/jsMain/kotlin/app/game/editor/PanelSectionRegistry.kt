package app.game.editor

/**
 * Registry for PanelSection components to communicate with each other.
 * This allows implementing features like "close all other panels when Ctrl+clicking a panel header".
 */
object PanelSectionRegistry {
    // List of callbacks to be called when a panel is Ctrl+clicked
    private val closeListeners = mutableListOf<() -> Unit>()
    
    /**
     * Add a listener to be notified when other panels should be closed
     */
    fun addCloseListener(callback: () -> Unit) {
        closeListeners.add(callback)
    }
    
    /**
     * Remove a listener
     */
    fun removeCloseListener(callback: () -> Unit) {
        closeListeners.remove(callback)
    }
    
    /**
     * Notify all registered panels that they should close if they're not the active one
     */
    fun notifyCloseOtherPanels() {
        closeListeners.forEach { it() }
    }
}
