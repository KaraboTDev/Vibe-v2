package com.karabotshivhase.vibe.ui.auth

import androidx.annotation.StringRes

data class RegisterUiState(
    val isLoading: Boolean = false,
    @StringRes val emailError: Int? = null,
    @StringRes val passwordError: Int? = null,
    @StringRes val confirmError: Int? = null,
    @StringRes val messageRes: Int? = null,
)
