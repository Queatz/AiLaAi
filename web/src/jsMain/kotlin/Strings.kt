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
    val viewReactions = Translation(
        en = "View reactions",
        vn = "Xem phản ứng"
    )
    val rate = Translation(
        en = "Rate",
        vn = "Đánh giá"
    )
    val editMessage = Translation(
        en = "Edit message",
        vn = "Chỉnh sửa tin nhắn"
    )
    val deleteThisMessage = Translation(
        en = "Delete this message?",
        vn = "Xóa tin nhắn này?"
    )
    val sentAt = Translation(
        en = "Sent at %1\$s",
        vn = "Đã gửi lúc %1\$s"
    )
    val tapToReact = Translation(
        en = "Tap to react",
        vn = "Nhấn để phản ứng"
    )
    val clearAll = Translation(
        en = "Clear all",
        vn = "Xóa tất cả"
    )
    val yourProfileUrl = Translation(
        en = "Your profile URL",
        vn = "URL trang cá nhân của bạn"
    )
    val copied = Translation(
        en = "Copied!",
        vn = "Đã sao chép!"
    )
    val copyProfileLink = Translation(
        en = "Copy profile link",
        vn = "Sao chép liên kết trang cá nhân"
    )
    val openProfile = Translation(
        en = "Open profile",
        vn = "Mở trang cá nhân"
    )
    val hour = Translation(
        en = "Hour",
        vn = "Giờ"
    )
    val minute = Translation(
        en = "Minute",
        vn = "Phút"
    )
    val hours = Translation(
        en = "Hours",
        vn = "Giờ"
    )
    val minutes = Translation(
        en = "Minutes",
        vn = "Phút"
    )
    val switchView = Translation(
        en = "Switch view",
        vn = "Chuyển chế độ xem"
    )
    val newScript = Translation(
        en = "New script",
        vn = "Tập lệnh mới"
    )
    val removeRating = Translation(
        en = "Remove rating",
        vn = "Xóa xếp hạng"
    )
    val help = Translation(
        en = "Help",
        vn = "Trợ giúp"
    )
    val yourRatingsAreVisible = Translation(
        en = "Your ratings are only visible to you.",
        vn = "Xếp hạng của bạn chỉ hiển thị với bạn."
    )
    val text = Translation(
        en = "Text",
        vn = "Văn bản"
    )
    val openLink = Translation(
        en = "Open link",
        vn = "Mở liên kết"
    )
    val appUseCases = Translation(
        en = "Hi Town Use Cases",
        vn = "Trường hợp sử dụng Hi Town"
    )
    
    val restart = Translation(
        en = "Restart",
        vn = "Khởi động lại"
    )
    val errorSubmittingForm = Translation(
        en = "There was an error submitting the form.",
        vn = "Đã xảy ra lỗi khi gửi biểu mẫu."
    )
    val tryAgainOrContact = Translation(
        en = "Please try again or contact the form owner.",
        vn = "Vui lòng thử lại hoặc liên hệ với chủ sở hữu biểu mẫu."
    )
    val please = Translation(
        "",
        "Hãy "
    )
    val formSubmitted = Translation(
        en = "Form submitted!",
        vn = "Biểu mẫu đã được gửi!"
    )
    val tapToRemove = Translation(
        en = "Tap to remove",
        vn = "Nhấn để xóa"
    )
    val removeThisPhoto = Translation(
        en = "Remove this photo?",
        vn = "Xóa ảnh này?"
    )
    val addPhotos = Translation(
        en = "Add photos",
        vn = "Thêm ảnh"
    )
    val submitting = Translation(
        en = "Submitting…",
        vn = "Đang gửi…"
    )
    val signInToSubmitForm = Translation(
        en = "Sign in to submit this form",
        vn = "Đăng nhập để gửi biểu mẫu này"
    )
    val noPeople = Translation(
        en = "No people.",
        vn = "Không có người."
    )
    val androidApk = Translation(
        en = "Android APK",
        vn = "APK Android"
    )
    val appStore = Translation(
        en = "App Store"
    )
    val noPagesInGroup = Translation(
        en = "This group currently has no pages.",
        vn = "Nhóm này hiện không có trang nào."
    )
    val createOpenGroupAbout = Translation(
        en = "Create an open group about \"%1\$s\".",
        vn = "Tạo một nhóm mở về \"%1\$s\"."
    )
    val setStatus = Translation(
        en = "Set status",
        vn = "Đặt trạng thái"
    )
    val isInvitingYouTo = Translation(
        en = "is inviting you to",
        vn = "đang mời bạn tham gia"
    )
    val inviteCodeCannotBeUsed = Translation(
        en = "The invite code cannot be used",
        vn = "Mã mời không thể sử dụng"
    )
    val acceptInvite = Translation(
        en = "Accept invite",
        vn = "Chấp nhận lời mời"
    )
    val name = Translation(
        en = "Name",
        vn = "Tên"
    )
    val publish = Translation(
        en = "Publish",
        vn = "Xuất bản"
    )
    val collapse = Translation(
        en = "Collapse",
        vn = "Thu gọn"
    )
    val expand = Translation(
        en = "Expand",
        vn = "Mở rộng"
    )
    val searchForPlaces = Translation(
        en = "Search for places, services, and more",
        vn = "Tìm kiếm địa điểm, dịch vụ và hơn thế nữa"
    )
    val paid = Translation(
        en = "Paid",
        vn = "Trả phí"
    )
    val viewList = Translation(
        en = "View list",
        vn = "Xem danh sách"
    )
    val pages = Translation(
        en = "Pages",
        vn = "Trang"
    )
    
    val enterYourMessage = Translation(
        en = "Enter your message"
    )
 
    val z = Translation(
        en = "z"
    )

    val howWouldYouLikeToBeContacted = Translation(
        en = "How would you like to be contacted?"
    )

    val yourPhoneNumberOrEmail = Translation(
        en = "Your phone number or email"
    )

    val general = Translation(
        en = "General"
    )

    val stylized = Translation(
        en = "Stylized"
    )

    val descriptionOptional = Translation(
        en = "Description (optional)"
    )

    val multipleUses = Translation(
        en = "Multiple uses"
    )

    val expires = Translation(
        en = "Expires"
    )

    val clearStatus = Translation(
        en = "Clear status"
    )

    val generating = Translation(
        en = "Generating"
    )

    val pageUpdatedWhenPhotoGenerated = Translation(
        en = "The page will be updated when the photo is generated."
    )

    val pageTitleHintDetailsSharedWithThirdParty = Translation(
        en = "Page title, hint, and details are shared with a 3rd party."
    )

    val dontShowThisAgain = Translation(
        en = "Don't show this again"
    )

    val thisWillReplaceCurrentPhoto = Translation(
        en = "This will replace the current photo."
    )

    val generateNewPhoto = Translation(
        en = "Generate a new photo?"
    )

    val deleteThisPage = Translation(
        en = "Delete this page?"
    )

    val youCannotUndoThis = Translation(
        en = "You cannot undo this."
    )

    val pageIsNotPublished = Translation(
        en = "Page is not published"
    )

    val required = Translation(
        en = "Required"
    )
    
    val pageIsPublished = Translation(
        en = "Page is published",
        vn = "Trang đã được xuất bản"
    )

    val pageIsSaved = Translation(
        en = "Page is saved",
        vn = "Trang đã được lưu"
    )
    
    val accountDeletion = Translation(
        en = "Account Deletion",
        vn = "Xóa tài khoản"
    )

    val sendAnEmail = Translation(
        en = "Send an email",
        vn = "Gửi một email"
    )

    val sendAnEmailDeleteAccount = Translation(
        en = "containing your profile URL and Transfer Code to permanently delete your account.",
        vn = "chứa URL trang cá nhân của bạn và mã chuyển để xóa vĩnh viễn tài khoản của bạn."
    )

    val sendAnEmailDeleteAccount2 = Translation(
        en = "You can find this information on your profile page.",
        vn = "Bạn có thể tìm thấy thông tin này trên trang trang cá nhân của mình."
    )

    val noBots = Translation(
        en = "No bots.",
        vn = "Không có bot."
    )

    val newBot = Translation(
        en = "New bot",
        vn = "Bot mới"
    )

    val noDescription = Translation(
        en = "No description",
        vn = "Không có mô tả"
    )

    val custom = Translation(
        en = "Custom",
        vn = "Tuỳ chỉnh"
    )

    val messageReactions = Translation(
        en = "Message reactions",
        vn = "Phản ứng tin nhắn"
    )

    val getTheApp = Translation(
        en = "Get the app",
        vn = "Tải ứng dụng"
    )

    val sentAPage = Translation(
        en = "Sent a page",
        vn = "Đã gửi một trang"
    )

    val sentAPhoto = Translation(
        en = "Sent a photo",
        vn = "Đã gửi một bức ảnh"
    )

    val sentAnAudioMessage = Translation(
        en = "Sent an audio message",
        vn = "Đã gửi một tin nhắn âm thanh"
    )

    val sentAVideo = Translation(
        en = "Sent a video",
        vn = "Đã gửi một video"
    )

    val sentAStory = Translation(
        en = "Sent a story",
        vn = "Đã gửi một câu chuyện"
    )

    val sentAGroup = Translation(
        en = "Sent a group",
        vn = "Đã gửi một nhóm"
    )

    val sentASticker = Translation(
        en = "Sent a sticker",
        vn = "Đã gửi một nhãn dán"
    )

    val sentAProfile = Translation(
        en = "Sent a profile",
        vn = "Đã gửi một trang cá nhân"
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
        vn = "Ai gửi tin nhắn đến nhóm này?"
    )

    val hosts = Translation(
        en = "Hosts",
        vn = "Chủ nhóm"
    )

    val everyone = Translation(
        en = "Everyone",
        vn = "Mọi người"
    )

    val every = Translation(
        en = "Every",
        vn = "Mỗi"
    )

    val whoEditsThisGroup = Translation(
        en = "Who edits this group?",
        vn = "Ai chỉnh sửa nhóm này?"
    )

    val nameIntroductionPhoto = Translation(
        en = "Name, introduction, photo",
        vn = "Tên, giới thiệu, hình ảnh"
    )

    val createInviteLink = Translation(
        en = "Create an invite link",
        vn = "Tạo liên kết mời"
    )

    val inviteLinkCouldNotBeCreated = Translation(
        en = "The invite link could not be created",
        vn = "Không thể tạo liên kết mời"
    )

    val inviteLinkIsActive = Translation(
        en = "Invite link is active",
        vn = "Liên kết mời đang hoạt động"
    )

    val copyLink = Translation(
        en = "Copy link",
        vn = "Sao chép liên kết"
    )

    val tapToAnswer = Translation(
        en = "Tap to answer",
        vn = ""
    )
    val editNote = Translation(
        en = "Edit note",
        vn = "Chỉnh sửa ghi chú"
    )
    val deleteOccurrence = Translation(
        en = "Delete this occurrence?",
        vn = "Xóa mục này?"
    )
    val yesDelete = Translation(
        en = "Yes, delete",
        vn = "Vâng, xóa"
    )
    val rescheduleOccurrence = Translation(
        en = "Reschedule occurrence",
        vn = "Lên lịch lại mục này"
    )
    val unmarkAsDone = Translation(
        en = "Unmark as done",
        vn = "Bỏ đánh dấu hoàn thành"
    )
    val markAsDone = Translation(
        en = "Mark as done",
        vn = "Đánh dấu hoàn thành"
    )
    val open = Translation(
        en = "Open",
        vn = "Mở"
    )
    val peopleInCall = Translation(
        en = "%1\$s in call",
        vn = "%1\$s đang trong cuộc gọi"
    )
    val reaction = Translation(
        en = "Reaction",
        vn = "Phản ứng"
    )
    val rating = Translation(
        en = "Rating",
        vn = "Xếp hạng"
    )
    
    val fullscreen = Translation(
        en = "Fullscreen",
        vn = ""
    )
    val microphone = Translation(
        en = "Microphone",
        vn = ""
    )
    val camera = Translation(
        en = "Camera",
        vn = ""
    )
    val shareScreen = Translation(
        en = "Share screen",
        vn = ""
    )
    val reoccurs = Translation(
        en = "Reoccurs",
        vn = ""
    )
    val everyDay = Translation(
        en = "Every day",
        vn = ""
    )
    val ofTheMonth = Translation(
        en = "%1\$s of the month",
        vn = ""
    )
    val lastDayOfTheMonth = Translation(
        en = "Last day of the month",
        vn = ""
    )
    val everyWeek = Translation(
        en = "Every week",
        vn = ""
    )
    val everyMonth = Translation(
        en = "Every month",
        vn = ""
    )
    val until = Translation(
        en = "Until",
        vn = ""
    )
    
    val nthWeek = Translation(
        en = "%1\$s week",
    )

    val tapToSwitch = Translation(
        en = "Tap to switch",
        vn = ""
    )
    
    val personCommentedOnYourStory = Translation(
        en = "%1\$s commented on your story",
        vn = ""
    )
    
    val personRepliedToYourComment = Translation(
        en = "%1\$s replied to your comment",
        vn = ""
    )
    
    val openPage = Translation(
        en = "Open page",
        vn = "Mở trang"
    )
    val posts = Translation(
        en = "Posts",
        vn = "Bài viết"
    )
    val appName = Translation(
        "Hi Town",
        "Chào Town",
        "Город приветик"
    )
    val rootPages = Translation(
        "Root pages",
        "Trang gốc"
    )
    val actionOpenGroup = Translation(
        "Open group",
        "Mở nhóm"
    )
    val actionCloseGroup = Translation(
        "Close group",
        "Đóng nhóm"
    )
    val makeOpenGroup = Translation(
        "Make group open",
        "Chuyển sang nhóm mở"
    )
    val makeCloseGroup = Translation(
        "Make group closed",
        "Chuyển sang nhóm đóng"
    )
    val actionOpenGroupDescription = Translation(
        "Anyone will be able to discover this group, see all members, messages, and request to become a member.",
        "Bất kì ai cũng có thể khám phá nhóm này, xem tất cả thành viên, tin nhắn và yêu cầu trở thành thành viên."
    )
    val actionCloseGroupDescription = Translation(
        "This group will only be accessible to members.",
        "Nhóm này sẽ chỉ có thể truy cập được đối với các thành viên."
    )
    val clickToHide = Translation(
        "Click to hide",
        "Nhấn để mở"
    )
    val error = Translation(
        "Error",
        "Lỗi"
    )
    val note = Translation(
        "Note",
        "Ghi chú"
    )
    val messages = Translation(
        "Messages",
        "Tin nhắn"
    )
    val orEnterTransferCode = Translation(
        "Or enter a transfer code here",
        "Hoặc nhập mã chuyển khoản tại đây"
    )
    val scanQrCode = Translation(
        "Use your phone to scan the QR code.",
        "Sử dụng điện thoại của bạn để quét mã QR."
    )
    val openGroup = Translation(
        "Open group",
        "Nhóm mở"
    )
    val accept = Translation(
        "Accept",
        "Chấp nhận"
    )
    val delete = Translation(
        "Delete",
        "Xóa bỏ"
    )
    val info = Translation(
        "Info",
        "Thông tin"
    )
    val edit = Translation(
        "Edit",
        "Chỉnh sửa"
    )
    val remove = Translation(
        "Remove",
        "Bỏ"
    )
    val inlineMember = Translation(
        "member",
        "thành viên"
    )
    val inlineMembers = Translation(
        "members",
        "thành viên"
    )
    val close = Translation(
        "Close",
        "Đóng"
    )
    val manage = Translation(
        "Manage",
        "Quản lý"
    )
    val selectAGroup = Translation(
        "Select a group",
        "Chọn một nhóm"
    )
    val introduction = Translation(
        "Introduction",
        "Giới thiệu"
    )
    val stories = Translation(
        "Posts",
        "Bài viết",
        "Истории"
    )
    val createStory = Translation(
        "New post",
        "Tạo bài viết"
    )
    val newStory = Translation(
        "New post",
        "Bài viết mới"
    )
    val newCard = Translation(
        "New page",
        "Trang mới"
    )
    val someone = Translation(
        "Someone",
        "Một người ai đó",
        "Кто-то"
    )
    val goBack = Translation(
        "Go back",
        "Quay lại",
        "Вернуться"
    )
    val inlineCard = Translation(
        "page",
        "trang",
//        "карта"
    )
    val inlineCards = Translation(
        "pages",
        "trang",
//        "карты"
    )
    val cards = Translation(
        "Pages",
        "Trang",
        "Карты"
    )
    val friends = Translation(
        "Friends",
        "Bạn bè",
        "Друзья"
    )
    val explore = Translation(
        "Explore",
        "Khám phá",
        "Исследовать"
    )
    val map = Translation(
        "Map",
        "Bản đồ"
    )
    val tapToOpen = Translation(
        "Tap to open",
        "Nhấn để mở",
        "Нажмите, чтобы открыть"
    )
    val profileNotFound = Translation(
        "Profile not found.",
        "Không tìm được trang cá nhân này.",
        "Профиль не найден."
    )
    val storyNotFound = Translation(
        "Post not found.",
        "Không tìm được bài viết này.",
        "История не найдена."
    )
    val groupNotFound = Translation(
        "Group not found.",
        "Không tìm được nhóm này."
    )
    val inviteNotFound = Translation(
        "Invite not found.",
        "Không tìm được lời mời này."
    )
    val cardNotFound = Translation(
        "Page not found.",
        "Không tìm được trang này.",
//        "Карта не найдена."
    )
    val joined = Translation(
        "Joined",
        "Đã tham gia",
        "Присоединился"
    )
    val inlineBy = Translation(
        "by",
        "bởi",
        ""
    )
    val inlineAnd = Translation(
        "and",
        "và",
        ""
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
        "Sending...",
        "Đang gửi...",
        // todo
    )
    val cancel = Translation(
        "Cancel",
        "Hủy",
        "Отмена"
    )
    val okay = Translation(
        "Okay",
        "Đòng ý"
    )
    val sendMessage = Translation(
        "Send message",
        "Gửi tin nhắn",
    )
    val sendPhoto = Translation(
        "Send photo",
        "Gửi ảnh"
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
        "Contact",
        "Liên hệ"
    )
    val isCreatedBy = Translation(
        "${appName.en} is being created by ",
        "${appName.vn} đang tạo ra bởi "
    )
    val sendMeAnEmail = Translation(
        "Send me an email",
        "gửi email cho tôi",
        "Вышли мне электронное письмо"
    )
    val forAllInquiries = Translation(
        " for all inquiries.",
        " cho tất cả các yêu cầu nha."
    )
    val searching = Translation(
        "Searching…",
        "Đang tìm kiếm…"
    )
    val searchResults = Translation(
        "Search results",
        "Kết quả tìm kiếm"
    )
    val noCards = Translation(
        "No pages",
        "Không tìm được trang để cho bạn xem."
    )
    val noSavedCards = Translation(
        "No saved pages",
        "Bạn chưa lưu trang"
    )
    val noCardsNearby = Translation(
        "No pages nearby",
        "Không tìm được trang gần đây."
    )
    val noStories = Translation(
        "No posts",
        "Không tìm được bài viết."
    )
    val noGroups = Translation(
        "No groups",
        "Không tìm được nhóm."
    )
    val search = Translation(
        "Search",
        "Tìm kiếm"
    )
    val privacyPolicy = Translation(
        "Privacy Policy",
        "Chính sách bảo mật"
    )
    val tos = Translation(
        "Terms of Use",
        "Điều khoản sử dụng"
    )
    val openSource = Translation(
        "Open Source",
        "Mã nguồn mở"
    )
    val signIn = Translation(
        "Sign in",
        "Đăng nhập"
    )
    val signUp = Translation(
        "Sign up",
        "Đăng ký"
    )
    val signOut = Translation(
        "Sign out",
        "Đăng xuất"
    )
    val signOutQuestion = Translation(
        "Sign out?",
        "Đăng xuất?"
    )
    val signOutQuestionLine1 = Translation(
        "You will permanently lose access to this account",
        "Bạn sẽ vĩnh viễn mất quyền truy cập vào tài khoản này"
    )
    val signOutQuestionLine2 = Translation(
        "if you are not currently signed in on another device.",
        "nếu bạn hiện chưa đăng nhập trên thiết bị khác."
    )
    val and = Translation(
        "and",
        "và"
    )
    val qrCode = Translation(
        "QR code",
        "Mã QR"
    )
    val yourName = Translation(
        "Your name",
        "Tên của bạn"
    )
    val update = Translation(
        "Update",
        "Cập nhật"
    )
    val introduceYourself = Translation(
        "Introduce yourself here",
        "Viết chút về bạn"
    )
    val filter = Translation(
        "Filter",
        "Cập nhật"
    )
    val published = Translation(
        "Published",
        "Xuất bản"
    )
    val notPublished = Translation(
        "Not published",
        "Không xuất bản"
    )
    val createCard = Translation(
            "New page",
        "Tạo trang"
    )
    val title = Translation(
        "Title",
        "Tiêu đề"
    )
    val create = Translation(
        "Create",
        "Tạo"
    )
    val createGroup = Translation(
        "Create group",
        "Tạo nhóm"
    )
    val createOpenGroup = Translation(
        "Create open group",
        "Tạo nhóm mở"
    )
    val local = Translation(
        "Local",
        "Gần đây"
    )
    val saved = Translation(
        "Saved",
        "Đã lưu"
    )
    val groups = Translation(
        "Groups",
        "Nhóm"
    )
    val groupName = Translation(
        "Group name",
        "Tên nhóm"
    )
    val reminders = Translation(
        "Reminders",
        "Lời nhắc"
    )
    val reminder = Translation(
        "Reminder",
        "Lời nhắc"
    )
    val noReminders = Translation(
        "No reminders",
        "Không có lời nhắc"
    )
    val createReminder = Translation(
        "Create reminder",
        "Tạo lời nhắc"
    )
    val hourly = Translation(
        "Hourly",
        "Theo giờ"
    )
    val daily = Translation(
        "Daily",
        "Theo ngày"
    )
    val weekly = Translation(
        "Weekly",
        "Theo tuần"
    )
    val monthly = Translation(
        "Monthly",
        "Theo tháng"
    )
    val yearly = Translation(
        "Yearly",
        "Theo năm"
    )
    val inlineHour = Translation(
        "hour",
        "tiếng"
    )
    val inlineHours = Translation(
        "hours",
        "tiếng"
    )
    val inlineMinute = Translation(
        "minute",
        "phút"
    )
    val inlineMinutes = Translation(
        "minutes",
        "phút"
    )
    val inlineDay = Translation(
        "day",
        "ngày"
    )
    val inlineWeekly = Translation(
        "week",
        "tuần"
    )
    val inlineMonthly = Translation(
        "month",
        "tháng"
    )
    val inlineYearly = Translation(
        "year",
        "năm"
    )
    val you = Translation(
        "You",
        "Bạn"
    )
    val newGroup = Translation(
        "New group",
        "Nhóm mới"
    )
    val newReminder = Translation(
        "New reminder",
        "Lời nhắc mới"
    )
    val created = Translation(
        "Created",
        "Đã tạo"
    )
    val inlinePersonWaiting = Translation(
        "person waiting",
        "người đang chờ"
    )
    val inlinePeopleWaiting = Translation(
        "people waiting",
        "người đang chờ"
    )
    val inlineIsAMember = Translation(
        "is a member",
        "là thành viên"
    )
    val onProfile = Translation(
        "On profile",
        "Trên trang cá nhân"
    )
    val inAGroup = Translation(
        "In a group",
        "Trong nhóm"
    )
    val atALocation = Translation(
        "At a location",
        "Tại một vị trí nào đó"
    )
    val inAPage = Translation(
        "In a page",
        "Trong trang"
    )
    val none = Translation(
        "None",
        "Không có"
    )
    val choosePhoto = Translation(
        "Choose photo",
        "Chọn ảnh"
    )
    val choose = Translation(
        "Choose",
        "Chọn"
    )
    val describePhoto = Translation(
        "Describe photo",
        "Mô tả ảnh"
    )
    val generatePhoto = Translation(
        "Generate photo",
        "Tạo ảnh"
    )
    val confirm = Translation(
        "Confirm",
        "Xác nhận"
    )
    val regeneratePhoto = Translation(
        "Regenerate photo",
        "Tạo ảnh mới"
    )
    val openEnclosingCard = Translation(
        "Open enclosing page",
        "Mở trang có trang này"
    )
    val previousDay = Translation(
        "Previous day",
        "Ngày trước"
    )
    val previousWeek = Translation(
        "Previous week",
        "Tuần trước"
    )
    val previousMonth = Translation(
        "Previous month",
        "Tháng trước"
    )
    val previousYear = Translation(
        "Previous year",
        "Năm trước"
    )
    val nextDay = Translation(
        "Next day",
        "Ngày sau"
    )
    val nextWeek = Translation(
        "Next week",
        "Tuần sau"
    )
    val nextMonth = Translation(
        "Next month",
        "Tháng sau"
    )
    val nextYear = Translation(
        "Next year",
        "Năm sau"
    )
    val today = Translation(
        "Today",
        "Hôm nay"
    )
    val yesterday = Translation(
        "Yesterday",
        "Hôm qua"
    )
    val tomorrow = Translation(
        "Tomorrow",
        "Ngày mai"
    )
    val loading = Translation(
        "Loading",
        "Đang tải"
    )
    val rename = Translation(
        "Rename",
        "Đổi tên"
    )

    val category = Translation(
        "Category",
        "Danh mục"
    )
    val reschedule = Translation(
        "Reschedule",
        "Lên lịch lại"
    )
    val schedule = Translation(
        "Schedule",
        "Lịch"
    )
    val timezone = Translation(
        "Timezone",
        "Múi giờ"
    )
    val duration = Translation(
        "Duration",
        "Khoảng thời gian"
    )
    val openInNewTab = Translation(
        "Open in new tab",
        "Mở ra trong tab mới"
    )
    val options = Translation(
        "Options",
        "Tùy chọn"
    )
    val unsave = Translation(
        "Unsave",
        "Bỏ lưu"
    )
    val save = Translation(
        "Save",
        "Lưu"
    )
    val hint = Translation(
        "Hint",
        "Lời gợi ý"
    )
    val location = Translation(
        "Location",
        "Vị trí"
    )
    val details = Translation(
        "Details",
        "Chi tiết"
    )
    val members = Translation(
        "Members",
        "Thành viên"
    )
    val invite = Translation(
        "Invite",
        "Mời ai đó"
    )
    val hide = Translation(
        "Hide",
        "Ẩn"
    )
    val pin = Translation(
        "Pin",
        "Ghim"
    )
    val unpin = Translation(
        "Unpin",
        "Bỏ ghim"
    )
    val leave = Translation(
        "Leave",
        "Rời khỏi"
    )
    val leaveGroup = Translation(
        "Leave group?",
        "Rời khỏi nhóm này không?"
    )
    val active = Translation(
        "Active",
        "Truy cập"
    )
    val discard = Translation(
        "Discard",
        "Bỏ"
    )
    val joinGroup = Translation(
        "Join group",
        "Tham gia nhóm"
    )
    val sendRequest = Translation(
        "Send request",
        "Gửi yêu cầu"
    )
    val cancelJoinRequest = Translation(
        "Cancel join request",
        "Hủy yêu cầu tham gia"
    )
    val stickers = Translation(
        "Stickers",
        "Hình dán"
    )
    val loadingGroup = Translation(
        "Loading group…",
        "Đang tải nhóm…"
    )
    val loadingComments = Translation(
        "Loading comments…",
        "Đang tải bình luận…"
    )
    val addPay = Translation(
        "Add pay",
        "Thêm lương"
    )
    val changePay = Translation(
        "Change pay",
        "Đổi lương"
    )
    val pay = Translation(
        "Pay",
        "Lương"
    )
    val connect = Translation(
        "Connect",
        "Kết nối"
    )
    val searchPagesOfPerson = Translation(
        "Search %1\$s's pages",
        "Tìm kiếm trang của %1\$s"
    )
    val call = Translation(
        "Call",
        "Gọi"
    )
    val minimize = Translation(
        "Minimize",
        "Thu gọn"
    )
    val maximize = Translation(
        "Maximize",
        "Mở rộng"
    )
    val subscribers = Translation(
        "Subscribers",
        "Người đăng ký"
    )
    val host = Translation(
        "Host",
        "Chủ nhóm"
    )
    val settings = Translation(
        "Settings",
        "Cài đặt"
    )
    val effects = Translation(
        "Effects",
        "Hiệu ứng"
    )
    val shareAComment = Translation(
        "Share a comment",
        "Chia sẻ một bình luận"
    )
    val post = Translation(
        "Post",
        "Đăng"
    )
    val signInToComment = Translation(
        "Sign in to comment",
        "Đăng nhập để chia sẻ một bình luận"
    )
    val signInToReply = Translation(
        "Sign in to reply",
        "Đăng nhập để trả lời"
    )
    val reply = Translation(
        "Reply",
        "Trả lời"
    )
    val react = Translation(
        "React",
        "Phản ứng"
    )
    val replyInNewGroup = Translation(
        "Reply in new group",
        "Trả lời trong nhóm mới"
    )
    val inlineReply = Translation(
        "reply",
        "câu trả lời"
    )
    val inlineReplies = Translation(
        "replies",
        "câu trả lời"
    )
    val replyTo = Translation(
        "Reply to",
        "Trả lời"
    )
    val rain = Translation(
        "Rain",
        "Mưa"
    )
    val commentReplies = Translation(
        "Comment replies",
        "Trả lời bình luận"
    )
    val showAllComment = Translation(
        "Show %1\$s more comment",
        "Coi thêm %1\$s bình luận"
    )
    val showAllComments = Translation(
        "Show %1\$s more comments",
        "Coi thêm %1\$s bình luận"
    )
    val platform = Translation(
        "Platform",
        "Nền tảng"
    )
    val bots = Translation(
        "Bots",
        "Bot"
    )
    val addBot = Translation(
        "Add a bot",
        "Thêm bot"
    )
    val createBot = Translation(
        "Create a bot",
        "Tạo bot"
    )
    val openBot = Translation(
        "Open bot",
        "Mở bot"
    )
    val groupBots = Translation(
        "Group bots",
        "Bot của nhóm"
    )
    val now = Translation(
        "Now",
        "Bây giờ"
    )
    val submit = Translation(
        "Submit",
        "Nộp"
    )
    val scripts = Translation(
        "Scripts",
        "Tập lệnh"
    )
    val shareAThought = Translation(
        "Share a thought",
        "Chia sẻ những suy nghĩ của bạn"
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
