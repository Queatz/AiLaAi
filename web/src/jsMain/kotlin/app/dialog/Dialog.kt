package app.dialog

import Styles
import androidx.compose.runtime.*
import application
import kotlinx.browser.document
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import org.jetbrains.compose.web.attributes.autoFocus
import org.jetbrains.compose.web.dom.*
import org.jetbrains.compose.web.renderComposable
import org.w3c.dom.HTMLDialogElement

suspend fun dialog(
    title: String?,
    confirmButton: String = application.appString { okay },
    cancelButton: String? = application.appString { cancel },
    cancellable: Boolean = true,
    content: @Composable (resolve: (Boolean?) -> Unit) -> Unit = {}
): Boolean? {
    val result = CompletableDeferred<Boolean?>()
    val dialog = document.createElement("dialog") as HTMLDialogElement
    dialog.classList.add(Styles.modal)
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
        if (title.isNullOrBlank().not()) {
            Header {
                Text(title ?: "")
            }
        }
        Section {
            content {
                result.complete(it)
            }
        }
        Footer {
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
