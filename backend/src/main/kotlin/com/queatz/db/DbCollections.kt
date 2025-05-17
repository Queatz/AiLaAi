package com.queatz.db

import com.arangodb.entity.CollectionType
import com.arangodb.model.FulltextIndexOptions
import com.arangodb.model.GeoIndexOptions
import com.arangodb.model.PersistentIndexOptions

fun collections() = listOf(
    GameScene::class.db {
        ensurePersistentIndex(listOf(GameScene::person.name), PersistentIndexOptions())
        ensurePersistentIndex(listOf(GameScene::published.name), PersistentIndexOptions())
        ensurePersistentIndex(listOf(GameScene::url.name), PersistentIndexOptions())
        ensurePersistentIndex(listOf(GameScene::categories.name), PersistentIndexOptions())
    },
    GameTile::class.db {
        ensurePersistentIndex(listOf(GameTile::person.name), PersistentIndexOptions())
        ensurePersistentIndex(listOf(GameTile::published.name), PersistentIndexOptions())
        ensurePersistentIndex(listOf(GameTile::categories.name), PersistentIndexOptions())
    },
    GameObject::class.db {
        ensurePersistentIndex(listOf(GameObject::person.name), PersistentIndexOptions())
        ensurePersistentIndex(listOf(GameObject::published.name), PersistentIndexOptions())
        ensurePersistentIndex(listOf(GameObject::categories.name), PersistentIndexOptions())
    },
    GameMusic::class.db {
        ensurePersistentIndex(listOf(GameMusic::person.name), PersistentIndexOptions())
        ensurePersistentIndex(listOf(GameMusic::published.name), PersistentIndexOptions())
        ensurePersistentIndex(listOf(GameMusic::categories.name), PersistentIndexOptions())
    },
    GameDiscussion::class.db {
        ensurePersistentIndex(listOf(GameDiscussion::person.name), PersistentIndexOptions())
        ensurePersistentIndex(listOf(GameDiscussion::scene.name), PersistentIndexOptions())
        ensurePersistentIndex(listOf(GameDiscussion::title.name), PersistentIndexOptions())
    },
    Impromptu::class.db {
        ensurePersistentIndex(listOf(Impromptu::person.name), PersistentIndexOptions())
        ensurePersistentIndex(listOf(Impromptu::mode.name), PersistentIndexOptions())
        ensurePersistentIndex(listOf(Impromptu::updateLocation.name), PersistentIndexOptions())
    },
    ImpromptuSeek::class.db {
        ensurePersistentIndex(listOf(ImpromptuSeek::person.name), PersistentIndexOptions())
        ensurePersistentIndex(listOf(ImpromptuSeek::name.name), PersistentIndexOptions())
        ensurePersistentIndex(listOf(ImpromptuSeek::offer.name), PersistentIndexOptions())
        ensurePersistentIndex(listOf(ImpromptuSeek::radius.name), PersistentIndexOptions())
        ensurePersistentIndex(listOf(ImpromptuSeek::expiresAt.name), PersistentIndexOptions())
    },
    ImpromptuHistory::class.db {
        ensurePersistentIndex(listOf(ImpromptuHistory::person.name), PersistentIndexOptions())
        ensurePersistentIndex(listOf(ImpromptuHistory::distance.name), PersistentIndexOptions())
        ensurePersistentIndex(listOf(ImpromptuHistory::gone.name), PersistentIndexOptions())
    },
    Person::class.db {
        ensurePersistentIndex(listOf(Person::name.name), PersistentIndexOptions())
    },
    Reminder::class.db {
        ensurePersistentIndex(listOf(Reminder::person.name), PersistentIndexOptions())
        ensurePersistentIndex(listOf(Reminder::people.name), PersistentIndexOptions())
        ensurePersistentIndex(listOf(Reminder::groups.name), PersistentIndexOptions())
        ensurePersistentIndex(listOf(Reminder::open.name), PersistentIndexOptions())
        ensurePersistentIndex(listOf(Reminder::attachment.name), PersistentIndexOptions())
        ensurePersistentIndex(listOf(Reminder::title.name), PersistentIndexOptions())
        ensurePersistentIndex(listOf(Reminder::note.name), PersistentIndexOptions())
        ensurePersistentIndex(listOf(Reminder::start.name), PersistentIndexOptions())
        ensurePersistentIndex(listOf(Reminder::end.name), PersistentIndexOptions())
        ensurePersistentIndex(listOf(Reminder::schedule.name), PersistentIndexOptions())
        ensurePersistentIndex(listOf(Reminder::categories.name), PersistentIndexOptions())
        ensureGeoIndex(listOf(Reminder::geo.name), GeoIndexOptions())
    },
    ReminderOccurrence::class.db {
        ensurePersistentIndex(listOf(ReminderOccurrence::reminder.name), PersistentIndexOptions())
        ensurePersistentIndex(listOf(ReminderOccurrence::occurrence.name), PersistentIndexOptions())
        ensurePersistentIndex(listOf(ReminderOccurrence::date.name), PersistentIndexOptions())
        ensurePersistentIndex(listOf(ReminderOccurrence::note.name), PersistentIndexOptions())
        ensurePersistentIndex(listOf(ReminderOccurrence::done.name), PersistentIndexOptions())
        ensurePersistentIndex(listOf(ReminderOccurrence::gone.name), PersistentIndexOptions())
    },
    Transfer::class.db {
        ensurePersistentIndex(listOf(Transfer::person.name), PersistentIndexOptions())
        ensurePersistentIndex(listOf(Transfer::code.name), PersistentIndexOptions())
    },
    LinkDeviceToken::class.db {
        ensurePersistentIndex(listOf(LinkDeviceToken::token.name), PersistentIndexOptions())
    },
    Device::class.db {
        ensurePersistentIndex(listOf(Device::person.name), PersistentIndexOptions())
        ensurePersistentIndex(listOf(Device::type.name, Device::token.name), PersistentIndexOptions())
    },
    Settings::class.db {
        ensurePersistentIndex(listOf(Settings::person.name), PersistentIndexOptions())
    },
    Presence::class.db {
        ensurePersistentIndex(listOf(Presence::person.name), PersistentIndexOptions())
    },
    Profile::class.db {
        ensurePersistentIndex(listOf(Profile::person.name), PersistentIndexOptions())
    },
    Invite::class.db {
        ensurePersistentIndex(listOf(Invite::code.name), PersistentIndexOptions())
        ensurePersistentIndex(listOf(Invite::expiry.name), PersistentIndexOptions())
        ensurePersistentIndex(listOf(Invite::group.name), PersistentIndexOptions())
        ensurePersistentIndex(listOf(Invite::person.name), PersistentIndexOptions())
    },
    Save::class.db {
        ensurePersistentIndex(listOf(Save::person.name), PersistentIndexOptions())
        ensurePersistentIndex(listOf(Save::card.name), PersistentIndexOptions())
    },
    Story::class.db {
        ensurePersistentIndex(listOf(Story::person.name), PersistentIndexOptions())
        ensurePersistentIndex(listOf(Story::title.name), PersistentIndexOptions())
        ensurePersistentIndex(listOf(Story::url.name), PersistentIndexOptions())
        ensurePersistentIndex(listOf(Story::published.name), PersistentIndexOptions())
        ensurePersistentIndex(listOf(Story::categories.name), PersistentIndexOptions())
    },
    StoryDraft::class.db {
        ensurePersistentIndex(listOf(StoryDraft::story.name), PersistentIndexOptions())
    },
    Card::class.db {
        ensurePersistentIndex(listOf(Card::person.name), PersistentIndexOptions())
        ensurePersistentIndex(listOf(Card::parent.name), PersistentIndexOptions())
        ensurePersistentIndex(listOf(Card::url.name), PersistentIndexOptions())
        ensurePersistentIndex(listOf(Card::active.name), PersistentIndexOptions())
        ensurePersistentIndex(listOf(Card::name.name), PersistentIndexOptions())
        ensurePersistentIndex(listOf(Card::location.name), PersistentIndexOptions())
        ensurePersistentIndex(listOf(Card::collaborators.name), PersistentIndexOptions())
        ensurePersistentIndex(listOf(Card::equipped.name), PersistentIndexOptions())
        ensurePersistentIndex(listOf(Card::conversation.name), PersistentIndexOptions())
        ensurePersistentIndex(listOf(Card::offline.name), PersistentIndexOptions())
        ensurePersistentIndex(listOf(Card::group.name), PersistentIndexOptions())
        ensurePersistentIndex(listOf(Card::pay.name), PersistentIndexOptions())
        ensurePersistentIndex(listOf(Card::active.name), PersistentIndexOptions())
        ensurePersistentIndex(listOf(Card::level.name), PersistentIndexOptions())
        ensurePersistentIndex(listOf(Card::size.name), PersistentIndexOptions())
        ensureFulltextIndex(listOf(Card::conversation.name), FulltextIndexOptions())
        ensureFulltextIndex(listOf(Card::categories.name), FulltextIndexOptions())
        ensureGeoIndex(listOf(Card::geo.name), GeoIndexOptions())
    },
    Group::class.db {
        ensurePersistentIndex(listOf(Group::seen.name), PersistentIndexOptions())
    },
    Member::class.db(CollectionType.EDGES, listOf(Group::class, Person::class)) {
        ensurePersistentIndex(listOf(Member::gone.name), PersistentIndexOptions())
    },
    Message::class.db {},
    Crash::class.db {},
    Report::class.db {
        ensurePersistentIndex(listOf(Report::type.name), PersistentIndexOptions())
    },
    AppFeedback::class.db {},
    Sticker::class.db {
        ensurePersistentIndex(listOf(Sticker::pack.name), PersistentIndexOptions())
        ensurePersistentIndex(listOf(Sticker::name.name), PersistentIndexOptions())
    },
    StickerPack::class.db {
        ensurePersistentIndex(listOf(StickerPack::person.name), PersistentIndexOptions())
        ensurePersistentIndex(listOf(StickerPack::active.name), PersistentIndexOptions())
        ensurePersistentIndex(listOf(StickerPack::description.name), PersistentIndexOptions())
        ensurePersistentIndex(listOf(StickerPack::name.name), PersistentIndexOptions())
    },
    StickerPackSave::class.db(CollectionType.EDGES, listOf(StickerPack::class, Person::class)) {
    },
    JoinRequest::class.db {
        ensurePersistentIndex(listOf(JoinRequest::person.name), PersistentIndexOptions())
        ensurePersistentIndex(listOf(JoinRequest::group.name), PersistentIndexOptions())
    },
    Widget::class.db {
        ensurePersistentIndex(listOf(Widget::person.name), PersistentIndexOptions())
    },
    Script::class.db {
        ensurePersistentIndex(listOf(Script::person.name), PersistentIndexOptions())
        ensurePersistentIndex(listOf(Script::name.name), PersistentIndexOptions())
        ensurePersistentIndex(listOf(Script::categories.name), PersistentIndexOptions())
        ensurePersistentIndex(listOf(Script::description.name), PersistentIndexOptions())
    },
    ScriptData::class.db {
        ensurePersistentIndex(listOf(ScriptData::script.name), PersistentIndexOptions())
    },
    Item::class.db {
        ensurePersistentIndex(listOf(Item::name.name), PersistentIndexOptions())
        ensurePersistentIndex(listOf(Item::creator.name), PersistentIndexOptions())
    },
    Inventory::class.db {
        ensurePersistentIndex(listOf(Inventory::person.name), PersistentIndexOptions())
        ensurePersistentIndex(listOf(Inventory::group.name), PersistentIndexOptions())
        ensurePersistentIndex(listOf(Inventory::card.name), PersistentIndexOptions())
        ensureGeoIndex(listOf(Inventory::geo.name), GeoIndexOptions())
    },
    InventoryItem::class.db {
        ensurePersistentIndex(listOf(InventoryItem::inventory.name), PersistentIndexOptions())
        ensurePersistentIndex(listOf(InventoryItem::item.name), PersistentIndexOptions())
        ensurePersistentIndex(listOf(InventoryItem::expiresAt.name), PersistentIndexOptions())
        ensurePersistentIndex(listOf(InventoryItem::quantity.name), PersistentIndexOptions())
    },
    Call::class.db {
        ensurePersistentIndex(listOf(Call::group.name), PersistentIndexOptions())
        ensurePersistentIndex(listOf(Call::room.name), PersistentIndexOptions())
    },
    Trade::class.db {
        ensurePersistentIndex(listOf(Trade::people.name), PersistentIndexOptions())
        ensurePersistentIndex(listOf(Trade::completedAt.name), PersistentIndexOptions())
        ensurePersistentIndex(listOf(Trade::cancelledAt.name), PersistentIndexOptions())
    },
    Subscription::class.db(CollectionType.EDGES, listOf(Person::class)) {
    },
    Reaction::class.db(CollectionType.EDGES, listOf(Person::class, Card::class, Story::class, Message::class)) {
        ensurePersistentIndex(listOf(Reaction::reaction.name), PersistentIndexOptions())
        ensureFulltextIndex(listOf(Reaction::comment.name), FulltextIndexOptions())
    },
    Comment::class.db(CollectionType.EDGES, listOf(
        Person::class,
        Card::class,
        Comment::class,
        Story::class,
        Message::class,
        GameDiscussion::class
    )) {
        ensureFulltextIndex(listOf(Comment::comment.name), FulltextIndexOptions())
    },
    PlatformConfig::class.db {
    },
    Bot::class.db {
        ensurePersistentIndex(listOf(Bot::name.name), PersistentIndexOptions())
        ensurePersistentIndex(listOf(Bot::creator.name), PersistentIndexOptions())
        ensurePersistentIndex(listOf(Bot::open.name), PersistentIndexOptions())
    },
    BotData::class.db {
        ensurePersistentIndex(listOf(BotData::bot.name), PersistentIndexOptions())
    },
    App::class.db {
        ensurePersistentIndex(listOf(App::name.name), PersistentIndexOptions())
        ensurePersistentIndex(listOf(App::creator.name), PersistentIndexOptions())
        ensurePersistentIndex(listOf(App::open.name), PersistentIndexOptions())
    },
    AppPerson::class.db {
        ensurePersistentIndex(listOf(AppPerson::person.name), PersistentIndexOptions())
        ensurePersistentIndex(listOf(AppPerson::app.name), PersistentIndexOptions())
        ensurePersistentIndex(listOf(AppPerson::permissions.name), PersistentIndexOptions())
    },
    GroupBot::class.db {
        ensurePersistentIndex(listOf(GroupBot::bot.name), PersistentIndexOptions())
        ensurePersistentIndex(listOf(GroupBot::group.name), PersistentIndexOptions())
    },
    GroupBotData::class.db {
        ensurePersistentIndex(listOf(GroupBotData::groupBot.name), PersistentIndexOptions())
        ensurePersistentIndex(listOf(GroupBotData::webhook.name), PersistentIndexOptions())
    },
    Status::class.db {
        ensurePersistentIndex(listOf(Status::name.name), PersistentIndexOptions())
    },
    PersonStatus::class.db {
        ensurePersistentIndex(listOf(PersonStatus::person.name), PersistentIndexOptions())
        ensurePersistentIndex(listOf(PersonStatus::status.name), PersistentIndexOptions())
    },
    Account::class.db {
        ensurePersistentIndex(listOf(Account::person.name), PersistentIndexOptions())
        ensurePersistentIndex(listOf(Account::points.name), PersistentIndexOptions())
    },
    CardVisit::class.db {
        ensurePersistentIndex(listOf(CardVisit::card.name), PersistentIndexOptions())
    },
    GroupPin::class.db {
        ensurePersistentIndex(listOf(GroupPin::person.name), PersistentIndexOptions())
        ensurePersistentIndex(listOf(GroupPin::group.name), PersistentIndexOptions())
    },
    Prompt::class.db {
        ensurePersistentIndex(listOf(Prompt::person.name), PersistentIndexOptions())
        ensurePersistentIndex(listOf(Prompt::context.name), PersistentIndexOptions())
        ensurePersistentIndex(listOf(Prompt::lastUsed.name), PersistentIndexOptions())
    },
    Rating::class.db(CollectionType.EDGES, listOf(Card::class, Story::class, Message::class)) {
        ensurePersistentIndex(listOf(Rating::rating.name), PersistentIndexOptions())
    }
)
