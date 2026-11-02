package com.karabotshivhase.vibe.venue

class FakeVenueSource : VenueSource {
    var tagsResult: LoadResult<List<Tag>> = LoadResult.Success(emptyList())
    var searchResult: LoadResult<List<Venue>> = LoadResult.Success(emptyList())
    var venueResult: LoadResult<VenueDetail> = LoadResult.Success(
        VenueDetail(1, "Test Venue", -25.0, 28.0, "123 St", "09:00-17:00", emptyList())
    )

    var tagsCallCount = 0
    var searchCallCount = 0
    var getVenueCallCount = 0

    var lastSearchMood: String? = null
    var lastSearchLat: Double? = null
    var lastSearchLng: Double? = null
    var lastSearchRadius: Double? = null
    var lastSearchLimit: Int? = null
    var lastVenueId: Int? = null

    override suspend fun getTags(): LoadResult<List<Tag>> {
        tagsCallCount++
        return tagsResult
    }

    override suspend fun search(
        mood: String,
        areaLat: Double,
        areaLng: Double,
        radiusKm: Double,
        limit: Int,
    ): LoadResult<List<Venue>> {
        searchCallCount++
        lastSearchMood = mood
        lastSearchLat = areaLat
        lastSearchLng = areaLng
        lastSearchRadius = radiusKm
        lastSearchLimit = limit
        return searchResult
    }

    override suspend fun getVenue(id: Int): LoadResult<VenueDetail> {
        getVenueCallCount++
        lastVenueId = id
        return venueResult
    }
}
