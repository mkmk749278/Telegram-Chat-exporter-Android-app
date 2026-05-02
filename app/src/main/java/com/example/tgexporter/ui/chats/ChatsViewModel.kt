package com.example.tgexporter.ui.chats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.tgexporter.data.model.ChatItem
import com.example.tgexporter.telegram.ChatRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ChatsViewModel(private val repo: ChatRepository) : ViewModel() {

    val chats: StateFlow<List<ChatItem>> = repo.chats

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    init { refresh() }

    fun refresh() = viewModelScope.launch {
        _loading.value = true
        runCatching { repo.load() }.onFailure { _error.value = it.message }
        _loading.value = false
    }

    fun consumeError() { _error.value = null }
}
