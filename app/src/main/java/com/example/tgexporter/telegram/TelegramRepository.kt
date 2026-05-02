package com.example.tgexporter.telegram

import android.content.Context
import android.os.Build
import android.util.Log
import com.example.tgexporter.BuildConfig
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import org.drinkless.tdlib.Client
import org.drinkless.tdlib.TdApi
import java.io.File

class TelegramRepository(context: Context) {

    private val tdlibDir: File = File(context.filesDir, "tdlib").apply { mkdirs() }
    private val systemLanguage: String = java.util.Locale.getDefault().language.ifEmpty { "en" }

    private val _authState = MutableStateFlow<AuthState>(AuthState.Initializing)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    private val _updates = MutableSharedFlow<TdApi.Update>(extraBufferCapacity = 64, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    val updates: SharedFlow<TdApi.Update> = _updates.asSharedFlow()

    private val client: Client = Client.create(
        { obj -> handleUpdate(obj) },
        { ex -> Log.e(TAG, "TDLib error", ex) },
        { ex -> Log.e(TAG, "Default exception", ex) },
    )

    private fun handleUpdate(obj: TdApi.Object) {
        if (obj is TdApi.UpdateAuthorizationState) onAuthState(obj.authorizationState)
        if (obj is TdApi.Update) _updates.tryEmit(obj)
    }

    private fun onAuthState(state: TdApi.AuthorizationState) {
        when (state) {
            is TdApi.AuthorizationStateWaitTdlibParameters -> sendTdlibParameters()
            is TdApi.AuthorizationStateWaitPhoneNumber -> _authState.value = AuthState.WaitPhone
            is TdApi.AuthorizationStateWaitCode -> {
                val phone = (_authState.value as? AuthState.WaitCode)?.phoneNumber ?: ""
                _authState.value = AuthState.WaitCode(phone)
            }
            is TdApi.AuthorizationStateWaitPassword -> _authState.value = AuthState.WaitPassword
            is TdApi.AuthorizationStateReady -> _authState.value = AuthState.Ready
            is TdApi.AuthorizationStateLoggingOut -> _authState.value = AuthState.LoggingOut
            is TdApi.AuthorizationStateClosing -> { /* wait for closed */ }
            is TdApi.AuthorizationStateClosed -> _authState.value = AuthState.Closed
            else -> { /* WaitOtherDeviceConfirmation, WaitRegistration, WaitEmailAddress: not handled in v0 */ }
        }
    }

    private fun sendTdlibParameters() {
        val params = TdApi.SetTdlibParameters().apply {
            databaseDirectory = tdlibDir.absolutePath
            useMessageDatabase = true
            useSecretChats = false
            apiId = BuildConfig.API_ID
            apiHash = BuildConfig.API_HASH
            systemLanguageCode = systemLanguage
            deviceModel = Build.MODEL ?: "Android"
            systemVersion = Build.VERSION.RELEASE ?: "?"
            applicationVersion = "0.1.0"
        }
        client.send(params) { result ->
            if (result is TdApi.Error) _authState.value = AuthState.Error(result.message)
        }
    }

    suspend fun submitPhone(phone: String) {
        _authState.value = AuthState.WaitCode(phone)
        client.await(TdApi.SetAuthenticationPhoneNumber(phone, null))
    }

    suspend fun submitCode(code: String) {
        client.await(TdApi.CheckAuthenticationCode(code))
    }

    suspend fun submitPassword(password: String) {
        client.await(TdApi.CheckAuthenticationPassword(password))
    }

    suspend fun logOut() {
        client.await(TdApi.LogOut())
    }

    fun client(): Client = client

    companion object {
        private const val TAG = "TelegramRepository"
    }
}
