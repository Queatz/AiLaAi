package app.bots

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import app.components.TextBox
import com.queatz.db.BotConfigField
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

@Composable fun BotConfigValues(config: List<BotConfigField>) {
    config.notEmpty?.let { config ->
        config.forEach {
            var value by remember {
                mutableStateOf("")
            }
            it.label?.notBlank?.let {
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
                placeholder = it.placeholder ?: "",
                styles = {
                    margin(0.r)
                    width(32.r)
                    maxWidth(100.percent)
                }
            )
        }
    }
}
