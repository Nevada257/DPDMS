# Starts DPDMS on Windows, each service in its own window.
#
#   Whole system:
#     powershell -ExecutionPolicy Bypass -File scripts\start-all.ps1
#   One or more hazards only (plus Eureka, auth, gateway and the front end):
#     powershell -ExecutionPolicy Bypass -File scripts\start-all.ps1 -Only flood
#     powershell -ExecutionPolicy Bypass -File scripts\start-all.ps1 -Only flood,fire
#   Hazard names: flood, drought, fire, zoonotic, mining
#   Add -WithExtras to also start alert, report and dashboard services.
#
# Prerequisites: JDK 21, MySQL 8 running, Node 20+, and a filled-in dpdms.env

param(
    [string[]]$Only = @(),
    [switch]$WithExtras
)

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

$hazardFolders = [ordered]@{
    flood    = "flood"
    drought  = "drought"
    fire     = "fire"
    zoonotic = "zoonotic-disease-service"
    mining   = "mining-accident-service"
}

# "powershell -File" passes "flood,fire" as one string - split it
$Only = @($Only | ForEach-Object { $_ -split ',' } | ForEach-Object { $_.Trim() } | Where-Object { $_ })

if ($Only.Count -gt 0) {
    foreach ($h in $Only) {
        if (-not $hazardFolders.Contains($h.ToLower())) {
            Write-Error "Unknown hazard '$h'. Use: $($hazardFolders.Keys -join ', ')"
            exit 1
        }
    }
    $hazards = $Only | ForEach-Object { $hazardFolders[$_.ToLower()] }
    $extras = $WithExtras.IsPresent
} else {
    $hazards = $hazardFolders.Values
    $extras = $true
}

# Order matters: discovery first, then auth, services, gateway last
Start-Service "discovery-service"        25
Start-Service "auth-service"             15
foreach ($folder in $hazards) { Start-Service $folder 5 }
if ($extras) {
    Start-Service "alert-service"            5
    Start-Service "report-service"           5
    Start-Service "dashboard-service"        20
}
Start-Service "dpdms-api-gateway"        15

Write-Host "Starting front end ..."
Start-Process powershell -WorkingDirectory (Join-Path $root "flood-frontend") -ArgumentList "-NoExit", "-Command",
    "`$host.UI.RawUI.WindowTitle = 'frontend'; if (-not (Test-Path node_modules)) { npm install }; npm run dev"

Write-Host ""
Write-Host "Eureka dashboard : http://localhost:8761"
Write-Host "Front end        : http://localhost:5173   (login e.g. national_user / password123)"
