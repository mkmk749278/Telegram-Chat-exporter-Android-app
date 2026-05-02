package com.example.tgexporter.telegram

import com.example.tgexporter.data.model.MessageItem
import kotlinx.coroutines.delay
import org.drinkless.tdlib.TdApi

class MessageRepository(private val telegram: TelegramRepository) {

    private val userCache = mutableMapOf<Long, String>()
    private val chatCache = mutableMapOf<Long, String>()

    suspend fun loadHistory(chatId: Long, fromMessageId: Long, limit: Int = 50): List<MessageItem> {
        val client = telegram.client()
        val result = retrying {
            client.await(TdApi.GetChatHistory(chatId, fromMessageId, 0, limit, false)) as TdApi.Messages
        }
        return result.messages.orEmpty().mapNotNull { it?.let { mapMessage(it) } }
    }

    private suspend fun mapMessage(msg: TdApi.Message): MessageItem {
        val sender = senderName(msg.senderId)
        val (text, media) = renderContent(msg.content)
        val replyInfo = resolveReply(msg)
        val forward = msg.forwardInfo?.let { fi ->
            when (val origin = fi.origin) {
                is TdApi.MessageOriginUser -> userName(origin.senderUserId)
                is TdApi.MessageOriginChat -> origin.authorSignature.ifBlank { chatTitle(origin.senderChatId) }
                is TdApi.MessageOriginChannel -> chatTitle(origin.chatId)
                is TdApi.MessageOriginHiddenUser -> origin.senderName
                else -> null
            }
        }
        return MessageItem(
            id = msg.id,
            chatId = msg.chatId,
            date = msg.date.toLong() * 1000L,
            senderName = sender,
            text = text,
            isEdited = msg.editDate > 0,
            replyToSender = replyInfo?.first,
            replyToText = replyInfo?.second,
            forwardedFrom = forward,
            mediaTag = media,
        )
    }

    private suspend fun senderName(sender: TdApi.MessageSender): String =
        when (sender) {
            is TdApi.MessageSenderUser -> userName(sender.userId)
            is TdApi.MessageSenderChat -> chatTitle(sender.chatId)
            else -> "?"
        }

    private suspend fun userName(userId: Long): String {
        userCache[userId]?.let { return it }
        return runCatching {
            val user = telegram.client().await(TdApi.GetUser(userId)) as TdApi.User
            val name = listOfNotNull(user.firstName.takeIf { it.isNotBlank() }, user.lastName.takeIf { it.isNotBlank() })
                .joinToString(" ").ifBlank { user.usernames?.editableUsername ?: "User $userId" }
            userCache[userId] = name
            name
        }.getOrDefault("User $userId")
    }

    private suspend fun chatTitle(chatId: Long): String {
        chatCache[chatId]?.let { return it }
        return runCatching {
            val chat = telegram.client().await(TdApi.GetChat(chatId)) as TdApi.Chat
            val title = chat.title.ifBlank { "Chat $chatId" }
            chatCache[chatId] = title
            title
        }.getOrDefault("Chat $chatId")
    }

    private suspend fun resolveReply(msg: TdApi.Message): Pair<String, String>? {
        val state = msg.replyTo as? TdApi.MessageReplyToMessage ?: return null
        val originChatId = if (state.chatId != 0L) state.chatId else msg.chatId
        return runCatching {
            val replied = telegram.client().await(TdApi.GetMessage(originChatId, state.messageId)) as TdApi.Message
            val sender = senderName(replied.senderId)
            val (body, _) = renderContent(replied.content)
            sender to body.take(120).let { if (body.length > 120) "$it…" else it }
        }.getOrNull()
    }

    private fun renderContent(content: TdApi.MessageContent): Pair<String, String?> = when (content) {
        is TdApi.MessageText -> content.text.text to null
        is TdApi.MessagePhoto -> content.caption.text to "[Photo]"
        is TdApi.MessageVideo -> content.caption.text to "[Video]"
        is TdApi.MessageDocument -> content.caption.text to "[Document: ${content.document.fileName}]"
        is TdApi.MessageAudio -> content.caption.text to "[Audio]"
        is TdApi.MessageVoiceNote -> content.caption.text to "[Voice ${content.voiceNote.duration}s]"
        is TdApi.MessageSticker -> "" to "[Sticker ${content.sticker.emoji}]"
        is TdApi.MessageAnimation -> content.caption.text to "[GIF]"
        is TdApi.MessageVideoNote -> "" to "[Video note ${content.videoNote.duration}s]"
        else -> "" to null
    }

    private suspend inline fun <T> retrying(block: () -> T): T {
        var attempt = 0
        while (true) {
            try {
                return block()
            } catch (e: TdException) {
                if (e.code == 420) {
                    val secs = Regex("FLOOD_WAIT_(\\d+)").find(e.message ?: "")?.groupValues?.get(1)?.toLongOrNull() ?: 5L
                    delay(secs * 1000L)
                    if (++attempt > 3) throw e
                } else throw e
            }
        }
    }
}
