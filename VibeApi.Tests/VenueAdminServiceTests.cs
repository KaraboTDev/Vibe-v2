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
        context.VenueTags.AddRange(
            new VenueTag { VenueId = venue.Id, TagId = 1 },
            new VenueTag { VenueId = venue.Id, TagId = 2 });
        await context.SaveChangesAsync();

        var result = await CreateService(context).SetTagsAsync(
            [new VenueTagAssignment(venue.Id, ["Focus", "Creative"])]);

        Assert.Equal(new AdminResult(AdminStatus.Success, 1, null), result);
        Assert.Equal(
            new[] { 3, 4 },
            await context.VenueTags
                .Where(venueTag => venueTag.VenueId == venue.Id)
                .Select(venueTag => venueTag.TagId)
                .OrderBy(tagId => tagId)
                .ToArrayAsync());
    }

    [Fact]
    public async Task SetTags_Is_Case_Insensitive_And_Removes_Duplicates()
    {
        await using var context = CreateContext();
        var venue = await AddVenueAsync(context, "Cafe One");

        var result = await CreateService(context).SetTagsAsync(
            [new VenueTagAssignment(venue.Id, [" quiet ", "QUIET", "SoCiAl"])]);

        Assert.Equal(AdminStatus.Success, result.Status);
        Assert.Equal(
            new[] { 1, 2 },
            await context.VenueTags
                .Where(venueTag => venueTag.VenueId == venue.Id)
                .Select(venueTag => venueTag.TagId)
                .OrderBy(tagId => tagId)
                .ToArrayAsync());
    }

    [Fact]
    public async Task SetTags_Keeps_Existing_Row_And_VoteCount_When_Tag_Unchanged()
    {
        await using var context = CreateContext();
        var venue = await AddVenueAsync(context, "Cafe One");
        var existingRow = new VenueTag { VenueId = venue.Id, TagId = 1, VoteCount = 17 };
        context.VenueTags.Add(existingRow);
        await context.SaveChangesAsync();
        var existingId = existingRow.Id;

        var result = await CreateService(context).SetTagsAsync(
            [new VenueTagAssignment(venue.Id, ["Quiet"])]);

        Assert.Equal(AdminStatus.Success, result.Status);
        var retainedRow = await context.VenueTags.SingleAsync();
        Assert.Equal(existingId, retainedRow.Id);
        Assert.Equal(17, retainedRow.VoteCount);
    }

    [Fact]
    public async Task SetTags_Unknown_Tag_Changes_Nothing()
    {
        await using var context = CreateContext();
        var firstVenue = await AddVenueAsync(context, "Cafe One");
        var secondVenue = await AddVenueAsync(context, "Cafe Two");
        context.VenueTags.Add(new VenueTag { VenueId = firstVenue.Id, TagId = 1 });
        await context.SaveChangesAsync();

        var result = await CreateService(context).SetTagsAsync(
        [
            new VenueTagAssignment(firstVenue.Id, ["Social"]),
            new VenueTagAssignment(secondVenue.Id, ["Not a mood"])
        ]);

        Assert.Equal(AdminStatus.UnknownTag, result.Status);
        Assert.Contains("Not a mood", result.Detail);
        Assert.Equal(
            new[] { 1 },
            await context.VenueTags
                .Where(venueTag => venueTag.VenueId == firstVenue.Id)
                .Select(venueTag => venueTag.TagId)
                .ToArrayAsync());
        Assert.Empty(await context.VenueTags
            .Where(venueTag => venueTag.VenueId == secondVenue.Id)
            .ToListAsync());
    }

    [Fact]
    public async Task SetTags_Unknown_Venue_Returns_UnknownVenue()
    {
        await using var context = CreateContext();
        var venue = await AddVenueAsync(context, "Cafe One");

        var result = await CreateService(context).SetTagsAsync(
            [new VenueTagAssignment(venue.Id + 100, ["Quiet"])]);

        Assert.Equal(AdminStatus.UnknownVenue, result.Status);
        Assert.Contains((venue.Id + 100).ToString(), result.Detail);
    }

    [Fact]
    public async Task SetOpeningHours_Trims_And_Stores()
    {
        await using var context = CreateContext();
        var venue = await AddVenueAsync(context, "Cafe One");

        var result = await CreateService(context).SetOpeningHoursAsync(
            [new VenueHoursAssignment(venue.Id, "  Monday-Friday  ")]);

        Assert.Equal(new AdminResult(AdminStatus.Success, 1, null), result);
        Assert.Equal("Monday-Friday", (await context.Venues.SingleAsync()).OpeningHours);
    }

    [Fact]
    public async Task SetOpeningHours_Blank_Becomes_Null()
    {
        await using var context = CreateContext();
        var venue = await AddVenueAsync(context, "Cafe One", "Old hours");

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

    private static VenueAdminService CreateService(VibeDbContext context) => new(context);
}
