package com.karabotshivhase.vibe.ui.auth

import androidx.annotation.StringRes

data class LoginUiState(
    val isLoading: Boolean = false,
    @StringRes val emailError: Int? = null,
    @StringRes val passwordError: Int? = null,
    @StringRes val messageRes: Int? = null,
)
