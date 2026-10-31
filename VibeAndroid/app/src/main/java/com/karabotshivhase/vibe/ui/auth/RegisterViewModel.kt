package com.karabotshivhase.vibe.ui.auth

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.karabotshivhase.vibe.R
import com.karabotshivhase.vibe.auth.AuthActions
import com.karabotshivhase.vibe.auth.AuthResult
import com.karabotshivhase.vibe.auth.AuthValidator
import com.karabotshivhase.vibe.auth.FailureKind
import com.karabotshivhase.vibe.auth.ValidationError
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class RegisterViewModel(
    private val authActions: AuthActions,
) : ViewModel() {

    private val _uiState = MutableStateFlow(RegisterUiState())
    val uiState: StateFlow<RegisterUiState> = _uiState.asStateFlow()

    private val _events = Channel<UiEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    fun register(email: String, password: String, confirm: String) {
        if (_uiState.value.isLoading) return

        val emailErr = AuthValidator.validateEmail(email)
        val passwordErr = AuthValidator.validatePassword(password)
        val confirmErr = AuthValidator.validateConfirm(password, confirm)

        if ((emailErr != null) || (passwordErr != null) || (confirmErr != null)) {
            _uiState.update {
                it.copy(
                    emailError = mapValidationError(emailErr),
                    passwordError = mapValidationError(passwordErr),
                    confirmError = mapValidationError(confirmErr),
                )
            }
            return
        }

        _uiState.update {
            it.copy(
                isLoading = true,
                emailError = null,
                passwordError = null,
                confirmError = null,
                messageRes = null,
            )
        }

        viewModelScope.launch {
            try {
                when (val result = authActions.register(email, password)) {
                    is AuthResult.Success -> {
                        _events.send(UiEvent.NavigateHome)
                    }
                    is AuthResult.Failure -> {
                        _uiState.update {
                            it.copy(messageRes = mapFailureKind(result.kind))
                        }
                    }
                }
            } finally {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    fun messageShown() {
        _uiState.update { it.copy(messageRes = null) }
    }

    fun clearEmailError() {
        _uiState.update { it.copy(emailError = null) }
    }

    fun clearPasswordError() {
        _uiState.update { it.copy(passwordError = null) }
    }

    fun clearConfirmError() {
        _uiState.update { it.copy(confirmError = null) }
    }

    @StringRes
    private fun mapValidationError(error: ValidationError?): Int? {
        return when (error) {
            ValidationError.EmailBlank -> R.string.error_email_blank
            ValidationError.EmailInvalid -> R.string.error_email_invalid
            ValidationError.PasswordBlank -> R.string.error_password_blank
            ValidationError.PasswordTooShort -> R.string.error_password_short
            ValidationError.PasswordsDoNotMatch -> R.string.error_passwords_mismatch
            null -> null
        }
    }

    @StringRes
    private fun mapFailureKind(kind: FailureKind): Int {
        return when (kind) {
            FailureKind.InvalidCredentials -> R.string.error_invalid_credentials
            FailureKind.EmailTaken -> R.string.error_email_taken
            FailureKind.InvalidInput -> R.string.error_invalid_input
            FailureKind.Network -> R.string.error_network
            FailureKind.Unknown -> R.string.error_unknown
        }
    }

    companion object {
        fun Factory(authActions: AuthActions) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return RegisterViewModel(authActions) as T
            }
        }
    }
}
