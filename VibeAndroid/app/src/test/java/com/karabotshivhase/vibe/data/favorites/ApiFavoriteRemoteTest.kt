package com.karabotshivhase.vibe.data.favorites

import com.karabotshivhase.vibe.network.ApiClient
import com.karabotshivhase.vibe.network.VibeApi
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ApiFavoriteRemoteTest {

    private lateinit var mockWebServer: MockWebServer
    private lateinit var api: VibeApi
    private lateinit var remote: ApiFavoriteRemote

    @Before
    fun setUp() {
        mockWebServer = MockWebServer()
        mockWebServer.start()
        val baseUrl = mockWebServer.url("/").toString()
        api = ApiClient.createApi(baseUrl = baseUrl, tokenProvider = { null })
        remote = ApiFavoriteRemote(api)
    }

    @After
    fun tearDown() {
        mockWebServer.shutdown()
    }

    @Test
    fun fetchAll_maps_fields_and_parses_seven_digit_timestamp() = runTest {
        val json = """
            [
                {
                    "venueId": 10,
                    "name": "Coffee Shop",
                    "latitude": -25.7,
                    "longitude": 28.2,
                    "address": "123 Main St",
                    "openingHours": "08:00-17:00",
                    "tags": ["Quiet", "Focus"],
                    "savedAt": "2026-09-22T02:02:19.1234567Z"
                }
            ]
        """.trimIndent()
        mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody(json))

        val result = remote.fetchAll()

        assertTrue(result is RemoteResult.Success)
        val list = (result as RemoteResult.Success).value
        assertEquals(1, list.size)
        val item = list[0]
        assertEquals(10, item.venueId)
        assertEquals("Coffee Shop", item.name)
        assertEquals(listOf("Quiet", "Focus"), item.tags)
        assertTrue(item.savedAtMillis > 0)
    }

    @Test
    fun add_404_maps_to_NotFound() = runTest {
        mockWebServer.enqueue(MockResponse().setResponseCode(404).setBody("""{"message":"Venue not found"}"""))

        val result = remote.add(999)

        assertEquals(RemoteResult.NotFound, result)
    }

    @Test
    fun add_401_maps_to_Unauthorized() = runTest {
        mockWebServer.enqueue(MockResponse().setResponseCode(401).setBody("""{"message":"Token expired"}"""))

        val result = remote.add(10)

        assertEquals(RemoteResult.Unauthorized, result)
    }

    @Test
    fun fetchAll_server_down_maps_to_NetworkError() = runTest {
        mockWebServer.shutdown()

        val result = remote.fetchAll()

        assertEquals(RemoteResult.NetworkError, result)
    }

    @Test
    fun add_malformed_json_maps_to_OtherError() = runTest {
        mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody("invalid-json"))

        val result = remote.add(10)

        assertEquals(RemoteResult.OtherError, result)
    }
}
