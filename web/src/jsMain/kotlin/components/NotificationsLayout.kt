package components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import app.AppStyles
import notifications
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.Text
import shadow

@Composable
fun NotificationsLayout() {
    val activeNotifications by notifications.notifications.collectAsState()

    if (activeNotifications.isNotEmpty()) {
        Div({
            classes(AppStyles.notificationsLayout)
        }) {
            activeNotifications.forEach { notification ->
                Div({
                    classes(AppStyles.notification)

                    onClick {
                        notifications.remove(notification)
                        notification.onClick()
                    }
                }) {
                    if (notification.icon != null) {
                        Div({
                            classes(AppStyles.notificationIcon)
                        }) {
                            IconButton(notification.icon, "") {
                                notifications.remove(notification)
                                notification.onClick()
                            }
                        }
                    }
                    Div({
                        classes(AppStyles.notificationBody)
                    }) {
                        Div(
                            {
                                classes(AppStyles.notificationTitle)
                            }
                        ) {
                            Text(notification.title)
                        }
                        Div({
                            classes(AppStyles.notificationMessage)
                        }) {
                            Text(notification.message)
                        }
                    }
                    Div({
                        classes(AppStyles.notificationActions)
                    }) {
                        // todo translate
                        IconButton("close", "Dismiss") {
                            notifications.remove(notification)
                            notification.onDismiss()
                        }
                    }
                }
            }

            if (activeNotifications.size >= 3) {
                IconButton(
                    // todo: translate
                    name = "mop",
                    // todo: translate
                    title = "Clear all",
                    background = true,
                    styles = {
                        shadow(elevation = 2)
                    }
                ) {
                    notifications.clearAll()
                }
            }
        }
    }
}
