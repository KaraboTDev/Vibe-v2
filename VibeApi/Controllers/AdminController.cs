using System.Security.Cryptography;
using System.Text;
using Microsoft.AspNetCore.Mvc;
using VibeApi.Services;

namespace VibeApi.Controllers;

public sealed record ImportRequest(decimal Latitude, decimal Longitude, int RadiusMeters = 5000, int Limit = 100);

[ApiController]
[Route("admin")]
public sealed class AdminController(
    IConfiguration configuration,
    IVenueImportService venueImportService) : ControllerBase
{
    [HttpPost("venues/import")]
    public async Task<IActionResult> ImportVenues(
        [FromHeader(Name = "X-Admin-Key")] string? adminKey,
        [FromBody] ImportRequest request)
    {
        if (!IsAuthorized(adminKey))
        {
            return Unauthorized();
        }

        if (request.RadiusMeters is < 100 or > 20000)
        {
            return BadRequest(new { message = "RadiusMeters must be between 100 and 20000." });
        }

        if (request.Limit is < 1 or > 500)
        {
            return BadRequest(new { message = "Limit must be between 1 and 500." });
        }

        var result = await venueImportService.ImportAsync(
            request.Latitude,
            request.Longitude,
            request.RadiusMeters,
            request.Limit);
        return Ok(result);
    }

    private bool IsAuthorized(string? providedKey)
    {
        var configuredKey = configuration["Admin:Key"];
        if (string.IsNullOrWhiteSpace(providedKey) ||
            string.IsNullOrWhiteSpace(configuredKey) ||
            configuredKey == "SET_IN_DEVELOPMENT_JSON")
        {
            return false;
        }

        return CryptographicOperations.FixedTimeEquals(
            Encoding.UTF8.GetBytes(providedKey),
            Encoding.UTF8.GetBytes(configuredKey));
    }
}
