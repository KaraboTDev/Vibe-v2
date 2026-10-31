package com.karabotshivhase.vibe.auth

import com.karabotshivhase.vibe.network.AuthResponse
import com.karabotshivhase.vibe.network.ErrorBody
import com.karabotshivhase.vibe.network.LoginRequest
import com.karabotshivhase.vibe.network.RegisterRequest
import com.karabotshivhase.vibe.network.VibeApi
import com.google.gson.Gson
import kotlinx.coroutines.CancellationException
import retrofit2.Response
import java.io.IOException

class AuthRepository(
    private val api: VibeApi,
    private val session: SessionStore,
) {
    private val gson = Gson()

    suspend fun login(email: String, password: String): AuthResult {
        val processedEmail = email.trim().lowercase()
        return executeAuthCall {
            api.login(LoginRequest(email = processedEmail, password = password))
        }
    }

    suspend fun register(email: String, password: String): AuthResult {
        val processedEmail = email.trim().lowercase()
        return executeAuthCall {
            api.register(RegisterRequest(email = processedEmail, password = password))
        }
    }

    private suspend fun executeAuthCall(call: suspend () -> Response<AuthResponse>): AuthResult {
        return try {
            val response = call()
            if (response.isSuccessful) {
                val body = response.body()
                if (body != null) {
                    session.save(body)
                    AuthResult.Success(body)
                } else {
                    AuthResult.Failure(FailureKind.Unknown, null)
                }
            } else {
                val kind = when (response.code()) {
                    400 -> FailureKind.InvalidInput
                    401 -> FailureKind.InvalidCredentials
                    409 -> FailureKind.EmailTaken
                    else -> FailureKind.Unknown
                }
                val rawError = response.errorBody()?.string()
                val serverMessage = parseErrorBody(rawError)
                AuthResult.Failure(kind, serverMessage)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (_: IOException) {
            AuthResult.Failure(FailureKind.Network, null)
        } catch (_: Throwable) {
            AuthResult.Failure(FailureKind.Unknown, null)
        }
    }

    private fun parseErrorBody(rawError: String?): String? {
        if (rawError.isNullOrBlank()) return null
        return try {
            val errorObj = gson.fromJson(rawError, ErrorBody::class.java)
            errorObj?.message
        } catch (_: Exception) {
            null
        }
    }
}
