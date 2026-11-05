using System.Text;
using Microsoft.AspNetCore.Authentication.JwtBearer;
using Microsoft.EntityFrameworkCore;
using Microsoft.IdentityModel.Tokens;
using Microsoft.OpenApi;
using Npgsql;
using VibeApi.Data;
using VibeApi.Services;

var builder = WebApplication.CreateBuilder(args);

var jwtKey = builder.Configuration["Jwt:Key"];
if (string.IsNullOrWhiteSpace(jwtKey) || jwtKey == "SET_IN_DEVELOPMENT_JSON")
{
    throw new InvalidOperationException("Configure Jwt:Key using development settings or a secret store.");
}

var connectionString = builder.Configuration.GetConnectionString("DefaultConnection");
if (string.IsNullOrWhiteSpace(connectionString))
{
    throw new InvalidOperationException("Configure ConnectionStrings:DefaultConnection using a secret store.");
}

// Add services to the container.

builder.Services.AddDbContext<VibeDbContext>(options =>
    options.UseNpgsql(ToNpgsqlConnectionString(connectionString)));
builder.Services.AddControllers();
builder.Services.AddEndpointsApiExplorer();
builder.Services.AddSwaggerGen(options =>
{
    options.AddSecurityDefinition("Bearer", new OpenApiSecurityScheme
    {
        Name = "Authorization",
        Type = SecuritySchemeType.Http,
        Scheme = "bearer",
        BearerFormat = "JWT",
        In = ParameterLocation.Header,
        Description = "Enter a valid JWT bearer token."
    });
    options.AddSecurityRequirement(document => new OpenApiSecurityRequirement
    {
        [new OpenApiSecuritySchemeReference("Bearer", document)] = []
    });
});
builder.Services.AddScoped<IAuthService, AuthService>();
builder.Services.AddHttpClient<IVenueDataProvider, GeoapifyVenueProvider>();
builder.Services.AddScoped<IVenueImportService, VenueImportService>();
builder.Services.AddScoped<IVenueAdminService, VenueAdminService>();
builder.Services.AddScoped<IVenueSearchService, VenueSearchService>();
builder.Services.AddScoped<IFavoriteService, FavoriteService>();
builder.Services.AddScoped<ISettingsService, SettingsService>();
builder.Services
    .AddAuthentication(JwtBearerDefaults.AuthenticationScheme)
    .AddJwtBearer(options =>
    {
        options.TokenValidationParameters = new TokenValidationParameters
        {
            ValidateIssuer = true,
            ValidIssuer = builder.Configuration["Jwt:Issuer"],
            ValidateAudience = true,
            ValidAudience = builder.Configuration["Jwt:Audience"],
            ValidateLifetime = true,
            ValidateIssuerSigningKey = true,
            IssuerSigningKey = new SymmetricSecurityKey(Encoding.UTF8.GetBytes(jwtKey))
        };
    });
builder.Services.AddAuthorization();

var app = builder.Build();

// Apply pending migrations when the application starts.
using (var scope = app.Services.CreateScope())
{
    var dbContext = scope.ServiceProvider.GetRequiredService<VibeDbContext>();
    dbContext.Database.Migrate();
}

// Configure the HTTP request pipeline.
if (app.Environment.IsDevelopment())
{
    app.UseSwagger();
    app.UseSwaggerUI();
}

// app.UseHttpsRedirection();

app.UseAuthentication();
app.UseAuthorization();

app.MapControllers();
app.MapGet("/health", () => Results.Ok(new { status = "ok" }));

app.Run();

static string ToNpgsqlConnectionString(string connectionString)
{
    if (!Uri.TryCreate(connectionString, UriKind.Absolute, out var databaseUri)
        || (databaseUri.Scheme != "postgres" && databaseUri.Scheme != "postgresql"))
    {
        return connectionString;
    }

    var separator = databaseUri.UserInfo.IndexOf(':');
    if (separator <= 0 || separator == databaseUri.UserInfo.Length - 1)
    {
        throw new InvalidOperationException("The PostgreSQL URL must include a username and password.");
    }

    return new NpgsqlConnectionStringBuilder
    {
        Host = databaseUri.Host,
        Port = databaseUri.Port > 0 ? databaseUri.Port : 5432,
        Username = Uri.UnescapeDataString(databaseUri.UserInfo[..separator]),
        Password = Uri.UnescapeDataString(databaseUri.UserInfo[(separator + 1)..]),
        Database = databaseUri.AbsolutePath.TrimStart('/'),
        SslMode = SslMode.Require
    }.ConnectionString;
}
