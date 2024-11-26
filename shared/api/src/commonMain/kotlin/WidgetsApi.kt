import app.ailaai.api.Api
import app.ailaai.api.ErrorBlock
import app.ailaai.api.SuccessBlock
import com.queatz.db.CreateWidgetBody
import com.queatz.db.RunWidgetBody
import com.queatz.db.RunWidgetResponse
import com.queatz.db.Widget
import com.queatz.widgets.Widgets

suspend fun Api.widget(
    id: String,
    onError: ErrorBlock = null,
    onSuccess: SuccessBlock<Widget>
) = get(
    url = "widgets/$id",
    onError = onError,
    onSuccess = onSuccess
)

suspend fun Api.createWidget(
    widget: Widgets,
    data: String? = null,
    onError: ErrorBlock = null,
    onSuccess: SuccessBlock<Widget>
) = post(
    url = "widgets",
    body = CreateWidgetBody(widget, data),
    onError = onError,
    onSuccess = onSuccess
)

suspend fun Api.updateWidget(
    id: String,
    widget: Widget,
    onError: ErrorBlock = null,
    onSuccess: SuccessBlock<Widget> = {}
) = post(
    url = "widgets/$id",
    body = widget,
    onError = onError,
    onSuccess = onSuccess
)

suspend fun Api.runWidget(
    id: String,
    data: RunWidgetBody,
    onError: ErrorBlock = null,
    onSuccess: SuccessBlock<RunWidgetResponse> = {}
) = post(
    url = "widgets/$id/run",
    body = data,
    onError = onError,
    onSuccess = onSuccess
)
