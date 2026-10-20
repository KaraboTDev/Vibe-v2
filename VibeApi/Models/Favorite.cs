namespace VibeApi.Models;

public class Favorite
{
    public int Id { get; set; }
    public int UserId { get; set; }
    public int VenueId { get; set; }
    public DateTime SavedAt { get; set; } = DateTime.UtcNow;
    public string SyncStatus { get; set; } = "synced";

    public User User { get; set; } = null!;
    public Venue Venue { get; set; } = null!;
}
