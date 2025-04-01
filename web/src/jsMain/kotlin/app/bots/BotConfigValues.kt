package app.bots

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import app.components.TextBox
import com.queatz.db.BotConfigField
import com.queatz.db.BotConfigValue
import notBlank
import notEmpty
import org.jetbrains.compose.web.css.fontSize
import org.jetbrains.compose.web.css.margin
import org.jetbrains.compose.web.css.maxWidth
import org.jetbrains.compose.web.css.percent
import org.jetbrains.compose.web.css.width
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.Text
import r

@Composable fun BotConfigValues(
    config: List<BotConfigField>,
    values: List<BotConfigValue>? = null,
    onValues: (List<BotConfigValue>) -> Unit
) {
    var values by remember {
        mutableStateOf(values ?: emptyList())
    }

    config.notEmpty?.let { config ->
        config.forEach { field ->
            var value by remember {
                mutableStateOf(values.firstOrNull { value -> field.key == value.key }?.value ?: "")
            }

            LaunchedEffect(value) {
                values = values.filter {
                    it.key != field.key
                } + BotConfigValue(key = field.key, value = value)

                onValues(values)
            }

            field.label?.notBlank?.let {
                Div({
                    style {
                        fontSize(120.percent)
                    }
                }) {
                    Text(it)
                }
            }

            TextBox(
                value = value,
                onValue = { value = it },
                placeholder = field.placeholder ?: "",
                styles = {
                    margin(0.r)
                    maxWidth(100.percent)
                }
            )
        }
    }
}
