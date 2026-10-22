using System.Text.Json;

namespace VibeApi.Services;

public static class GeoapifyParser
{
    public static List<ExternalVenueDto> Parse(string json)
    {
        using var document = JsonDocument.Parse(json);
        var root = document.RootElement;
        if (!root.TryGetProperty("features", out var features) || features.ValueKind != JsonValueKind.Array)
        {
            return [];
        }

        var venues = new List<ExternalVenueDto>();
        foreach (var feature in features.EnumerateArray())
        {
            if (feature.ValueKind != JsonValueKind.Object ||
                !feature.TryGetProperty("properties", out var properties) ||
                properties.ValueKind != JsonValueKind.Object)
            {
                continue;
            }

            var externalRefId = ReadString(properties, "place_id");
            var name = ReadString(properties, "name");
            if (externalRefId is null || name is null)
            {
                continue;
            }

            var latitude = ReadDecimal(properties, "lat");
            var longitude = ReadDecimal(properties, "lon");
            if ((latitude is null || longitude is null) &&
                feature.TryGetProperty("geometry", out var geometry) &&
                geometry.ValueKind == JsonValueKind.Object &&
                geometry.TryGetProperty("coordinates", out var coordinates) &&
                coordinates.ValueKind == JsonValueKind.Array &&
                coordinates.GetArrayLength() >= 2)
            {
                longitude ??= ReadDecimal(coordinates[0]);
                latitude ??= ReadDecimal(coordinates[1]);
            }

            if (latitude is null || longitude is null)
            {
                continue;
            }

            venues.Add(new ExternalVenueDto(
                externalRefId,
                name,
                latitude.Value,
                longitude.Value,
                ReadString(properties, "formatted"),
                ReadString(properties, "opening_hours")));
        }

        return venues;
    }

    private static string? ReadString(JsonElement element, string propertyName)
    {
        if (!element.TryGetProperty(propertyName, out var value) || value.ValueKind != JsonValueKind.String)
        {
            return null;
        }

        var text = value.GetString()?.Trim();
        return string.IsNullOrEmpty(text) ? null : text;
    }

    private static decimal? ReadDecimal(JsonElement element, string propertyName) =>
        element.TryGetProperty(propertyName, out var value) ? ReadDecimal(value) : null;

    private static decimal? ReadDecimal(JsonElement value) =>
        value.ValueKind == JsonValueKind.Number && value.TryGetDecimal(out var number) ? number : null;
}
