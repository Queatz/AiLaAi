package lib

import org.w3c.dom.*
import kotlin.js.*

@JsModule("monaco-editor")
@JsNonModule
external object Monaco {
    val editor: EditorFactory

    interface EditorFactory {
        fun create(container: HTMLElement, options: EditorOptions): IEditor
    }

    interface IEditor {
        fun dispose()
        fun setValue(value: String)
        fun setScrollTop(offset: Int)
        fun getValue(): String
        fun updateOptions(options: EditorOptions)
        fun onDidChangeModelContent(listener: () -> Unit)
        fun deltaDecorations(oldDecorations: Array<String>, newDecorations: Array<LineDecoration>): Array<String>
    }

    interface Range {
        var startLineNumber: Int
        var startColumn: Int
        var endLineNumber: Int
        var endColumn: Int
    }

    interface DecorationOptions {
        var isWholeLine: Boolean
        var className: String
    }

    interface LineDecoration {
        var range: Range
        var options: DecorationOptions
    }

    interface EditorOptions {
        var value: String
        var language: String
        var theme: String
        var automaticLayout: Boolean
    }
}
