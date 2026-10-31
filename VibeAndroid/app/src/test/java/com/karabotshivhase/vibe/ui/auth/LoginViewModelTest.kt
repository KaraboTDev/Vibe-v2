package com.karabotshivhase.vibe.ui.auth

import com.karabotshivhase.vibe.MainDispatcherRule
import com.karabotshivhase.vibe.R
import com.karabotshivhase.vibe.auth.AuthResult
import com.karabotshivhase.vibe.auth.FailureKind
import com.karabotshivhase.vibe.auth.FakeAuthActions
import com.karabotshivhase.vibe.network.AuthResponse
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class LoginViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var fakeAuthActions: FakeAuthActions
    private lateinit var viewModel: LoginViewModel

    @Before
    fun setUp() {
        fakeAuthActions = FakeAuthActions()
        viewModel = LoginViewModel(fakeAuthActions)
    }

    @Test
    fun login_blank_email_sets_email_error_and_does_not_call_repository() {
        viewModel.login("   ", "password123")

        assertEquals(R.string.error_email_blank, viewModel.uiState.value.emailError)
        assertNull(viewModel.uiState.value.passwordError)
        assertEquals(0, fakeAuthActions.loginCallCount)
    }

    @Test
    fun login_blank_password_sets_password_error() {
        viewModel.login("user@test.com", "")

        assertNull(viewModel.uiState.value.emailError)
        assertEquals(R.string.error_password_blank, viewModel.uiState.value.passwordError)
        assertEquals(0, fakeAuthActions.loginCallCount)
    }

    @Test
    fun login_success_emits_NavigateHome() = runTest {
        val response = AuthResponse("token123", 1, "user@test.com", "2030-01-01T00:00:00Z")
        fakeAuthActions.loginResult = AuthResult.Success(response)

        var emittedEvent: UiEvent? = null
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.events.collect { event ->
                emittedEvent = event
            }
        }

        viewModel.login("user@test.com", "password123")

        assertEquals(UiEvent.NavigateHome, emittedEvent)
        assertEquals(1, fakeAuthActions.loginCallCount)
    }

    @Test
    fun login_wrong_password_shows_invalid_credentials_message() = runTest {
        fakeAuthActions.loginResult = AuthResult.Failure(FailureKind.InvalidCredentials, "Wrong password")

        viewModel.login("user@test.com", "wrongpass")

        assertEquals(R.string.error_invalid_credentials, viewModel.uiState.value.messageRes)
        assertEquals(1, fakeAuthActions.loginCallCount)
    }

    @Test
    fun login_network_failure_shows_network_message() = runTest {
        fakeAuthActions.loginResult = AuthResult.Failure(FailureKind.Network, null)

        viewModel.login("user@test.com", "password123")

        assertEquals(R.string.error_network, viewModel.uiState.value.messageRes)
        assertEquals(1, fakeAuthActions.loginCallCount)
    }

    @Test
    fun login_double_tap_while_loading_calls_repository_once() = runTest {
        val gate = CompletableDeferred<Unit>()
        fakeAuthActions.suspendGate = gate
        fakeAuthActions.loginResult = AuthResult.Success(AuthResponse("token", 1, "user@test.com", "2030-01-01T00:00:00Z"))

        val loginJob = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.login("user@test.com", "password123")
        }

        assertTrue(viewModel.uiState.value.isLoading)

        // Attempt second login call while loading
        viewModel.login("user@test.com", "password123")

        // Release gate
        gate.complete(Unit)
        loginJob.join()

        assertEquals(1, fakeAuthActions.loginCallCount)
    }
}
