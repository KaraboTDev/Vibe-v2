using Microsoft.EntityFrameworkCore;
using VibeApi.Data;
using VibeApi.DTOs;
using VibeApi.Models;
using VibeApi.Services;

namespace VibeApi.Tests;

public class VenueSearchServiceTests
{
    [Fact]
    public async Task Search_Returns_Only_Venues_With_Requested_Mood()
    {
        await using var context = CreateContext();
        await AddVenueAsync(context, "Quiet Cafe", -25.7479, 28.2293, "Quiet");
        await AddVenueAsync(context, "Social Cafe", -25.7479, 28.2293, "Social");

        var result = await CreateService(context).SearchAsync("Quiet", -25.7479, 28.2293, 10, 20);

        Assert.Equal(SearchStatus.Success, result.Status);
        Assert.Equal(["Quiet Cafe"], result.Venues.Select(venue => venue.Name));
    }

    [Fact]
    public async Task Search_Is_Case_Insensitive()
    {
        await using var context = CreateContext();
        await AddVenueAsync(context, "Quiet Cafe", -25.7479, 28.2293, "Quiet");

        var result = await CreateService(context).SearchAsync(" qUiEt ", -25.7479, 28.2293, 10, 20);

        Assert.Equal(SearchStatus.Success, result.Status);
        Assert.Single(result.Venues);
    }

    [Fact]
    public async Task Search_Sorts_By_Distance_Nearest_First()
    {
        await using var context = CreateContext();
        await AddVenueAsync(context, "Far Cafe", -25.95, 28.1, "Quiet");
        await AddVenueAsync(context, "Near Cafe", -25.75, 28.23, "Quiet");

        var result = await CreateService(context).SearchAsync("Quiet", -25.7479, 28.2293, 50, 20);

        Assert.Equal(["Near Cafe", "Far Cafe"], result.Venues.Select(venue => venue.Name));
    }

    [Fact]
    public async Task Search_Excludes_Venues_Outside_Radius()
    {
        await using var context = CreateContext();
        await AddVenueAsync(context, "Nearby Cafe", -25.75, 28.23, "Quiet");
        await AddVenueAsync(context, "Far Cafe", -26.2041, 28.0473, "Quiet");

        var result = await CreateService(context).SearchAsync("Quiet", -25.7479, 28.2293, 1, 20);

        Assert.Equal(["Nearby Cafe"], result.Venues.Select(venue => venue.Name));
    }

    [Fact]
    public async Task Search_Respects_Limit()
    {
        await using var context = CreateContext();
        await AddVenueAsync(context, "Cafe One", -25.75, 28.23, "Quiet");
        await AddVenueAsync(context, "Cafe Two", -25.751, 28.231, "Quiet");
        await AddVenueAsync(context, "Cafe Three", -25.752, 28.232, "Quiet");

        var result = await CreateService(context).SearchAsync("Quiet", -25.7479, 28.2293, 10, 2);

        Assert.Equal(SearchStatus.Success, result.Status);
        Assert.Equal(2, result.Venues.Count);
    }

    [Fact]
    public async Task Search_Unknown_Mood_Returns_UnknownMood()
    {
        await using var context = CreateContext();

        var result = await CreateService(context).SearchAsync("Energetic", -25.7479, 28.2293, 10, 20);

        Assert.Equal(SearchStatus.UnknownMood, result.Status);
        Assert.Contains("Quiet", result.Message!);
        Assert.Contains("Cheap Wifi", result.Message!);
    }

    [Fact]
    public async Task Search_Invalid_Coordinates_Returns_InvalidInput()
    {
        await using var context = CreateContext();

        var result = await CreateService(context).SearchAsync("Quiet", 91, 28.2293, 10, 20);

        Assert.Equal(SearchStatus.InvalidInput, result.Status);
        Assert.NotEmpty(result.Message!);
    }

    [Fact]
    public async Task GetById_Returns_Tags_With_VoteCounts()
    {
        await using var context = CreateContext();
        var venue = await AddVenueAsync(context, "Voted Cafe", -25.7479, 28.2293, "Quiet", ("Social", 4));

        var result = await CreateService(context).GetByIdAsync(venue.Id);

        Assert.NotNull(result);
        Assert.Equal(
            [new TagVoteDto("Social", 4), new TagVoteDto("Quiet", 1)],
            result.Tags);
    }

    [Fact]
    public async Task GetById_Unknown_Id_Returns_Null()
    {
        await using var context = CreateContext();

        var result = await CreateService(context).GetByIdAsync(9999);

        Assert.Null(result);
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

    private static async Task<Venue> AddVenueAsync(
        VibeDbContext context,
        string name,
        double latitude,
        double longitude,
        string firstTag,
        params (string Name, int VoteCount)[] additionalTags)
    {
        var venue = new Venue
        {
            ExternalRefId = Guid.NewGuid().ToString(),
            Name = name,
            Latitude = (decimal)latitude,
            Longitude = (decimal)longitude
        };
        context.Venues.Add(venue);
        await context.SaveChangesAsync();

        var tagNames = new[] { (firstTag, 1) }.Concat(additionalTags);
        foreach (var (tagName, voteCount) in tagNames)
        {
            var tag = await context.Tags.SingleAsync(item => item.Name == tagName);
            context.VenueTags.Add(new VenueTag
            {
                VenueId = venue.Id,
                TagId = tag.Id,
                VoteCount = voteCount
            });
        }

        await context.SaveChangesAsync();
        return venue;
    }

    private static VenueSearchService CreateService(VibeDbContext context) => new(context);
}
