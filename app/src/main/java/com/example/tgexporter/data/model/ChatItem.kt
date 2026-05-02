package com.example.tgexporter.data.model

data class ChatItem(
    val id: Long,
    val title: String,
    val lastMessagePreview: String,
    val unreadCount: Int,
    val order: Long,
)
