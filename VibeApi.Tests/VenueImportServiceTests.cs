using Microsoft.EntityFrameworkCore;
using VibeApi.Data;
using VibeApi.Models;
using VibeApi.Services;

namespace VibeApi.Tests;

public class VenueImportServiceTests
{
    [Fact]
    public async Task Import_Adds_New_Venues()
    {
        await using var context = CreateContext();
        var service = CreateService(context, CreateVenue("place-1", "Cafe One"));

        var result = await service.ImportAsync(40.1m, -73.2m, 5000, 100);

        Assert.Equal(new ImportResult(1, 1, 0), result);
        var venue = await context.Venues.SingleAsync();
        Assert.Equal("place-1", venue.ExternalRefId);
        Assert.Equal("Cafe One", venue.Name);
    }

    [Fact]
    public async Task Import_Twice_Does_Not_Create_Duplicates()
    {
        await using var context = CreateContext();
        var service = CreateService(context, CreateVenue("place-1", "Cafe One"));

        await service.ImportAsync(40.1m, -73.2m, 5000, 100);
        var secondResult = await service.ImportAsync(40.1m, -73.2m, 5000, 100);

        Assert.Equal(new ImportResult(1, 0, 0), secondResult);
        Assert.Equal(1, await context.Venues.CountAsync());
    }

    [Fact]
    public async Task Import_Updates_Changed_Opening_Hours()
    {
        await using var context = CreateContext();
        context.Venues.Add(new Venue
        {
            ExternalRefId = "place-1",
            Name = "Cafe One",
            Latitude = 40.1m,
            Longitude = -73.2m,
            Address = "1 Main Street",
            OpeningHours = "Old hours"
        });
        await context.SaveChangesAsync();
        var service = CreateService(context, CreateVenue("place-1", "Cafe One", "New hours"));

        var result = await service.ImportAsync(40.1m, -73.2m, 5000, 100);

        Assert.Equal(new ImportResult(1, 0, 1), result);
        Assert.Equal("New hours", (await context.Venues.SingleAsync()).OpeningHours);
    }

    [Fact]
    public async Task Import_Deduplicates_Incoming_Duplicates()
    {
        await using var context = CreateContext();
        var service = CreateService(
            context,
            CreateVenue("place-1", "Cafe One"),
            CreateVenue("place-1", "Duplicate Cafe"));

        var result = await service.ImportAsync(40.1m, -73.2m, 5000, 100);

        Assert.Equal(new ImportResult(2, 1, 0), result);
        var venue = await context.Venues.SingleAsync();
        Assert.Equal("Cafe One", venue.Name);
    }

    private static VibeDbContext CreateContext()
    {
        var options = new DbContextOptionsBuilder<VibeDbContext>()
            .UseInMemoryDatabase(Guid.NewGuid().ToString())
            .Options;
        return new VibeDbContext(options);
    }

    private static VenueImportService CreateService(VibeDbContext context, params ExternalVenueDto[] venues) =>
        new(context, new FakeVenueDataProvider(venues.ToList()));

    private static ExternalVenueDto CreateVenue(
        string externalRefId,
        string name,
        string? openingHours = "Monday-Friday") =>
        new(externalRefId, name, 40.1m, -73.2m, "1 Main Street", openingHours);

    private sealed class FakeVenueDataProvider(List<ExternalVenueDto> venues) : IVenueDataProvider
    {
        public Task<List<ExternalVenueDto>> FetchCafesAsync(
            decimal latitude,
            decimal longitude,
            int radiusMeters,
            int limit,
            CancellationToken ct = default) =>
            Task.FromResult(venues);
    }
}
