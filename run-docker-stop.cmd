@echo off
cd /d "%~dp0"

echo Stopping ID995A Docker services...
docker-compose down

echo.
echo Services stopped.
pause
