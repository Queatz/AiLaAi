package com.queatz.ailaai.ui.card.template

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import app.ailaai.api.newCard
import com.queatz.ailaai.R
import com.queatz.ailaai.data.api
import com.queatz.ailaai.data.json
import com.queatz.ailaai.extensions.notBlank
import com.queatz.ailaai.extensions.rememberStateOf
import com.queatz.ailaai.ui.components.ConversationAction
import com.queatz.ailaai.ui.components.ConversationItem
import com.queatz.ailaai.ui.components.DialogBase
import com.queatz.ailaai.ui.components.DialogLayout
import com.queatz.ailaai.ui.components.SetPhotoButton
import com.queatz.ailaai.ui.dialogs.DialogCloseButton
import com.queatz.ailaai.ui.theme.pad
import com.queatz.db.Card
import com.queatz.db.Pay
import kotlinx.coroutines.launch

enum class CreateCardWithTemplate {
    Product, // List an item for sale
    Service, // Offer your skills and services
    Job, // Find someone for a job
    Classes, // Offer classes or tutoring
    Music, // Lessons and performances
}

@Composable
fun CreateCardWithTemplateDialog(
    onDismissRequest: () -> Unit,
    template: CreateCardWithTemplate,
    onCard: (Card) -> Unit
) {
    val scope = rememberCoroutineScope()
    var isLoading by rememberStateOf(false)

    // Common fields
    var name by rememberStateOf("")
    var description by rememberStateOf("")
    var price by rememberStateOf("")
    var pay by rememberStateOf("")
    var title by rememberStateOf("")
    var photoUrl by rememberStateOf<String?>(null)

    DialogBase(onDismissRequest) {
        DialogLayout(
            content = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 1.pad),
                    verticalArrangement = Arrangement.spacedBy(1.pad)
                ) {
                    // Title based on template type
                    Text(
                        text = when (template) {
                            CreateCardWithTemplate.Product -> "Post a product"
                            CreateCardWithTemplate.Service -> "Post a service"
                            CreateCardWithTemplate.Job -> "Post a job"
                            CreateCardWithTemplate.Classes -> "Create a class"
                            CreateCardWithTemplate.Music -> "Share music"
                        },
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.padding(bottom = 1.pad)
                    )

                    when (template) {
                        CreateCardWithTemplate.Product, CreateCardWithTemplate.Service -> {
                            // Photo button
                            SetPhotoButton(
                                photo = photoUrl.orEmpty(),
                                photoText = title,
                                onPhoto = { photoUrl = it },
                                onRemove = { photoUrl = null },
                                modifier = Modifier.fillMaxWidth()
                            )

                            // Name field
                            OutlinedTextField(
                                value = name,
                                onValueChange = { name = it },
                                label = { Text(stringResource(R.string.name)) },
                                shape = MaterialTheme.shapes.large,
                                modifier = Modifier.fillMaxWidth(),
                                keyboardOptions = KeyboardOptions(
                                    capitalization = KeyboardCapitalization.Sentences
                                )
                            )

                            // Description field
                            OutlinedTextField(
                                value = description,
                                onValueChange = { description = it },
                                label = { Text(stringResource(R.string.description)) },
                                shape = MaterialTheme.shapes.large,
                                modifier = Modifier.fillMaxWidth(),
                                minLines = 3,
                                keyboardOptions = KeyboardOptions(
                                    capitalization = KeyboardCapitalization.Sentences
                                )
                            )

                            // Price field
                            OutlinedTextField(
                                value = price,
                                onValueChange = { price = it },
                                label = { Text(stringResource(R.string.price)) },
                                shape = MaterialTheme.shapes.large,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }

                        CreateCardWithTemplate.Job -> {
                            // Title field
                            OutlinedTextField(
                                value = title,
                                onValueChange = { title = it },
                                label = { Text(stringResource(R.string.title)) },
                                shape = MaterialTheme.shapes.large,
                                modifier = Modifier.fillMaxWidth(),
                                keyboardOptions = KeyboardOptions(
                                    capitalization = KeyboardCapitalization.Sentences
                                )
                            )

                            // Job description field
                            OutlinedTextField(
                                value = description,
                                onValueChange = { description = it },
                                label = { Text("Job description") },
                                shape = MaterialTheme.shapes.large,
                                modifier = Modifier.fillMaxWidth(),
                                minLines = 3,
                                keyboardOptions = KeyboardOptions(
                                    capitalization = KeyboardCapitalization.Sentences
                                )
                            )

                            // Pay field
                            OutlinedTextField(
                                value = pay,
                                onValueChange = { pay = it },
                                label = { Text(stringResource(R.string.pay)) },
                                shape = MaterialTheme.shapes.large,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }

                        CreateCardWithTemplate.Classes,
                        CreateCardWithTemplate.Music -> {
                            // Photo button
                            SetPhotoButton(
                                photo = photoUrl.orEmpty(),
                                photoText = title,
                                onPhoto = { photoUrl = it },
                                onRemove = { photoUrl = null },
                                modifier = Modifier.fillMaxWidth()
                            )

                            // Title field
                            OutlinedTextField(
                                value = title,
                                onValueChange = { title = it },
                                label = { Text(stringResource(R.string.title)) },
                                shape = MaterialTheme.shapes.large,
                                modifier = Modifier.fillMaxWidth(),
                                keyboardOptions = KeyboardOptions(
                                    capitalization = KeyboardCapitalization.Sentences
                                )
                            )

                            // Description field
                            OutlinedTextField(
                                value = description,
                                onValueChange = { description = it },
                                label = { Text(stringResource(R.string.description)) },
                                shape = MaterialTheme.shapes.large,
                                modifier = Modifier.fillMaxWidth(),
                                minLines = 3,
                                keyboardOptions = KeyboardOptions(
                                    capitalization = KeyboardCapitalization.Sentences
                                )
                            )
                        }
                    }
                }
            },
            actions = {
                DialogCloseButton(onDismissRequest)
                Button(
                    onClick = {
                        isLoading = true
                        scope.launch {
                            // Create conversation item with a single button
                            val conversation = ConversationItem(
                                message = description.notBlank.orEmpty().trim()
                            ).apply {
                                items = mutableListOf(
                                    ConversationItem().apply {
                                        this.title = when (template) {
                                            CreateCardWithTemplate.Product -> "Buy now"
                                            CreateCardWithTemplate.Service -> "Book now"
                                            CreateCardWithTemplate.Job -> "Apply now"
                                            CreateCardWithTemplate.Classes -> "Enroll now"
                                            CreateCardWithTemplate.Music -> "Connect now"
                                        }
                                        action = ConversationAction.Message
                                    }
                                )
                            }

                            api.newCard(
                                Card().apply {
                                    this.name = name.trim()
                                    this.pay = price.notBlank { Pay(pay = it.trim())}
                                    this.photo = photoUrl?.notBlank
                                    this.active = true
                                    this.conversation = json.encodeToString(conversation)
                                }
                            ) {
                                onCard(it)
                                onDismissRequest()
                            }
                            isLoading = false
                        }
                    },
                    enabled = !isLoading && when (template) {
                        CreateCardWithTemplate.Product,
                        CreateCardWithTemplate.Service ->
                            name.isNotBlank() && description.isNotBlank() && price.isNotBlank()

                        CreateCardWithTemplate.Job ->
                            title.isNotBlank() && description.isNotBlank()

                        CreateCardWithTemplate.Classes,
                        CreateCardWithTemplate.Music ->
                            title.isNotBlank() && description.isNotBlank()
                    },
                ) {
                    Text(stringResource(R.string.post))
                }
            }
        )
    }
}
