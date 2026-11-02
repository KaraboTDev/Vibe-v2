package com.karabotshivhase.vibe.venue

import com.google.gson.JsonParseException
import com.google.gson.stream.MalformedJsonException
import com.karabotshivhase.vibe.network.VibeApi
import kotlinx.coroutines.CancellationException
import java.io.IOException

class VenueRepository(
    private val api: VibeApi,
) : VenueSource {

    override suspend fun getTags(): LoadResult<List<Tag>> {
        val response = try {
            api.getTags()
        } catch (e: CancellationException) {
            throw e
        } catch (_: MalformedJsonException) {
            return LoadResult.Failure(LoadFailure.Unknown)
        } catch (_: JsonParseException) {
            return LoadResult.Failure(LoadFailure.Unknown)
        } catch (_: IOException) {
            return LoadResult.Failure(LoadFailure.Network)
        } catch (_: Throwable) {
            return LoadResult.Failure(LoadFailure.Unknown)
        }

        return try {
            if (response.isSuccessful) {
                val body = response.body()
                if (body != null) {
                    val domainTags = body.map { dto ->
                        Tag(
                            id = dto.id ?: 0,
                            name = dto.name.orEmpty(),
                        )
                    }
                    LoadResult.Success(domainTags)
                } else {
                    LoadResult.Failure(LoadFailure.Unknown)
                }
            } else {
                mapErrorCode(response.code())
            }
        } catch (_: Throwable) {
            LoadResult.Failure(LoadFailure.Unknown)
        }
    }

    override suspend fun search(
        mood: String,
        areaLat: Double,
        areaLng: Double,
        radiusKm: Double,
        limit: Int,
    ): LoadResult<List<Venue>> {
        val response = try {
            api.searchVenues(mood, areaLat, areaLng, radiusKm, limit)
        } catch (e: CancellationException) {
            throw e
        } catch (_: MalformedJsonException) {
            return LoadResult.Failure(LoadFailure.Unknown)
        } catch (_: JsonParseException) {
            return LoadResult.Failure(LoadFailure.Unknown)
        } catch (_: IOException) {
            return LoadResult.Failure(LoadFailure.Network)
        } catch (_: Throwable) {
            return LoadResult.Failure(LoadFailure.Unknown)
        }

        return try {
            if (response.isSuccessful) {
                val body = response.body()
                if (body != null) {
                    val venues = body.map { dto ->
                        Venue(
                            id = dto.id ?: 0,
                            name = dto.name.orEmpty(),
                            latitude = dto.latitude ?: 0.0,
                            longitude = dto.longitude ?: 0.0,
                            address = dto.address,
                            openingHours = dto.openingHours,
                            tags = dto.tags.orEmpty(),
                            distanceMeters = dto.distanceMeters ?: 0,
                        )
                    }
                    LoadResult.Success(venues)
                } else {
                    LoadResult.Failure(LoadFailure.Unknown)
                }
            } else {
                mapErrorCode(response.code())
            }
        } catch (_: Throwable) {
            LoadResult.Failure(LoadFailure.Unknown)
        }
    }

    override suspend fun getVenue(id: Int): LoadResult<VenueDetail> {
        val response = try {
            api.getVenue(id)
        } catch (e: CancellationException) {
            throw e
        } catch (_: MalformedJsonException) {
            return LoadResult.Failure(LoadFailure.Unknown)
        } catch (_: JsonParseException) {
            return LoadResult.Failure(LoadFailure.Unknown)
        } catch (_: IOException) {
            return LoadResult.Failure(LoadFailure.Network)
        } catch (_: Throwable) {
            return LoadResult.Failure(LoadFailure.Unknown)
        }

        return try {
            if (response.isSuccessful) {
                val body = response.body()
                if (body != null) {
                    val detail = VenueDetail(
                        id = body.id ?: id,
                        name = body.name.orEmpty(),
                        latitude = body.latitude ?: 0.0,
                        longitude = body.longitude ?: 0.0,
                        address = body.address,
                        openingHours = body.openingHours,
                        tags = body.tags.orEmpty().map { voteDto ->
                            TagVote(
                                name = voteDto.name.orEmpty(),
                                voteCount = voteDto.voteCount ?: 0,
                            )
                        },
                    )
                    LoadResult.Success(detail)
                } else {
                    LoadResult.Failure(LoadFailure.Unknown)
                }
            } else {
                mapErrorCode(response.code())
            }
        } catch (_: Throwable) {
            LoadResult.Failure(LoadFailure.Unknown)
        }
    }

    private fun mapErrorCode(code: Int): LoadResult.Failure {
        return when (code) {
            400 -> LoadResult.Failure(LoadFailure.InvalidInput)
            404 -> LoadResult.Failure(LoadFailure.NotFound)
            else -> LoadResult.Failure(LoadFailure.Unknown)
        }
    }
}
