using VibeApi.Services;

namespace VibeApi.Tests;

public class GeoapifyParserTests
{
    private const string SampleJson = """
        {
          "type": "FeatureCollection",
          "features": [
            {
              "type": "Feature",
              "properties": {
                "place_id": " place-1 ",
                "name": " Cafe One ",
                "formatted": " 1 Main Street ",
                "opening_hours": " Monday-Friday ",
                "lat": 40.1,
                "lon": -73.2
              },
              "geometry": { "type": "Point", "coordinates": [-73.2, 40.1] }
            },
            {
              "type": "Feature",
              "properties": {
                "place_id": "place-2",
                "formatted": "2 Main Street",
                "lat": 40.2,
                "lon": -73.3
              },
              "geometry": { "type": "Point", "coordinates": [-73.3, 40.2] }
            },
            {
              "type": "Feature",
              "properties": {
                "place_id": "place-3",
                "name": "Cafe Three",
                "formatted": "   "
              },
              "geometry": { "type": "Point", "coordinates": [-73.4, 40.3] }
            }
          ]
        }
        """;

    [Fact]
    public void Parse_Maps_Complete_Feature()
    {
        var venue = GeoapifyParser.Parse(SampleJson).Single(venue => venue.ExternalRefId == "place-1");

        Assert.Equal("place-1", venue.ExternalRefId);
        Assert.Equal("Cafe One", venue.Name);
        Assert.Equal(40.1m, venue.Latitude);
        Assert.Equal(-73.2m, venue.Longitude);
        Assert.Equal("1 Main Street", venue.Address);
        Assert.Equal("Monday-Friday", venue.OpeningHours);
    }

    [Fact]
    public void Parse_Skips_Features_Without_Name()
    {
        var venues = GeoapifyParser.Parse(SampleJson);

        Assert.DoesNotContain(venues, venue => venue.ExternalRefId == "place-2");
    }

    [Fact]
    public void Parse_Uses_Geometry_When_Lat_Lon_Missing()
    {
        var venue = GeoapifyParser.Parse(SampleJson).Single(venue => venue.ExternalRefId == "place-3");

        Assert.Equal(40.3m, venue.Latitude);
        Assert.Equal(-73.4m, venue.Longitude);
    }

    [Fact]
    public void Parse_Returns_Null_OpeningHours_When_Missing()
    {
        var venue = GeoapifyParser.Parse(SampleJson).Single(venue => venue.ExternalRefId == "place-3");

        Assert.Null(venue.OpeningHours);
    }

    [Fact]
    public void Parse_Returns_Empty_List_When_No_Features()
    {
        var venues = GeoapifyParser.Parse("{\"type\":\"FeatureCollection\"}");

        Assert.Empty(venues);
    }
}
