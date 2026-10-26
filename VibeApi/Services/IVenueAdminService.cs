using VibeApi.DTOs;

namespace VibeApi.Services;

public enum AdminStatus
{
    Success,
    UnknownVenue,
    UnknownTag,
    InvalidInput
}

public sealed record AdminResult(AdminStatus Status, int Count, string? Detail);

public interface IVenueAdminService
{
    Task<AdminResult> SetTagsAsync(List<VenueTagAssignment> items);

    Task<AdminResult> SetOpeningHoursAsync(List<VenueHoursAssignment> items);
}