package com.queatz.ailaai.ui.story

import androidx.compose.foundation.text.ClickableText
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.*
import androidx.compose.ui.text.style.TextDecoration
import com.queatz.ailaai.AppNav
import com.queatz.ailaai.R
import com.queatz.ailaai.extensions.appNavigate
import com.queatz.ailaai.extensions.timeAgo
import com.queatz.ailaai.nav
import com.queatz.db.Person
import kotlinx.datetime.Instant

@OptIn(ExperimentalTextApi::class)
@Composable
fun StoryAuthors(publishDate: Instant?, authors: List<Person>) {
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
                withStyle(
                    SpanStyle(
                        textDecoration = TextDecoration.Underline,
                        color = MaterialTheme.colorScheme.primary
                    )
                ) {
                    withAnnotation(person.id!!, person.id!!) {
                        append(person.name ?: someone)
                    }
                }
            }
    }
    ClickableText(
        authorsText,
        style = MaterialTheme.typography.bodyMedium.merge(
            SpanStyle(
                color = MaterialTheme.colorScheme.secondary
            )
        )
    ) {
        authorsText.getStringAnnotations(it, it).firstOrNull()?.tag?.let { id ->
            nav.appNavigate(AppNav.Profile(id))
        }
    }
}
