package com.example.tgexporter.telegram

sealed interface AuthState {
    data object Initializing : AuthState
    data object WaitPhone : AuthState
    data class WaitCode(val phoneNumber: String) : AuthState
    data object WaitPassword : AuthState
    data object Ready : AuthState
    data object LoggingOut : AuthState
    data object Closed : AuthState
    data class Error(val message: String) : AuthState
}
