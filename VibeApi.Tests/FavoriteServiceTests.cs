using Microsoft.EntityFrameworkCore;
using VibeApi.Data;
using VibeApi.Models;
using VibeApi.Services;

namespace VibeApi.Tests;

public class FavoriteServiceTests
{
    [Fact]
    public async Task Add_Creates_Favorite()
    {
        await using var context = CreateContext();
        var user = await AddUserAsync(context, "first@example.com");
        var venue = await AddVenueAsync(context, "Cafe One");

        var result = await CreateService(context).AddAsync(user.Id, venue.Id);

        Assert.Equal(FavoriteStatus.Success, result.Status);
        Assert.Equal(venue.Id, result.Favorite!.VenueId);
        Assert.Equal("synced", (await context.Favorites.SingleAsync()).SyncStatus);
    }

    [Fact]
    public async Task Add_Twice_Is_Idempotent_And_Keeps_One_Row()
    {
        await using var context = CreateContext();
        var user = await AddUserAsync(context, "first@example.com");
        var venue = await AddVenueAsync(context, "Cafe One");
        var service = CreateService(context);

        var first = await service.AddAsync(user.Id, venue.Id);
        var second = await service.AddAsync(user.Id, venue.Id);

        Assert.Equal(first.Favorite!.VenueId, second.Favorite!.VenueId);
        Assert.Equal(first.Favorite.SavedAt, second.Favorite.SavedAt);
        Assert.Single(await context.Favorites.ToListAsync());
    }

    [Fact]
    public async Task Add_Unknown_Venue_Returns_UnknownVenue()
    {
        await using var context = CreateContext();
        var user = await AddUserAsync(context, "first@example.com");

        var result = await CreateService(context).AddAsync(user.Id, 9999);

        Assert.Equal(FavoriteStatus.UnknownVenue, result.Status);
        Assert.Null(result.Favorite);
    }

    [Fact]
    public async Task Remove_Deletes_Favorite()
    {
        await using var context = CreateContext();
        var user = await AddUserAsync(context, "first@example.com");
        var venue = await AddVenueAsync(context, "Cafe One");
        var service = CreateService(context);
        await service.AddAsync(user.Id, venue.Id);

        await service.RemoveAsync(user.Id, venue.Id);

        Assert.Empty(await context.Favorites.ToListAsync());
    }

    [Fact]
    public async Task Remove_When_Not_Saved_Is_Still_Success()
    {
        await using var context = CreateContext();
        var user = await AddUserAsync(context, "first@example.com");

        await CreateService(context).RemoveAsync(user.Id, 9999);

        Assert.Empty(await context.Favorites.ToListAsync());
    }

    [Fact]
    public async Task GetAll_Returns_Only_Current_Users_Favorites()
    {
        await using var context = CreateContext();
        var firstUser = await AddUserAsync(context, "first@example.com");
        var secondUser = await AddUserAsync(context, "second@example.com");
        var firstVenue = await AddVenueAsync(context, "First Cafe");
        var secondVenue = await AddVenueAsync(context, "Second Cafe");
        var service = CreateService(context);
        await service.AddAsync(firstUser.Id, firstVenue.Id);
        await service.AddAsync(secondUser.Id, secondVenue.Id);

        var result = await service.GetAllAsync(firstUser.Id);

        Assert.Equal(["First Cafe"], result.Select(favorite => favorite.Name));
    }

    [Fact]
    public async Task GetAll_Is_Ordered_Newest_First()
    {
        await using var context = CreateContext();
        var user = await AddUserAsync(context, "first@example.com");
        var firstVenue = await AddVenueAsync(context, "First Cafe");
        var secondVenue = await AddVenueAsync(context, "Second Cafe");
        context.Favorites.AddRange(
            new Favorite
            {
                UserId = user.Id,
                VenueId = firstVenue.Id,
                SavedAt = DateTime.UtcNow.AddMinutes(-1)
            },
            new Favorite
            {
                UserId = user.Id,
                VenueId = secondVenue.Id,
                SavedAt = DateTime.UtcNow
            });
        await context.SaveChangesAsync();

        var result = await CreateService(context).GetAllAsync(user.Id);

        Assert.Equal(["Second Cafe", "First Cafe"], result.Select(favorite => favorite.Name));
    }

    private static VibeDbContext CreateContext()
    {
        var options = new DbContextOptionsBuilder<VibeDbContext>()
            .UseInMemoryDatabase(Guid.NewGuid().ToString())
            .Options;
        var context = new VibeDbContext(options);
        context.Database.EnsureCreated();
        return context;
    }

    private static async Task<User> AddUserAsync(VibeDbContext context, string email)
    {
        var user = new User { Email = email };
        context.Users.Add(user);
        await context.SaveChangesAsync();
        return user;
    }

    private static async Task<Venue> AddVenueAsync(VibeDbContext context, string name)
    {
        var venue = new Venue
        {
            ExternalRefId = Guid.NewGuid().ToString(),
            Name = name,
            Latitude = -25.7479m,
            Longitude = 28.2293m
        };
        context.Venues.Add(venue);
        await context.SaveChangesAsync();
        return venue;
    }

    private static FavoriteService CreateService(VibeDbContext context) => new(context);
}
