using VibeApi.Services;

namespace VibeApi.Tests;

public class GeoDistanceTests
{
    [Fact]
    public void Meters_Same_Point_Is_Zero()
    {
        Assert.Equal(0, GeoDistance.Meters(-25.7479, 28.2293, -25.7479, 28.2293), precision: 10);
    }

    [Fact]
    public void Meters_One_Degree_Latitude_Is_About_111_Km()
    {
        var distance = GeoDistance.Meters(0, 0, 1, 0);

        Assert.InRange(distance, 110_995, 111_395);
    }

    [Fact]
    public void Meters_Pretoria_To_Johannesburg_Is_About_54_Km()
    {
        var distance = GeoDistance.Meters(-25.7479, 28.2293, -26.2041, 28.0473);

        Assert.InRange(distance, 53_000, 55_000);
    }

    [Fact]
    public void Meters_Is_Symmetric()
    {
        var forward = GeoDistance.Meters(-25.7479, 28.2293, -26.2041, 28.0473);
        var reverse = GeoDistance.Meters(-26.2041, 28.0473, -25.7479, 28.2293);

        Assert.Equal(forward, reverse, precision: 10);
    }
}
