package com.karabotshivhase.vibe

import android.app.Application
import com.karabotshivhase.vibe.auth.AuthRepository
import com.karabotshivhase.vibe.auth.TokenStore
import com.karabotshivhase.vibe.network.ApiClient
import com.karabotshivhase.vibe.network.ApiConfig
import com.karabotshivhase.vibe.network.VibeApi

class VibeApp : Application() {
    val tokenStore: TokenStore by lazy {
        TokenStore(this)
    }

    val api: VibeApi by lazy {
        ApiClient.createApi(
            baseUrl = ApiConfig.BASE_URL,
            tokenProvider = { tokenStore.token() },
        )
    }

    val authRepository: AuthRepository by lazy {
        AuthRepository(api = api, session = tokenStore)
    }
}
