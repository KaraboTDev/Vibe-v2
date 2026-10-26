using Microsoft.EntityFrameworkCore;
using VibeApi.Data;
using VibeApi.Models;

namespace VibeApi.Services;

public sealed class VenueImportService(
    VibeDbContext dbContext,
    IVenueDataProvider venueDataProvider) : IVenueImportService
{
    public async Task<ImportResult> ImportAsync(
        decimal latitude,
        decimal longitude,
        int radiusMeters,
        int limit)
    {
        var fetchedVenues = await venueDataProvider.FetchCafesAsync(latitude, longitude, radiusMeters, limit);

        var incomingVenues = fetchedVenues
            .Select(venue => venue with
            {
                Latitude = Math.Round(
                    venue.Latitude,
                    6,
                    MidpointRounding.AwayFromZero),
                Longitude = Math.Round(
                    venue.Longitude,
                    6,
                    MidpointRounding.AwayFromZero)
            })
            .DistinctBy(venue => venue.ExternalRefId)
            .ToList();

        var externalRefIds = incomingVenues.Select(venue => venue.ExternalRefId).ToList();

        var existingVenues = await dbContext.Venues
            .Where(venue => externalRefIds.Contains(venue.ExternalRefId))
            .ToDictionaryAsync(venue => venue.ExternalRefId);

        var added = 0;
        var updated = 0;

        foreach (var incoming in incomingVenues)
        {
            if (!existingVenues.TryGetValue(incoming.ExternalRefId, out var existing))
            {
                dbContext.Venues.Add(new Venue
                {
                    ExternalRefId = incoming.ExternalRefId,
                    Name = incoming.Name,
                    Address = incoming.Address,
                    OpeningHours = incoming.OpeningHours,
                    Latitude = incoming.Latitude,
                    Longitude = incoming.Longitude
                });

                added++;
                continue;
            }

            if (UpdateVenue(existing, incoming))
            {
                updated++;
            }
        }

        await dbContext.SaveChangesAsync();

        return new ImportResult(fetchedVenues.Count, added, updated);
    }

    private static bool UpdateVenue(Venue existing, ExternalVenueDto incoming)
    {
        var changed = false;

        if (existing.Name != incoming.Name)
        {
            existing.Name = incoming.Name;
            changed = true;
        }

        if (existing.Address != incoming.Address)
        {
            existing.Address = incoming.Address;
            changed = true;
        }

        if (existing.OpeningHours != incoming.OpeningHours)
        {
            existing.OpeningHours = incoming.OpeningHours;
            changed = true;
        }

        if (existing.Latitude != incoming.Latitude)
        {
            existing.Latitude = incoming.Latitude;
            changed = true;
        }

        if (existing.Longitude != incoming.Longitude)
        {
            existing.Longitude = incoming.Longitude;
            changed = true;
        }

        return changed;
    }
}