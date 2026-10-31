package com.karabotshivhase.vibe.auth

import com.karabotshivhase.vibe.network.ApiClient
import com.karabotshivhase.vibe.network.AuthResponse
import com.karabotshivhase.vibe.network.VibeApi
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class AuthRepositoryTest {

    private lateinit var mockWebServer: MockWebServer
    private lateinit var fakeSessionStore: FakeSessionStore
    private lateinit var api: VibeApi
    private lateinit var repository: AuthRepository

    private class FakeSessionStore : SessionStore {
        var savedResponse: AuthResponse? = null

        override fun save(response: AuthResponse) {
            savedResponse = response
        }

        override fun clear() {
            savedResponse = null
        }
    }

    @Before
    fun setUp() {
        mockWebServer = MockWebServer()
        mockWebServer.start()
        fakeSessionStore = FakeSessionStore()
        val baseUrl = mockWebServer.url("/").toString()
        api = ApiClient.createApi(baseUrl = baseUrl, tokenProvider = { fakeSessionStore.savedResponse?.token })
        repository = AuthRepository(api, fakeSessionStore)
    }

    @After
    fun tearDown() {
        try {
            mockWebServer.shutdown()
        } catch (_: Exception) {
        }
    }

    @Test
    fun login_success_saves_session_and_returns_Success() = runTest {
        val jsonResponse = """
            {
                "token": "token123",
                "userId": 42,
                "email": "user@test.com",
                "expiresAt": "2026-09-22T03:02:18.8974058Z"
            }
        """.trimIndent()
        mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody(jsonResponse))

        val result = repository.login("user@test.com", "password123")

        assertTrue(result is AuthResult.Success)
        val success = result as AuthResult.Success
        val expected = AuthResponse(
            token = "token123",
            userId = 42,
            email = "user@test.com",
            expiresAt = "2026-09-22T03:02:18.8974058Z"
        )
        assertEquals(expected, success.response)
        assertEquals(expected, fakeSessionStore.savedResponse)
    }

    @Test
    fun register_201_returns_Success() = runTest {
        val jsonResponse = """
            {
                "token": "token456",
                "userId": 99,
                "email": "newuser@test.com",
                "expiresAt": "2026-09-22T03:02:18.8974058Z"
            }
        """.trimIndent()
        mockWebServer.enqueue(MockResponse().setResponseCode(201).setBody(jsonResponse))

        val result = repository.register("newuser@test.com", "password123")

        assertTrue(result is AuthResult.Success)
        val success = result as AuthResult.Success
        assertEquals("token456", success.response.token)
        assertEquals(99, success.response.userId)
        assertEquals(fakeSessionStore.savedResponse, success.response)
    }

    @Test
    fun login_401_returns_InvalidCredentials_with_server_message() = runTest {
        val jsonResponse = """{"message":"Invalid credentials"}"""
        mockWebServer.enqueue(MockResponse().setResponseCode(401).setBody(jsonResponse))

        val result = repository.login("user@test.com", "wrongpass")

        assertTrue(result is AuthResult.Failure)
        val failure = result as AuthResult.Failure
        assertEquals(FailureKind.InvalidCredentials, failure.kind)
        assertEquals("Invalid credentials", failure.serverMessage)
    }

    @Test
    fun register_409_returns_EmailTaken() = runTest {
        val jsonResponse = """{"message":"Email already registered"}"""
        mockWebServer.enqueue(MockResponse().setResponseCode(409).setBody(jsonResponse))

        val result = repository.register("existing@test.com", "password123")

        assertTrue(result is AuthResult.Failure)
        val failure = result as AuthResult.Failure
        assertEquals(FailureKind.EmailTaken, failure.kind)
        assertEquals("Email already registered", failure.serverMessage)
    }

    @Test
    fun login_400_returns_InvalidInput() = runTest {
        val jsonResponse = """{"message":"Bad request"}"""
        mockWebServer.enqueue(MockResponse().setResponseCode(400).setBody(jsonResponse))

        val result = repository.login("user@test.com", "badreq")

        assertTrue(result is AuthResult.Failure)
        val failure = result as AuthResult.Failure
        assertEquals(FailureKind.InvalidInput, failure.kind)
        assertEquals("Bad request", failure.serverMessage)
    }

    @Test
    fun login_html_error_body_does_not_crash_and_returns_Unknown() = runTest {
        val htmlResponse = "<html><body>500 Internal Server Error</body></html>"
        mockWebServer.enqueue(MockResponse().setResponseCode(500).setBody(htmlResponse))

        val result = repository.login("user@test.com", "password123")

        assertTrue(result is AuthResult.Failure)
        val failure = result as AuthResult.Failure
        assertEquals(FailureKind.Unknown, failure.kind)
        assertNull(failure.serverMessage)
    }

    @Test
    fun login_server_down_returns_Network() = runTest {
        mockWebServer.shutdown()

        val result = repository.login("user@test.com", "password123")

        assertTrue(result is AuthResult.Failure)
        val failure = result as AuthResult.Failure
        assertEquals(FailureKind.Network, failure.kind)
    }

    @Test
    fun register_sends_trimmed_lowercase_email() = runTest {
        val jsonResponse = """
            {
                "token": "token789",
                "userId": 100,
                "email": "user@test.com",
                "expiresAt": "2026-09-22T03:02:18.8974058Z"
            }
        """.trimIndent()
        mockWebServer.enqueue(MockResponse().setResponseCode(201).setBody(jsonResponse))

        repository.register("   USER@TEST.COM   ", "password123")

        val recordedRequest = mockWebServer.takeRequest()
        val requestBody = recordedRequest.body.readUtf8()
        assertTrue(requestBody.contains("user@test.com"))
        assertTrue(!requestBody.contains("USER@TEST.COM"))
    }
}
