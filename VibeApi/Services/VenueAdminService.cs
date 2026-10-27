using Microsoft.EntityFrameworkCore;
using VibeApi.Data;
using VibeApi.DTOs;
using VibeApi.Models;

namespace VibeApi.Services;

public sealed class VenueAdminService(VibeDbContext dbContext) : IVenueAdminService
{
    public async Task<AdminResult> SetTagsAsync(List<VenueTagAssignment> items)
    {
        if (items is null || items.Count is < 1 or > 200)
        {
            return InvalidInput("Items must contain between 1 and 200 assignments.");
        }

        if (items.Any(item => item is null))
        {
            return InvalidInput("Items cannot contain a null assignment.");
        }

        var assignmentWithNullTags = items.FirstOrDefault(item => item.Tags is null);
        if (assignmentWithNullTags is not null)
        {
            return InvalidInput($"Tags for VenueId {assignmentWithNullTags.VenueId} cannot be null.");
        }

        var duplicateVenueId = items
            .GroupBy(item => item.VenueId)
            .FirstOrDefault(group => group.Count() > 1)?.Key;
        if (duplicateVenueId.HasValue)
        {
            return InvalidInput($"VenueId {duplicateVenueId.Value} appears more than once.");
        }

        var venueIds = items.Select(item => item.VenueId).ToList();
        var existingVenueIds = await dbContext.Venues
            .Where(venue => venueIds.Contains(venue.Id))
            .Select(venue => venue.Id)
            .ToListAsync();
        var existingVenueIdSet = existingVenueIds.ToHashSet();

        foreach (var item in items)
        {
            if (!existingVenueIdSet.Contains(item.VenueId))
            {
                return new AdminResult(
                    AdminStatus.UnknownVenue,
                    0,
                    $"VenueId {item.VenueId} does not exist.");
            }
        }

        var tags = await dbContext.Tags.ToListAsync();
        var tagsByName = tags
            .GroupBy(tag => tag.Name, StringComparer.OrdinalIgnoreCase)
            .ToDictionary(group => group.Key, group => group.First(), StringComparer.OrdinalIgnoreCase);
        var requestedTagIds = new Dictionary<int, HashSet<int>>();

        foreach (var item in items)
        {
            var tagIds = new HashSet<int>();
            foreach (var tagName in item.Tags)
            {
                var trimmedTagName = tagName?.Trim() ?? string.Empty;
                if (!tagsByName.TryGetValue(trimmedTagName, out var tag))
                {
                    return new AdminResult(
                        AdminStatus.UnknownTag,
                        0,
                        $"Unknown tag '{tagName ?? "<null>"}' for VenueId {item.VenueId}.");
                }

                tagIds.Add(tag.Id);
            }

            requestedTagIds[item.VenueId] = tagIds;
        }

        var currentVenueTags = await dbContext.VenueTags
            .Where(venueTag => venueIds.Contains(venueTag.VenueId))
            .ToListAsync();

        foreach (var item in items)
        {
            var targetTagIds = requestedTagIds[item.VenueId];
            var currentRows = currentVenueTags
                .Where(venueTag => venueTag.VenueId == item.VenueId)
                .ToList();
            var currentTagIds = currentRows.Select(venueTag => venueTag.TagId).ToHashSet();

            foreach (var venueTag in currentRows)
            {
                if (!targetTagIds.Contains(venueTag.TagId))
                {
                    dbContext.VenueTags.Remove(venueTag);
                }
            }

            foreach (var tagId in targetTagIds)
            {
                if (!currentTagIds.Contains(tagId))
                {
                    dbContext.VenueTags.Add(new VenueTag
                    {
                        VenueId = item.VenueId,
                        TagId = tagId
                    });
                }
            }
        }

        await dbContext.SaveChangesAsync();
        return new AdminResult(AdminStatus.Success, items.Count, null);
    }

    public async Task<AdminResult> SetOpeningHoursAsync(List<VenueHoursAssignment> items)
    {
        if (items is null || items.Count is < 1 or > 200)
        {
            return InvalidInput("Items must contain between 1 and 200 assignments.");
        }

        if (items.Any(item => item is null))
        {
            return InvalidInput("Items cannot contain a null assignment.");
        }

        var duplicateVenueId = items
            .GroupBy(item => item.VenueId)
            .FirstOrDefault(group => group.Count() > 1)?.Key;
        if (duplicateVenueId.HasValue)
        {
            return InvalidInput($"VenueId {duplicateVenueId.Value} appears more than once.");
        }

        foreach (var item in items)
        {
            if (item.OpeningHours?.Length > 500)
            {
                return InvalidInput($"OpeningHours for VenueId {item.VenueId} cannot exceed 500 characters.");
            }
        }

        var venueIds = items.Select(item => item.VenueId).ToList();
        var venues = await dbContext.Venues
            .Where(venue => venueIds.Contains(venue.Id))
            .ToListAsync();
        var venuesById = venues.ToDictionary(venue => venue.Id);

        foreach (var item in items)
        {
            if (!venuesById.ContainsKey(item.VenueId))
            {
                return new AdminResult(
                    AdminStatus.UnknownVenue,
                    0,
                    $"VenueId {item.VenueId} does not exist.");
            }

        }

        foreach (var item in items)
        {
            var trimmedHours = item.OpeningHours?.Trim();
            venuesById[item.VenueId].OpeningHours = string.IsNullOrEmpty(trimmedHours) ? null : trimmedHours;
        }

        await dbContext.SaveChangesAsync();
        return new AdminResult(AdminStatus.Success, items.Count, null);
    }

    private static AdminResult InvalidInput(string detail) =>
        new(AdminStatus.InvalidInput, 0, detail);
}