package com.karabotshivhase.vibe.data.favorites

import com.google.gson.JsonParseException
import com.google.gson.stream.MalformedJsonException
import com.karabotshivhase.vibe.network.FavoriteDto
import com.karabotshivhase.vibe.network.VibeApi
import kotlinx.coroutines.CancellationException
import retrofit2.Response
import java.io.IOException
import java.time.Instant

class ApiFavoriteRemote(
    private val api: VibeApi,
) : FavoriteRemote {

    override suspend fun fetchAll(): RemoteResult<List<RemoteFavorite>> {
        return executeCall {
            val response = api.getFavorites()
            handleResponse(response) { dtos ->
                dtos.map { mapDto(it) }
            }
        }
    }

    override suspend fun add(venueId: Int): RemoteResult<RemoteFavorite> {
        return executeCall {
            val response = api.addFavorite(mapOf("venueId" to venueId))
            handleResponse(response) { dto ->
                mapDto(dto)
            }
        }
    }

    override suspend fun remove(venueId: Int): RemoteResult<Unit> {
        return executeCall {
            val response = api.removeFavorite(venueId)
            when {
                response.isSuccessful -> RemoteResult.Success(Unit)
                response.code() == 401 -> RemoteResult.Unauthorized
                response.code() == 404 -> RemoteResult.NotFound
                else -> RemoteResult.OtherError
            }
        }
    }

    private inline fun <T, R> handleResponse(response: Response<T>, mapper: (T) -> R): RemoteResult<R> {
        return when {
            response.isSuccessful -> {
                val body = response.body()
                if (body != null) {
                    try {
                        RemoteResult.Success(mapper(body))
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

    private fun mapDto(dto: FavoriteDto): RemoteFavorite {
        val savedAtMillis = parseSavedAt(dto.savedAt)
        return RemoteFavorite(
            venueId = dto.venueId ?: 0,
            name = dto.name.orEmpty(),
            latitude = dto.latitude ?: 0.0,
            longitude = dto.longitude ?: 0.0,
            address = dto.address,
            openingHours = dto.openingHours,
            tags = dto.tags.orEmpty().filterNotNull(),
            savedAtMillis = savedAtMillis,
        )
    }

    private fun parseSavedAt(raw: String?): Long {
        if (raw.isNullOrBlank()) return System.currentTimeMillis()
        return try {
            Instant.parse(raw).toEpochMilli()
        } catch (_: Exception) {
            System.currentTimeMillis()
        }
    }
}
