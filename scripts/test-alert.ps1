# Sends a DPDMS test alert (email + WhatsApp) and prints the result.
# Usage: powershell -ExecutionPolicy Bypass -File scripts\test-alert.ps1
# The system must be running (start-dpdms.bat).
$api = "http://localhost:8080"
try {
    $login = Invoke-RestMethod -Method Post -Uri "$api/api/auth/login" -ContentType "application/json" `
        -Body '{"username":"provincial_admin","password":"password123"}'
} catch {
    Write-Host "Could not log in through the gateway: $($_.Exception.Message)" -ForegroundColor Red
    Write-Host "Is the system running? Check http://localhost:8761"
    exit 1
}
$headers = @{ Authorization = "Bearer $($login.token)" }

try {
    $r = Invoke-RestMethod -Method Post -Uri "$api/api/alerts/test" -Headers $headers
    Write-Host "Test alert queued for $($r.subscribers) subscriber(s). Waiting 15 seconds..."
} catch {
    Write-Host "alert-service refused the test: $($_.Exception.Message)" -ForegroundColor Red
    exit 1
}
Start-Sleep -Seconds 15

$logs = Invoke-RestMethod -Uri "$api/api/alerts/logs" -Headers $headers
Write-Host ""
$logs | Select-Object -First 2 | ForEach-Object {
    $colour = switch ($_.deliveryStatus) { "SENT" { "Green" } "FAILED" { "Red" } default { "Yellow" } }
    Write-Host ("{0}  {1,-8} to {2}  ->  {3} (tries: {4})" -f $_.sentAt, $_.channel, $_.recipient, $_.deliveryStatus, $_.attempts) -ForegroundColor $colour
    if ($_.errorMessage) { Write-Host "   reason: $($_.errorMessage)" }
}
