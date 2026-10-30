namespace VibeApi.DTOs;

public sealed record SettingsDto(
    string PreferredLanguage,
    string? DefaultArea,
    bool NotificationsEnabled);

public sealed record UpdateSettingsRequest(
    string? PreferredLanguage,
    string? DefaultArea,
    bool? NotificationsEnabled);
