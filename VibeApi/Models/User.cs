namespace VibeApi.Models;

public class User
{
    public int Id { get; set; }
    public required string Email { get; set; }
    public string? PasswordHash { get; set; }
    public string? GoogleId { get; set; }
    public string PreferredLanguage { get; set; } = "en";
    public string? DefaultArea { get; set; }
    public bool NotificationsEnabled { get; set; } = true;
    public string? FcmToken { get; set; }
    public DateTime CreatedAt { get; set; } = DateTime.UtcNow;
}
