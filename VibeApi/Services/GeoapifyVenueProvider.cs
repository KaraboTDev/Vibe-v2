using System.Globalization;
using Microsoft.Extensions.Configuration;

namespace VibeApi.Services;

public sealed class GeoapifyVenueProvider : IVenueDataProvider
{
    private readonly HttpClient httpClient;
    private readonly string apiKey;

    public GeoapifyVenueProvider(HttpClient httpClient, IConfiguration configuration)
    {
        this.httpClient = httpClient;
        this.httpClient.BaseAddress ??= new Uri("https://api.geoapify.com/");

        var configuredApiKey = configuration["Geoapify:ApiKey"];
        if (string.IsNullOrWhiteSpace(configuredApiKey) || configuredApiKey == "SET_IN_DEVELOPMENT_JSON")
        {
            throw new InvalidOperationException("Configure Geoapify:ApiKey in development settings or a secret store.");
        }

        apiKey = configuredApiKey.Trim();
    }

    public async Task<List<ExternalVenueDto>> FetchCafesAsync(
        decimal latitude,
        decimal longitude,
        int radiusMeters,
        int limit,
        CancellationToken ct = default)
    {
        var requestUri = string.Create(
            CultureInfo.InvariantCulture,
            $"v2/places?categories=catering.cafe&filter=circle:{longitude},{latitude},{radiusMeters}&limit={limit}&apiKey={Uri.EscapeDataString(apiKey)}");

        HttpResponseMessage response;
        try
        {
            response = await httpClient.GetAsync(requestUri, ct);
        }
        catch (HttpRequestException)
        {
            throw new HttpRequestException("Geoapify request failed.");
        }

        using (response)
        {
            if (!response.IsSuccessStatusCode)
            {
                throw new HttpRequestException(
                    $"Geoapify request failed with status code {(int)response.StatusCode}.",
                    null,
                    response.StatusCode);
            }

            string json;
            try
            {
                json = await response.Content.ReadAsStringAsync(ct);
            }
            catch (HttpRequestException)
            {
                throw new HttpRequestException("Geoapify response could not be read.");
            }

            return GeoapifyParser.Parse(json);
        }
    }
}
