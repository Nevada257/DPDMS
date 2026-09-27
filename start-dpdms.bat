@echo off
REM Double-click to start the whole DPDMS system (one window, one tab per service)
cd /d "%~dp0"
powershell -ExecutionPolicy Bypass -File scripts\start-all.ps1 %*
echo.
echo Open http://localhost:5173 in your browser once the frontend tab says "ready".
pause
