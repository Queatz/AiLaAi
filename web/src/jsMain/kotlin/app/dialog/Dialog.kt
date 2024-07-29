package app.dialog

import Styles
import androidx.compose.runtime.Composable
import app.components.HorizontalSpacer
import application
import kotlinx.browser.document
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import org.jetbrains.compose.web.attributes.autoFocus
import org.jetbrains.compose.web.css.AlignItems
import org.jetbrains.compose.web.css.CSSSizeValue
import org.jetbrains.compose.web.css.CSSUnit
import org.jetbrains.compose.web.css.DisplayStyle
import org.jetbrains.compose.web.css.JustifyContent
import org.jetbrains.compose.web.css.alignItems
import org.jetbrains.compose.web.css.display
import org.jetbrains.compose.web.css.flex
import org.jetbrains.compose.web.css.gap
import org.jetbrains.compose.web.css.justifyContent
import org.jetbrains.compose.web.dom.Button
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.Footer
import org.jetbrains.compose.web.dom.Header
import org.jetbrains.compose.web.dom.Section
import org.jetbrains.compose.web.dom.Text
import org.jetbrains.compose.web.renderComposable
import org.w3c.dom.HTMLDialogElement
import r

suspend fun dialog(
    title: String?,
    confirmButton: String? = application.appString { okay },
    cancelButton: String? = application.appString { cancel },
    cancellable: Boolean = true,
    maxWidth: CSSSizeValue<out CSSUnit>? = null,
    actions: (@Composable (resolve: (Boolean?) -> Unit) -> Unit)? = null,
    extraButtons: (@Composable (resolve: (Boolean?) -> Unit) -> Unit)? = null,
    content: @Composable (resolve: (Boolean?) -> Unit) -> Unit = {}
): Boolean? {
    val result = CompletableDeferred<Boolean?>()
    val dialog = document.createElement("dialog") as HTMLDialogElement
    dialog.classList.add(Styles.modal)

    maxWidth?.let {
        dialog.style.maxWidth = it.toString()
    }

    dialog.onclose = {
        if (!result.isCompleted) {
            result.complete(null)
        }
        dialog.remove()
    }

    if (cancellable) {
        dialog.onclick = { event ->
            if (event.target == dialog) {
                val rect = dialog.getBoundingClientRect()
                val isInDialog = (rect.top <= event.clientY && event.clientY <= rect.top + rect.height &&
                        rect.left <= event.clientX && event.clientX <= rect.left + rect.width)
                if (!isInDialog) {
                    dialog.close()
                }
            }
        }
    }

    document.body?.appendChild(dialog)

    renderComposable(dialog) {
        if (title.isNullOrBlank().not() || actions != null) {
            Header({
                style {
                    display(DisplayStyle.Flex)
                    alignItems(AlignItems.Center)
                    justifyContent(JustifyContent.SpaceBetween)
                    gap(1.r)
                }
            }) {
                Text(title ?: "")
                actions?.also {
                    HorizontalSpacer()
                }?.invoke {
                    result.complete(it)
                }
            }
        }
        Section {
            content {
                result.complete(it)
            }
        }
        Footer {
            if (confirmButton != null) {
                Button({
                    classes(Styles.button)
                    onClick {
                        result.complete(true)
                    }

                    if (cancelButton == null) {
                        autoFocus()

                        ref {
                            it.focus()
                            onDispose {  }
                        }
                    }
                }) {
                    Text(confirmButton)
                }
            }
            if (cancelButton != null) {
                Button({
                    classes(Styles.textButton)
                    onClick {
                        result.complete(false)
                    }
                }) {
                    Text(cancelButton)
                }
            }
            if (extraButtons != null) {
                Div({
                    style {
                        flex(1)
                        display(DisplayStyle.Flex)
                        alignItems(AlignItems.Center)
                    }
                }) {
                    extraButtons.invoke {
                        result.complete(it)
                    }
                }
            }
        }
    }

    dialog.showModal()

    return try {
        result.await()
    } catch (e: CancellationException) {
        e.printStackTrace()
        null
    } finally {
        dialog.close()
    }
}
