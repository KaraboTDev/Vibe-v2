using Microsoft.EntityFrameworkCore;
using VibeApi.Data;
using VibeApi.DTOs;

namespace VibeApi.Services;

public enum SearchStatus
{
    Success,
    InvalidInput,
    UnknownMood
}

public sealed record SearchResult(
    SearchStatus Status,
    List<VenueSearchResultDto> Venues,
    string? Message);

public interface IVenueSearchService
{
    Task<SearchResult> SearchAsync(
        string? mood,
        double lat,
        double lng,
        double radiusKm,
        int limit);

    Task<VenueDetailDto?> GetByIdAsync(int id);
}

public sealed class VenueSearchService(VibeDbContext dbContext) : IVenueSearchService
{
    public async Task<SearchResult> SearchAsync(
        string? mood,
        double lat,
        double lng,
        double radiusKm,
        int limit)
    {
        if (string.IsNullOrWhiteSpace(mood) ||
            lat is < -90 or > 90 ||
            lng is < -180 or > 180 ||
            radiusKm <= 0 ||
            radiusKm > 50 ||
            limit is < 1 or > 50)
        {
            return new SearchResult(
                SearchStatus.InvalidInput,
                [],
                "Mood, coordinates, radius, and limit are invalid.");
        }

        var normalizedMood = mood.Trim().ToLower();
        var matchingTag = await dbContext.Tags
            .FirstOrDefaultAsync(tag => tag.Name.ToLower() == normalizedMood);
        if (matchingTag is null)
        {
            var validTags = await dbContext.Tags
                .OrderBy(tag => tag.Id)
                .Select(tag => tag.Name)
                .ToListAsync();
            return new SearchResult(
                SearchStatus.UnknownMood,
                [],
                $"Unknown mood. Valid tags: {string.Join(", ", validTags)}.");
        }

        var matchingVenueIds = await dbContext.VenueTags
            .Where(venueTag => venueTag.TagId == matchingTag.Id)
            .Select(venueTag => venueTag.VenueId)
            .ToListAsync();
        var venues = await dbContext.Venues
            .Where(venue => matchingVenueIds.Contains(venue.Id))
            .ToListAsync();
        var venueTags = await dbContext.VenueTags
            .Where(venueTag => matchingVenueIds.Contains(venueTag.VenueId))
            .Include(venueTag => venueTag.Tag)
            .ToListAsync();
        var tagsByVenueId = venueTags
            .GroupBy(venueTag => venueTag.VenueId)
            .ToDictionary(
                group => group.Key,
                group => group
                    .Select(venueTag => venueTag.Tag.Name)
                    .OrderBy(name => name)
                    .ToList());
        var radiusMeters = radiusKm * 1000;

        var results = venues
            .Select(venue =>
            {
                var distanceMeters = GeoDistance.Meters(
                    lat,
                    lng,
                    (double)venue.Latitude,
                    (double)venue.Longitude);
                return new
                {
                    Venue = venue,
                    DistanceMeters = distanceMeters
                };
            })
            .Where(result => result.DistanceMeters <= radiusMeters)
            .OrderBy(result => result.DistanceMeters)
            .ThenBy(result => result.Venue.Name)
            .Take(limit)
            .Select(result => new VenueSearchResultDto(
                result.Venue.Id,
                result.Venue.Name,
                result.Venue.Latitude,
                result.Venue.Longitude,
                result.Venue.Address,
                result.Venue.OpeningHours,
                tagsByVenueId[result.Venue.Id],
                (int)Math.Round(result.DistanceMeters)))
            .ToList();

        return new SearchResult(SearchStatus.Success, results, null);
    }

    public async Task<VenueDetailDto?> GetByIdAsync(int id)
    {
        var venue = await dbContext.Venues
            .SingleOrDefaultAsync(item => item.Id == id);
        if (venue is null)
        {
            return null;
        }

        var tags = await dbContext.VenueTags
            .Where(venueTag => venueTag.VenueId == id)
            .Include(venueTag => venueTag.Tag)
            .OrderByDescending(venueTag => venueTag.VoteCount)
            .ThenBy(venueTag => venueTag.Tag.Name)
            .Select(venueTag => new TagVoteDto(venueTag.Tag.Name, venueTag.VoteCount))
            .ToListAsync();

        return new VenueDetailDto(
            venue.Id,
            venue.Name,
            venue.Latitude,
            venue.Longitude,
            venue.Address,
            venue.OpeningHours,
            tags);
    }
}
