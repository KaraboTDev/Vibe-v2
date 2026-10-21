using System.IdentityModel.Tokens.Jwt;
using System.Security.Claims;
using System.Text;
using Microsoft.AspNetCore.Identity;
using Microsoft.EntityFrameworkCore;
using Microsoft.IdentityModel.Tokens;
using VibeApi.Data;
using VibeApi.DTOs;
using VibeApi.Models;

namespace VibeApi.Services;

public sealed class AuthService : IAuthService
{
    private const int DefaultExpiresMinutes = 10080;

    private readonly VibeDbContext _dbContext;
    private readonly IConfiguration _configuration;
    private readonly PasswordHasher<User> _passwordHasher = new();

    public AuthService(VibeDbContext dbContext, IConfiguration configuration)
    {
        _dbContext = dbContext;
        _configuration = configuration;
    }

    public async Task<AuthResult> RegisterAsync(RegisterRequest request)
    {
        if (string.IsNullOrWhiteSpace(request.Email)
            || !request.Email.Contains('@')
            || request.Password is null
            || request.Password.Length < 8)
        {
            return new AuthResult(AuthStatus.InvalidInput, null);
        }

        var normalizedEmail = NormalizeEmail(request.Email);
        if (await _dbContext.Users.AnyAsync(user => user.Email == normalizedEmail))
        {
            return new AuthResult(AuthStatus.EmailTaken, null);
        }

        var user = new User { Email = normalizedEmail };
        user.PasswordHash = _passwordHasher.HashPassword(user, request.Password);

        _dbContext.Users.Add(user);
        await _dbContext.SaveChangesAsync();

        return new AuthResult(AuthStatus.Success, CreateAuthResponse(user));
    }

    public async Task<AuthResult> LoginAsync(LoginRequest request)
    {
        if (string.IsNullOrWhiteSpace(request.Email) || string.IsNullOrEmpty(request.Password))
        {
            return new AuthResult(AuthStatus.InvalidInput, null);
        }

        var normalizedEmail = NormalizeEmail(request.Email);
        var user = await _dbContext.Users
            .SingleOrDefaultAsync(existingUser => existingUser.Email == normalizedEmail);

        if (user?.PasswordHash is not { Length: > 0 } passwordHash
            || _passwordHasher.VerifyHashedPassword(user, passwordHash, request.Password)
                == PasswordVerificationResult.Failed)
        {
            return new AuthResult(AuthStatus.InvalidCredentials, null);
        }

        return new AuthResult(AuthStatus.Success, CreateAuthResponse(user));
    }

    private AuthResponse CreateAuthResponse(User user)
    {
        var key = RequiredSetting("Jwt:Key");
        if (key == "SET_IN_DEVELOPMENT_JSON")
        {
            throw new InvalidOperationException("Configure Jwt:Key in Development settings or a secret store.");
        }

        var issuer = RequiredSetting("Jwt:Issuer");
        var audience = RequiredSetting("Jwt:Audience");
        var expiresMinutes = _configuration.GetValue<int?>("Jwt:ExpiresMinutes") ?? DefaultExpiresMinutes;
        var expiresAt = DateTime.UtcNow.AddMinutes(expiresMinutes);

        var claims = new[]
        {
            new Claim(ClaimTypes.NameIdentifier, user.Id.ToString()),
            new Claim(ClaimTypes.Email, user.Email)
        };
        var signingKey = new SymmetricSecurityKey(Encoding.UTF8.GetBytes(key));
        var credentials = new SigningCredentials(signingKey, SecurityAlgorithms.HmacSha256);
        var token = new JwtSecurityToken(
            issuer,
            audience,
            claims,
            expires: expiresAt,
            signingCredentials: credentials);

        return new AuthResponse(new JwtSecurityTokenHandler().WriteToken(token), user.Id, user.Email, expiresAt);
    }

    private string RequiredSetting(string key) =>
        _configuration[key] is { Length: > 0 } value
            ? value
            : throw new InvalidOperationException($"The configuration value '{key}' is required.");

    private static string NormalizeEmail(string email) => email.Trim().ToLowerInvariant();
}
