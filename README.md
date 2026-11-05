[![API CI](https://github.com/KaraboTDev/Vibe-v2/actions/workflows/api-ci.yml/badge.svg?branch=main)](https://github.com/KaraboTDev/Vibe-v2/actions/workflows/api-ci.yml)
[![Android CI](https://github.com/KaraboTDev/Vibe-v2/actions/workflows/android-ci.yml/badge.svg?branch=main)](https://github.com/KaraboTDev/Vibe-v2/actions/workflows/android-ci.yml)

# Vibe-v2

## Deploying the API to Render

The repository includes a Render Blueprint (`render.yaml`) and a Dockerfile for
the ASP.NET Core API. To deploy:

1. Push the repository to GitHub.
2. In Render, choose **New > Blueprint** and select this repository.
3. Apply the Blueprint. It creates the `vibe-api` web service and a PostgreSQL
   database, then runs the EF Core migrations when the service starts.
4. In the `vibe-api` environment settings, set `Geoapify__ApiKey` and
   `Admin__Key`.

The API listens on Render's `PORT` through the container's port 8080. Swagger
is disabled in production; use the service URL and the controller routes for
API requests.
