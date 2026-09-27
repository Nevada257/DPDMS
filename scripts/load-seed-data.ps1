# Loads the schema and demo data for every service into MySQL.
# Usage: powershell -ExecutionPolicy Bypass -File scripts\load-seed-data.ps1
# Requires the mysql client on PATH and DB_PASSWORD set (or you will be prompted).
$root = Split-Path -Parent $PSScriptRoot
Get-ChildItem (Join-Path $root "database") -Filter *.sql | Sort-Object Name | ForEach-Object {
    Write-Host "Loading $($_.Name)"
    if ($env:DB_PASSWORD) {
        Get-Content $_.FullName -Raw | mysql -u root "-p$($env:DB_PASSWORD)"
    } else {
        Get-Content $_.FullName -Raw | mysql -u root -p
    }
}
