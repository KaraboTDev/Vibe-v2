FROM mcr.microsoft.com/dotnet/sdk:10.0 AS build
WORKDIR /src

COPY VibeApi/VibeApi.csproj VibeApi/
RUN dotnet restore VibeApi/VibeApi.csproj

COPY VibeApi/ VibeApi/
RUN dotnet publish VibeApi/VibeApi.csproj \
    --configuration Release \
    --no-restore \
    --output /app/publish \
    --property UseAppHost=false

FROM mcr.microsoft.com/dotnet/aspnet:10.0 AS runtime
WORKDIR /app
ENV ASPNETCORE_URLS=http://+:8080
EXPOSE 8080

COPY --from=build /app/publish .
ENTRYPOINT ["dotnet", "VibeApi.dll"]
