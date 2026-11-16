package com.queatz.ailaai.ui.story

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import com.queatz.ailaai.AppNav
import com.queatz.ailaai.R
import com.queatz.ailaai.extensions.appNavigate
import com.queatz.ailaai.extensions.timeAgo
import com.queatz.ailaai.nav
import com.queatz.db.Person
import kotlin.time.Instant

@Composable
fun StoryAuthors(
    publishDate: Instant?,
    authors: List<Person>,
) {
    val nav = nav
    val someone = stringResource(R.string.someone)
    val and = stringResource(R.string.inline_and)
    val authorsText = buildAnnotatedString {
        append("${publishDate?.timeAgo() ?: stringResource(R.string.draft)} ${stringResource(R.string.inline_by)} ")
        authors
            .forEachIndexed { index, person ->
                if (index != 0) {
                    append(", ")
                    if (index == authors.lastIndex) {
                        append(and)
                        append(" ")
                    }
                }
                val id = person.id!!
                withLink(
                    LinkAnnotation.Clickable(
                        tag = id,
                        styles = TextLinkStyles(
                            style = SpanStyle(
                                textDecoration = TextDecoration.Underline,
                                color = MaterialTheme.colorScheme.primary
                            )
                        )
                    ) {
                        nav.appNavigate(AppNav.Profile(id))
                    }
                ) {
                    append(person.name ?: someone)
                }
            }
    }
    Text(
        authorsText,
        style = MaterialTheme.typography.bodyMedium.merge(
            SpanStyle(
                color = MaterialTheme.colorScheme.secondary
            )
        )
    )
}
