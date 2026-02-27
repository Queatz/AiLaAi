package components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import application
import com.queatz.db.GroupExtended
import com.queatz.db.Person
import org.jetbrains.compose.web.css.*
import org.jetbrains.compose.web.dom.Div
import r

@Composable
fun GroupPhoto(group: GroupExtended, me: Person?) {
    val otherMembers = remember(group) {
        group.members
            ?.filter { it.person?.id != me?.id }
            ?: emptyList()
    }

    if (otherMembers.size > 1) {
        Div({
            style {
                width(54.px)
                height(54.px)
                position(Position.Relative)
                marginRight(.5.r)
            }
        }) {
            ProfilePhoto(otherMembers[1].person!!, size = 33.px, border = true) {
                position(Position.Absolute)
                top(0.r)
                right(0.r)
            }
            ProfilePhoto(otherMembers[0].person!!, size = 33.px, border = true) {
                position(Position.Absolute)
                bottom(0.r)
                left(0.r)
            }
        }
    } else if (otherMembers.size == 1) {
        ProfilePhoto(otherMembers.first().person!!, size = 54.px) {
            marginRight(.5.r)
        }
    } else if (me != null) {
        ProfilePhoto(me, size = 54.px) {
            marginRight(.5.r)
        }
    }
}

@Composable
fun GroupPhoto(
    items: List<GroupPhotoItem>,
    size: CSSSizeValue<CSSUnit.px> = 54.px,
    mergeTitles: Boolean = false,
    fallback: String = "account_circle",
    fallbackTitle: String = application.appString { someone },
    onClick: (() -> Unit)? = null
) {
    if (items.size > 1) {
        Div({
            style {
                width(size)
                height(size)
                position(Position.Relative)
                marginRight(.5.r)
            }

            if (mergeTitles) {
                title(items.mapNotNull { it.name }.joinToString("\n"))
            }
        }) {
            ProfilePhoto(
                photo = items[1].photo,
                name = items[1].name,
                size = size / 2,
                fontSize = size / 4,
                border = true,
                showTitle = false,
                onClick = onClick
            ) {
                position(Position.Absolute)
                top(0.r)
                right(0.r)
            }
            ProfilePhoto(
                photo = items[0].photo,
                name = items[0].name,
                size = size / 2,
                fontSize = size / 4,
                border = true,
                showTitle = false,
                onClick = onClick
            ) {
                position(Position.Absolute)
                bottom(0.r)
                left(0.r)
            }
        }
    } else if (items.size == 1) {
        ProfilePhoto(
            photo = items[0].photo,
            name = items[0].name,
            size = size,
            fontSize = size / 2,
            onClick = onClick

        ) {
            marginRight(.5.r)
        }
    } else {
        ProfilePhoto(
            photo = null,
            name = null,
            size = size,
            fontSize = size / 2,
            onClick = onClick,
            fallback = fallback,
            fallbackTitle = fallbackTitle
        ) {
            marginRight(.5.r)
        }
    }
}

data class GroupPhotoItem(val photo: String?, val name: String?)
