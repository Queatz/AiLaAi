package app.group

import androidx.compose.runtime.*
import api
import app.AppStyles
import app.FullPageLayout
import app.ailaai.api.group
import application
import baseUrl
import com.queatz.db.GroupExtended
import components.Loading
import org.jetbrains.compose.web.css.*
import org.jetbrains.compose.web.dom.Div
import r

@Composable
fun GroupCoverPage(groupId: String, onGroupLoaded: (GroupExtended) -> Unit) {
    Style(AppStyles)

    var group by remember {
        mutableStateOf<GroupExtended?>(null)
    }

    application.background(group?.group?.background?.let { "$baseUrl$it" })

    LaunchedEffect(groupId) {
        api.group(groupId) {
            group = it
            onGroupLoaded(it)
        }
    }

    // todo failed to load state
    if (group == null) {
        Loading()
    } else {
        Div({
            classes(Styles.mainContent)
        }) {
            FullPageLayout {
                Div({
                    style {
                        display(DisplayStyle.Flex)
                        flexDirection(FlexDirection.Column)
                        padding(0.r, 1.r, 1.r, 1.r)
                    }
                }) {
                    GroupCover(group!!)
                }
            }
        }
    }
}
