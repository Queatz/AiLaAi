package components

import Styles
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import toDoubleOrNullAllowEmpty
import org.jetbrains.compose.web.attributes.AttrsScope
import org.jetbrains.compose.web.attributes.placeholder
import org.jetbrains.compose.web.css.StyleScope
import org.jetbrains.compose.web.dom.TextInput
import org.w3c.dom.HTMLInputElement
import kotlin.math.pow
import kotlin.math.round

/**
 * A text field component for number inputs that supports empty text and trailing periods.
 * 
 * @param value The current numeric value
 * @param onValueChange Callback that is triggered when the value changes
 * @param placeholder Placeholder text to display when the field is empty
 * @param decimals Number of decimal places to format the value (default is 3, 0 for integers)
 * @param attrs Additional attributes to apply to the TextInput
 * @param styleScope Additional styles to apply to the TextInput
 */
@Composable
fun NumberTextField(
    value: Number,
    onValueChange: (Number) -> Unit,
    placeholder: String = "",
    decimals: Int = 3,
    attrs: (AttrsScope<HTMLInputElement>.() -> Unit)? = null,
    styleScope: (StyleScope.() -> Unit)? = null
) {
    // Keep track of the string representation locally
    var textValue by remember(value) { 
        mutableStateOf(formatNumber(value, decimals))
    }

    TextInput(textValue) {
        classes(Styles.textarea)

        placeholder(placeholder)

        styleScope?.let { style(it) }

        attrs?.let { it() }

        onBlur {
            // Update the value if the text has changed
            if (textValue != value.toString()) {
                textValue = value.toString()
            }
        }

        onInput { event ->
            val inputValue = event.value

            // Allow empty input (will be treated as 0)
            if (inputValue.isEmpty()) {
                textValue = ""
                onValueChange(0)
                return@onInput
            }

            // Allow a single "." if decimals > 0
            if (inputValue == "." && decimals > 0) {
                textValue = "."
                onValueChange(0)
                return@onInput
            }

            // Handle trailing "." if decimals > 0
            if (decimals > 0 && inputValue.endsWith(".")) {
                val numericPart = inputValue.dropLast(1)
                if (numericPart.toDoubleOrNull() != null) {
                    textValue = inputValue
                    onValueChange(numericPart.toDouble())
                    return@onInput
                }
            }

            // Parse the input value as a double
            val number = inputValue.toDoubleOrNullAllowEmpty()

            if (number != null) {
                // For integers (decimals = 0), convert to Int
                val finalNumber = if (decimals == 0) number.toInt() else number

                // Update the value
                onValueChange(finalNumber)

                // Only update the text if it's different to avoid cursor jumping
                if (textValue != inputValue) {
                    textValue = inputValue
                }
            } else {
                // Keep the raw input if it's not a valid number
                textValue = inputValue
            }
        }
    }
}

/**
 * Formats a number according to the specified number of decimal places.
 */
private fun formatNumber(value: Number, decimals: Int): String {
    if (decimals == 0) {
        return value.toInt().toString()
    }

    val factor = 10.0.pow(decimals)
    val rounded = round(value.toDouble() * factor) / factor

    return rounded.toString().let {
        if (it.endsWith(".0")) it.removeSuffix(".0") else it
    }
}
