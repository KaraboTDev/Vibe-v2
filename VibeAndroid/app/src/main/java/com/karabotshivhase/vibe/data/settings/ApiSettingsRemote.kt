package com.karabotshivhase.vibe.data.settings

import com.google.gson.JsonParseException
import com.google.gson.stream.MalformedJsonException
import com.karabotshivhase.vibe.data.favorites.RemoteResult
import com.karabotshivhase.vibe.network.SettingsDto
import com.karabotshivhase.vibe.network.VibeApi
import kotlinx.coroutines.CancellationException
import retrofit2.Response
import java.io.IOException

class ApiSettingsRemote(
    private val api: VibeApi,
) : SettingsRemote {

    override suspend fun fetch(): RemoteResult<RemoteSettings> {
        return executeCall {
            val response = api.getSettings()
            handleResponse(response)
        }
    }

    override suspend fun update(settings: RemoteSettings): RemoteResult<RemoteSettings> {
        return executeCall {
            val dto = SettingsDto(
                preferredLanguage = settings.language,
                defaultArea = settings.defaultArea,
                notificationsEnabled = settings.notificationsEnabled,
            )
            val response = api.updateSettings(dto)
            handleResponse(response)
        }
    }

    private fun handleResponse(response: Response<SettingsDto>): RemoteResult<RemoteSettings> {
        return when {
            response.isSuccessful -> {
                val body = response.body()
                if (body != null) {
                    try {
                        val remote = RemoteSettings(
                            language = LanguageCodes.normalize(body.preferredLanguage),
                            defaultArea = body.defaultArea,
                            notificationsEnabled = body.notificationsEnabled ?: true,
                        )
                        RemoteResult.Success(remote)
                    } catch (_: Exception) {
                        RemoteResult.OtherError
                    }
                } else {
                    RemoteResult.OtherError
                }
            }
            response.code() == 401 -> RemoteResult.Unauthorized
            response.code() == 404 -> RemoteResult.NotFound
            else -> RemoteResult.OtherError
        }
    }

    private suspend fun <T> executeCall(block: suspend () -> RemoteResult<T>): RemoteResult<T> {
        return try {
            block()
        } catch (e: CancellationException) {
            throw e
        } catch (_: MalformedJsonException) {
            RemoteResult.OtherError
        } catch (_: JsonParseException) {
            RemoteResult.OtherError
        } catch (_: IOException) {
            RemoteResult.NetworkError
        } catch (_: Throwable) {
            RemoteResult.OtherError
        }
    }
}
