package com.karabotshivhase.vibe.auth

object AuthValidator {
    fun validateEmail(email: String?): ValidationError? {
        val trimmed = email?.trim() ?: ""
        if (trimmed.isEmpty()) {
            return ValidationError.EmailBlank
        }
        val atCount = trimmed.count { it == '@' }
        if (atCount != 1) {
            return ValidationError.EmailInvalid
        }
        val atIndex = trimmed.indexOf('@')
        if (atIndex == 0) {
            return ValidationError.EmailInvalid
        }
        val domain = trimmed.substring(atIndex + 1)
        if (!domain.contains('.')) {
            return ValidationError.EmailInvalid
        }
        return null
    }

    fun validatePassword(password: String?): ValidationError? {
        if (password.isNullOrBlank()) {
            return ValidationError.PasswordBlank
        }
        if (password.length < 8) {
            return ValidationError.PasswordTooShort
        }
        return null
    }

    fun validateConfirm(password: String?, confirm: String?): ValidationError? {
        if (password != confirm) {
            return ValidationError.PasswordsDoNotMatch
        }
        return null
    }
}
