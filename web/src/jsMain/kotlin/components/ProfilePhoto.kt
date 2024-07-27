package components

import Styles
import androidx.compose.runtime.Composable
import application
import baseUrl
import com.queatz.db.Person
import focusable
import notBlank
import org.jetbrains.compose.web.css.CSSNumeric
import org.jetbrains.compose.web.css.StyleScope
import org.jetbrains.compose.web.css.backgroundImage
import org.jetbrains.compose.web.css.cursor
import org.jetbrains.compose.web.css.height
import org.jetbrains.compose.web.css.px
import org.jetbrains.compose.web.css.width
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.Span
import org.jetbrains.compose.web.dom.Text

@Composable
fun ProfilePhoto(
    person: Person,
    size: CSSNumeric = 36.px,
    title: String? = null,
    border: Boolean = false,
    onClick: (() -> Unit)? = null,
    styles: (StyleScope.() -> Unit)? = null
) {
    ProfilePhoto(
        person.photo,
        person.name,
        size,
        title,
        border,
        onClick,
        styles
    )
}

@Composable
fun ProfilePhoto(
    photo: String?,
    name: String?,
    size: CSSNumeric = 36.px,
    title: String? = null,
    border: Boolean = false,
    onClick: (() -> Unit)? = null,
    styles: (StyleScope.() -> Unit)? = null
) {
    if (photo == null) {
        Div({
            classes(listOf(Styles.profilePhotoText) + if (border) {
                listOf(Styles.profilePhotoBorder)
            } else {
                emptyList()
            })

            if (onClick != null) {
                focusable()
            }

            style {
                width(size)
                height(size)

                if (onClick != null) {
                    cursor("pointer")
                }
                styles?.invoke(this)
            }

            title(title ?: name ?: application.appString { someone })

            onClick {
                onClick?.invoke()
            }
        }) {
            name?.notBlank?.take(1)?.let {
                Text(it)
            } ?: let {
                Span(
                    {
                        classes("material-symbols-outlined")
                    }
                ) {
                    Text("account_circle")
                }
            }
        }
    } else {
        Div({
            classes(listOf(Styles.profilePhotoPhoto) + if (border) {
                listOf(Styles.profilePhotoBorder)
            } else {
                emptyList()
            })
            style {
                width(size)
                height(size)
                backgroundImage("url('$baseUrl${photo}')")
                if (onClick != null) {
                    cursor("pointer")
                }
                styles?.invoke(this)
            }

            title(title ?: name ?: application.appString { someone })

            if (onClick != null) {
                focusable()
            }

            onClick {
                onClick?.invoke()
            }
        })
    }
}
