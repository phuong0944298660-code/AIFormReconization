@echo off
cd /d "%~dp0"
if exist "%~dp0.venv312\Scripts\python.exe" (
  "%~dp0.venv312\Scripts\python.exe" -m uvicorn app.main:app --host 127.0.0.1 --port 8090 >> "%~dp0rag-dev.log" 2>&1
) else (
  python -m uvicorn app.main:app --host 127.0.0.1 --port 8090 >> "%~dp0rag-dev.log" 2>&1
)
