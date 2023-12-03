package app.components

import androidx.compose.runtime.*
import androidx.compose.web.attributes.SelectAttrsScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import onSelectedOptionsChange
import org.jetbrains.compose.web.attributes.selected
import org.jetbrains.compose.web.dom.Option
import org.jetbrains.compose.web.dom.Select
import org.jetbrains.compose.web.dom.Text

class MultiSelectScope(
    private val selected: List<String>,
    private val changes: Flow<List<String>>,
) {
    @Composable
    fun option(value: String, title: String) {
        var onValueChange by remember { mutableStateOf<(List<String>) -> Unit>({}) }

        LaunchedEffect(Unit) {
            changes.collectLatest {
                onValueChange(it)
            }
        }

        Option(value, {
            if (value in selected) {
                selected()
            }

            ref { element ->
                onValueChange = {
                    element.selected = value in it
                }

                onDispose {
                    onValueChange = {}
                }
            }
        }) { Text(title) }
    }
}

@Composable
fun MultiSelect(
    selected: List<String>,
    onSelected: (List<String>) -> Unit,
    attrs: SelectAttrsScope.() -> Unit = {},
    options: @Composable MultiSelectScope.() -> Unit
) {
    Select(
        {
            classes(Styles.dateTimeInput)

            onSelectedOptionsChange {
                onSelected(it)
            }

            attrs()
        },
        multiple = true
    ) {
        val changes = remember {
            MutableStateFlow(selected)
        }

        LaunchedEffect(selected) {
            changes.emit(selected)
        }

        MultiSelectScope(selected, changes).options()
    }
}
