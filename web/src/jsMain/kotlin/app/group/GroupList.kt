package app.group

import androidx.compose.runtime.Composable
import com.queatz.db.GroupExtended
import org.jetbrains.compose.web.css.CSSSizeValue
import org.jetbrains.compose.web.css.CSSUnit
import org.jetbrains.compose.web.css.marginBottom
import r

@Composable
fun GroupList(
    groups: List<GroupExtended>,
    onSurface: Boolean = false,
    coverPhoto: Boolean = false,
    maxWidth: CSSSizeValue<CSSUnit.rem>? = null,
    onSelected: (GroupExtended) -> Unit
) {
    groups.forEachIndexed { index, it ->
        GroupItem(
            it,
            selectable = onSurface,
            onSurface = onSurface,
            coverPhoto = coverPhoto,
            maxWidth = maxWidth,
            onSelected = {
                onSelected(it)
            },
            info = GroupInfo.Members,
            styles = {
                if (index != groups.lastIndex) {
                    marginBottom(1.r)
                }
            }
        )
    }
}
