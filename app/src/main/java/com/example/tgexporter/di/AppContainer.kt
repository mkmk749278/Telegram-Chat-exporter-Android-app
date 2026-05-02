package com.example.tgexporter.di

import android.content.Context
import com.example.tgexporter.data.model.MessageItem
import com.example.tgexporter.telegram.ChatRepository
import com.example.tgexporter.telegram.MessageRepository
import com.example.tgexporter.telegram.TelegramRepository

class AppContainer(context: Context) {
    val telegram: TelegramRepository = TelegramRepository(context.applicationContext)
    val chats: ChatRepository = ChatRepository(telegram)
    val messages: MessageRepository = MessageRepository(telegram)

    /** Holds the messages selected for export between MessagesScreen and ExportPreviewScreen. */
    var pendingExport: List<MessageItem> = emptyList()
}
