namespace VibeApi.Models;

public class VenueTag
{
    public int Id { get; set; }
    public int VenueId { get; set; }
    public int TagId { get; set; }
    public int VoteCount { get; set; } = 1;

    public Venue Venue { get; set; } = null!;
    public Tag Tag { get; set; } = null!;
}
