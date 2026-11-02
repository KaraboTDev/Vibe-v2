package com.karabotshivhase.vibe.venue

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

class VenueRepositoryTest {

    private lateinit var mockWebServer: MockWebServer
    private lateinit var api: VibeApi
    private lateinit var repository: VenueRepository

    @Before
    fun setUp() {
        mockWebServer = MockWebServer()
        mockWebServer.start()
        val baseUrl = mockWebServer.url("/").toString()
        api = ApiClient.createApi(baseUrl = baseUrl, tokenProvider = { null })
        repository = VenueRepository(api)
    }

    @After
    fun tearDown() {
        mockWebServer.shutdown()
    }

    @Test
    fun getTags_returns_tags() = runTest {
        val json = """[{"id":1,"name":"Quiet"},{"id":2,"name":"Social"}]"""
        mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody(json))

        val result = repository.getTags()

        assertTrue(result is LoadResult.Success)
        val tags = (result as LoadResult.Success).data
        assertEquals(2, tags.size)
        assertEquals("Quiet", tags[0].name)
    }

    @Test
    fun search_sends_expected_query_parameters() = runTest {
        val json = """[{"id":10,"name":"Coffee Shop","latitude":-25.0,"longitude":28.0,"address":"St 1","openingHours":"08-17","tags":["Quiet"],"distanceMeters":500}]"""
        mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody(json))

        repository.search("Quiet", -25.7, 28.2, 10.0, 20)

        val request = mockWebServer.takeRequest()
        val path = request.path ?: ""
        assertTrue(path.contains("mood=Quiet"))
        assertTrue(path.contains("lat=-25.7"))
        assertTrue(path.contains("lng=28.2"))
        assertTrue(path.contains("radiusKm=10.0"))
        assertTrue(path.contains("limit=20"))
    }

    @Test
    fun search_400_returns_InvalidInput() = runTest {
        mockWebServer.enqueue(MockResponse().setResponseCode(400).setBody("""{"message":"bad input"}"""))

        val result = repository.search("Quiet", -25.7, 28.2, 10.0, 20)

        assertTrue(result is LoadResult.Failure)
        assertEquals(LoadFailure.InvalidInput, (result as LoadResult.Failure).reason)
    }

    @Test
    fun getVenue_404_returns_NotFound() = runTest {
        mockWebServer.enqueue(MockResponse().setResponseCode(404))

        val result = repository.getVenue(999)

        assertTrue(result is LoadResult.Failure)
        assertEquals(LoadFailure.NotFound, (result as LoadResult.Failure).reason)
    }

    @Test
    fun search_server_down_returns_Network() = runTest {
        mockWebServer.shutdown()

        val result = repository.search("Quiet", -25.7, 28.2, 10.0, 20)

        assertTrue(result is LoadResult.Failure)
        assertEquals(LoadFailure.Network, (result as LoadResult.Failure).reason)
    }

    @Test
    fun getVenue_malformed_json_returns_Unknown() = runTest {
        mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody("not-json"))

        val result = repository.getVenue(1)

        assertTrue(result is LoadResult.Failure)
        assertEquals(LoadFailure.Unknown, (result as LoadResult.Failure).reason)
    }
}
