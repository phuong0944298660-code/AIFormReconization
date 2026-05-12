@echo off
cd /d "%~dp0"

REM OCR is core capability - must be enabled
set OCR_ENABLED=true
set OCR_LANGUAGE=eng+chi_sim+chi_tra
set TESSDATA_PREFIX=C:/Apps/common/TesseractOCR/tessdata

REM RAG service connection
set RAG_ENABLED=true
set RAG_BASE_URL=http://127.0.0.1:8090

REM LLM disabled by default for demo
set LLM_ENABLED=false

java -jar "%~dp0target\id995a-review-backend-0.1.0.jar" >> "%~dp0backend-dev.out.log" 2>&1
