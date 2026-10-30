using System.Security.Claims;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using VibeApi.DTOs;
using VibeApi.Services;

namespace VibeApi.Controllers;

[ApiController]
[Authorize]
[Route("settings")]
public sealed class SettingsController(ISettingsService settingsService) : ControllerBase
{
    [HttpGet]
    public async Task<IActionResult> Get()
    {
        if (!TryGetUserId(out var userId))
        {
            return Unauthorized();
        }

        var result = await settingsService.GetAsync(userId);
        return result.Status == SettingsStatus.UnknownUser
            ? NotFound(new { message = result.Message })
            : Ok(result.Settings);
    }

    [HttpPut]
    public async Task<IActionResult> Update(UpdateSettingsRequest request)
    {
        if (!TryGetUserId(out var userId))
        {
            return Unauthorized();
        }

        var result = await settingsService.UpdateAsync(userId, request);
        return result.Status switch
        {
            SettingsStatus.Success => Ok(result.Settings),
            SettingsStatus.InvalidInput => BadRequest(new { message = result.Message }),
            SettingsStatus.UnknownUser => NotFound(new { message = result.Message }),
            _ => BadRequest(new { message = "Settings could not be updated." })
        };
    }

    private bool TryGetUserId(out int userId)
    {
        return int.TryParse(
            User.FindFirstValue(ClaimTypes.NameIdentifier),
            out userId);
    }
}
