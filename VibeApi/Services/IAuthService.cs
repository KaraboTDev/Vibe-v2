using VibeApi.DTOs;

namespace VibeApi.Services;

public enum AuthStatus
{
    Success,
    EmailTaken,
    InvalidCredentials,
    InvalidInput
}

public sealed record AuthResult(AuthStatus Status, AuthResponse? Response);

public interface IAuthService
{
    Task<AuthResult> RegisterAsync(RegisterRequest request);

    Task<AuthResult> LoginAsync(LoginRequest request);
}
