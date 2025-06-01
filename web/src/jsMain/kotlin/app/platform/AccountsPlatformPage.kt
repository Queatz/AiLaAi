package app.platform

import Styles
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import api
import app.ailaai.api.platformAccountsByPoints
import app.ailaai.api.platformAddPoints
import app.ailaai.api.profile
import app.dialog.dialog
import app.dialog.inputDialog
import com.queatz.db.Account
import com.queatz.db.PersonProfile
import kotlinx.coroutines.launch
import org.jetbrains.compose.web.css.AlignItems
import org.jetbrains.compose.web.css.DisplayStyle
import org.jetbrains.compose.web.css.alignItems
import org.jetbrains.compose.web.css.cursor
import org.jetbrains.compose.web.css.display
import org.jetbrains.compose.web.css.fontSize
import org.jetbrains.compose.web.css.margin
import org.jetbrains.compose.web.css.marginBottom
import org.jetbrains.compose.web.css.padding
import org.jetbrains.compose.web.css.px
import org.jetbrains.compose.web.dom.Button
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.Text
import r

@Composable
fun AccountsPlatformPage() {
    val scope = rememberCoroutineScope()
    var accountsByPoints by remember {
        mutableStateOf(emptyList<Account>())
    }

    fun addPoints(account: PersonProfile) {
        scope.launch {
            val pointsString = inputDialog(
                // todo: translate
                "Add points to ${account.person.name}'s account",
                // todo: translate
                placeholder = "Number of points"
            )

            if (pointsString != null) {
                val points = pointsString.toIntOrNull() ?: 0

                if (points > 0) {
                    api.platformAddPoints(
                        account = account.person.id!!,
                        points = points,
                        onError = {
                            dialog(
                                // todo: translate
                                title = "Failed",
                                cancelButton = null
                            )
                        }
                    ) {
                        dialog(
                            // todo: translate
                            title = "Success",
                            cancelButton = null
                        )
                    }
                } else {
                    dialog(
                        // todo: translate
                        title = "Failed",
                        cancelButton = null
                    )
                }
            }
        }
    }

    fun chooseAccount() {
        scope.launch {
            val personId = inputDialog(
                // todo: translate
                "Profile ID"
            )

            if (personId != null) {
                api.profile(
                    personId = personId,
                    onError = {
                        dialog(
                            // todo: translate
                            title = "Failed",
                            cancelButton = null
                        )
                    }
                ) {
                    addPoints(it)
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        api.platformAccountsByPoints {
            accountsByPoints = it
        }
    }

    Div({
        style {
            fontSize(18.px)
            margin(1.r)
        }
    }) {
        Div(
            {
                style {
                    display(DisplayStyle.Flex)
                    alignItems(AlignItems.Center)
                    marginBottom(1.r)
                }
            }
        ) {
            Button(
                {
                    classes(Styles.outlineButton)

                    onClick {
                        chooseAccount()
                    }
                }
            ) {
                // todo: translate
                Text("Add points")
            }
        }
        Div {
            accountsByPoints.forEach { account ->
                var profile by remember { mutableStateOf<PersonProfile?>(null) }

                Div({
                    style {
                        padding(1.r)
                        cursor("pointer")
                    }

                    onClick {
                        scope.launch {
                            api.profile(account.person!!) {
                                profile = it
                            }
                        }
                    }
                }) {
                    // todo: translate
                    Text("${account.person} (${account.points} points)")

                    profile?.let { profile ->
                        Text(" - ${profile.person.name}")
                    }
                }
            }
        }
    }
}
