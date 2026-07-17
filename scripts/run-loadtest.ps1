# Run k6 load test against local Spring (default :8080) via Docker image.
# Prerequisites: Spring F5 + Docker DB seeded. From repo root:
#   .\scripts\run-loadtest.ps1
#   .\scripts\run-loadtest.ps1 -Vus 20 -Duration 30s

param(
    [int]$Vus = 10,
    [string]$Duration = "20s"
)

$ErrorActionPreference = "Stop"
$Root = Split-Path -Parent $PSScriptRoot
Set-Location $Root

$scriptPath = Join-Path $Root "scripts\loadtest\orders.js"
if (-not (Test-Path $scriptPath)) {
    Write-Error "Missing $scriptPath"
}

Write-Host "Checking Spring at http://localhost:8080 ..."
try {
    Invoke-WebRequest -Uri "http://localhost:8080/api/v1/pizzas" -UseBasicParsing -TimeoutSec 5 | Out-Null
}
catch {
    Write-Error "Spring not reachable. Start app (F5) with Docker DB first."
}

Write-Host "Running k6: VUs=$Vus duration=$Duration"
# host.docker.internal = máy bạn từ trong container k6
docker run --rm `
  -e "BASE_URL=http://host.docker.internal:8080" `
  -e "VUS=$Vus" `
  -e "DURATION=$Duration" `
  -e "LOGIN=admin@gmail.com" `
  -e "PASSWORD=123456" `
  -v "${scriptPath}:/scripts/orders.js:ro" `
  grafana/k6:0.54.0 run /scripts/orders.js
