using Microsoft.EntityFrameworkCore;
using VibeApi.Data;
using VibeApi.DTOs;
using VibeApi.Models;

namespace VibeApi.Services;

public sealed class VenueAdminService(VibeDbContext dbContext) : IVenueAdminService
{
    public async Task<AdminResult> SetTagsAsync(List<VenueTagAssignment> items)
    {
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

        foreach (var item in items)
        {
            var tagIds = new HashSet<int>();
            foreach (var tagName in item.Tags)
            {
                var normalizedName = tagName?.Trim();
                if (string.IsNullOrEmpty(normalizedName) || !tagsByName.TryGetValue(normalizedName, out var tag))
                {
                    return new AdminResult(
                        AdminStatus.UnknownTag,
                        0,
                        $"Unknown tag '{normalizedName ?? ""}' for venue id {item.VenueId}.");
                }

                tagIds.Add(tag.Id);
            }

            normalizedTags.Add(item.VenueId, tagIds);
        }

        var existingVenueTags = await dbContext.VenueTags
            .Where(venueTag => venueIds.Contains(venueTag.VenueId))
            .ToListAsync();

        foreach (var item in items)
        {
            var wantedTagIds = normalizedTags[item.VenueId];
            var currentVenueTags = existingVenueTags
                .Where(venueTag => venueTag.VenueId == item.VenueId)
                .ToList();

            foreach (var venueTag in currentVenueTags)
            {
                if (!wantedTagIds.Contains(venueTag.TagId))
                {
                    dbContext.VenueTags.Remove(venueTag);
                }
            }

            var currentTagIds = currentVenueTags.Select(venueTag => venueTag.TagId).ToHashSet();
            foreach (var tagId in wantedTagIds.Except(currentTagIds))
            {
                dbContext.VenueTags.Add(new VenueTag
                {
                    VenueId = item.VenueId,
                    TagId = tagId
                });
            }
        }

        await dbContext.SaveChangesAsync();
        return new AdminResult(AdminStatus.Success, items.Count, null);
    }

    public async Task<AdminResult> SetOpeningHoursAsync(List<VenueHoursAssignment> items)
    {
        var inputError = ValidateBatch(items, item => item.VenueId);
        if (inputError is not null)
        {
            return inputError;
        }

        var invalidHours = items.FirstOrDefault(item => item.OpeningHours?.Length > 500);
        if (invalidHours is not null)
        {
            return InvalidInput($"Opening hours exceed 500 characters for venue id {invalidHours.VenueId}.");
        }

        var venueIds = items.Select(item => item.VenueId).ToList();
        var venues = await dbContext.Venues
            .Where(venue => venueIds.Contains(venue.Id))
            .ToListAsync();
        var knownVenueIds = venues.Select(venue => venue.Id).ToHashSet();
        var unknownVenue = items.FirstOrDefault(item => !knownVenueIds.Contains(item.VenueId));
        if (unknownVenue is not null)
        {
            return UnknownVenue(unknownVenue.VenueId);
        }

        foreach (var item in items)
        {
            var venue = venues.Single(existingVenue => existingVenue.Id == item.VenueId);
            var openingHours = item.OpeningHours?.Trim();
            venue.OpeningHours = string.IsNullOrEmpty(openingHours) ? null : openingHours;
        }

        await dbContext.SaveChangesAsync();
        return new AdminResult(AdminStatus.Success, items.Count, null);
    }

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
}