using Microsoft.AspNetCore.Mvc;
using VibeApi.DTOs;
using VibeApi.Services;

namespace VibeApi.Controllers;

[ApiController]
[Route("auth")]
public sealed class AuthController(IAuthService authService) : ControllerBase
{
    [HttpPost("register")]
    public async Task<IActionResult> Register(RegisterRequest request)
    {
        var result = await authService.RegisterAsync(request);

        return result.Status switch
        {
            AuthStatus.Success => StatusCode(StatusCodes.Status201Created, result.Response),
            AuthStatus.EmailTaken => Conflict(new { message = "An account with this email already exists." }),
            AuthStatus.InvalidInput => BadRequest(new { message = "Email or password is invalid." }),
            _ => BadRequest(new { message = "Registration could not be completed." })
        };
    }

    [HttpPost("login")]
    public async Task<IActionResult> Login(LoginRequest request)
    {
        var result = await authService.LoginAsync(request);

        return result.Status switch
        {
            AuthStatus.Success => Ok(result.Response),
            AuthStatus.InvalidCredentials => Unauthorized(new { message = "Email or password is incorrect." }),
            AuthStatus.InvalidInput => BadRequest(new { message = "Email or password is invalid." }),
            _ => BadRequest(new { message = "Login could not be completed." })
        };
    }
}
