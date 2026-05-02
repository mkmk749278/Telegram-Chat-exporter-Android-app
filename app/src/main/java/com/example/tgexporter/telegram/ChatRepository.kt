package com.example.tgexporter.telegram

import com.example.tgexporter.data.model.ChatItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.drinkless.tdlib.TdApi

class ChatRepository(private val telegram: TelegramRepository) {

    private val _chats = MutableStateFlow<List<ChatItem>>(emptyList())
    val chats: StateFlow<List<ChatItem>> = _chats.asStateFlow()

    suspend fun load() {
        val client = telegram.client()
        // Pull main chat list into TDLib's local cache.
        runCatching { client.await(TdApi.LoadChats(TdApi.ChatListMain(), 200)) }
        val chatsResult = client.await(TdApi.GetChats(TdApi.ChatListMain(), 500)) as TdApi.Chats
        val items = chatsResult.chatIds.mapNotNull { id ->
            runCatching {
                val chat = client.await(TdApi.GetChat(id)) as TdApi.Chat
                val order = chat.positions.firstOrNull { it.list is TdApi.ChatListMain }?.order ?: 0L
                ChatItem(
                    id = chat.id,
                    title = chat.title.ifBlank { "Chat $id" },
                    lastMessagePreview = previewOf(chat.lastMessage),
                    unreadCount = chat.unreadCount,
                    order = order,
                )
            }.getOrNull()
        }.sortedByDescending { it.order }
        _chats.value = items
    }

    private fun previewOf(message: TdApi.Message?): String {
        if (message == null) return ""
        return when (val c = message.content) {
            is TdApi.MessageText -> c.text.text.take(80)
            is TdApi.MessagePhoto -> "[Photo] " + c.caption.text.take(60)
            is TdApi.MessageVideo -> "[Video] " + c.caption.text.take(60)
            is TdApi.MessageDocument -> "[Document] " + c.caption.text.take(60)
            is TdApi.MessageVoiceNote -> "[Voice]"
            is TdApi.MessageSticker -> "[Sticker ${c.sticker.emoji}]"
            is TdApi.MessageAnimation -> "[GIF] " + c.caption.text.take(60)
            else -> ""
        }.trim()
    }
}
