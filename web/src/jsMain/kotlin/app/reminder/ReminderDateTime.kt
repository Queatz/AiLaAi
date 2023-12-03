package app.reminder

import Styles
import androidx.compose.runtime.Composable
import org.jetbrains.compose.web.attributes.disabled
import org.jetbrains.compose.web.css.*
import org.jetbrains.compose.web.dom.DateInput
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.TimeInput
import r

@Composable
fun ReminderDateTime(
    date: String,
    time: String,
    onDate: (String) -> Unit,
    onTime: (String) -> Unit,
    disabled: Boolean = false,
    styles: StyleScope.() -> Unit = {}
) {
    Div({
        style {
            display(DisplayStyle.Flex)
            styles()
        }
    }) {
        DateInput(date) {
            classes(Styles.dateTimeInput)

            style {
                marginRight(1.r)
                padding(1.r)
                flex(1)
            }

            onChange {
                onDate(it.value)
            }

            if (disabled) {
                disabled()
            }
        }

        TimeInput(time) {
            classes(Styles.dateTimeInput)

            style {
                padding(1.r)
            }

            onChange {
                onTime(it.value)
            }

            if (disabled) {
                disabled()
            }
        }
    }
}
