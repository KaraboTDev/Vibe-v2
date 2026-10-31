package com.karabotshivhase.vibe.auth

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AuthValidatorTest {

    @Test
    fun validateEmail_blank_returns_EmailBlank() {
        assertEquals(ValidationError.EmailBlank, AuthValidator.validateEmail("   "))
        assertEquals(ValidationError.EmailBlank, AuthValidator.validateEmail(""))
        assertEquals(ValidationError.EmailBlank, AuthValidator.validateEmail(null))
    }

    @Test
    fun validateEmail_without_at_returns_EmailInvalid() {
        assertEquals(ValidationError.EmailInvalid, AuthValidator.validateEmail("invalidemail.com"))
    }

    @Test
    fun validateEmail_valid_with_spaces_returns_null() {
        assertNull(AuthValidator.validateEmail("  user@domain.com  "))
    }

    @Test
    fun validatePassword_short_returns_PasswordTooShort() {
        assertEquals(ValidationError.PasswordTooShort, AuthValidator.validatePassword("1234567"))
    }

    @Test
    fun validatePassword_eight_chars_returns_null() {
        assertNull(AuthValidator.validatePassword("12345678"))
    }

    @Test
    fun validateConfirm_mismatch_returns_PasswordsDoNotMatch() {
        assertEquals(ValidationError.PasswordsDoNotMatch, AuthValidator.validateConfirm("pass1234", "pass5678"))
    }
}
