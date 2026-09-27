# Runs one DPDMS service (or the front end) with the settings from dpdms.env.
# Used by start-all.ps1; can also be run on its own:
#   powershell -ExecutionPolicy Bypass -File scripts\run-service.ps1 flood
param([Parameter(Mandatory = $true)][string]$Folder)

$root = Split-Path -Parent $PSScriptRoot
$envFile = Join-Path $root "dpdms.env"
if (Test-Path $envFile) {
    Get-Content $envFile | Where-Object { $_ -match '^\s*[A-Z_]+=' } | ForEach-Object {
        $name, $value = $_ -split '=', 2
        [Environment]::SetEnvironmentVariable($name.Trim(), $value.Trim(), "Process")
    }
} else {
    Write-Warning "dpdms.env not found - copy dpdms.env.example to dpdms.env and fill it in."
}

$host.UI.RawUI.WindowTitle = $Folder
Set-Location (Join-Path $root $Folder)
if ($Folder -eq "flood-frontend") {
    npm run dev
} else {
    .\mvnw.cmd -q spring-boot:run
}
