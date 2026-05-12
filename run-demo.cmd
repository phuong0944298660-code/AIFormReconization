@echo off
cd /d "%~dp0"

start "ID995A RAG Service" /min "%~dp0rag-service\run-dev-log.cmd"
timeout /t 3 /nobreak >nul

start "ID995A Backend" /min "%~dp0backend\run-jar-dev.cmd"
timeout /t 5 /nobreak >nul

start "ID995A Frontend" /min "%~dp0frontend\run-dev.cmd"

echo Demo services are starting.
echo Frontend: http://127.0.0.1:5173
echo Backend:  http://127.0.0.1:8080/api/health
echo RAG:      http://127.0.0.1:8090/health
echo.
echo Logs:
echo   rag-service\rag-dev.log
echo   backend\backend-dev.out.log
echo   frontend\frontend-dev.out.log
