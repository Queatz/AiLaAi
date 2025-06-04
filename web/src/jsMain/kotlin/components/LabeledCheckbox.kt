package components

import Styles
import androidx.compose.runtime.Composable
import org.jetbrains.compose.web.attributes.disabled
import org.jetbrains.compose.web.css.DisplayStyle
import org.jetbrains.compose.web.css.StyleScope
import org.jetbrains.compose.web.css.backgroundColor
import org.jetbrains.compose.web.css.display
import org.jetbrains.compose.web.css.gap
import org.jetbrains.compose.web.dom.CheckboxInput
import org.jetbrains.compose.web.dom.Label
import org.jetbrains.compose.web.dom.Text
import r

@Composable
fun LabeledCheckbox(
    value: Boolean,
    onValue: (Boolean) -> Unit,
    text: String,
    enabled: Boolean = true,
    styles: StyleScope.() -> Unit = {},
) {
    Label(attrs = {
        style {
            display(DisplayStyle.Flex)
            gap(.5.r)
            styles()
        }
    }) {
        CheckboxInput(value) {
            onChange {
                onValue(it.value)
            }

            if (!enabled) {
                disabled()
            }

            style {
                property("accent-color", Styles.colors.primary)
                backgroundColor(Styles.colors.background)
                property("scale", "1.125")
            }
        }
        Text(text)
    }
}
