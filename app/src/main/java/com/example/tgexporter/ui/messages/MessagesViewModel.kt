package com.example.tgexporter.ui.messages

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.tgexporter.data.model.MessageItem
import com.example.tgexporter.telegram.MessageRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MessagesViewModel(
    private val chatId: Long,
    private val repo: MessageRepository,
) : ViewModel() {

    private val _messages = MutableStateFlow<List<MessageItem>>(emptyList())
    val messages: StateFlow<List<MessageItem>> = _messages.asStateFlow()

    private val _selected = MutableStateFlow<Set<Long>>(emptySet())
    val selected: StateFlow<Set<Long>> = _selected.asStateFlow()

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    private val _exhausted = MutableStateFlow(false)
    val exhausted: StateFlow<Boolean> = _exhausted.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    init { loadMore() }

    fun loadMore() {
        if (_loading.value || _exhausted.value) return
        viewModelScope.launch {
            _loading.value = true
            val from = _messages.value.lastOrNull()?.id ?: 0L
            runCatching { repo.loadHistory(chatId, from) }
                .onSuccess { newer ->
                    if (newer.isEmpty()) _exhausted.value = true
                    else _messages.value = _messages.value + newer
                }
                .onFailure { _error.value = it.message }
            _loading.value = false
        }
    }

    fun toggleSelection(id: Long) {
        _selected.value = _selected.value.toMutableSet().apply {
            if (!add(id)) remove(id)
        }
    }

    fun clearSelection() { _selected.value = emptySet() }

    fun selectedMessages(): List<MessageItem> {
        val ids = _selected.value
        return _messages.value.filter { it.id in ids }
    }

    fun consumeError() { _error.value = null }
}
