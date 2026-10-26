using Microsoft.EntityFrameworkCore;
using VibeApi.Data;
using VibeApi.DTOs;
using VibeApi.Models;

namespace VibeApi.Services;

public sealed class VenueAdminService(VibeDbContext dbContext) : IVenueAdminService
{
    public async Task<AdminResult> SetTagsAsync(List<VenueTagAssignment> items)
    {
<<<<<<< HEAD
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
=======
        var inputError = ValidateBatch(items, item => item.VenueId);
        if (inputError is not null)
        {
            return inputError;
        }

        if (items.Any(item => item.Tags is null))
        {
            var invalidItem = items.First(item => item.Tags is null);
            return InvalidInput($"Tags must not be null for venue id {invalidItem.VenueId}.");
        }

        var venueIds = items.Select(item => item.VenueId).ToList();
        var venues = await dbContext.Venues
            .Where(venue => venueIds.Contains(venue.Id))
            .Select(venue => venue.Id)
            .ToListAsync();
        var knownVenueIds = venues.ToHashSet();
        var unknownVenue = items.FirstOrDefault(item => !knownVenueIds.Contains(item.VenueId));
        if (unknownVenue is not null)
        {
            return UnknownVenue(unknownVenue.VenueId);
        }

        var tagsByName = await dbContext.Tags
            .ToDictionaryAsync(tag => tag.Name, StringComparer.OrdinalIgnoreCase);
        var normalizedTags = new Dictionary<int, HashSet<int>>();
>>>>>>> 66c6ac5eaaa951b55c755a19d47cf77520f7c7a5

        foreach (var item in items)
        {
            var tagIds = new HashSet<int>();
            foreach (var tagName in item.Tags)
            {
<<<<<<< HEAD
                var trimmedTagName = tagName?.Trim() ?? string.Empty;
                if (!tagsByName.TryGetValue(trimmedTagName, out var tag))
=======
                var normalizedName = tagName?.Trim();
                if (string.IsNullOrEmpty(normalizedName) || !tagsByName.TryGetValue(normalizedName, out var tag))
>>>>>>> 66c6ac5eaaa951b55c755a19d47cf77520f7c7a5
                {
                    return new AdminResult(
                        AdminStatus.UnknownTag,
                        0,
<<<<<<< HEAD
                        $"Unknown tag '{tagName ?? "<null>"}' for VenueId {item.VenueId}.");
=======
                        $"Unknown tag '{normalizedName ?? ""}' for venue id {item.VenueId}.");
>>>>>>> 66c6ac5eaaa951b55c755a19d47cf77520f7c7a5
                }

                tagIds.Add(tag.Id);
            }

<<<<<<< HEAD
            requestedTagIds[item.VenueId] = tagIds;
        }

        var currentVenueTags = await dbContext.VenueTags
=======
            normalizedTags.Add(item.VenueId, tagIds);
        }

        var existingVenueTags = await dbContext.VenueTags
>>>>>>> 66c6ac5eaaa951b55c755a19d47cf77520f7c7a5
            .Where(venueTag => venueIds.Contains(venueTag.VenueId))
            .ToListAsync();

        foreach (var item in items)
        {
<<<<<<< HEAD
            var targetTagIds = requestedTagIds[item.VenueId];
            var currentRows = currentVenueTags
                .Where(venueTag => venueTag.VenueId == item.VenueId)
                .ToList();
            var currentTagIds = currentRows.Select(venueTag => venueTag.TagId).ToHashSet();

            foreach (var venueTag in currentRows)
            {
                if (!targetTagIds.Contains(venueTag.TagId))
=======
            var wantedTagIds = normalizedTags[item.VenueId];
            var currentVenueTags = existingVenueTags
                .Where(venueTag => venueTag.VenueId == item.VenueId)
                .ToList();

            foreach (var venueTag in currentVenueTags)
            {
                if (!wantedTagIds.Contains(venueTag.TagId))
>>>>>>> 66c6ac5eaaa951b55c755a19d47cf77520f7c7a5
                {
                    dbContext.VenueTags.Remove(venueTag);
                }
            }

<<<<<<< HEAD
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
=======
            var currentTagIds = currentVenueTags.Select(venueTag => venueTag.TagId).ToHashSet();
            foreach (var tagId in wantedTagIds.Except(currentTagIds))
            {
                dbContext.VenueTags.Add(new VenueTag
                {
                    VenueId = item.VenueId,
                    TagId = tagId
                });
>>>>>>> 66c6ac5eaaa951b55c755a19d47cf77520f7c7a5
            }
        }

        await dbContext.SaveChangesAsync();
        return new AdminResult(AdminStatus.Success, items.Count, null);
    }

    public async Task<AdminResult> SetOpeningHoursAsync(List<VenueHoursAssignment> items)
    {
<<<<<<< HEAD
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
=======
        var inputError = ValidateBatch(items, item => item.VenueId);
        if (inputError is not null)
        {
            return inputError;
        }

        var invalidHours = items.FirstOrDefault(item => item.OpeningHours?.Length > 500);
        if (invalidHours is not null)
        {
            return InvalidInput($"Opening hours exceed 500 characters for venue id {invalidHours.VenueId}.");
>>>>>>> 66c6ac5eaaa951b55c755a19d47cf77520f7c7a5
        }

        var venueIds = items.Select(item => item.VenueId).ToList();
        var venues = await dbContext.Venues
            .Where(venue => venueIds.Contains(venue.Id))
            .ToListAsync();
<<<<<<< HEAD
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

=======
        var knownVenueIds = venues.Select(venue => venue.Id).ToHashSet();
        var unknownVenue = items.FirstOrDefault(item => !knownVenueIds.Contains(item.VenueId));
        if (unknownVenue is not null)
        {
            return UnknownVenue(unknownVenue.VenueId);
>>>>>>> 66c6ac5eaaa951b55c755a19d47cf77520f7c7a5
        }

        foreach (var item in items)
        {
<<<<<<< HEAD
            var trimmedHours = item.OpeningHours?.Trim();
            venuesById[item.VenueId].OpeningHours = string.IsNullOrEmpty(trimmedHours) ? null : trimmedHours;
=======
            var venue = venues.Single(existingVenue => existingVenue.Id == item.VenueId);
            var openingHours = item.OpeningHours?.Trim();
            venue.OpeningHours = string.IsNullOrEmpty(openingHours) ? null : openingHours;
>>>>>>> 66c6ac5eaaa951b55c755a19d47cf77520f7c7a5
        }

        await dbContext.SaveChangesAsync();
        return new AdminResult(AdminStatus.Success, items.Count, null);
    }

<<<<<<< HEAD
    private static AdminResult InvalidInput(string detail) =>
        new(AdminStatus.InvalidInput, 0, detail);
=======
    private static AdminResult? ValidateBatch<T>(List<T> items, Func<T, int> getVenueId)
    {
        if (items is null || items.Count == 0)
        {
            return InvalidInput("At least one venue assignment is required.");
        }

        if (items.Count > 200)
        {
            return InvalidInput("A maximum of 200 venue assignments is allowed.");
        }

        var duplicateVenueId = items
            .GroupBy(getVenueId)
            .FirstOrDefault(group => group.Count() > 1)?.Key;
        return duplicateVenueId is null
            ? null
            : InvalidInput($"Duplicate venue id {duplicateVenueId.Value}.");
    }

    private static AdminResult InvalidInput(string detail) =>
        new(AdminStatus.InvalidInput, 0, detail);

    private static AdminResult UnknownVenue(int venueId) =>
        new(AdminStatus.UnknownVenue, 0, $"Unknown venue id {venueId}.");
>>>>>>> 66c6ac5eaaa951b55c755a19d47cf77520f7c7a5
}