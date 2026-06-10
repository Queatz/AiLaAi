package components

import Styles
import androidx.compose.runtime.Composable
import com.queatz.db.Activity
import org.jetbrains.compose.web.css.*
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.Text
import r

@Composable
fun CardActivity(activity: Activity) {
    Div({
        classes(Styles.cardActivity)
    }) {
        activity.duration?.let {
            ActivityItem("schedule", "Duration: ${it / 1000 / 60} minutes")
        }
        val minAge = activity.minAge
        val maxAge = activity.maxAge
        if (minAge != null || maxAge != null) {
            val text = when {
                minAge != null && maxAge != null -> "Age: $minAge - $maxAge"
                minAge != null -> "Age: $minAge+"
                else -> "Age: up to $maxAge"
            }
            ActivityItem("face", text)
        }
        val minGroup = activity.minGroupSize
        val maxGroup = activity.maxGroupSize
        if (minGroup != null || maxGroup != null) {
            val text = when {
                minGroup != null && maxGroup != null -> "Group size: $minGroup - $maxGroup"
                minGroup != null -> "Group size: $minGroup+"
                else -> "Group size: up to $maxGroup"
            }
            ActivityItem("group", text)
        }
        activity.pets?.let { ActivityItem("pets", if (it) "Pets allowed" else "No pets") }
        activity.languages?.takeIf { it.isNotEmpty() }?.let {
            ActivityItem("language", "Languages: ${it.joinToString(", ")}")
        }
        activity.outdoors?.let { ActivityItem("nature", if (it) "Outdoors" else "Indoors") }
    }
}

@Composable
fun ActivityItem(iconName: String, text: String) {
    Div({
        style {
            display(DisplayStyle.Flex)
            alignItems(AlignItems.Center)
            gap(.5.r)
        }
    }) {
        Icon(iconName)
        Text(text)
    }
}
