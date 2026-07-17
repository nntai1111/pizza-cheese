# Apply load-test seed to Docker Postgres (Flyway schema must already exist).
# Usage from repo root:
#   docker compose up -d
#   # start Spring once against Docker (block B) so Flyway migrates, then you can stop it
#   .\scripts\seed-loadtest.ps1

$ErrorActionPreference = "Stop"
$Root = Split-Path -Parent $PSScriptRoot
Set-Location $Root

$SqlFile = Join-Path $Root "scripts\seed-loadtest.sql"
if (-not (Test-Path $SqlFile)) {
    Write-Error "Missing $SqlFile"
}

Write-Host "Ensuring Docker Postgres is up..."
docker compose up -d | Out-Null
Start-Sleep -Seconds 2

Write-Host "Running seed (may take several minutes for ~300k orders)..."
Get-Content -Raw -Encoding UTF8 $SqlFile | docker compose exec -T db psql -U postgres -d pizza_cheese -v ON_ERROR_STOP=1

Write-Host "Done."
