package app.group

import androidx.compose.runtime.*
import api
import app.AppStyles
import app.FullPageLayout
import app.ailaai.api.group
import app.components.Empty
import appText
import application
import baseUrl
import com.queatz.db.GroupExtended
import components.Loading
import org.jetbrains.compose.web.css.*
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.Text
import r

@Composable
fun GroupCoverPage(groupId: String, onGroupLoaded: (GroupExtended) -> Unit) {
    Style(AppStyles)

    var isLoading by remember {
        mutableStateOf(true)
    }
    var isError by remember {
        mutableStateOf(false)
    }
    var group by remember {
        mutableStateOf<GroupExtended?>(null)
    }

    application.background(group?.group?.background?.let { "$baseUrl$it" })

    LaunchedEffect(groupId) {
        api.group(
            groupId,
            onError = {
                isError = true
            }
        ) {
            group = it
            onGroupLoaded(it)
        }
        isLoading = false
    }

    Div({
        classes(Styles.mainContent)
    }) {
        if (isError) {
            Empty {
                appText { groupNotFound }
            }
        } else if (isLoading) {
            Loading()
        } else if (group != null) {
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
