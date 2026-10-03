# Prepares the assignment submission in two steps:
#   1. Exports every DPDMS database (structure + current data) to
#      database\export\dpdms_database_dump.sql inside the project folder.
#   2. Zips the whole project into one file next to the project folder,
#      leaving out build output, node_modules, git history and dpdms.env
#      (dpdms.env holds passwords and must never be submitted).
#
# Usage: double-click package-submission.bat, or
#   powershell -ExecutionPolicy Bypass -File scripts\package-submission.ps1
# Put the peer evaluation (and any other document to hand in) in the project
# folder first; everything in the folder is included.
$root = Split-Path -Parent $PSScriptRoot
$databases = @("auth_db", "flood_db", "drought_db", "fire_db", "zoonotic_db", "mining_accident_db", "alert_db")

# ---- 1. Find mysqldump (prefer the MySQL 8 client over XAMPP/WAMP's MariaDB one)
$dump = (Get-Command mysqldump -ErrorAction SilentlyContinue).Source
if (-not $dump) {
    foreach ($base in @("$env:ProgramFiles\MySQL", "${env:ProgramFiles(x86)}\MySQL", "C:\xampp\mysql", "C:\wamp64\bin\mysql")) {
        if ($base -and (Test-Path $base)) {
            $found = Get-ChildItem -Path $base -Filter mysqldump.exe -Recurse -ErrorAction SilentlyContinue -Depth 5 |
                Sort-Object FullName -Descending | Select-Object -First 1
            if ($found) { $dump = $found.FullName; break }
        }
    }
}
if (-not $dump) {
    Write-Host "Could not find mysqldump.exe (it comes with MySQL Server 8)." -ForegroundColor Red
    exit 1
}

# ---- 2. MySQL root password: environment, then dpdms.env, then ask
$password = $env:DB_PASSWORD
$envFile = Join-Path $root "dpdms.env"
if (-not $password -and (Test-Path $envFile)) {
    $line = Get-Content $envFile | Where-Object { $_ -match '^\s*DB_PASSWORD=' } | Select-Object -Last 1
    if ($line) { $password = ($line -split '=', 2)[1].Trim() }
}
if (-not $password) {
    $secure = Read-Host "MySQL root password" -AsSecureString
    $password = [Runtime.InteropServices.Marshal]::PtrToStringAuto(
        [Runtime.InteropServices.Marshal]::SecureStringToBSTR($secure))
}

# ---- 3. Export the databases
$exportDir = Join-Path $root "database\export"
New-Item -ItemType Directory -Force -Path $exportDir | Out-Null
$dumpFile = Join-Path $exportDir "dpdms_database_dump.sql"
Write-Host "Exporting $($databases.Count) databases with $dump ..."
$dumpArgs = @("-u", "root", "-p$password", "--databases") + $databases +
        @("--routines", "--single-transaction", "--set-gtid-purged=OFF", "--add-drop-database", "--default-character-set=utf8mb4", "--result-file=$dumpFile")
& $dump @dumpArgs 2>&1 | Where-Object { $_ -notmatch 'Using a password on the command line' } | ForEach-Object { Write-Host $_ }
if ($LASTEXITCODE -ne 0 -or -not (Test-Path $dumpFile)) {
    Write-Host "Database export failed. Is MySQL running and is DB_PASSWORD in dpdms.env correct?" -ForegroundColor Red
    exit 1
}
Write-Host ("Database saved: database\export\dpdms_database_dump.sql ({0:N0} KB)" -f ((Get-Item $dumpFile).Length / 1KB)) -ForegroundColor Green

# ---- 4. Copy the project without build output and secrets, then zip it
$name = "DPDMS_Submission"
$parent = Split-Path -Parent $root
$staging = Join-Path $env:TEMP $name
$zip = Join-Path $parent "$name.zip"
if (Test-Path $staging) { Remove-Item $staging -Recurse -Force }
Write-Host "Copying project files (without node_modules, target, .git and dpdms.env) ..."
robocopy $root $staging /E /NFL /NDL /NJH /NJS /NP `
    /XD node_modules target .git .idea .vscode dist `
    /XF dpdms.env alert-log.txt *.log | Out-Null
if ($LASTEXITCODE -ge 8) {
    Write-Host "Copying the project failed (robocopy code $LASTEXITCODE)." -ForegroundColor Red
    exit 1
}
if (Test-Path $zip) { Remove-Item $zip -Force }
Compress-Archive -Path (Join-Path $staging "*") -DestinationPath $zip
Remove-Item $staging -Recurse -Force

Write-Host ""
Write-Host ("Submission ready: {0} ({1:N1} MB)" -f $zip, ((Get-Item $zip).Length / 1MB)) -ForegroundColor Green
Write-Host "It contains the code, database\export\dpdms_database_dump.sql, the README and any documents you put in the project folder."
Write-Host "dpdms.env (passwords) was left out on purpose."
