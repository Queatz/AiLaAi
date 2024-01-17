import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

val notifications = Notifications()

class Notification(
    val icon: String,
    val title: String,
    val message: String,
    val onClick: () -> Unit
)

class Notifications {
    private val _notifications = MutableStateFlow(emptyList<Notification>())

    val notifications = _notifications.asStateFlow()

    fun add(notification: Notification) {
        _notifications.update {
            it + notification
        }
    }

    fun remove(notification: Notification) {
        _notifications.update {
            it - notification
        }
    }
}
