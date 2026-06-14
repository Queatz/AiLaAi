package app.dialog

import Configuration
import api
import app.AppStyles
import app.ailaai.api.people
import appString
import application
import com.queatz.db.Person
import com.queatz.db.PersonProfile
import components.ProfilePhoto
import focusable
import org.jetbrains.compose.web.css.marginRight
import org.jetbrains.compose.web.css.px
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.Text
import r

suspend fun selectPersonDialog(
    configuration: Configuration,
    title: String = application.appString { profile },
    onSelected: (Person) -> Unit
) {
    val cancel = application.appString { cancel }

    searchDialog<PersonProfile>(
        configuration = configuration,
        title = title,
        confirmButton = cancel,
        load = {
            var loadedPeople = emptyList<PersonProfile>()
            api.people {
                loadedPeople = it
            }
            loadedPeople
        },
        filter = { it, value ->
            it.person.name?.contains(value, ignoreCase = true) ?: false
        }
    ) { personProfile, resolve ->
        Div({
            classes(AppStyles.groupItem)
            focusable()
            onClick {
                onSelected(personProfile.person)
                resolve(false)
            }
        }) {
            ProfilePhoto(
                person = personProfile.person,
                size = 32.px,
                styles = {
                    marginRight(.5.r)
                }
            )
            Text(personProfile.person.name ?: application.appString { someone })
        }
    }
}
