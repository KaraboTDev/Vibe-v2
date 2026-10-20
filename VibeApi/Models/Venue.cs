namespace VibeApi.Models;

public class Venue
{
    public int Id { get; set; }
    public required string ExternalRefId { get; set; }
    public required string Name { get; set; }
    public decimal Latitude { get; set; }
    public decimal Longitude { get; set; }
    public string? Address { get; set; }
    public string? OpeningHours { get; set; }
}
