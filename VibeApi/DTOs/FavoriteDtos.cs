namespace VibeApi.DTOs;

public sealed record AddFavoriteRequest(int VenueId);

public sealed record FavoriteDto(
    int VenueId,
    string Name,
    decimal Latitude,
    decimal Longitude,
    string? Address,
    string? OpeningHours,
    List<string> Tags,
    DateTime SavedAt);
