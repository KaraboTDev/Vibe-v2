namespace VibeApi.Services;

public record ExternalVenueDto(
    string ExternalRefId,
    string Name,
    decimal Latitude,
    decimal Longitude,
    string? Address,
    string? OpeningHours);

public interface IVenueDataProvider
{
    Task<List<ExternalVenueDto>> FetchCafesAsync(
        decimal latitude,
        decimal longitude,
        int radiusMeters,
        int limit,
        CancellationToken ct = default);
}
