import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.neverEqualPolicy
import org.jetbrains.compose.web.dom.Text

/**
 * Holds translations for a single term or phrase in multiple languages.
 *
 * @property en The English translation (default language).
 * @property vn The Vietnamese translation.
 * @property ru The Russian translation (default to English if not provided).
 */
data class Translation(
    val en: String,
    val vn: String = en,
    val ru: String = en,
)

/**
 * A central repository for all application strings, encapsulating translations
 * for different languages (English, Vietnamese, and optionally Russian).
 */
object Strings {
    val people = Translation(
        en = "People",
        vn = "Con người",
        ru = "Люди"
    )
    val noScripts = Translation(
        en = "No scripts.",
        vn = "Không có tập lệnh.",
        ru = "Нет скриптов."
    )
    val myQr = Translation(
        en = "My QR",
        vn = "Mã QR của tôi",
        ru = "Мой QR"
    )
    val viewReactions = Translation(
        en = "View reactions",
        vn = "Xem phản ứng",
        ru = "Просмотр реакций"
    )
    val rate = Translation(
        en = "Rate",
        vn = "Đánh giá",
        ru = "Оценить"
    )
    val editMessage = Translation(
        en = "Edit message",
        vn = "Chỉnh sửa tin nhắn",
        ru = "Редактировать сообщение"
    )
    val deleteThisMessage = Translation(
        en = "Delete this message?",
        vn = "Xóa tin nhắn này?",
        ru = "Удалить это сообщение?"
    )
    val sentAt = Translation(
        en = "Sent %1\$s",
        vn = "Đã gửi %1\$s",
        ru = "Отправлено %1\$s"
    )
    val tapToReact = Translation(
        en = "Tap to react",
        vn = "Nhấn để phản ứng",
        ru = "Нажмите, чтобы отреагировать"
    )
    val clearAll = Translation(
        en = "Clear all",
        vn = "Xóa tất cả",
        ru = "Очистить все"
    )
    val yourProfileUrl = Translation(
        en = "Your profile URL",
        vn = "URL trang cá nhân của bạn",
        ru = "URL вашего профиля"
    )
    val copied = Translation(
        en = "Copied!",
        vn = "Đã sao chép!",
        ru = "Скопировано!"
    )
    val copyProfileLink = Translation(
        en = "Copy profile link",
        vn = "Sao chép liên kết cá nhân",
        ru = "Копировать ссылку на профиль"
    )
    val openProfile = Translation(
        en = "Open profile",
        vn = "Mở trang cá nhân",
        ru = "Открыть профиль"
    )
    val hour = Translation(
        en = "Hour",
        vn = "Giờ",
        ru = "Час"
    )
    val minute = Translation(
        en = "Minute",
        vn = "Phút",
        ru = "Минута"
    )
    val hours = Translation(
        en = "Hours",
        vn = "Giờ",
        ru = "Часы"
    )
    val minutes = Translation(
        en = "Minutes",
        vn = "Phút",
        ru = "Минуты"
    )
    val switchView = Translation(
        en = "Switch view",
        vn = "Chuyển chế độ xem",
        ru = "Переключить вид"
    )
    val newScript = Translation(
        en = "New script",
        vn = "Tập lệnh mới",
        ru = "Новый скрипт"
    )
    val removeRating = Translation(
        en = "Remove rating",
        vn = "Xóa đánh giá",
        ru = "Удалить оценку"
    )
    val help = Translation(
        en = "Help",
        vn = "Trợ giúp",
        ru = "Помощь"
    )
    val yourRatingsAreVisible = Translation(
        en = "Your ratings are only visible to you.",
        vn = "Đánh giá của bạn chỉ hiển thị với bạn.",
        ru = "Ваши оценки видны только вам."
    )
    val text = Translation(
        en = "Text",
        vn = "Văn bản",
        ru = "Текст"
    )
    val openLink = Translation(
        en = "Open link",
        vn = "Mở liên kết",
        ru = "Открыть ссылку"
    )
    val appUseCases = Translation(
        en = "Hi Town Use Cases",
        vn = "Trường hợp sử dụng Hi Town",
        ru = "Варианты использования Hi Town"
    )

    val restart = Translation(
        en = "Restart",
        vn = "Khởi động lại",
        ru = "Перезапустить"
    )
    val errorSubmittingForm = Translation(
        en = "There was an error submitting the form.",
        vn = "Đã xảy ra lỗi khi gửi biểu mẫu.",
        ru = "Произошла ошибка при отправке формы."
    )
    val tryAgainOrContact = Translation(
        en = "Please try again or contact the form owner.",
        vn = "Vui lòng thử lại hoặc liên hệ với chủ sở hữu biểu mẫu.",
        ru = "Пожалуйста, попробуйте еще раз или свяжитесь с владельцем формы."
    )
    val please = Translation(
        en = "",
        vn = "Hãy ",
        ru = "Пожалуйста "
    )
    val formSubmitted = Translation(
        en = "Form submitted!",
        vn = "Biểu mẫu đã được gửi!",
        ru = "Форма отправлена!"
    )
    val tapToRemove = Translation(
        en = "Tap to remove",
        vn = "Nhấn để xóa",
        ru = "Нажмите, чтобы удалить"
    )
    val removeThisPhoto = Translation(
        en = "Remove this photo?",
        vn = "Xóa ảnh này?",
        ru = "Удалить это фото?"
    )
    val addPhotos = Translation(
        en = "Add photos",
        vn = "Thêm ảnh",
        ru = "Добавить фотографии"
    )
    val submitting = Translation(
        en = "Submitting…",
        vn = "Đang gửi…",
        ru = "Отправка…"
    )
    val signInToSubmitForm = Translation(
        en = "Sign in to submit this form",
        vn = "Đăng nhập để gửi biểu mẫu này",
        ru = "Войдите, чтобы отправить эту форму"
    )
    val noPeople = Translation(
        en = "No people.",
        vn = "Không có người.",
        ru = "Нет людей."
    )
    val androidApk = Translation(
        en = "Android APK",
        vn = "APK Android",
        ru = "Android APK"
    )
    val appStore = Translation(
        en = "App Store"
    )
    val noPagesInGroup = Translation(
        en = "This group currently has no pages.",
        vn = "Nhóm này hiện không có trang nào.",
        ru = "В этой группе пока нет страниц."
    )
    val createOpenGroupAbout = Translation(
        en = "Create an open group about \"%1\$s\".",
        vn = "Tạo một nhóm mở về \"%1\$s\".",
        ru = "Создать открытую группу о \"%1\$s\"."
    )
    val setStatus = Translation(
        en = "Set status",
        vn = "Đặt trạng thái",
        ru = "Установить статус"
    )
    val isInvitingYouTo = Translation(
        en = "is inviting you to",
        vn = "đang mời bạn tham gia",
        ru = "приглашает вас в"
    )
    val inviteCodeCannotBeUsed = Translation(
        en = "The invite code cannot be used",
        vn = "Mã mời không thể sử dụng",
        ru = "Код приглашения не может быть использован"
    )
    val acceptInvite = Translation(
        en = "Accept invite",
        vn = "Chấp nhận lời mời",
        ru = "Принять приглашение"
    )
    val name = Translation(
        en = "Name",
        vn = "Tên",
        ru = "Имя"
    )
    val publish = Translation(
        en = "Publish",
        vn = "Xuất bản",
        ru = "Опубликовать"
    )
    val collapse = Translation(
        en = "Collapse",
        vn = "Thu gọn",
        ru = "Свернуть"
    )
    val expand = Translation(
        en = "Expand",
        vn = "Mở rộng",
        ru = "Развернуть"
    )
    val searchForPlaces = Translation(
        en = "Search for places, services, and more",
        vn = "Tìm kiếm địa điểm, dịch vụ và hơn thế nữa",
        ru = "Поиск мест, услуг и многое другое"
    )
    val paid = Translation(
        en = "Paid",
        vn = "Trả phí",
        ru = "Платный"
    )
    val viewList = Translation(
        en = "View list",
        vn = "Xem danh sách",
        ru = "Просмотр списка"
    )
    val pages = Translation(
        en = "Pages",
        vn = "Trang",
        ru = "Страницы"
    )

    val enterYourMessage = Translation(
        en = "Enter your message",
        vn = "Nhập tin nhắn của bạn",
        ru = "Введите ваше сообщение"
    )

    val z = Translation(
        en = "z",
        vn = "z",
        ru = "z"
    )

    val howWouldYouLikeToBeContacted = Translation(
        en = "How would you like to be contacted?",
        vn = "Bạn muốn được liên lạc như thế nào?",
        ru = "Как бы вы хотели, чтобы с вами связались?"
    )

    val yourPhoneNumberOrEmail = Translation(
        en = "Your phone number or email",
        vn = "Số điện thoại hoặc email của bạn",
        ru = "Ваш номер телефона или электронная почта"
    )

    val general = Translation(
        en = "General",
        vn = "Chung",
        ru = "Общие"
    )

    val stylized = Translation(
        en = "Stylized",
        vn = "Phong cách",
        ru = "Стилизованный"
    )

    val description = Translation(
        en = "Description",
        vn = "Mô tả",
        ru = "Описание"
    )
    val secret = Translation(
        en = "Secret",
        vn = "Bí mật",
        ru = "Секрет"
    )
    val deleteThisScript = Translation(
        en = "Delete this script?",
        vn = "Xóa Tập lệnh này?",
        ru = "Удалить этот скрипт?"
    )
    val swapEditorPosition = Translation(
        en = "Swap editor position",
        vn = "Đổi vị trí trình soạn thảo",
        ru = "Поменять позицию редактора"
    )
    val goToAuthorProfile = Translation(
        en = "Go to author's profile",
        vn = "Đi đến hồ sơ của tác giả",
        ru = "Перейти к профилю автора"
    )
    val openScriptInNewPage = Translation(
        en = "Open script in new page",
        vn = "Mở Tập lệnh trong trang mới",
        ru = "Открыть скрипт в новой странице"
    )
    val renameScript = Translation(
        en = "Rename script",
        vn = "Đổi tên của tập lệnh",
        ru = "Переименовать скрипт"
    )
    val scriptName = Translation(
        en = "Script name",
        vn = "Tên của tập lệnh",
        ru = "Имя скрипта"
    )

    val undoAiChanges = Translation(
        en = "Undo AI changes",
        vn = "Hoàn tác thay đổi AI",
        ru = "Отменить изменения ИИ"
    )

    val runScriptHoldShift = Translation(
        en = "Run script (Hold SHIFT to skip cache)",
        vn = "Chạy tập lệnh (Giữ SHIFT để bỏ qua bộ nhớ đệm)",
        ru = "Запустить скрипт (удерживайте SHIFT, чтобы пропустить кеш)"
    )

    val codeWithAi = Translation(
        en = "Code with AI",
        vn = "Lập trình với AI",
        ru = "Программирование с ИИ"
    )

    val stopScriptGeneration = Translation(
        en = "Stop script generation?",
        vn = "Dừng tạo tập lệnh?",
        ru = "Остановить генерацию скрипта?"
    )

    val yesStop = Translation(
        en = "Yes, stop",
        vn = "Vâng, dừng lại",
        ru = "Да, остановить"
    )

    val aiPrompt = Translation(
        en = "AI Prompt",
        vn = "Lời nhắc AI",
        ru = "Подсказка ИИ"
    )

    val send = Translation(
        en = "Send",
        vn = "Gửi",
        ru = "Отправить"
    )

    val createScriptThat = Translation(
        en = "Create a script that...",
        vn = "Tạo một tập lệnh mà...",
        ru = "Создать скрипт, который..."
    )

    val modifyThisScriptTo = Translation(
        en = "Modify this script to...",
        vn = "Sửa đổi tập lệnh này để...",
        ru = "Изменить этот скрипт, чтобы..."
    )

    val noPromptHistory = Translation(
        en = "No prompt history",
        vn = "Không có lịch sử lời nhắc",
        ru = "Нет истории подсказок"
    )

    val somethingWentWrong = Translation(
        en = "Something went wrong",
        vn = "Đã xảy ra lỗi",
        ru = "Что-то пошло не так"
    )

    val pleaseTryAgain = Translation(
        en = "Please try again.",
        vn = "Vui lòng thử lại.",
        ru = "Пожалуйста, попробуйте еще раз."
    )

    val dependOnScript = Translation(
        en = "Depend on script",
        vn = "Phụ thuộc vào tập lệnh khác",
        ru = "Зависит от скрипта"
    )

    val kotlinScripts = Translation(
        en = "Kotlin Scripts",
        vn = "Tập lệnh Kotlin",
        ru = "Скрипты Kotlin"
    )

    val menu = Translation(
        en = "Menu",
        vn = "Menu",
        ru = "Меню"
    )

    val descriptionOptional = Translation(
        en = "Description (optional)",
        vn = "Mô tả (tùy chọn)",
        ru = "Описание (необязательно)"
    )

    val multipleUses = Translation(
        en = "Multiple uses",
        vn = "Sử dụng nhiều lần",
        ru = "Многократное использование"
    )

    val expires = Translation(
        en = "Expires",
        vn = "Hết hạn",
        ru = "Истекает"
    )

    val clearStatus = Translation(
        en = "Clear status",
        vn = "Xóa trạng thái",
        ru = "Очистить статус"
    )

    val generating = Translation(
        en = "Generating",
        vn = "Đang tạo",
        ru = "Генерация"
    )

    val pageUpdatedWhenPhotoGenerated = Translation(
        en = "The page will be updated when the photo is generated.",
        vn = "Trang sẽ được cập nhật khi ảnh được tạo.",
        ru = "Страница будет обновлена после создания фотографии."
    )

    val pageTitleHintDetailsSharedWithThirdParty = Translation(
        en = "Page title, hint, and details are shared with a 3rd party.",
        vn = "Tiêu đề trang, gợi ý và chi tiết được chia sẻ với bên thứ ba.",
        ru = "Заголовок страницы, подсказка и детали передаются третьей стороне."
    )

    val dontShowThisAgain = Translation(
        en = "Don't show this again",
        vn = "Đừng hiển thị điều này nữa",
        ru = "Больше не показывать"
    )

    val thisWillReplaceCurrentPhoto = Translation(
        en = "This will replace the current photo.",
        vn = "Điều này sẽ thay thế ảnh hiện tại.",
        ru = "Это заменит текущую фотографию."
    )

    val generateNewPhoto = Translation(
        en = "Generate a new photo?",
        vn = "Tạo ảnh mới?",
        ru = "Создать новую фотографию?"
    )

    val deleteThisPage = Translation(
        en = "Delete this page?",
        vn = "Xóa trang này?",
        ru = "Удалить эту страницу?"
    )

    val youCannotUndoThis = Translation(
        en = "You cannot undo this.",
        vn = "Bạn không thể lấy lại điều này.",
        ru = "Это действие нельзя отменить."
    )

    val pageIsNotPublished = Translation(
        en = "Page is not published",
        vn = "Trang chưa được xuất bản",
        ru = "Страница не опубликована"
    )

    val required = Translation(
        en = "Required",
        vn = "Bắt buộc",
        ru = "Обязательно"
    )

    val pageIsPublished = Translation(
        en = "Page is published",
        vn = "Trang đã được xuất bản",
        ru = "Страница опубликована"
    )

    val reminderIsOpen = Translation(
        en = "Reminder is open",
        vn = "Lời nhắc đang mở",
        ru = "Напоминание открыто"
    )

    val reminderIsClosed = Translation(
        en = "Reminder is closed",
        vn = "Lời nhắc đã đóng",
        ru = "Напоминание закрыто"
    )

    val pageIsSaved = Translation(
        en = "Page is saved",
        vn = "Trang đã được lưu",
        ru = "Страница сохранена"
    )

    val accountDeletion = Translation(
        en = "Account Deletion",
        vn = "Xóa tài khoản",
        ru = "Удаление аккаунта"
    )

    val sendAnEmail = Translation(
        en = "Send an email",
        vn = "Gửi một email",
        ru = "Отправить письмо"
    )

    val sendAnEmailDeleteAccount = Translation(
        en = "containing your profile URL and Transfer Code to permanently delete your account.",
        vn = "chứa URL trang cá nhân của bạn và mã chuyển để xóa vĩnh viễn tài khoản của bạn.",
        ru = "содержащее URL вашего профиля и код передачи для безвозвратного удаления вашего аккаунта."
    )

    val sendAnEmailDeleteAccount2 = Translation(
        en = "You can find this information on your profile page.",
        vn = "Bạn có thể tìm thấy thông tin này trên trang trang cá nhân của mình.",
        ru = "Вы можете найти эту информацию на странице своего профиля."
    )

    val noBots = Translation(
        en = "No bots.",
        vn = "Không có bot.",
        ru = "Нет ботов."
    )

    val newBot = Translation(
        en = "New bot",
        vn = "Bot mới",
        ru = "Новый бот"
    )

    val noDescription = Translation(
        en = "No description",
        vn = "Không có mô tả",
        ru = "Нет описания"
    )

    val custom = Translation(
        en = "Custom",
        vn = "Tuỳ chỉnh",
        ru = "Пользовательский"
    )

    val messageReactions = Translation(
        en = "Message reactions",
        vn = "Phản ứng tin nhắn",
        ru = "Реакции на сообщения"
    )

    val getTheApp = Translation(
        en = "Get the app",
        vn = "Tải ứng dụng",
        ru = "Получить приложение"
    )

    val sentAPage = Translation(
        en = "Sent a page",
        vn = "Đã gửi một trang",
        ru = "Отправил страницу"
    )

    val sentAPhoto = Translation(
        en = "Sent a photo",
        vn = "Đã gửi một bức ảnh",
        ru = "Отправил фото"
    )

    val sentAnAudioMessage = Translation(
        en = "Sent an audio message",
        vn = "Đã gửi một tin nhắn âm thanh",
        ru = "Отправил голосовое сообщение"
    )

    val sentAVideo = Translation(
        en = "Sent a video",
        vn = "Đã gửi một video",
        ru = "Отправил видео"
    )

    val sentAStory = Translation(
        en = "Sent a story",
        vn = "Đã gửi một câu chuyện",
        ru = "Отправил историю"
    )

    val sentAGroup = Translation(
        en = "Sent a group",
        vn = "Đã gửi một nhóm",
        ru = "Отправил группу"
    )

    val sentASticker = Translation(
        en = "Sent a sticker",
        vn = "Đã gửi một nhãn dán",
        ru = "Отправил стикер"
    )

    val sentAProfile = Translation(
        en = "Sent a profile",
        vn = "Đã gửi một trang cá nhân",
        ru = "Отправил профиль"
    )

    val orGetItOn = Translation(
        en = "or get it on",
        vn = "hoặc tải xuống trên",
        ru = "или скачайте это на"
    )

    val googlePlay = Translation(
        en = "Google Play"
    )

    val iOSSupportComingSoon = Translation(
        en = "iOS support is coming soon!",
        vn = "Hỗ trợ iOS sẽ sớm ra mắt!",
        ru = "Поддержка iOS скоро появится!"
    )

    val dismiss = Translation(
        en = "Dismiss",
        vn = "Bỏ qua",
        ru = "Закрыть"
    )

    val history = Translation(
        en = "History",
        vn = "Lịch sử",
        ru = "История"
    )

    val fromUntil = Translation(
        en = "From %1\$s until %2\$s",
        vn = "Từ %1\$s đến %2\$s",
        ru = "С %1\$s до %2\$s"
    )

    val section = Translation(
        en = "Section",
        vn = "Phần",
        ru = "Раздел"
    )

    val write = Translation(
        en = "Write",
        vn = "Viết",
        ru = "Написать"
    )

    val whoSendsMessagesToThisGroup = Translation(
        en = "Who sends messages to this group?",
        vn = "Ai gửi tin nhắn đến nhóm này?",
        ru = "Кто отправляет сообщения в эту группу?"
    )

    val hosts = Translation(
        en = "Hosts",
        vn = "Chủ nhóm",
        ru = "Администраторы"
    )

    val everyone = Translation(
        en = "Everyone",
        vn = "Mọi người",
        ru = "Все"
    )

    val every = Translation(
        en = "Every",
        vn = "Mỗi",
        ru = "Каждый"
    )

    val whoEditsThisGroup = Translation(
        en = "Who edits this group?",
        vn = "Ai chỉnh sửa nhóm này?",
        ru = "Кто редактирует эту группу?"
    )

    val nameIntroductionPhoto = Translation(
        en = "Name, introduction, photo",
        vn = "Tên, giới thiệu, hình ảnh",
        ru = "Имя, описание, фото"
    )

    val createInviteLink = Translation(
        en = "Create an invite link",
        vn = "Tạo liên kết mời",
        ru = "Создать ссылку-приглашение"
    )

    val inviteLinkCouldNotBeCreated = Translation(
        en = "The invite link could not be created",
        vn = "Không thể tạo liên kết mời",
        ru = "Не удалось создать ссылку-приглашение"
    )

    val inviteLinkIsActive = Translation(
        en = "Invite link is active",
        vn = "Liên kết mời đang hoạt động",
        ru = "Ссылка-приглашение активна"
    )

    val copyLink = Translation(
        en = "Copy link",
        vn = "Sao chép liên kết",
        ru = "Копировать ссылку"
    )

    val tapToAnswer = Translation(
        en = "Tap to answer",
        vn = "Nhấn để trả lời",
        ru = "Нажмите, чтобы ответить"
    )
    val editNote = Translation(
        en = "Edit note",
        vn = "Chỉnh sửa ghi chú",
        ru = "Редактировать заметку"
    )
    val deleteOccurrence = Translation(
        en = "Delete this occurrence?",
        vn = "Xóa mục này?",
        ru = "Удалить это событие?"
    )
    val yesDelete = Translation(
        en = "Yes, delete",
        vn = "Vâng, xóa",
        ru = "Да, удалить"
    )
    val rescheduleOccurrence = Translation(
        en = "Reschedule occurrence",
        vn = "Lên lịch lại mục này",
        ru = "Перенести событие"
    )
    val unmarkAsDone = Translation(
        en = "Unmark as done",
        vn = "Bỏ đánh dấu hoàn thành",
        ru = "Отметить как невыполненное"
    )
    val markAsDone = Translation(
        en = "Mark as done",
        vn = "Đánh dấu hoàn thành",
        ru = "Отметить как выполненное"
    )
    val open = Translation(
        en = "Open",
        vn = "Mở",
        ru = "Открыть"
    )
    val peopleInCall = Translation(
        en = "%1\$s in call",
        vn = "%1\$s đang trong cuộc gọi",
        ru = "%1\$s в звонке"
    )
    val reaction = Translation(
        en = "Reaction",
        vn = "Phản ứng",
        ru = "Реакция"
    )
    val rating = Translation(
        en = "Rating",
        vn = "Đánh giá",
        ru = "Рейтинг"
    )

    val fullscreen = Translation(
        en = "Fullscreen",
        vn = "Toàn màn hình",
        ru = "На весь экран"
    )
    val microphone = Translation(
        en = "Microphone",
        vn = "Micro",
        ru = "Микрофон"
    )
    val camera = Translation(
        en = "Camera",
        vn = "Máy ảnh",
        ru = "Камера"
    )
    val shareScreen = Translation(
        en = "Share screen",
        vn = "Chia sẻ màn hình",
        ru = "Поделиться экраном"
    )
    val reoccurs = Translation(
        en = "Reoccurs",
        vn = "Lặp lại",
        ru = "Повторяется"
    )
    val everyDay = Translation(
        en = "Every day",
        vn = "Mỗi ngày",
        ru = "Каждый день"
    )
    val ofTheMonth = Translation(
        en = "%1\$s of the month",
        vn = "%1\$s của tháng",
        ru = "%1\$s месяца"
    )
    val lastDayOfTheMonth = Translation(
        en = "Last day of the month",
        vn = "Ngày cuối cùng của tháng",
        ru = "Последний день месяца"
    )
    val everyWeek = Translation(
        en = "Every week",
        vn = "Mỗi tuần",
        ru = "Каждую неделю"
    )
    val everyMonth = Translation(
        en = "Every month",
        vn = "Mỗi tháng",
        ru = "Каждый месяц"
    )
    val inlineUntil = Translation(
        en = "until",
        vn = "cho đến khi",
        ru = "до"
    )

    val nthWeek = Translation(
        en = "%1\$s week",
        vn = "%1\$s tuần",
        ru = "%1\$s неделя"
    )

    val inlineLast = Translation(
        en = "last",
        vn = "cuối cùng",
        ru = "последний"
    )

    val inlineDayOfTheMonth = Translation(
        en = "day of the month",
        vn = "ngày của tháng",
        ru = "день месяца"
    )

    val day = Translation(
        en = "Day",
        vn = "Ngày",
        ru = "День"
    )

    val inlineAt = Translation(
        en = "at",
        vn = "lúc",
        ru = "в"
    )

    val inlineDuring = Translation(
        en = "during",
        vn = "trong",
        ru = "в течение"
    )

    val inlineOf = Translation(
        en = "of",
        vn = "của",
        ru = ""
    )

    val inlineOfEveryMonth = Translation(
        en = "of every month",
        vn = "của mỗi tháng",
        ru = "каждого месяца"
    )

    val inlineFrom = Translation(
        en = "from",
        vn = "từ",
        ru = "с"
    )

    val inlineThe = Translation(
        en = "the",
        vn = "các",
        ru = ""
    )

    val tapToSwitch = Translation(
        en = "Tap to switch",
        vn = "Nhấn để chuyển đổi",
        ru = "Нажмите, чтобы переключить"
    )

    val personCommentedOnYourStory = Translation(
        en = "%1\$s commented on your post",
        vn = "%1\$s đã bình luận về bài viết của bạn",
        ru = "%1\$s прокомментировал ваш пост"
    )

    val personRepliedToYourComment = Translation(
        en = "%1\$s replied to your comment",
        vn = "%1\$s đã trả lời bình luận của bạn",
        ru = "%1\$s ответил на ваш комментарий"
    )

    val openPage = Translation(
        en = "Open page",
        vn = "Mở trang",
        ru = "Открыть страницу"
    )
    val posts = Translation(
        en = "Posts",
        vn = "Bài viết",
        ru = "Посты"
    )
    val appName = Translation(
        "Hi Town",
        "Chào Town",
        "Город приветик"
    )
    val rootPages = Translation(
        en = "Root pages",
        vn = "Trang gốc",
        ru = "Корневые страницы"
    )
    val actionOpenGroup = Translation(
        en = "Open group",
        vn = "Mở nhóm",
        ru = "Открыть группу"
    )
    val actionCloseGroup = Translation(
        en = "Close group",
        vn = "Đóng nhóm",
        ru = "Закрыть группу"
    )
    val makeOpenGroup = Translation(
        en = "Make group open",
        vn = "Chuyển sang nhóm mở",
        ru = "Сделать группу открытой"
    )
    val makeCloseGroup = Translation(
        en = "Make group closed",
        vn = "Chuyển sang nhóm đóng",
        ru = "Сделать группу закрытой"
    )
    val actionOpenGroupDescription = Translation(
        en = "Anyone will be able to discover this group, see all members, messages, and request to become a member.",
        vn = "Bất kì ai cũng có thể khám phá nhóm này, xem tất cả thành viên, tin nhắn và yêu cầu trở thành thành viên.",
        ru = "Любой сможет найти эту группу, увидеть всех участников, сообщения и запросить членство."
    )
    val actionCloseGroupDescription = Translation(
        en = "This group will only be accessible to members.",
        vn = "Nhóm này sẽ chỉ có thể truy cập được đối với các thành viên.",
        ru = "Эта группа будет доступна только участникам."
    )
    val clickToHide = Translation(
        en = "Click to hide",
        vn = "Nhấn để mở",
        ru = "Нажмите, чтобы скрыть"
    )
    val error = Translation(
        en = "Error",
        vn = "Lỗi",
        ru = "Ошибка"
    )
    val note = Translation(
        en = "Note",
        vn = "Ghi chú",
        ru = "Заметка"
    )
    val messages = Translation(
        en = "Messages",
        vn = "Tin nhắn",
        ru = "Сообщения"
    )
    val orEnterTransferCode = Translation(
        en = "Or enter a transfer code here",
        vn = "Hoặc nhập mã chuyển khoản tại đây",
        ru = "Или введите код передачи здесь"
    )
    val scanQrCode = Translation(
        en = "Use your phone to scan the QR code.",
        vn = "Sử dụng điện thoại của bạn để quét mã QR.",
        ru = "Используйте телефон для сканирования QR-кода."
    )
    val openGroup = Translation(
        en = "Open group",
        vn = "Nhóm mở",
        ru = "Открытая группа"
    )
    val accept = Translation(
        en = "Accept",
        vn = "Chấp nhận",
        ru = "Принять"
    )
    val delete = Translation(
        en = "Delete",
        vn = "Xóa bỏ",
        ru = "Удалить"
    )
    val info = Translation(
        en = "Info",
        vn = "Thông tin",
        ru = "Информация"
    )
    val edit = Translation(
        en = "Edit",
        vn = "Chỉnh sửa",
        ru = "Редактировать"
    )
    val remove = Translation(
        en = "Remove",
        vn = "Bỏ",
        ru = "Удалить"
    )
    val inlineMember = Translation(
        en = "member",
        vn = "thành viên",
        ru = "участник"
    )
    val inlineMembers = Translation(
        en = "members",
        vn = "thành viên",
        ru = "участники"
    )
    val close = Translation(
        en = "Close",
        vn = "Đóng",
        ru = "Закрыть"
    )
    val manage = Translation(
        en = "Manage",
        vn = "Quản lý",
        ru = "Управлять"
    )
    val selectAGroup = Translation(
        en = "Select a group",
        vn = "Chọn một nhóm",
        ru = "Выберите группу"
    )
    val introduction = Translation(
        en = "Introduction",
        vn = "Giới thiệu",
        ru = "Введение"
    )
    val stories = Translation(
        en = "Posts",
        vn = "Bài viết",
        ru = "Посты"
    )
    val createStory = Translation(
        en = "New post",
        vn = "Tạo bài viết",
        ru = "Новый пост"
    )
    val newStory = Translation(
        en = "New post",
        vn = "Bài viết mới",
        ru = "Новый пост"
    )
    val newCard = Translation(
        en = "New page",
        vn = "Trang mới",
        ru = "Новая страница"
    )
    val someone = Translation(
        en = "Someone",
        vn = "Một người ai đó",
        ru = "Кто-то"
    )
    val goBack = Translation(
        en = "Go back",
        vn = "Quay lại",
        ru = "Вернуться"
    )
    val inlineCard = Translation(
        en = "page",
        vn = "trang",
        ru = "страница"
    )
    val inlineCards = Translation(
        en = "pages",
        vn = "trang",
        ru = "страницы"
    )
    val cards = Translation(
        en = "Pages",
        vn = "Trang",
        ru = "Страницы"
    )
    val friends = Translation(
        en = "Friends",
        vn = "Bạn bè",
        ru = "Друзья"
    )
    val explore = Translation(
        en = "Explore",
        vn = "Khám phá",
        ru = "Исследовать"
    )
    val map = Translation(
        en = "Map",
        vn = "Bản đồ",
        ru = "Карта"
    )
    val tapToOpen = Translation(
        en = "Tap to open",
        vn = "Nhấn để mở",
        ru = "Нажмите, чтобы открыть"
    )
    val profileNotFound = Translation(
        en = "Profile not found.",
        vn = "Không tìm được trang cá nhân này.",
        ru = "Профиль не найден."
    )
    val storyNotFound = Translation(
        en = "Post not found.",
        vn = "Không tìm được bài viết này.",
        ru = "Пост не найден."
    )
    val groupNotFound = Translation(
        en = "Group not found.",
        vn = "Không tìm được nhóm này.",
        ru = "Группа не найдена."
    )
    val inviteNotFound = Translation(
        en = "Invite not found.",
        vn = "Không tìm được lời mời này.",
        ru = "Приглашение не найдено."
    )
    val cardNotFound = Translation(
        "Page not found.",
        "Không tìm được trang này.",
        "Карта не найдена."
    )
    val joined = Translation(
        "Joined",
        "Đã tham gia",
        "Присоединился"
    )
    val inlineBy = Translation(
        en = "by",
        vn = "bởi",
        ru = "от"
    )
    val inlineAnd = Translation(
        en = "and",
        vn = "và",
        ru = "и"
    )
    val draft = Translation(
        "Draft",
        "Bản nháp",
        "Черновик"
    )
    val message = Translation(
        "Message",
        "Nhắn tin",
        "Cообщение"
    )
    val sending = Translation(
        en = "Sending...",
        vn = "Đang gửi...",
        ru = "Отправка..."
    )
    val cancel = Translation(
        "Cancel",
        "Hủy",
        "Отмена"
    )
    val okay = Translation(
        en = "Okay",
        vn = "Đòng ý",
        ru = "Хорошо"
    )
    val sendMessage = Translation(
        en = "Send message",
        vn = "Gửi tin nhắn",
        ru = "Отправить сообщение"
    )
    val sendPhoto = Translation(
        en = "Send photo",
        vn = "Gửi ảnh",
        ru = "Отправить фото"
    )
    val viewProfile = Translation(
        "View profile",
        "Xem trang cá nhân",
        "Просмотреть профиль"
    )
    val profile = Translation(
        "Profile",
        "Trang cá nhân",
        "Профиль"
    )
    val didntWork = Translation(
        "That didn't work",
        "Điều đó đã không làm được",
        "Это не сработало"
    )
    val messageWasSent = Translation(
        "Your message was sent!",
        "Tin nhắn của bạn đã được gửi!",
        "Ваше сообщение отправлено!"
    )
    val homeTagline = Translation(
        "Be the life of the city!",
        "Hãy cùng làm thành phố\n" +
                "năng động và đầy màu sắc!",
        "Расширяйте и оживляйте свой город"
    )
    val downloadApp = Translation(
        "Download ${appName.en} Beta for Android",
        "Tải xuống ${appName.vn} beta cho Android",
        "Скачать ${appName.ru} Бета для Андроид"
    )
    val appTagline = Translation(
        "Local Messaging, Exploration, and Inspiration",
        "Nhắn tin, khám phá và đọc cảm hứng ở gần",
        "Локальный обмен сообщениями, торговая площадка и вдохновение"
    )
    val homeAboutTitle = Translation(
        "What is ${appName.en}?",
        "${appName.vn} là gì?", "Что такое ${appName.ru}?"
    )
    val homeAboutDescription = Translation(
        "${appName.en} is a collaboration platform that helps you discover and stay connected to your city, enabling you to do more, externalize all of your visions, and go farther than you ever imagined. Think of it as a home page for your city!",
        "${appName.vn} là một nền tảng hợp tác giúp bạn khám phá và giữ liên lạc với mọi người trong thành phố của bạn, cho phép bạn làm được nhiều hơn, hiện thực hóa tất cả tầm nhìn của bạn và tiến xa hơn những gì bạn tưởng tượng. Hãy nghĩ về ${appName.vn} như một trang chủ cho thành phố của bạn!",
        ""
    )
    val inviteEmailSubject = Translation(
        "${appName.en} invite to join",
        "${appName.vn} lời mời tham gia",
        "Ай ла ай приглашаю присоединиться"
    )
    val peopleToKnow = Translation(
        "People to know",
        "Những người cần biết",
        "Люди, которых знать"
    )
    val placesToKnow = Translation(
        "Places to know",
        "Những chỗ cần biết",
        "Места, чтобы знать"
    )
    val madeWith = Translation(
        "Made with",
        "Tạo với",
        "Сделано с"
    )
    val inHCMC = Translation(
        "in HCMC",
        "ở TP.HCM",
        "в Хошимине"
    )
    val contact = Translation(
        en = "Contact",
        vn = "Liên hệ",
        ru = "Связаться"
    )
    val isCreatedBy = Translation(
        en = "${appName.en} is being created by ",
        vn = "${appName.vn} đang tạo ra bởi ",
        ru = "${appName.ru} создается "
    )
    val sendMeAnEmail = Translation(
        "Send me an email",
        "gửi email cho tôi",
        "Вышли мне электронное письмо"
    )
    val forAllInquiries = Translation(
        en = " for all inquiries.",
        vn = " cho tất cả các yêu cầu nha.",
        ru = " по всем вопросам."
    )
    val searching = Translation(
        en = "Searching…",
        vn = "Đang tìm kiếm…",
        ru = "Поиск…"
    )
    val searchResults = Translation(
        en = "Search results",
        vn = "Kết quả tìm kiếm",
        ru = "Результаты поиска"
    )
    val noCards = Translation(
        "No pages",
        "Không tìm được trang để cho bạn xem.",
        ru = "Нет страниц"
    )
    val noSavedCards = Translation(
        "No saved pages",
        "Bạn chưa lưu trang",
        ru = "Нет сохраненных страниц"
    )
    val noCardsNearby = Translation(
        "No pages nearby.",
        "Không tìm được trang gần đây.",
        ru = "Нет страниц поблизости."
    )
    val noStories = Translation(
        "No posts",
        "Không tìm được bài viết.",
        ru = "Нет постов"
    )
    val noGroups = Translation(
        "No groups",
        "Không tìm được nhóm.",
        ru = "Нет групп"
    )
    val search = Translation(
        "Search",
        "Tìm kiếm",
        ru = "Поиск"
    )
    val privacyPolicy = Translation(
        "Privacy Policy",
        "Chính sách bảo mật",
        ru = "Политика конфиденциальности"
    )
    val tos = Translation(
        "Terms of Use",
        "Điều khoản sử dụng",
        ru = "Условия использования"
    )
    val openSource = Translation(
        "Open Source",
        "Mã nguồn mở",
        ru = "Открытый исходный код"
    )
    val signIn = Translation(
        en = "Sign in",
        vn = "Đăng nhập",
        ru = "Войти"
    )
    val signUp = Translation(
        en = "Sign up",
        vn = "Đăng ký",
        ru = "Регистрация"
    )
    val signOut = Translation(
        en = "Sign out",
        vn = "Đăng xuất",
        ru = "Выйти"
    )
    val signOutOrTransfer = Translation(
        en = "Sign out or transfer",
        vn = "Đăng xuất hoặc chuyển",
        ru = "Выйти или перенести"
    )
    val signOutQuestion = Translation(
        en = "Sign out?",
        vn = "Đăng xuất?",
        ru = "Выйти?"
    )
    val signOutQuestionLine1 = Translation(
        en = "You will permanently lose access to this account",
        vn = "Bạn sẽ vĩnh viễn mất quyền truy cập vào tài khoản này",
        ru = "Вы навсегда потеряете доступ к этому аккаунту"
    )
    val signOutQuestionLine2 = Translation(
        en = "if you are not currently signed in on another device.",
        vn = "nếu bạn hiện chưa đăng nhập trên thiết bị khác.",
        ru = "если вы не вошли в систему на другом устройстве."
    )
    val yourTransferCodeIs = Translation(
        en = "Your transfer code is:",
        vn = "Mã chuyển của bạn là:",
        ru = "Ваш код переноса:"
    )
    val signOutWarning = Translation(
        en = "Email this code to yourself or keep it somewhere where you won't lose it!\n\nYou will need this transfer code to regain access to your account after signing out.",
        vn = "Gửi email mã này cho chính bạn hoặc giữ nó ở nơi bạn sẽ không mất nó!\n\nBạn sẽ cần mã chuyển này để truy cập lại tài khoản của mình sau khi đăng xuất.",
        ru = "Отправьте этот код себе на почту или сохраните его в надежном месте!\n\nЭтот код переноса понадобится вам для восстановления доступа к аккаунту после выхода."
    )
    val signOutDescription = Translation(
        en = "If you sign out without a transfer code you will permanently lose access to your account!",
        vn = "Nếu bạn đăng xuất mà không có mã chuyển, bạn sẽ vĩnh viễn mất quyền truy cập vào tài khoản của mình!",
        ru = "Если вы выйдете без кода переноса, вы навсегда потеряете доступ к своему аккаунту!"
    )
    val showTransferCode = Translation(
        en = "Show transfer code",
        vn = "Hiển thị mã chuyển",
        ru = "Показать код переноса"
    )
    val and = Translation(
        en = "and",
        vn = "và",
        ru = "и"
    )
    val qrCode = Translation(
        en = "QR code",
        vn = "Mã QR",
        ru = "QR-код"
    )
    val yourName = Translation(
        en = "Your name",
        vn = "Tên của bạn",
        ru = "Ваше имя"
    )
    val update = Translation(
        en = "Update",
        vn = "Cập nhật",
        ru = "Обновить"
    )
    val introduceYourself = Translation(
        en = "Introduce yourself here",
        vn = "Viết chút về bạn",
        ru = "Представьтесь здесь"
    )
    val filter = Translation(
        en = "Filter",
        vn = "Cập nhật",
        ru = "Фильтр"
    )
    val published = Translation(
        en = "Published",
        vn = "Xuất bản",
        ru = "Опубликовано"
    )
    val notPublished = Translation(
        en = "Not published",
        vn = "Không xuất bản",
        ru = "Не опубликовано"
    )
    val createCard = Translation(
        en = "New page",
        vn = "Tạo trang",
        ru = "Новая страница"
    )
    val title = Translation(
        en = "Title",
        vn = "Tiêu đề",
        ru = "Заголовок"
    )
    val create = Translation(
        en = "Create",
        vn = "Tạo",
        ru = "Создать"
    )
    val createGroup = Translation(
        en = "Create group",
        vn = "Tạo nhóm",
        ru = "Создать группу"
    )
    val createOpenGroup = Translation(
        en = "Create open group",
        vn = "Tạo nhóm mở",
        ru = "Создать открытую группу"
    )
    val local = Translation(
        en = "Local",
        vn = "Gần đây",
        ru = "Местные"
    )
    val saved = Translation(
        en = "Saved",
        vn = "Đã lưu",
        ru = "Сохранённые"
    )
    val groups = Translation(
        en = "Groups",
        vn = "Nhóm",
        ru = "Группы"
    )
    val groupName = Translation(
        "Group name",
        "Tên nhóm",
        ru = "Название группы"
    )
    val reminders = Translation(
        "Reminders",
        "Lời nhắc",
        ru = "Напоминания"
    )
    val reminder = Translation(
        "Reminder",
        "Lời nhắc",
        ru = "Напоминание"
    )
    val noReminders = Translation(
        "No reminders",
        "Không có lời nhắc",
        ru = "Нет напоминаний"
    )
    val createReminder = Translation(
        "Create reminder",
        "Tạo lời nhắc",
        ru = "Создать напоминание"
    )
    val hourly = Translation(
        "Hourly",
        "Theo giờ",
        ru = "Почасовой"
    )
    val daily = Translation(
        "Daily",
        "Theo ngày",
        ru = "Ежедневный"
    )
    val weekly = Translation(
        "Weekly",
        "Theo tuần",
        ru = "Еженедельный"
    )
    val stickiness = Translation(
        en = "Stickiness",
        vn = "Độ bám",
        ru = "Прилипание"
    )
    val monthly = Translation(
        "Monthly",
        "Theo tháng",
        ru = "Ежемесячный"
    )
    val yearly = Translation(
        "Yearly",
        "Theo năm",
        ru = "Ежегодный"
    )
    val inlineHour = Translation(
        en = "hour",
        vn = "tiếng",
        ru = "час"
    )
    val inlineHours = Translation(
        en = "hours",
        vn = "tiếng",
        ru = "часов"
    )
    val inlineMinute = Translation(
        en = "minute",
        vn = "phút",
        ru = "минута"
    )
    val inlineMinutes = Translation(
        en = "minutes",
        vn = "phút",
        ru = "минут"
    )
    val inlineDay = Translation(
        en = "day",
        vn = "ngày",
        ru = "день"
    )
    val inlineWeekly = Translation(
        en = "week",
        vn = "tuần",
        ru = "неделя"
    )
    val inlineMonthly = Translation(
        en = "month",
        vn = "tháng",
        ru = "месяц"
    )
    val inlineYearly = Translation(
        en = "year",
        vn = "năm",
        ru = "год"
    )
    val you = Translation(
        en = "You",
        vn = "Bạn",
        ru = "Вы"
    )
    val newGroup = Translation(
        en = "New group",
        vn = "Nhóm mới",
        ru = "Новая группа"
    )
    val newReminder = Translation(
        en = "New reminder",
        vn = "Lời nhắc mới",
        ru = "Новое напоминание"
    )
    val created = Translation(
        en = "Created",
        vn = "Đã tạo",
        ru = "Создано"
    )
    val inlinePersonWaiting = Translation(
        en = "person waiting",
        vn = "người đang chờ",
        ru = "человек ожидает"
    )
    val inlinePeopleWaiting = Translation(
        en = "people waiting",
        vn = "người đang chờ",
        ru = "человек ожидают"
    )
    val inlineIsAMember = Translation(
        en = "is a member",
        vn = "là thành viên",
        ru = "является участником"
    )
    val onProfile = Translation(
        en = "On profile",
        vn = "Trên trang cá nhân",
        ru = "В профиле"
    )
    val inAGroup = Translation(
        en = "In a group",
        vn = "Trong nhóm",
        ru = "В группе"
    )
    val atALocation = Translation(
        en = "At a location",
        vn = "Tại một vị trí nào đó",
        ru = "В локации"
    )
    val inAPage = Translation(
        en = "In a page",
        vn = "Trong trang",
        ru = "На странице"
    )
    val none = Translation(
        en = "None",
        vn = "Không có",
        ru = "Нет"
    )
    val choosePhoto = Translation(
        en = "Choose photo",
        vn = "Chọn ảnh",
        ru = "Выбрать фото"
    )
    val choose = Translation(
        en = "Choose",
        vn = "Chọn",
        ru = "Выбрать"
    )
    val describePhoto = Translation(
        en = "Describe photo",
        vn = "Mô tả ảnh",
        ru = "Описание фото"
    )
    val photoCount = Translation(
        en = "Count",
        vn = "Số lượng",
        ru = "Количество"
    )
    val generatePhoto = Translation(
        en = "Generate photo",
        vn = "Tạo ảnh",
        ru = "Сгенерировать фото"
    )
    val confirm = Translation(
        en = "Confirm",
        vn = "Xác nhận",
        ru = "Подтвердить"
    )
    val regeneratePhoto = Translation(
        en = "Regenerate photo",
        vn = "Tạo ảnh mới",
        ru = "Сгенерировать новое фото"
    )
    val openEnclosingCard = Translation(
        en = "Open enclosing page",
        vn = "Mở trang có trang này",
        ru = "Открыть содержащую страницу"
    )
    val previousDay = Translation(
        en = "Previous day",
        vn = "Ngày trước",
        ru = "Предыдущий день"
    )
    val previousWeek = Translation(
        en = "Previous week",
        vn = "Tuần trước",
        ru = "Предыдущая неделя"
    )
    val previousMonth = Translation(
        en = "Previous month",
        vn = "Tháng trước",
        ru = "Предыдущий месяц"
    )
    val previousYear = Translation(
        en = "Previous year",
        vn = "Năm trước",
        ru = "Предыдущий год"
    )
    val nextDay = Translation(
        en = "Next day",
        vn = "Ngày sau",
        ru = "Следующий день"
    )
    val nextWeek = Translation(
        en = "Next week",
        vn = "Tuần sau",
        ru = "Следующая неделя"
    )
    val nextMonth = Translation(
        en = "Next month",
        vn = "Tháng sau",
        ru = "Следующий месяц"
    )
    val nextYear = Translation(
        en = "Next year",
        vn = "Năm sau",
        ru = "Следующий год"
    )
    val today = Translation(
        en = "Today",
        vn = "Hôm nay",
        ru = "Сегодня"
    )
    val yesterday = Translation(
        en = "Yesterday",
        vn = "Hôm qua",
        ru = "Вчера"
    )
    val tomorrow = Translation(
        en = "Tomorrow",
        vn = "Ngày mai",
        ru = "Завтра"
    )
    val loading = Translation(
        en = "Loading",
        vn = "Đang tải",
        ru = "Загрузка"
    )
    val rename = Translation(
        en = "Rename",
        vn = "Đổi tên",
        ru = "Переименовать"
    )
    val category = Translation(
        en = "Category",
        vn = "Danh mục",
        ru = "Категория"
    )
    val reschedule = Translation(
        en = "Reschedule",
        vn = "Lên lịch lại",
        ru = "Перенести"
    )
    val schedule = Translation(
        en = "Schedule",
        vn = "Lịch",
        ru = "Расписание"
    )
    val timezone = Translation(
        en = "Timezone",
        vn = "Múi giờ",
        ru = "Часовой пояс"
    )
    val duration = Translation(
        en = "Duration",
        vn = "Khoảng thời gian",
        ru = "Продолжительность"
    )
    val openInNewTab = Translation(
        en = "Open in new tab",
        vn = "Mở ra trong tab mới",
        ru = "Открыть в новой вкладке"
    )
    val options = Translation(
        en = "Options",
        vn = "Tùy chọn",
        ru = "Настройки"
    )
    val unsave = Translation(
        en = "Unsave",
        vn = "Bỏ lưu",
        ru = "Отменить сохранение"
    )
    val save = Translation(
        en = "Save",
        vn = "Lưu",
        ru = "Сохранить"
    )
    val fork = Translation(
        en = "Fork",
        vn = "Tạo bản sao",
        ru = "Форк"
    )
    val hint = Translation(
        en = "Hint",
        vn = "Lời gợi ý",
        ru = "Подсказка"
    )
    val location = Translation(
        en = "Location",
        vn = "Vị trí",
        ru = "Местоположение"
    )
    val details = Translation(
        en = "Details",
        vn = "Chi tiết",
        ru = "Детали"
    )
    val members = Translation(
        en = "Members",
        vn = "Thành viên",
        ru = "Участники"
    )
    val invite = Translation(
        en = "Invite",
        vn = "Mời ai đó",
        ru = "Пригласить"
    )
    val hide = Translation(
        en = "Hide",
        vn = "Ẩn",
        ru = "Скрыть"
    )
    val pin = Translation(
        en = "Pin",
        vn = "Ghim",
        ru = "Закрепить"
    )
    val unpin = Translation(
        en = "Unpin",
        vn = "Bỏ ghim",
        ru = "Открепить"
    )
    val leave = Translation(
        en = "Leave",
        vn = "Rời khỏi",
        ru = "Покинуть"
    )
    val leaveGroup = Translation(
        en = "Leave group?",
        vn = "Rời khỏi nhóm này không?",
        ru = "Покинуть группу?"
    )
    val active = Translation(
        en = "Active",
        vn = "Truy cập",
        ru = "Активный"
    )
    val discard = Translation(
        en = "Discard",
        vn = "Bỏ",
        ru = "Отменить"
    )
    val joinGroup = Translation(
        en = "Join group",
        vn = "Tham gia nhóm",
        ru = "Присоединиться к группе"
    )
    val sendRequest = Translation(
        en = "Send request",
        vn = "Gửi yêu cầu",
        ru = "Отправить запрос"
    )
    val cancelJoinRequest = Translation(
        en = "Cancel join request",
        vn = "Hủy yêu cầu tham gia",
        ru = "Отменить запрос на вступление"
    )
    val stickers = Translation(
        en = "Stickers",
        vn = "Hình dán",
        ru = "Стикеры"
    )
    val loadingGroup = Translation(
        en = "Loading group…",
        vn = "Đang tải nhóm…",
        ru = "Загрузка группы…"
    )
    val loadingComments = Translation(
        en = "Loading comments…",
        vn = "Đang tải bình luận…",
        ru = "Загрузка комментариев…"
    )
    val addPay = Translation(
        en = "Add pay",
        vn = "Thêm lương",
        ru = "Добавить оплату"
    )
    val changePay = Translation(
        en = "Change pay",
        vn = "Đổi lương",
        ru = "Изменить оплату"
    )
    val pay = Translation(
        en = "Pay",
        vn = "Lương",
        ru = "Оплата"
    )
    val connect = Translation(
        en = "Connect",
        vn = "Kết nối",
        ru = "Подключиться"
    )
    val searchPagesOfPerson = Translation(
        en = "Search %1\$s's pages",
        vn = "Tìm kiếm trang của %1\$s",
        ru = "Поиск страниц %1\$s"
    )
    val call = Translation(
        en = "Call",
        vn = "Gọi",
        ru = "Звонок"
    )
    val minimize = Translation(
        en = "Minimize",
        vn = "Thu gọn",
        ru = "Свернуть"
    )
    val maximize = Translation(
        en = "Maximize",
        vn = "Mở rộng",
        ru = "Развернуть"
    )
    val subscribers = Translation(
        en = "Subscribers",
        vn = "Người đăng ký",
        ru = "Подписчики"
    )
    val host = Translation(
        en = "Host",
        vn = "Chủ nhóm",
        ru = "Владелец"
    )
    val settings = Translation(
        en = "Settings",
        vn = "Cài đặt",
        ru = "Настройки"
    )
    val effects = Translation(
        en = "Effects",
        vn = "Hiệu ứng",
        ru = "Эффекты"
    )
    val shareAComment = Translation(
        en = "Share a comment",
        vn = "Chia sẻ một bình luận",
        ru = "Поделиться комментарием"
    )
    val post = Translation(
        en = "Post",
        vn = "Đăng",
        ru = "Опубликовать"
    )
    val signInToComment = Translation(
        en = "Sign in to comment",
        vn = "Đăng nhập để chia sẻ một bình luận",
        ru = "Войдите, чтобы комментировать"
    )
    val signInToReply = Translation(
        en = "Sign in to reply",
        vn = "Đăng nhập để trả lời",
        ru = "Войдите, чтобы ответить"
    )
    val reply = Translation(
        en = "Reply",
        vn = "Trả lời",
        ru = "Ответить"
    )
    val react = Translation(
        en = "React",
        vn = "Phản ứng",
        ru = "Реакция"
    )
    val replyInNewGroup = Translation(
        en = "Reply in new group",
        vn = "Trả lời trong nhóm mới",
        ru = "Ответить в новой группе"
    )
    val inlineReply = Translation(
        en = "reply",
        vn = "câu trả lời",
        ru = "ответ"
    )
    val inlineReplies = Translation(
        en = "replies",
        vn = "câu trả lời",
        ru = "ответы"
    )
    val replyTo = Translation(
        en = "Reply to",
        vn = "Trả lời",
        ru = "Ответить"
    )
    val rain = Translation(
        en = "Rain",
        vn = "Mưa",
        ru = "Дождь"
    )
    val commentReplies = Translation(
        en = "Comment replies",
        vn = "Trả lời bình luận",
        ru = "Ответы на комментарий"
    )
    val showAllComment = Translation(
        en = "Show %1\$s more comment",
        vn = "Coi thêm %1\$s bình luận",
        ru = "Показать ещё %1\$s комментарий"
    )
    val showAllComments = Translation(
        en = "Show %1\$s more comments",
        vn = "Coi thêm %1\$s bình luận",
        ru = "Показать ещё %1\$s комментариев"
    )
    val platform = Translation(
        en = "Platform",
        vn = "Nền tảng",
        ru = "Платформа"
    )
    val bots = Translation(
        en = "Bots",
        vn = "Bot",
        ru = "Боты"
    )
    val addBot = Translation(
        en = "Add a bot",
        vn = "Thêm bot",
        ru = "Добавить бота"
    )
    val createBot = Translation(
        en = "Create a bot",
        vn = "Tạo bot",
        ru = "Создать бота"
    )
    val openBot = Translation(
        en = "Open bot",
        vn = "Mở bot",
        ru = "Открыть бота"
    )
    val groupBots = Translation(
        en = "Group bots",
        vn = "Bot của nhóm",
        ru = "Боты группы"
    )
    val now = Translation(
        en = "Now",
        vn = "Bây giờ",
        ru = "Сейчас"
    )
    val submit = Translation(
        en = "Submit",
        vn = "Nộp",
        ru = "Отправить"
    )
    val scripts = Translation(
        en = "Scripts",
        vn = "Tập lệnh",
        ru = "Скрипты"
    )
    val shareAThought = Translation(
        en = "Share a thought",
        vn = "Chia sẻ những suy nghĩ của bạn",
        ru = "Поделитесь мыслью"
    )
    val offline = Translation(
        en = "Offline",
        vn = "Ngoài đời",
        ru = "Оффлайн"
    )
    val multiple = Translation(
        en = "Multiple",
        vn = "Nhiều",
        ru = "Множество"
    )
}

/**
 * Retrieves the appropriate translation of a given string based on the specified language.
 *
 * @param string The `Translation` object containing translations in multiple languages.
 * @param language The language code to select the translation ("vi" for Vietnamese, "ru" for Russian, default to English).
 * @return The translated string in the requested language.
 */
fun getString(string: Translation, language: String) = when (language) {
    "vi" -> string.vn
    "ru" -> string.ru
    else -> string.en
}

/**
 * A composable function to fetch the translated string specifically for use
 * in the application's user interface based on the current language configuration.
 *
 * @param string The `Translation` object to translate.
 * @return The localized string for the current language.
 */
@Composable
private fun appString(string: Translation) = getString(string, LocalConfiguration.current.language)

/**
 * A composable function that takes a block to fetch a specific translation
 * from the `Strings` object in the current language configuration.
 *
 * @param block A lambda to retrieve a `Translation` object from `Strings`.
 * @return The localized string for the current language.
 */
@Composable
fun appString(block: Strings.() -> Translation) = appString(block(Strings))

/**
 * A composable function that directly displays a localized string based on
 * the current language configuration.
 *
 * @param block A lambda to retrieve a `Translation` object from `Strings`.
 */
@Composable
fun appText(block: Strings.() -> Translation) = Text(appString(block))

/**
 * Holds the application's language configuration and allows switching between languages.
 *
 * @property language The currently selected language code (default is "en").
 * @constructor Initializes with a default language and a callback to change the language.
 */
class Configuration(
    var language: String = "en",
    private val onLanguage: (String) -> Unit,
) {
    fun set(language: String) {
        onLanguage(language)
    }
}

/**
 * A CompositionLocal that provides the current language configuration throughout the application.
 * This helps dynamically update UI elements based on language changes.
 */
val LocalConfiguration = compositionLocalOf(
    neverEqualPolicy()
) {
Configuration(application.language) {}
}


/**
 * Formats a string with placeholder replacements. The placeholders must follow the pattern %1$s, %2$s, etc.
 * Example: "Hello, %1$s!".format("world") => "Hello, world!"
 *
 * @receiver The string containing placeholders.
 * @param values The values to replace the placeholders.
 * @return The formatted string with placeholders replaced by the provided values.
 */
fun String.format(vararg values: String) =
    values.foldIndexed(this) { index, acc, it ->
        acc.replace("%${index + 1}\$s", it)
    }
