using Microsoft.EntityFrameworkCore;
using VibeApi.Data;
using VibeApi.DTOs;
using VibeApi.Models;
using VibeApi.Services;

namespace VibeApi.Tests;

public class VenueAdminServiceTests
{
    [Fact]
    public async Task SetTags_Replaces_Existing_Tags()
    {
        await using var context = CreateContext();
        var venue = await AddVenueAsync(context, "Cafe One");
        await AddVenueTagAsync(context, venue.Id, "Quiet");
        await AddVenueTagAsync(context, venue.Id, "Social");

        var result = await CreateService(context).SetTagsAsync(
            [new VenueTagAssignment(venue.Id, ["Focus"])]);

        Assert.Equal(new AdminResult(AdminStatus.Success, 1, null), result);
        Assert.Equal(["Focus"], await GetTagNamesAsync(context, venue.Id));
    }

    [Fact]
    public async Task SetTags_Is_Case_Insensitive_And_Removes_Duplicates()
    {
        await using var context = CreateContext();
        var venue = await AddVenueAsync(context, "Cafe One");

        var result = await CreateService(context).SetTagsAsync(
            [new VenueTagAssignment(venue.Id, ["quiet", " Quiet ", "QUIET"])]);

        Assert.Equal(AdminStatus.Success, result.Status);
        Assert.Equal(["Quiet"], await GetTagNamesAsync(context, venue.Id));
    }

    [Fact]
    public async Task SetTags_Keeps_Existing_Row_And_VoteCount_When_Tag_Unchanged()
    {
        await using var context = CreateContext();
        var venue = await AddVenueAsync(context, "Cafe One");
        var venueTag = await AddVenueTagAsync(context, venue.Id, "Quiet", voteCount: 7);
        var originalId = venueTag.Id;

        var result = await CreateService(context).SetTagsAsync(
            [new VenueTagAssignment(venue.Id, ["Quiet"])]);

        Assert.Equal(AdminStatus.Success, result.Status);
        var retained = await context.VenueTags.AsNoTracking().SingleAsync();
        Assert.Equal(originalId, retained.Id);
        Assert.Equal(7, retained.VoteCount);
    }

    [Fact]
    public async Task SetTags_Unknown_Tag_Changes_Nothing()
    {
        await using var context = CreateContext();
        var firstVenue = await AddVenueAsync(context, "Cafe One");
        var secondVenue = await AddVenueAsync(context, "Cafe Two");
        await AddVenueTagAsync(context, firstVenue.Id, "Quiet");

        var result = await CreateService(context).SetTagsAsync(
        [
            new VenueTagAssignment(firstVenue.Id, ["Social"]),
            new VenueTagAssignment(secondVenue.Id, ["not-a-tag"])
        ]);

        Assert.Equal(AdminStatus.UnknownTag, result.Status);
        Assert.Contains("not-a-tag", result.Detail);
        Assert.Equal(["Quiet"], await GetTagNamesAsync(context, firstVenue.Id));
        Assert.Empty(await GetTagNamesAsync(context, secondVenue.Id));
    }

    [Fact]
    public async Task SetTags_Unknown_Venue_Returns_UnknownVenue()
    {
        await using var context = CreateContext();

        var result = await CreateService(context).SetTagsAsync(
            [new VenueTagAssignment(9876, ["Quiet"])]);

        Assert.Equal(AdminStatus.UnknownVenue, result.Status);
        Assert.Contains("9876", result.Detail);
    }

    [Fact]
    public async Task SetOpeningHours_Trims_And_Stores()
    {
        await using var context = CreateContext();
        var venue = await AddVenueAsync(context, "Cafe One");

        var result = await CreateService(context).SetOpeningHoursAsync(
            [new VenueHoursAssignment(venue.Id, "  Mon-Fri 9:00-17:00  ")]);

        Assert.Equal(new AdminResult(AdminStatus.Success, 1, null), result);
        Assert.Equal("Mon-Fri 9:00-17:00", (await context.Venues.SingleAsync()).OpeningHours);
    }

    [Fact]
    public async Task SetOpeningHours_Blank_Becomes_Null()
    {
        await using var context = CreateContext();
        var venue = await AddVenueAsync(context, "Cafe One", "Existing hours");

        var result = await CreateService(context).SetOpeningHoursAsync(
            [new VenueHoursAssignment(venue.Id, "   ")]);

        Assert.Equal(AdminStatus.Success, result.Status);
        Assert.Null((await context.Venues.SingleAsync()).OpeningHours);
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

    private static VenueAdminService CreateService(VibeDbContext context) => new(context);

    private static async Task<Venue> AddVenueAsync(
        VibeDbContext context,
        string name,
        string? openingHours = null)
    {
        var venue = new Venue
        {
            ExternalRefId = Guid.NewGuid().ToString(),
            Name = name,
            OpeningHours = openingHours
        };
        context.Venues.Add(venue);
        await context.SaveChangesAsync();
        return venue;
    }

    private static async Task<VenueTag> AddVenueTagAsync(
        VibeDbContext context,
        int venueId,
        string tagName,
        int voteCount = 1)
    {
        var tag = await context.Tags.SingleAsync(tag => tag.Name == tagName);
        var venueTag = new VenueTag
        {
            VenueId = venueId,
            TagId = tag.Id,
            VoteCount = voteCount
        };
        context.VenueTags.Add(venueTag);
        await context.SaveChangesAsync();
        return venueTag;
    }

    private static async Task<List<string>> GetTagNamesAsync(VibeDbContext context, int venueId) =>
        await context.VenueTags
            .Where(venueTag => venueTag.VenueId == venueId)
            .Join(context.Tags, venueTag => venueTag.TagId, tag => tag.Id, (_, tag) => tag.Name)
            .OrderBy(name => name)
            .ToListAsync();
}