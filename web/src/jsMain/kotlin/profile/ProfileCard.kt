package profile

import Styles
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import api
import app.ailaai.api.profile
import appString
import baseUrl
import com.queatz.db.PersonProfile
import components.ProfilePhoto
import notBlank
import org.jetbrains.compose.web.css.Position
import org.jetbrains.compose.web.css.backgroundAttachment
import org.jetbrains.compose.web.css.backgroundColor
import org.jetbrains.compose.web.css.backgroundImage
import org.jetbrains.compose.web.css.backgroundPosition
import org.jetbrains.compose.web.css.backgroundSize
import org.jetbrains.compose.web.css.bottom
import org.jetbrains.compose.web.css.color
import org.jetbrains.compose.web.css.div
import org.jetbrains.compose.web.css.fontSize
import org.jetbrains.compose.web.css.left
import org.jetbrains.compose.web.css.paddingBottom
import org.jetbrains.compose.web.css.paddingLeft
import org.jetbrains.compose.web.css.paddingRight
import org.jetbrains.compose.web.css.paddingTop
import org.jetbrains.compose.web.css.percent
import org.jetbrains.compose.web.css.position
import org.jetbrains.compose.web.css.px
import org.jetbrains.compose.web.css.textAlign
import org.jetbrains.compose.web.css.unaryMinus
import org.jetbrains.compose.web.css.width
import org.jetbrains.compose.web.dom.B
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.Text
import r

@Composable
fun ProfileCard(personId: String, onClick: () -> Unit) {
    var profile by remember { mutableStateOf<PersonProfile?>(null) }

    LaunchedEffect(personId) {
        api.profile(personId) {
            profile = it
        }
    }

    profile?.let {
        ProfileCard(it, onClick)
    }
}

@Composable
fun ProfileCard(personProfile: PersonProfile, onClick: () -> Unit) {
    Div({
        classes(Styles.profileCard)

        onClick { onClick() }
    }) {
        Div({
            style {
                position(Position.Relative)
                width(100.percent)
                property("aspect-ratio", "2")
                backgroundColor(Styles.colors.background)

                // todo: support video
                personProfile.profile.photo?.let { cover ->
                    backgroundPosition("center")
                    backgroundSize("cover")
                    backgroundImage("url('$baseUrl$cover')")
                }
            }
        }) {
            ProfilePhoto(
                person = personProfile.person, styles = {
                    bottom(-48.px / 2)
                    left(50.percent)
                    property("transform", "translateX(-50%)")
                    position(Position.Absolute)
                },
                size = 48.px,
                border = true
            )
        }
        Div({
            style {
                textAlign("center")
                paddingTop(2.r)
                paddingLeft(1.r)
                paddingRight(1.r)
            }
        }) {
            B {
                Text(personProfile.person.name ?: appString { someone })
            }
        }
        personProfile.profile.location?.notBlank?.let { location ->
            Div({
                style {
                    textAlign("center")
                    paddingLeft(1.r)
                    paddingRight(1.r)
                    fontSize(12.px)
                    color(Styles.colors.secondary)
                }
            }) {
                Text(location)
            }
        }
        personProfile.profile.about?.notBlank?.let { about ->
            Div({
                style {
                    paddingTop(1.r)
                    paddingLeft(1.r)
                    paddingRight(1.r)
                    paddingBottom(1.r)
                }
            }) {
                Text(about)
            }
        }
    }
}
