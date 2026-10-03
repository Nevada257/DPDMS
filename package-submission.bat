@echo off
REM Double-click to export the databases into database\export and zip the project for submission
cd /d "%~dp0"
powershell -ExecutionPolicy Bypass -File scripts\package-submission.ps1
echo.
pause
