package com.example.tgexporter.telegram

import kotlinx.coroutines.suspendCancellableCoroutine
import org.drinkless.tdlib.Client
import org.drinkless.tdlib.TdApi
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class TdException(val code: Int, message: String) : RuntimeException(message)

suspend fun Client.await(query: TdApi.Function<*>): TdApi.Object =
    suspendCancellableCoroutine { cont ->
        send(query) { result ->
            when (result) {
                is TdApi.Error -> cont.resumeWithException(TdException(result.code, result.message))
                else -> cont.resume(result)
            }
        }
    }

@Suppress("UNCHECKED_CAST")
suspend inline fun <reified T : TdApi.Object> Client.awaitTyped(query: TdApi.Function<*>): T =
    await(query) as T
