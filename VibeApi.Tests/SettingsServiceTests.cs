using Microsoft.EntityFrameworkCore;
using VibeApi.Data;
using VibeApi.DTOs;
using VibeApi.Models;
using VibeApi.Services;

namespace VibeApi.Tests;

public class SettingsServiceTests
{
    [Fact]
    public async Task Get_Returns_Defaults_For_New_User()
    {
        await using var context = CreateContext();
        var user = await AddUserAsync(context);

        var result = await CreateService(context).GetAsync(user.Id);

        Assert.Equal(new SettingsDto("en", null, true), result.Settings);
    }

    [Fact]
    public async Task Update_Changes_Only_Provided_Fields()
    {
        await using var context = CreateContext();
        var user = await AddUserAsync(context);
        user.DefaultArea = "Pretoria";
        await context.SaveChangesAsync();

        var result = await CreateService(context).UpdateAsync(
            user.Id,
            new UpdateSettingsRequest("zu", null, null));

        Assert.Equal(SettingsStatus.Success, result.Status);
        Assert.Equal(new SettingsDto("zu", "Pretoria", true), result.Settings);
    }

    [Fact]
    public async Task Update_Rejects_Unsupported_Language_And_Changes_Nothing()
    {
        await using var context = CreateContext();
        var user = await AddUserAsync(context);
        var service = CreateService(context);

        var result = await service.UpdateAsync(
            user.Id,
            new UpdateSettingsRequest("xx", "Changed", false));

        Assert.Equal(SettingsStatus.InvalidInput, result.Status);
        var unchanged = await service.GetAsync(user.Id);
        Assert.Equal(new SettingsDto("en", null, true), unchanged.Settings);
        Assert.Contains("en", result.Message!);
        Assert.Contains("zu", result.Message!);
        Assert.Contains("af", result.Message!);
    }

    [Fact]
    public async Task Update_Trims_DefaultArea_And_Blank_Clears_It()
    {
        await using var context = CreateContext();
        var user = await AddUserAsync(context);
        var service = CreateService(context);

        await service.UpdateAsync(user.Id, new UpdateSettingsRequest(null, "  Sandton  ", null));
        var trimmed = await service.GetAsync(user.Id);
        Assert.Equal("Sandton", trimmed.Settings!.DefaultArea);

        await service.UpdateAsync(user.Id, new UpdateSettingsRequest(null, "   ", null));
        var cleared = await service.GetAsync(user.Id);
        Assert.Null(cleared.Settings!.DefaultArea);
    }

    [Fact]
    public async Task Update_Unknown_User_Returns_UnknownUser()
    {
        await using var context = CreateContext();

        var result = await CreateService(context).UpdateAsync(
            9999,
            new UpdateSettingsRequest("en", null, null));

        Assert.Equal(SettingsStatus.UnknownUser, result.Status);
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

    private static async Task<User> AddUserAsync(VibeDbContext context)
    {
        var user = new User { Email = $"{Guid.NewGuid()}@example.com" };
        context.Users.Add(user);
        await context.SaveChangesAsync();
        return user;
    }

    private static SettingsService CreateService(VibeDbContext context) => new(context);
}
