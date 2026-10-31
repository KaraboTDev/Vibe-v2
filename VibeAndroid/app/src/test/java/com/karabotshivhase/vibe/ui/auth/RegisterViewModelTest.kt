package com.karabotshivhase.vibe.ui.auth

import com.karabotshivhase.vibe.MainDispatcherRule
import com.karabotshivhase.vibe.R
import com.karabotshivhase.vibe.auth.AuthResult
import com.karabotshivhase.vibe.auth.FailureKind
import com.karabotshivhase.vibe.auth.FakeAuthActions
import com.karabotshivhase.vibe.network.AuthResponse
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class RegisterViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var fakeAuthActions: FakeAuthActions
    private lateinit var viewModel: RegisterViewModel

    @Before
    fun setUp() {
        fakeAuthActions = FakeAuthActions()
        viewModel = RegisterViewModel(fakeAuthActions)
    }

    @Test
    fun register_password_mismatch_sets_confirm_error_and_does_not_call_repository() {
        viewModel.register("user@test.com", "password123", "different123")

        assertNull(viewModel.uiState.value.emailError)
        assertNull(viewModel.uiState.value.passwordError)
        assertEquals(R.string.error_passwords_mismatch, viewModel.uiState.value.confirmError)
        assertEquals(0, fakeAuthActions.registerCallCount)
    }

    @Test
    fun register_email_taken_shows_email_taken_message() = runTest {
        fakeAuthActions.registerResult = AuthResult.Failure(FailureKind.EmailTaken, "Email registered")

        viewModel.register("existing@test.com", "password123", "password123")

        assertEquals(R.string.error_email_taken, viewModel.uiState.value.messageRes)
        assertEquals(1, fakeAuthActions.registerCallCount)
    }

    @Test
    fun register_success_emits_NavigateHome() = runTest {
        val response = AuthResponse("token456", 2, "newuser@test.com", "2030-01-01T00:00:00Z")
        fakeAuthActions.registerResult = AuthResult.Success(response)

        var emittedEvent: UiEvent? = null
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.events.collect { event ->
                emittedEvent = event
            }
        }

        viewModel.register("newuser@test.com", "password123", "password123")

        assertEquals(UiEvent.NavigateHome, emittedEvent)
        assertEquals(1, fakeAuthActions.registerCallCount)
    }
}
