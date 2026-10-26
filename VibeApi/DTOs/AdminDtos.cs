namespace VibeApi.DTOs;

public sealed record VenueTagAssignment(int VenueId, List<string> Tags);

public sealed record VenueHoursAssignment(int VenueId, string? OpeningHours);