# Stops every DPDMS service and the front end by the ports they listen on.
# Usage: powershell -ExecutionPolicy Bypass -File scripts\stop-all.ps1
$ports = [ordered]@{
    8761 = "discovery-service"; 8080 = "dpdms-api-gateway"; 8086 = "auth-service"
    8081 = "flood";             8082 = "drought";           8083 = "fire"
    8084 = "zoonotic";          8085 = "mining";            8087 = "report-service"
    8088 = "alert-service";     8089 = "dashboard-service"; 5173 = "frontend"
}
foreach ($port in $ports.Keys) {
    $conn = Get-NetTCPConnection -LocalPort $port -State Listen -ErrorAction SilentlyContinue | Select-Object -First 1
    if ($conn) {
        Stop-Process -Id $conn.OwningProcess -Force -ErrorAction SilentlyContinue
        Write-Host ("Stopped {0,-20} (port {1})" -f $ports[$port], $port)
    }
}
Write-Host "Done. You can close the terminal tabs."
