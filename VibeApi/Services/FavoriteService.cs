using Microsoft.EntityFrameworkCore;
using VibeApi.Data;
using VibeApi.DTOs;
using VibeApi.Models;

namespace VibeApi.Services;

public enum FavoriteStatus
{
    Success,
    UnknownVenue
}

public sealed record FavoriteResult(FavoriteStatus Status, FavoriteDto? Favorite);

public interface IFavoriteService
{
    Task<List<FavoriteDto>> GetAllAsync(int userId);
    Task<FavoriteResult> AddAsync(int userId, int venueId);
    Task RemoveAsync(int userId, int venueId);
}

public sealed class FavoriteService(VibeDbContext dbContext) : IFavoriteService
{
    public async Task<List<FavoriteDto>> GetAllAsync(int userId)
    {
        var favorites = await dbContext.Favorites
            .Where(favorite => favorite.UserId == userId)
            .Include(favorite => favorite.Venue)
            .OrderByDescending(favorite => favorite.SavedAt)
            .ToListAsync();

        return await MapFavoritesAsync(favorites);
    }

    public async Task<FavoriteResult> AddAsync(int userId, int venueId)
    {
        var venue = await dbContext.Venues
            .SingleOrDefaultAsync(item => item.Id == venueId);
        if (venue is null)
        {
            return new FavoriteResult(FavoriteStatus.UnknownVenue, null);
        }

        var favorite = await dbContext.Favorites
            .Include(item => item.Venue)
            .SingleOrDefaultAsync(item => item.UserId == userId && item.VenueId == venueId);
        if (favorite is null)
        {
            favorite = new Favorite
            {
                UserId = userId,
                VenueId = venueId,
                Venue = venue,
                SavedAt = DateTime.UtcNow,
                SyncStatus = "synced"
            };
            dbContext.Favorites.Add(favorite);
            await dbContext.SaveChangesAsync();
        }

        return new FavoriteResult(FavoriteStatus.Success, await MapFavoriteAsync(favorite));
    }

    public async Task RemoveAsync(int userId, int venueId)
    {
        var favorite = await dbContext.Favorites
            .SingleOrDefaultAsync(item => item.UserId == userId && item.VenueId == venueId);
        if (favorite is null)
        {
            return;
        }

        dbContext.Favorites.Remove(favorite);
        await dbContext.SaveChangesAsync();
    }

    private async Task<List<FavoriteDto>> MapFavoritesAsync(List<Favorite> favorites)
    {
        var favoriteIds = favorites.Select(favorite => favorite.VenueId).ToList();
        var venueTags = await dbContext.VenueTags
            .Where(venueTag => favoriteIds.Contains(venueTag.VenueId))
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

        return favorites
            .Select(favorite => ToDto(
                favorite,
                tagsByVenueId.GetValueOrDefault(favorite.VenueId, [])))
            .ToList();
    }

    private async Task<FavoriteDto> MapFavoriteAsync(Favorite favorite)
    {
        var tags = await dbContext.VenueTags
            .Where(venueTag => venueTag.VenueId == favorite.VenueId)
            .Include(venueTag => venueTag.Tag)
            .Select(venueTag => venueTag.Tag.Name)
            .OrderBy(name => name)
            .ToListAsync();

        return ToDto(favorite, tags);
    }

    private static FavoriteDto ToDto(Favorite favorite, List<string> tags) =>
        new(
            favorite.VenueId,
            favorite.Venue.Name,
            favorite.Venue.Latitude,
            favorite.Venue.Longitude,
            favorite.Venue.Address,
            favorite.Venue.OpeningHours,
            tags,
            favorite.SavedAt);
}
