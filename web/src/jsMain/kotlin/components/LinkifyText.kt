package components

import androidx.compose.runtime.Composable
import org.jetbrains.compose.web.attributes.ATarget
import org.jetbrains.compose.web.attributes.target
import org.jetbrains.compose.web.dom.A
import org.jetbrains.compose.web.dom.Span
import org.jetbrains.compose.web.dom.Text

val autolinkPattern = "((https|http)://)?[a-zA-Z0-9@:.-]+(\\.[a-zA-Z0-9@%:-]{2,}){1,3}(#?/?[a-zA-Z0-9-@%:_#z?=&./,()]+)*".toRegex()

fun Regex.splitInclusive(input: String): List<Pair<String, Boolean>> {
    return buildList {
        findAll(input).let { matches ->
            var previousMatch: MatchResult? = null
            matches.forEachIndexed { index, match ->
                if (previousMatch != null) {
                    if (match.range.first != previousMatch!!.range.last) {
                        add(input.substring(previousMatch!!.range.last + 1 until match.range.first) to false)
                    }
                } else if (match.range.first > 0) {
                    add(input.take(match.range.first) to false)
                }

                add(match.value to true)
                previousMatch = match
            }

            previousMatch?.let { match ->
                if (match.range.last < input.lastIndex) {
                    add(input.takeLast(input.lastIndex - match.range.last) to false)
                }
            }
        }

        if (isEmpty()) {
            return listOf(input to false)
        }
    }
}

@Composable
fun LinkifyText(text: String) {
    autolinkPattern.splitInclusive(text).forEach { part ->
        if (part.second) {
            A(
                href = part.first.let {
                    when {
                        it.contains("@") && !it.contains("/") -> "mailto:$it"
                        it.contains("://") -> it
                        else -> "https://$it"
                    }
                }, {
                    target(ATarget.Blank)
                }) {
                Text(part.first)
            }
        } else {
            Span {
                Text(part.first)
            }
        }
    }
}
