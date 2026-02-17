package app.group

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import api
import app.ailaai.api.updateGroup
import app.components.FlexInput
import com.queatz.db.Group
import com.queatz.db.GroupExtended
import json
import kotlinx.browser.window
import kotlinx.serialization.encodeToString
import org.jetbrains.compose.web.css.borderRadius
import org.jetbrains.compose.web.css.flex
import org.jetbrains.compose.web.css.marginTop
import org.jetbrains.compose.web.dom.Iframe
import r
import com.queatz.db.GroupContent as GroupContentModel

@Composable
fun GroupContentWebsite(
    group: GroupExtended,
    content: GroupContentModel.Website,
    onUpdated: (GroupExtended) -> Unit,
    setTitle: (String?) -> Unit
) {
    var url by remember { mutableStateOf(content.url ?: "") }
    FlexInput(
        value = url,
        onChange = {
            url = it
        },
        onSubmit = {
            api.updateGroup(
                group.group!!.id!!,
                Group(content = json.encodeToString<GroupContentModel>(GroupContentModel.Website(url)))
            ) {
                onUpdated(group.apply { this.group!!.content = it.content })
            }
            true
        },
        placeholder = "example.com"
    )
    content.url?.takeIf { it.isNotBlank() }?.let { iframeUrl ->
        Iframe({
            style {
                flex(1)
                property("border", "none")
                marginTop(1.r)
                borderRadius(1.r)
            }
            attr("src", iframeUrl.ensureScheme())
            attr("allowfullscreen", "true")
            attr("allow", "autoplay; fullscreen")
            ref {
                val interval = window.setInterval({
                    try {
                        it.contentWindow?.document?.title?.takeIf { it.isNotBlank() }?.let {
                            setTitle(it)
                        }
                    } catch (_: Throwable) {
                    }
                }, 1000)
                onDispose {
                    window.clearInterval(interval)
                }
            }
        })
    }
}

private fun String.ensureScheme() = if (contains("://")) this else "https://$this"
