import app.ailaai.api.Api
import app.ailaai.api.ErrorBlock
import app.ailaai.api.SuccessBlock
import com.queatz.db.CreateWidgetBody
import com.queatz.db.Widget
import com.queatz.widgets.Widgets

suspend fun Api.widget(
    id: String,
    onError: ErrorBlock = null,
    onSuccess: SuccessBlock<Widget>
) = get(
    "widgets/$id",
    onError = onError,
    onSuccess = onSuccess
)

suspend fun Api.createWidget(
    widget: Widgets,
    data: String? = null,
    onError: ErrorBlock = null,
    onSuccess: SuccessBlock<Widget>
) = post(
    "widgets",
    CreateWidgetBody(widget, data),
    onError = onError,
    onSuccess = onSuccess
)

suspend fun Api.updateWidget(
    id: String,
    widget: Widget,
    onError: ErrorBlock = null,
    onSuccess: SuccessBlock<Widget>
) = post(
    "widgets/$id",
    widget,
    onError = onError,
    onSuccess = onSuccess
)
