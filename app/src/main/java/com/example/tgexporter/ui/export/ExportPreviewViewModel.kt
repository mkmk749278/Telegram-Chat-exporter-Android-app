package com.example.tgexporter.ui.export

import androidx.lifecycle.ViewModel
import com.example.tgexporter.data.model.MessageItem
import com.example.tgexporter.export.MarkdownFormatter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class ExportPreviewViewModel(
    val chatTitle: String,
    selected: List<MessageItem>,
) : ViewModel() {
    private val _markdown = MutableStateFlow(MarkdownFormatter.format(chatTitle, selected))
    val markdown: StateFlow<String> = _markdown.asStateFlow()
}
