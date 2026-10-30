using Microsoft.EntityFrameworkCore;
using VibeApi.Data;
using VibeApi.DTOs;

namespace VibeApi.Services;

public enum SettingsStatus
{
    Success,
    InvalidInput,
    UnknownUser
}

public sealed record SettingsResult(
    SettingsStatus Status,
    SettingsDto? Settings,
    string? Message);

public interface ISettingsService
{
    Task<SettingsResult> GetAsync(int userId);
    Task<SettingsResult> UpdateAsync(int userId, UpdateSettingsRequest request);
}

public sealed class SettingsService(VibeDbContext dbContext) : ISettingsService
{
    private static readonly string[] AllowedLanguages = ["en", "zu", "af"];

    public async Task<SettingsResult> GetAsync(int userId)
    {
        var user = await dbContext.Users.SingleOrDefaultAsync(item => item.Id == userId);
        return user is null
            ? UnknownUser()
            : Success(user);
    }

    public async Task<SettingsResult> UpdateAsync(int userId, UpdateSettingsRequest request)
    {
        var user = await dbContext.Users.SingleOrDefaultAsync(item => item.Id == userId);
        if (user is null)
        {
            return UnknownUser();
        }

        var language = request.PreferredLanguage?.Trim().ToLowerInvariant();
        if (language is not null && !AllowedLanguages.Contains(language))
        {
            return InvalidInput(
                $"PreferredLanguage must be one of: {string.Join(", ", AllowedLanguages)}.");
        }

        var defaultArea = request.DefaultArea?.Trim();
        if (defaultArea is { Length: > 100 })
        {
            return InvalidInput("DefaultArea must be 100 characters or fewer.");
        }

        if (request.PreferredLanguage is not null)
        {
            user.PreferredLanguage = language!;
        }

        if (request.DefaultArea is not null)
        {
            user.DefaultArea = string.IsNullOrEmpty(defaultArea) ? null : defaultArea;
        }

        if (request.NotificationsEnabled is not null)
        {
            user.NotificationsEnabled = request.NotificationsEnabled.Value;
        }

        await dbContext.SaveChangesAsync();
        return Success(user);
    }

    private static SettingsResult Success(Models.User user) =>
        new(
            SettingsStatus.Success,
            new SettingsDto(user.PreferredLanguage, user.DefaultArea, user.NotificationsEnabled),
            null);

    private static SettingsResult UnknownUser() =>
        new(SettingsStatus.UnknownUser, null, "User was not found.");

    private static SettingsResult InvalidInput(string message) =>
        new(SettingsStatus.InvalidInput, null, message);
}
