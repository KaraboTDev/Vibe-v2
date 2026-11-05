package com.karabotshivhase.vibe

import com.karabotshivhase.vibe.auth.AuthRepository
import com.karabotshivhase.vibe.auth.AuthResult
import com.karabotshivhase.vibe.network.ApiClient
import com.karabotshivhase.vibe.ui.auth.LoginViewModel
import com.karabotshivhase.vibe.ui.auth.RegisterViewModel
import com.karabotshivhase.vibe.ui.auth.UiEvent
import com.karabotshivhase.vibe.ui.settings.FakeSessionStore
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class GoogleSsoTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun googleLogin_sends_idToken_and_saves_session_on_success() = runTest {
        val mockWebServer = MockWebServer()
        mockWebServer.start()
        try {
            val session = FakeSessionStore()
            val api = ApiClient.createApi(baseUrl = mockWebServer.url("/").toString(), tokenProvider = { session.token() })
            val authRepository = AuthRepository(api, session)

            val responseJson = """
                {
                    "token": "fake-google-jwt",
                    "userId": 42,
                    "email": "googleuser@example.com",
                    "expiresAt": "2026-12-31T23:59:59Z"
                }
            """.trimIndent()
            mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody(responseJson))

            val result = authRepository.googleLogin("sample-google-id-token")

            val recordedRequest = mockWebServer.takeRequest()
            assertEquals("POST", recordedRequest.method)
            assertEquals("/auth/google", recordedRequest.path)
            assertTrue(recordedRequest.body.readUtf8().contains("sample-google-id-token"))

            assertTrue(result is AuthResult.Success)
            assertEquals("fake-google-jwt", session.tokenValue)
            assertEquals("googleuser@example.com", session.emailValue)
        } finally {
            mockWebServer.shutdown()
        }
    }

    @Test
    fun loginViewModel_loginWithGoogle_emits_NavigateHome_on_success() = runTest {
        val mockWebServer = MockWebServer()
        mockWebServer.start()
        try {
            val session = FakeSessionStore()
            val api = ApiClient.createApi(baseUrl = mockWebServer.url("/").toString(), tokenProvider = { session.token() })
            val authRepository = AuthRepository(api, session)

            val responseJson = """
                {
                    "token": "fake-google-jwt",
                    "userId": 42,
                    "email": "googleuser@example.com",
                    "expiresAt": "2026-12-31T23:59:59Z"
                }
            """.trimIndent()
            mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody(responseJson))

            val viewModel = LoginViewModel(authRepository)

            viewModel.loginWithGoogle("sample-id-token", authRepository)

            val event = viewModel.events.first()
            assertEquals(UiEvent.NavigateHome, event)
        } finally {
            mockWebServer.shutdown()
        }
    }

    @Test
    fun registerViewModel_registerWithGoogle_emits_NavigateHome_on_success() = runTest {
        val mockWebServer = MockWebServer()
        mockWebServer.start()
        try {
            val session = FakeSessionStore()
            val api = ApiClient.createApi(baseUrl = mockWebServer.url("/").toString(), tokenProvider = { session.token() })
            val authRepository = AuthRepository(api, session)

            val responseJson = """
                {
                    "token": "fake-google-jwt",
                    "userId": 42,
                    "email": "googleuser@example.com",
                    "expiresAt": "2026-12-31T23:59:59Z"
                }
            """.trimIndent()
            mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody(responseJson))

            val viewModel = RegisterViewModel(authRepository)

            viewModel.registerWithGoogle("sample-id-token", authRepository)

            val event = viewModel.events.first()
            assertEquals(UiEvent.NavigateHome, event)
        } finally {
            mockWebServer.shutdown()
        }
    }
}
