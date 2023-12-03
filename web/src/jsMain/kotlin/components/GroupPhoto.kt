package components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
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
            ?.sortedByDescending { it.member?.seen?.toEpochMilliseconds() ?: 0 } ?: emptyList()
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
