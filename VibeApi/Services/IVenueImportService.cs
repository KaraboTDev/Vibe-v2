namespace VibeApi.Services;

public record ImportResult(int Fetched, int Added, int Updated);

public interface IVenueImportService
{
    Task<ImportResult> ImportAsync(
        decimal latitude,
        decimal longitude,
        int radiusMeters,
        int limit);
}
