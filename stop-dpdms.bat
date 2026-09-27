@echo off
REM Double-click to stop every DPDMS service
cd /d "%~dp0"
powershell -ExecutionPolicy Bypass -File scripts\stop-all.ps1
pause
