using Microsoft.EntityFrameworkCore;
using VibeApi.Data;
using VibeApi.Models;

namespace VibeApi.Tests;

public class VibeDbContextTests
{
    [Fact]
    public void Seeded_Tags_Contain_The_Five_Mood_Tags()
    {
        using var context = CreateContext();
        context.Database.EnsureCreated();

        var tagNames = context.Tags
            .Select(tag => tag.Name)
            .ToList();

        Assert.Equal(5, tagNames.Count);
        Assert.Equal(
            new[] { "Quiet", "Social", "Focus", "Creative", "Cheap Wifi" }.OrderBy(name => name),
            tagNames.OrderBy(name => name));
    }

    [Fact]
    public void Favorite_Can_Be_Saved_For_A_User_And_Venue()
    {
        using var context = CreateContext();
        context.Database.EnsureCreated();

        var user = new User { Email = "vibe@example.com" };
        var venue = new Venue
        {
            ExternalRefId = "venue-1",
            Name = "Vibe Cafe"
        };
        context.Users.Add(user);
        context.Venues.Add(venue);
        context.SaveChanges();

        context.Favorites.Add(new Favorite
        {
            UserId = user.Id,
            VenueId = venue.Id
        });
        context.SaveChanges();

        var savedFavorite = context.Favorites.Single();

        Assert.Equal(user.Id, savedFavorite.UserId);
        Assert.Equal(venue.Id, savedFavorite.VenueId);
        Assert.Equal("synced", savedFavorite.SyncStatus);
    }

    private static VibeDbContext CreateContext()
    {
        var options = new DbContextOptionsBuilder<VibeDbContext>()
            .UseInMemoryDatabase(Guid.NewGuid().ToString())
            .Options;

        return new VibeDbContext(options);
    }
}
