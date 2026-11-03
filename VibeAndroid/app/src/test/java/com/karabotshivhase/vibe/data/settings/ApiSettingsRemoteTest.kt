package com.karabotshivhase.vibe.data.settings

import com.karabotshivhase.vibe.data.favorites.RemoteResult
import com.karabotshivhase.vibe.network.ApiClient
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

class ApiSettingsRemoteTest {

    private lateinit var mockWebServer: MockWebServer
    private lateinit var api: VibeApi
    private lateinit var remote: ApiSettingsRemote

    @Before
    fun setUp() {
        mockWebServer = MockWebServer()
        mockWebServer.start()
        val baseUrl = mockWebServer.url("/").toString()
        api = ApiClient.createApi(baseUrl = baseUrl, tokenProvider = { null })
        remote = ApiSettingsRemote(api)
    }

    @After
    fun tearDown() {
        mockWebServer.shutdown()
    }

    @Test
    fun fetch_maps_fields() = runTest {
        val json = """
            {
                "preferredLanguage": "zu",
                "defaultArea": "Hatfield",
                "notificationsEnabled": false
            }
        """.trimIndent()
        mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody(json))

        val result = remote.fetch()

        assertTrue(result is RemoteResult.Success)
        val value = (result as RemoteResult.Success).value
        assertEquals("zu", value.language)
        assertEquals("Hatfield", value.defaultArea)
        assertEquals(false, value.notificationsEnabled)
    }

    @Test
    fun fetch_null_area_maps_to_null() = runTest {
        val json = """
            {
                "preferredLanguage": "en",
                "defaultArea": null,
                "notificationsEnabled": true
            }
        """.trimIndent()
        mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody(json))

        val result = remote.fetch()

        assertTrue(result is RemoteResult.Success)
        val value = (result as RemoteResult.Success).value
        assertNull(value.defaultArea)
    }

    @Test
    fun update_sends_expected_json_body() = runTest {
        val json = """
            {
                "preferredLanguage": "af",
                "defaultArea": "Menlyn",
                "notificationsEnabled": true
            }
        """.trimIndent()
        mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody(json))

        val input = RemoteSettings("af", "Menlyn", true)
        val result = remote.update(input)

        val recordedRequest = mockWebServer.takeRequest()
        assertEquals("PUT", recordedRequest.method)
        assertEquals("/settings", recordedRequest.path)
        assertTrue(recordedRequest.body.readUtf8().contains("preferredLanguage"))
        assertTrue(result is RemoteResult.Success)
    }

    @Test
    fun update_401_maps_to_Unauthorized() = runTest {
        mockWebServer.enqueue(MockResponse().setResponseCode(401).setBody("""{"message":"Unauthorized"}"""))

        val result = remote.update(RemoteSettings("en", null, true))

        assertEquals(RemoteResult.Unauthorized, result)
    }

    @Test
    fun fetch_server_down_maps_to_NetworkError() = runTest {
        mockWebServer.shutdown()

        val result = remote.fetch()

        assertEquals(RemoteResult.NetworkError, result)
    }

    @Test
    fun update_malformed_body_maps_to_OtherError() = runTest {
        mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody("not-a-json"))

        val result = remote.update(RemoteSettings("en", null, true))

        assertEquals(RemoteResult.OtherError, result)
    }
}
