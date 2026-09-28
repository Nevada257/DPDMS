# Loads the schema and demo data for every service into MySQL.
# Usage: powershell -ExecutionPolicy Bypass -File scripts\load-seed-data.ps1
#
# Finds mysql.exe even when it is not on PATH (standard MySQL Server and
# XAMPP install folders), and reads DB_PASSWORD from dpdms.env if it exists.
$root = Split-Path -Parent $PSScriptRoot

# 1. Find the mysql client
$mysql = (Get-Command mysql -ErrorAction SilentlyContinue).Source
if (-not $mysql) {
    # In order of preference: the real MySQL 8 client first. XAMPP/WAMP ship a
    # MariaDB client that cannot log in to MySQL 8 (caching_sha2_password).
    foreach ($base in @("$env:ProgramFiles\MySQL", "${env:ProgramFiles(x86)}\MySQL", "C:\xampp\mysql", "C:\wamp64\bin\mysql")) {
        if ($base -and (Test-Path $base)) {
            $found = Get-ChildItem -Path $base -Filter mysql.exe -Recurse -ErrorAction SilentlyContinue -Depth 5 |
                Sort-Object FullName -Descending | Select-Object -First 1
            if ($found) { $mysql = $found.FullName; break }
        }
    }
}
if (-not $mysql) {
    Write-Host ""
    Write-Host "Could not find mysql.exe." -ForegroundColor Red
    Write-Host "Install MySQL Server 8 (it includes the client), or run the files in the database folder"
    Write-Host "one by one in MySQL Workbench: File > Open SQL Script, then the lightning button."
    exit 1
}
Write-Host "Using $mysql"

# 2. Get the MySQL root password: environment, then dpdms.env, then ask once
$password = $env:DB_PASSWORD
$envFile = Join-Path $root "dpdms.env"
if (-not $password -and (Test-Path $envFile)) {
    $line = Get-Content $envFile | Where-Object { $_ -match '^\s*DB_PASSWORD=' } | Select-Object -Last 1
    if ($line) { $password = ($line -split '=', 2)[1].Trim() }
}
if (-not $password -or $password -eq "change-me") {
    $secure = Read-Host "MySQL root password" -AsSecureString
    $password = [Runtime.InteropServices.Marshal]::PtrToStringAuto(
        [Runtime.InteropServices.Marshal]::SecureStringToBSTR($secure))
}

# 3. Load every file in order
$failed = 0
Get-ChildItem (Join-Path $root "database") -Filter *.sql | Sort-Object Name | ForEach-Object {
    Write-Host "Loading $($_.Name)"
    Get-Content $_.FullName -Raw | & $mysql -u root "-p$password" 2>&1 |
        Where-Object { $_ -notmatch 'Using a password on the command line' }
    if ($LASTEXITCODE -ne 0) { $failed++ }
}

if ($failed -eq 0) {
    Write-Host "Done. All demo data loaded." -ForegroundColor Green
} else {
    Write-Host "$failed file(s) failed. Check the MySQL password in dpdms.env." -ForegroundColor Red
}
