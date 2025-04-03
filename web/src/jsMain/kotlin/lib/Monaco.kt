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
        fun getValue(): String
        fun updateOptions(options: EditorOptions)
        fun onDidChangeModelContent(listener: () -> Unit)
    }

    interface EditorOptions {
        var value: String
        var language: String
        var theme: String
        var automaticLayout: Boolean
    }
}
