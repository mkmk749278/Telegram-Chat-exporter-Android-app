package com.example.tgexporter.data.model

data class MessageItem(
    val id: Long,
    val chatId: Long,
    val date: Long,
    val senderName: String,
    val text: String,
    val isEdited: Boolean = false,
    val replyToSender: String? = null,
    val replyToText: String? = null,
    val forwardedFrom: String? = null,
    val mediaTag: String? = null,
)
