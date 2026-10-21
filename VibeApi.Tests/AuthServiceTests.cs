using System.IdentityModel.Tokens.Jwt;
using System.Security.Claims;
using System.Text;
using Microsoft.AspNetCore.Identity;
using Microsoft.EntityFrameworkCore;
using Microsoft.Extensions.Configuration;
using Microsoft.IdentityModel.Tokens;
using VibeApi.Data;
using VibeApi.DTOs;
using VibeApi.Models;
using VibeApi.Services;

namespace VibeApi.Tests;

public class AuthServiceTests
{
    private const string TestKey = "TestSigningKeyWithAtLeastFortyEightCharactersLong!";

    [Fact]
    public async Task Register_Stores_Hashed_Password_Not_Plaintext()
    {
        using var context = CreateContext();
        var service = CreateService(context);
        const string password = "correct-horse-battery";

        var result = await service.RegisterAsync(new RegisterRequest("person@example.com", password));
        var user = await context.Users.SingleAsync();

        Assert.Equal(AuthStatus.Success, result.Status);
        Assert.NotNull(user.PasswordHash);
        Assert.NotEqual(password, user.PasswordHash);
        Assert.Equal(
            PasswordVerificationResult.Success,
            new PasswordHasher<User>().VerifyHashedPassword(user, user.PasswordHash, password));
    }

    [Fact]
    public async Task Register_Normalizes_Email_To_Lowercase()
    {
        using var context = CreateContext();
        var service = CreateService(context);

        var result = await service.RegisterAsync(new RegisterRequest("  JohnDoe@Gmail.com  ", "password123"));

        Assert.Equal(AuthStatus.Success, result.Status);
        Assert.Equal("johndoe@gmail.com", (await context.Users.SingleAsync()).Email);
    }

    [Fact]
    public async Task Register_Duplicate_Email_Returns_EmailTaken()
    {
        using var context = CreateContext();
        var service = CreateService(context);

        var firstResult = await service.RegisterAsync(new RegisterRequest("JohnDoe@Gmail.com", "password123"));
        var duplicateResult = await service.RegisterAsync(new RegisterRequest("johndoe@gmail.com", "password456"));

        Assert.Equal(AuthStatus.Success, firstResult.Status);
        Assert.Equal(AuthStatus.EmailTaken, duplicateResult.Status);
        Assert.Single(await context.Users.ToListAsync());
    }

    [Fact]
    public async Task Register_Short_Password_Returns_InvalidInput()
    {
        using var context = CreateContext();
        var service = CreateService(context);

        var result = await service.RegisterAsync(new RegisterRequest("person@example.com", "short7"));

        Assert.Equal(AuthStatus.InvalidInput, result.Status);
        Assert.Empty(await context.Users.ToListAsync());
    }

    [Fact]
    public async Task Login_With_Correct_Password_Returns_Token()
    {
        using var context = CreateContext();
        var service = CreateService(context);
        const string password = "correct-password";
        await service.RegisterAsync(new RegisterRequest("person@example.com", password));

        var result = await service.LoginAsync(new LoginRequest("PERSON@example.com", password));

        Assert.Equal(AuthStatus.Success, result.Status);
        Assert.NotNull(result.Response);
        Assert.False(string.IsNullOrWhiteSpace(result.Response.Token));
    }

    [Fact]
    public async Task Login_With_Wrong_Password_Returns_InvalidCredentials()
    {
        using var context = CreateContext();
        var service = CreateService(context);
        await service.RegisterAsync(new RegisterRequest("person@example.com", "correct-password"));

        var result = await service.LoginAsync(new LoginRequest("person@example.com", "wrong-password"));

        Assert.Equal(AuthStatus.InvalidCredentials, result.Status);
        Assert.Null(result.Response);
    }

    [Fact]
    public async Task Token_Contains_User_Id_And_Email_Claims()
    {
        using var context = CreateContext();
        var configuration = CreateConfiguration();
        var service = new AuthService(context, configuration);
        const string email = "person@example.com";
        var result = await service.RegisterAsync(new RegisterRequest(email, "correct-password"));
        Assert.Equal(AuthStatus.Success, result.Status);

        var handler = new JwtSecurityTokenHandler();
        var parameters = new TokenValidationParameters
        {
            ValidateIssuer = true,
            ValidIssuer = "VibeApi",
            ValidateAudience = true,
            ValidAudience = "VibeApp",
            ValidateLifetime = true,
            ValidateIssuerSigningKey = true,
            IssuerSigningKey = new SymmetricSecurityKey(Encoding.UTF8.GetBytes(configuration["Jwt:Key"]!))
        };
        var principal = handler.ValidateToken(result.Response!.Token, parameters, out _);
        var user = await context.Users.SingleAsync();

        Assert.Equal(user.Id.ToString(), principal.FindFirst(ClaimTypes.NameIdentifier)?.Value);
        Assert.Equal(email, principal.FindFirst(ClaimTypes.Email)?.Value);
    }

    private static VibeDbContext CreateContext()
    {
        var options = new DbContextOptionsBuilder<VibeDbContext>()
            .UseInMemoryDatabase(Guid.NewGuid().ToString())
            .Options;

        return new VibeDbContext(options);
    }

    private static AuthService CreateService(VibeDbContext context) =>
        new(context, CreateConfiguration());

    private static IConfiguration CreateConfiguration() =>
        new ConfigurationBuilder()
            .AddInMemoryCollection(new Dictionary<string, string?>
            {
                ["Jwt:Key"] = TestKey,
                ["Jwt:Issuer"] = "VibeApi",
                ["Jwt:Audience"] = "VibeApp",
                ["Jwt:ExpiresMinutes"] = "10080"
            })
            .Build();
}
