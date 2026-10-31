package com.karabotshivhase.vibe.network

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface VibeApi {
    @POST("auth/register")
    suspend fun register(@Body request: RegisterRequest): Response<AuthResponse>

    @POST("auth/login")
    suspend fun login(@Body request: LoginRequest): Response<AuthResponse>
}
