using Microsoft.AspNetCore.Mvc;
using VibeApi.Services;

namespace VibeApi.Controllers;

[ApiController]
[Route("venues")]
public sealed class VenuesController(IVenueSearchService venueSearchService) : ControllerBase
{
    [HttpGet]
    public async Task<IActionResult> Search(
        [FromQuery] string? mood,
        [FromQuery] double lat,
        [FromQuery] double lng,
        [FromQuery] double radiusKm = 10,
        [FromQuery] int limit = 20)
    {
        var result = await venueSearchService.SearchAsync(mood, lat, lng, radiusKm, limit);
        return result.Status switch
        {
            SearchStatus.Success => Ok(result.Venues),
            SearchStatus.InvalidInput or SearchStatus.UnknownMood =>
                BadRequest(new { message = result.Message }),
            _ => BadRequest(new { message = "Venue search could not be completed." })
        };
    }

    [HttpGet("{id:int}")]
    public async Task<IActionResult> GetById(int id)
    {
        var venue = await venueSearchService.GetByIdAsync(id);
        return venue is null ? NotFound() : Ok(venue);
    }
}
