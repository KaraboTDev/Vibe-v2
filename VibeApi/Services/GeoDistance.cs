namespace VibeApi.Services;

public static class GeoDistance
{
    public static double Meters(double lat1, double lon1, double lat2, double lon2)
    {
        const double earthRadiusMeters = 6_371_000;
        var latitudeDifference = DegreesToRadians(lat2 - lat1);
        var longitudeDifference = DegreesToRadians(lon2 - lon1);
        var firstLatitude = DegreesToRadians(lat1);
        var secondLatitude = DegreesToRadians(lat2);

        var haversine = Math.Pow(Math.Sin(latitudeDifference / 2), 2) +
            Math.Cos(firstLatitude) *
            Math.Cos(secondLatitude) *
            Math.Pow(Math.Sin(longitudeDifference / 2), 2);

        return 2 * earthRadiusMeters * Math.Asin(Math.Sqrt(haversine));
    }

    private static double DegreesToRadians(double degrees) => degrees * Math.PI / 180;
}
