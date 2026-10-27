namespace VibeApi.DTOs;

public sealed record TagDto(int Id, string Name);

public sealed record TagVoteDto(string Name, int VoteCount);

public sealed record VenueSearchResultDto(
    int Id,
    string Name,
    decimal Latitude,
    decimal Longitude,
    string? Address,
    string? OpeningHours,
    List<string> Tags,
    int DistanceMeters);

public sealed record VenueDetailDto(
    int Id,
    string Name,
    decimal Latitude,
    decimal Longitude,
    string? Address,
    string? OpeningHours,
    List<TagVoteDto> Tags);
