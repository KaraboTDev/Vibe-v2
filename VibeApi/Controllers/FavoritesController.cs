using System.Security.Claims;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using VibeApi.DTOs;
using VibeApi.Services;

namespace VibeApi.Controllers;

[ApiController]
[Authorize]
[Route("favorites")]
public sealed class FavoritesController(IFavoriteService favoriteService) : ControllerBase
{
    [HttpGet]
    public async Task<IActionResult> Get()
    {
        if (!TryGetUserId(out var userId))
        {
            return Unauthorized();
        }

        return Ok(await favoriteService.GetAllAsync(userId));
    }

    [HttpPost]
    public async Task<IActionResult> Add(AddFavoriteRequest request)
    {
        if (!TryGetUserId(out var userId))
        {
            return Unauthorized();
        }

        var result = await favoriteService.AddAsync(userId, request.VenueId);
        return result.Status switch
        {
            FavoriteStatus.Success => Ok(result.Favorite),
            FavoriteStatus.UnknownVenue => NotFound(new { message = "Venue was not found." }),
            _ => BadRequest(new { message = "Favorite could not be saved." })
        };
    }

    [HttpDelete("{venueId:int}")]
    public async Task<IActionResult> Remove(int venueId)
    {
        if (!TryGetUserId(out var userId))
        {
            return Unauthorized();
        }

        await favoriteService.RemoveAsync(userId, venueId);
        return NoContent();
    }

    private bool TryGetUserId(out int userId)
    {
        return int.TryParse(
            User.FindFirstValue(ClaimTypes.NameIdentifier),
            out userId);
    }
}
