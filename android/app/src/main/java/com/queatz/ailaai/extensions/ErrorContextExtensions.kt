package com.queatz.ailaai.extensions

import android.content.Context
import androidx.annotation.StringRes
import com.queatz.ailaai.R

@StringRes
fun errorContextStringRes(errorContext: String): Int? = when (errorContext) {
    "accept join request" -> R.string.error_accept_join_request
    "add bot" -> R.string.error_add_bot
    "add comment" -> R.string.error_add_comment
    "add member" -> R.string.error_add_member
    "add points" -> R.string.error_add_points
    "cancel signal" -> R.string.error_cancel_signal
    "cancel trade" -> R.string.error_cancel_trade
    "check link device" -> R.string.error_check_link_device
    "confirm trade" -> R.string.error_confirm_trade
    "create app" -> R.string.error_create_app
    "create bot" -> R.string.error_create_bot
    "create discussion" -> R.string.error_create_discussion
    "create group" -> R.string.error_create_group
    "create invite" -> R.string.error_create_invite
    "create item" -> R.string.error_create_item
    "create music" -> R.string.error_create_music
    "create object" -> R.string.error_create_object
    "create page" -> R.string.error_create_page
    "create post" -> R.string.error_create_post
    "create reminder" -> R.string.error_create_reminder
    "create scene" -> R.string.error_create_scene
    "create script" -> R.string.error_create_script
    "create seek" -> R.string.error_create_seek
    "create signal" -> R.string.error_create_signal
    "create status" -> R.string.error_create_status
    "create sticker" -> R.string.error_create_sticker
    "create sticker pack" -> R.string.error_create_sticker_pack
    "create tile" -> R.string.error_create_tile
    "create trade" -> R.string.error_create_trade
    "create widget" -> R.string.error_create_widget
    "delete account" -> R.string.error_delete_account
    "delete bot" -> R.string.error_delete_bot
    "delete comment" -> R.string.error_delete_comment
    "delete discussion" -> R.string.error_delete_discussion
    "delete history" -> R.string.error_delete_history
    "delete invite" -> R.string.error_delete_invite
    "delete join request" -> R.string.error_delete_join_request
    "delete message" -> R.string.error_delete_message
    "delete music" -> R.string.error_delete_music
    "delete object" -> R.string.error_delete_object
    "delete occurrence" -> R.string.error_delete_occurrence
    "delete page" -> R.string.error_delete_page
    "delete post" -> R.string.error_delete_post
    "delete reminder" -> R.string.error_delete_reminder
    "delete scene" -> R.string.error_delete_scene
    "delete script" -> R.string.error_delete_script
    "delete seek" -> R.string.error_delete_seek
    "delete status" -> R.string.error_delete_status
    "delete sticker" -> R.string.error_delete_sticker
    "delete sticker pack" -> R.string.error_delete_sticker_pack
    "delete tile" -> R.string.error_delete_tile
    "downgrade page" -> R.string.error_downgrade_page
    "drop item" -> R.string.error_drop_item
    "edit comment" -> R.string.error_edit_comment
    "equip item" -> R.string.error_equip_item
    "export data" -> R.string.error_export_data
    "generate JSON" -> R.string.error_generate_json
    "generate image" -> R.string.error_generate_image
    "generate script" -> R.string.error_generate_script
    "generate speech" -> R.string.error_generate_speech
    "invalid link code" -> R.string.error_invalid_link_code
    "invalid quantity" -> R.string.error_invalid_quantity
    "invite member" -> R.string.error_invite_member
    "join reminder" -> R.string.error_join_reminder
    "leave collaboration" -> R.string.error_leave_collaboration
    "leave reminder" -> R.string.error_leave_reminder
    "link device" -> R.string.error_link_device
    "load AI styles" -> R.string.error_load_ai_styles
    "load account" -> R.string.error_load_account
    "load accounts" -> R.string.error_load_accounts
    "load active signals" -> R.string.error_load_active_signals
    "load apps" -> R.string.error_load_apps
    "load bot" -> R.string.error_load_bot
    "load bot data" -> R.string.error_load_bot_data
    "load bots" -> R.string.error_load_bots
    "load call" -> R.string.error_load_call
    "load calls" -> R.string.error_load_calls
    "load categories" -> R.string.error_load_categories
    "load collaborations" -> R.string.error_load_collaborations
    "load comment" -> R.string.error_load_comment
    "load comments" -> R.string.error_load_comments
    "load crash reports" -> R.string.error_load_crash_reports
    "load discussion" -> R.string.error_load_discussion
    "load discussions" -> R.string.error_load_discussions
    "load downgrade details" -> R.string.error_load_downgrade_details
    "load draft" -> R.string.error_load_draft
    "load equipped items" -> R.string.error_load_equipped_items
    "load feedback" -> R.string.error_load_feedback
    "load group" -> R.string.error_load_group
    "load group pages" -> R.string.error_load_group_pages
    "load groups" -> R.string.error_load_groups
    "load health" -> R.string.error_load_health
    "load hidden groups" -> R.string.error_load_hidden_groups
    "load history" -> R.string.error_load_history
    "load impromptu" -> R.string.error_load_impromptu
    "load inventories" -> R.string.error_load_inventories
    "load inventory" -> R.string.error_load_inventory
    "load invite" -> R.string.error_load_invite
    "load invites" -> R.string.error_load_invites
    "load items" -> R.string.error_load_items
    "load join requests" -> R.string.error_load_join_requests
    "load members" -> R.string.error_load_members
    "load message" -> R.string.error_load_message
    "load messages" -> R.string.error_load_messages
    "load music" -> R.string.error_load_music
    "load my pages" -> R.string.error_load_my_pages
    "load my posts" -> R.string.error_load_my_posts
    "load my scripts" -> R.string.error_load_my_scripts
    "load my signals" -> R.string.error_load_my_signals
    "load object" -> R.string.error_load_object
    "load objects" -> R.string.error_load_objects
    "load occurrences" -> R.string.error_load_occurrences
    "load page" -> R.string.error_load_page
    "load page group" -> R.string.error_load_page_group
    "load page people" -> R.string.error_load_page_people
    "load page visits" -> R.string.error_load_page_visits
    "load pages" -> R.string.error_load_pages
    "load people" -> R.string.error_load_people
    "load platform" -> R.string.error_load_platform
    "load post" -> R.string.error_load_post
    "load posts" -> R.string.error_load_posts
    "load presence" -> R.string.error_load_presence
    "load profile" -> R.string.error_load_profile
    "load prompts" -> R.string.error_load_prompts
    "load rating" -> R.string.error_load_rating
    "load ratings" -> R.string.error_load_ratings
    "load reactions" -> R.string.error_load_reactions
    "load release notes" -> R.string.error_load_release_notes
    "load reminder" -> R.string.error_load_reminder
    "load reminders" -> R.string.error_load_reminders
    "load reports" -> R.string.error_load_reports
    "load saved pages" -> R.string.error_load_saved_pages
    "load scene" -> R.string.error_load_scene
    "load scenes" -> R.string.error_load_scenes
    "load script" -> R.string.error_load_script
    "load script data" -> R.string.error_load_script_data
    "load script stats" -> R.string.error_load_script_stats
    "load scripts" -> R.string.error_load_scripts
    "load signals" -> R.string.error_load_signals
    "load stats" -> R.string.error_load_stats
    "load statuses" -> R.string.error_load_statuses
    "load sticker" -> R.string.error_load_sticker
    "load sticker pack" -> R.string.error_load_sticker_pack
    "load sticker packs" -> R.string.error_load_sticker_packs
    "load stickers" -> R.string.error_load_stickers
    "load tile" -> R.string.error_load_tile
    "load tiles" -> R.string.error_load_tiles
    "load trade" -> R.string.error_load_trade
    "load trades" -> R.string.error_load_trades
    "load transfer code" -> R.string.error_load_transfer_code
    "load upgrade details" -> R.string.error_load_upgrade_details
    "load widget" -> R.string.error_load_widget
    "mark posts read" -> R.string.error_mark_posts_read
    "mint item" -> R.string.error_mint_item
    "open camera" -> R.string.error_open_camera
    "open link" -> R.string.error_open_link
    "pin group" -> R.string.error_pin_group
    "pin script" -> R.string.error_pin_script
    "promote member" -> R.string.error_promote_member
    "rate message" -> R.string.error_rate_message
    "react to message" -> R.string.error_react_to_message
    "react to post" -> R.string.error_react_to_post
    "record audio" -> R.string.error_record_audio
    "refresh transfer code" -> R.string.error_refresh_transfer_code
    "register device" -> R.string.error_register_device
    "reload bot" -> R.string.error_reload_bot
    "remove bot" -> R.string.error_remove_bot
    "remove member" -> R.string.error_remove_member
    "reply to comment" -> R.string.error_reply_to_comment
    "reply to signal" -> R.string.error_reply_to_signal
    "resolve crash" -> R.string.error_resolve_crash
    "resolve discussion" -> R.string.error_resolve_discussion
    "resolve feedback" -> R.string.error_resolve_feedback
    "resolve report" -> R.string.error_resolve_report
    "run script" -> R.string.error_run_script
    "run widget" -> R.string.error_run_widget
    "save audio" -> R.string.error_save_audio
    "save draft" -> R.string.error_save_draft
    "save drawing" -> R.string.error_save_drawing
    "save image" -> R.string.error_save_image
    "save page" -> R.string.error_save_page
    "save post" -> R.string.error_save_post
    "save prompt" -> R.string.error_save_prompt
    "save sticker pack" -> R.string.error_save_sticker_pack
    "scan QR code" -> R.string.error_scan_qr_code
    "search groups" -> R.string.error_search_groups
    "send audio" -> R.string.error_send_audio
    "send crash report" -> R.string.error_send_crash_report
    "send feedback" -> R.string.error_send_feedback
    "send image" -> R.string.error_send_image
    "send join request" -> R.string.error_send_join_request
    "send message" -> R.string.error_send_message
    "send post" -> R.string.error_send_post
    "send profile" -> R.string.error_send_profile
    "send reply" -> R.string.error_send_reply
    "send report" -> R.string.error_send_report
    "send signal" -> R.string.error_send_signal
    "send video" -> R.string.error_send_video
    "share screen" -> R.string.error_share_screen
    "sign in" -> R.string.error_sign_in
    "sign up" -> R.string.error_sign_up
    "start call" -> R.string.error_start_call
    "start call service" -> R.string.error_start_call_service
    "start incoming call service" -> R.string.error_start_incoming_call_service
    "subscribe" -> R.string.error_subscribe
    "take items" -> R.string.error_take_items
    "toggle signal" -> R.string.error_toggle_signal
    "transcribe audio" -> R.string.error_transcribe_audio
    "unconfirm trade" -> R.string.error_unconfirm_trade
    "unequip item" -> R.string.error_unequip_item
    "unpin group" -> R.string.error_unpin_group
    "unpin script" -> R.string.error_unpin_script
    "unsave page" -> R.string.error_unsave_page
    "unsave sticker pack" -> R.string.error_unsave_sticker_pack
    "unsubscribe" -> R.string.error_unsubscribe
    "update bot" -> R.string.error_update_bot
    "update bot data" -> R.string.error_update_bot_data
    "update group" -> R.string.error_update_group
    "update impromptu" -> R.string.error_update_impromptu
    "update invite" -> R.string.error_update_invite
    "update location" -> R.string.error_update_location
    "update member" -> R.string.error_update_member
    "update message" -> R.string.error_update_message
    "update music" -> R.string.error_update_music
    "update object" -> R.string.error_update_object
    "update occurrence" -> R.string.error_update_occurrence
    "update page" -> R.string.error_update_page
    "update platform" -> R.string.error_update_platform
    "update post" -> R.string.error_update_post
    "update profile" -> R.string.error_update_profile
    "update reminder" -> R.string.error_update_reminder
    "update scene" -> R.string.error_update_scene
    "update script" -> R.string.error_update_script
    "update script data" -> R.string.error_update_script_data
    "update seek" -> R.string.error_update_seek
    "update status" -> R.string.error_update_status
    "update sticker" -> R.string.error_update_sticker
    "update sticker pack" -> R.string.error_update_sticker_pack
    "update tile" -> R.string.error_update_tile
    "update trade" -> R.string.error_update_trade
    "update trade items" -> R.string.error_update_trade_items
    "update widget" -> R.string.error_update_widget
    "upgrade page" -> R.string.error_upgrade_page
    "upload audio" -> R.string.error_upload_audio
    "upload image" -> R.string.error_upload_image
    "upload page image" -> R.string.error_upload_page_image
    "upload page video" -> R.string.error_upload_page_video
    "upload profile image" -> R.string.error_upload_profile_image
    "upload profile video" -> R.string.error_upload_profile_video
    "upload screenshot" -> R.string.error_upload_screenshot
    "upload sticker" -> R.string.error_upload_sticker
    "upload video" -> R.string.error_upload_video
    "use invite" -> R.string.error_use_invite
    "verify biometrics" -> R.string.error_verify_biometrics
    else -> null
}

fun Context.localizedErrorContext(errorContext: String): String =
    errorContextStringRes(errorContext)?.let { getString(it) } ?: errorContext
