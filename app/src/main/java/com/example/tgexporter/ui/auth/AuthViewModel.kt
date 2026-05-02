package com.example.tgexporter.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.tgexporter.telegram.AuthState
import com.example.tgexporter.telegram.TelegramRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AuthViewModel(private val telegram: TelegramRepository) : ViewModel() {

    val state: StateFlow<AuthState> = telegram.authState

    private val _busy = MutableStateFlow(false)
    val busy: StateFlow<Boolean> = _busy.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    fun submitPhone(raw: String) = launch { telegram.submitPhone(raw.trim()) }
    fun submitCode(raw: String) = launch { telegram.submitCode(raw.trim()) }
    fun submitPassword(raw: String) = launch { telegram.submitPassword(raw) }

    fun consumeError() { _errorMessage.value = null }

    private fun launch(block: suspend () -> Unit) = viewModelScope.launch {
        _busy.value = true
        runCatching { block() }
            .onFailure { _errorMessage.value = it.message ?: "Unknown error" }
        _busy.value = false
    }
}
