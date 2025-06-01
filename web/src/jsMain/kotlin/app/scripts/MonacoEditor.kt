package app.scripts

import Styles
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import app.compose.rememberDarkMode
import kotlinx.coroutines.flow.MutableSharedFlow
import lib.Monaco
import lib.jsObject
import org.jetbrains.compose.web.css.StyleScope
import org.jetbrains.compose.web.css.borderRadius
import org.jetbrains.compose.web.css.overflow
import org.jetbrains.compose.web.dom.Div
import r

/**
 * Finds the longest common subsequence of two lists.
 * This is used to identify added, deleted, and modified lines in a diff-like manner.
 */
private fun <T> longestCommonSubsequence(a: List<T>, b: List<T>): List<T> {
    val lengths = Array(a.size + 1) { IntArray(b.size + 1) }

    // Fill the lengths array
    for (i in a.indices) {
        for (j in b.indices) {
            lengths[i + 1][j + 1] = if (a[i] == b[j]) {
                lengths[i][j] + 1
            } else {
                maxOf(lengths[i + 1][j], lengths[i][j + 1])
            }
        }
    }

    // Backtrack to find the LCS
    val result = mutableListOf<T>()
    var i = a.size
    var j = b.size

    while (i > 0 && j > 0) {
        if (a[i - 1] == b[j - 1]) {
            result.add(0, a[i - 1])
            i--
            j--
        } else if (lengths[i - 1][j] > lengths[i][j - 1]) {
            i--
        } else {
            j--
        }
    }

    return result
}

class MonacoEditorState {
    internal val onSetValue = MutableSharedFlow<String>()

    suspend fun setValue(value: String) {
        onSetValue.emit(value)
    }
}

@Composable
fun MonacoEditor(
    state: MonacoEditorState = remember { MonacoEditorState() },
    initialValue: String = "",
    onValueChange: (String) -> Unit = {},
    onIsEdited: (Boolean) -> Unit = {},
    styles: StyleScope.() -> Unit = {},
) {
    var editor: Monaco.IEditor? by remember { mutableStateOf(null) }
    val darkMode = rememberDarkMode()
    var currentValue by remember(initialValue) { mutableStateOf(initialValue) }
    var originalLines by remember(initialValue) { mutableStateOf(initialValue.lines()) }
    var decorationIds by remember { mutableStateOf(emptyArray<String>()) }

    LaunchedEffect(currentValue) {
        onValueChange(currentValue)
        onIsEdited(currentValue.trimEnd() != initialValue.trimEnd())

        // Compare lines and identify changes
        val newLines = currentValue.lines()
        val addedLineNumbers = mutableListOf<Int>()
        val deletedLineNumbers = mutableListOf<Int>()
        val modifiedLineNumbers = mutableListOf<Int>()

        // Use a diff-like algorithm to identify added, deleted, and modified lines
        // First, identify common lines using longest common subsequence
        val lcs = longestCommonSubsequence(originalLines, newLines)

        // Create mappings to track line correspondences
        val origToNew = mutableMapOf<Int, Int>()
        val newToOrig = mutableMapOf<Int, Int>()

        // Identify matching lines
        var origIndex = 0
        var newIndex = 0
        var lcsIndex = 0

        while (origIndex < originalLines.size || newIndex < newLines.size) {
            // Both lines match a common subsequence line
            if (origIndex < originalLines.size && newIndex < newLines.size && 
                lcsIndex < lcs.size && originalLines[origIndex] == lcs[lcsIndex] && 
                newLines[newIndex] == lcs[lcsIndex]) {
                // These lines match, so they're neither added nor deleted
                origToNew[origIndex] = newIndex
                newToOrig[newIndex] = origIndex
                origIndex++
                newIndex++
                lcsIndex++
            } 
            // Line exists in original but not in new (deleted or modified)
            else if (origIndex < originalLines.size && 
                    (lcsIndex >= lcs.size || originalLines[origIndex] != lcs[lcsIndex])) {
                // This line was in the original but not in the new version
                deletedLineNumbers.add(newIndex + 1) // Monaco uses 1-based line numbers
                origIndex++
            } 
            // Line exists in new but not in original (added or modified)
            else if (newIndex < newLines.size && 
                    (lcsIndex >= lcs.size || newLines[newIndex] != lcs[lcsIndex])) {
                // This line is in the new version but not in the original
                addedLineNumbers.add(newIndex + 1) // Monaco uses 1-based line numbers
                newIndex++
            }
        }

        // Identify modified lines by looking for pairs of deleted and added lines
        // that are close to each other in their respective versions
        val deletedIndices = (0 until originalLines.size).filter { it !in origToNew }
        val addedIndices = (0 until newLines.size).filter { it !in newToOrig }

        // For each deleted line, check if there's a nearby added line
        for (deletedIdx in deletedIndices) {
            // Find the position where this line would be in the new version
            val expectedNewIdx = origToNew.entries
                .filter { it.key < deletedIdx }
                .maxByOrNull { it.key }
                ?.value?.plus(1) ?: 0

            // Look for added lines near this expected position
            val nearbyAddedIndices = addedIndices.filter { 
                kotlin.math.abs(it - expectedNewIdx) <= 3 // Within 3 lines of expected position
            }

            if (nearbyAddedIndices.isNotEmpty()) {
                // Find the closest added line
                val closestAddedIdx = nearbyAddedIndices.minByOrNull { 
                    kotlin.math.abs(it - expectedNewIdx) 
                } ?: continue

                // Mark this as a modified line
                modifiedLineNumbers.add(closestAddedIdx + 1) // Monaco uses 1-based line numbers

                // Remove it from the added lines
                addedLineNumbers.remove(closestAddedIdx + 1)

                // Remove the corresponding deleted line
                deletedLineNumbers.remove(expectedNewIdx + 1)
            }
        }

        // Create decorations for each type of line change
        val decorations = mutableListOf<Monaco.LineDecoration>()

        // Add decorations for added lines
        addedLineNumbers.distinct().forEach { lineNumber ->
            decorations.add(jsObject {
                range = jsObject {
                    startLineNumber = lineNumber
                    startColumn = 1
                    endLineNumber = lineNumber
                    endColumn = 1
                }
                options = jsObject {
                    isWholeLine = true
                    className = Styles.addedLine
                }
            })
        }

        // Add decorations for deleted lines
        deletedLineNumbers.distinct().forEach { lineNumber ->
            decorations.add(jsObject {
                range = jsObject {
                    startLineNumber = lineNumber
                    startColumn = 1
                    endLineNumber = lineNumber
                    endColumn = 1
                }
                options = jsObject {
                    isWholeLine = true
                    className = Styles.deletedLine
                }
            })
        }

        // Add decorations for modified lines
        modifiedLineNumbers.distinct().forEach { lineNumber ->
            decorations.add(jsObject {
                range = jsObject {
                    startLineNumber = lineNumber
                    startColumn = 1
                    endLineNumber = lineNumber
                    endColumn = 1
                }
                options = jsObject {
                    isWholeLine = true
                    className = Styles.changedLine
                }
            })
        }

        // Convert the list to an array for the deltaDecorations method
        val decorationsArray = decorations.toTypedArray()

        editor?.let {
            decorationIds = it.deltaDecorations(decorationIds, decorationsArray)
        }
    }

    LaunchedEffect(editor, state) {
        state.onSetValue.collect { value ->
            editor?.setValue(value)
        }
    }

    LaunchedEffect(editor, darkMode) {
        editor?.updateOptions(
            jsObject {
                theme = if (darkMode) "vs-dark" else "vs-light"
            }
        )
    }

    LaunchedEffect(editor, initialValue) {
        editor?.apply {
            setValue(initialValue)
            setScrollTop(0)
            onDidChangeModelContent {
                currentValue = getValue()
            }
        }
    }

    Div({
        style {
            borderRadius(1.r)
            overflow("hidden")
            styles()
        }

        ref { container ->
            editor = Monaco.editor.create(
                container = container,
                options = jsObject {
                    value = initialValue
                    language = "kotlin"
                    theme = if (darkMode) "vs-dark" else "vs-light"
                    automaticLayout = true
                }
            )

            onDispose {
                editor?.dispose()
                editor = null
            }
        }
    })
}
