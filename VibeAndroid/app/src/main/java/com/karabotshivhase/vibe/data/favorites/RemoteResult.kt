package com.karabotshivhase.vibe.data.favorites

sealed interface RemoteResult<out T> {
    data class Success<out T>(val value: T) : RemoteResult<T>
    object NotFound : RemoteResult<Nothing>
    object Unauthorized : RemoteResult<Nothing>
    object NetworkError : RemoteResult<Nothing>
    object OtherError : RemoteResult<Nothing>
}
