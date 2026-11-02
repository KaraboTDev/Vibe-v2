package com.karabotshivhase.vibe.network

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.POST
import retrofit2.http.Query

interface VibeApi {
    @POST("auth/register")
    suspend fun register(@Body request: RegisterRequest): Response<AuthResponse>

    @POST("auth/login")
    suspend fun login(@Body request: LoginRequest): Response<AuthResponse>

    @GET("tags")
    suspend fun getTags(): Response<List<TagDto>>

    @GET("venues")
    suspend fun searchVenues(
        @Query("mood") mood: String,
        @Query("lat") lat: Double,
        @Query("lng") lng: Double,
        @Query("radiusKm") radiusKm: Double,
        @Query("limit") limit: Int,
    ): Response<List<VenueSummaryDto>>

    @GET("venues/{id}")
    suspend fun getVenue(
        @Path("id") id: Int,
    ): Response<VenueDetailDto>

    @GET("favorites")
    suspend fun getFavorites(): Response<List<FavoriteDto>>

    @POST("favorites")
    suspend fun addFavorite(
        @Body body: Map<String, Int>,
    ): Response<FavoriteDto>

    @DELETE("favorites/{venueId}")
    suspend fun removeFavorite(
        @Path("venueId") venueId: Int,
    ): Response<Unit>
}
