# Starts the whole DPDMS stack on Windows, each service in its own window.
# Usage (from the repository root):   powershell -ExecutionPolicy Bypass -File scripts\start-all.ps1
# Prerequisites: JDK 21, MySQL 8 running, Node 20+, and a filled-in dpdms.env

$root = Split-Path -Parent $PSScriptRoot
$envFile = Join-Path $root "dpdms.env"
if (-not (Test-Path $envFile)) {
    Write-Error "dpdms.env not found. Copy dpdms.env.example to dpdms.env and fill it in."
    exit 1
}

# Load KEY=VALUE pairs into this session; child windows inherit them
Get-Content $envFile | Where-Object { $_ -match '^\s*[A-Z_]+=' } | ForEach-Object {
    $name, $value = $_ -split '=', 2
    [Environment]::SetEnvironmentVariable($name.Trim(), $value.Trim(), "Process")
}

function Start-Service($folder, $waitSeconds) {
    $path = Join-Path $root $folder
    if (-not (Test-Path $path)) { Write-Warning "$folder not found - skipped"; return }
    Write-Host "Starting $folder ..."
    Start-Process powershell -WorkingDirectory $path -ArgumentList "-NoExit", "-Command",
        "`$host.UI.RawUI.WindowTitle = '$folder'; .\mvnw.cmd -q spring-boot:run"
    Start-Sleep -Seconds $waitSeconds
}

# Order matters: discovery first, then auth, services, gateway last
Start-Service "discovery-service"        25
Start-Service "auth-service"             15
Start-Service "flood"                    5
Start-Service "drought"                  5
Start-Service "fire"                     5
Start-Service "zoonotic-disease-service" 5
Start-Service "mining-accident-service"  5
Start-Service "alert-service"            5
Start-Service "report-service"           5
Start-Service "dashboard-service"        20
Start-Service "dpdms-api-gateway"        15

Write-Host "Starting front end ..."
Start-Process powershell -WorkingDirectory (Join-Path $root "flood-frontend") -ArgumentList "-NoExit", "-Command",
    "`$host.UI.RawUI.WindowTitle = 'frontend'; if (-not (Test-Path node_modules)) { npm install }; npm run dev"

Write-Host ""
Write-Host "Eureka dashboard : http://localhost:8761"
Write-Host "Front end        : http://localhost:5173   (login e.g. national_user / password123)"
