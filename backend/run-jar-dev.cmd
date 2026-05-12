@echo off
cd /d "%~dp0"
set RAG_ENABLED=true
set RAG_BASE_URL=http://127.0.0.1:8090
java -jar "%~dp0target\id995a-review-backend-0.1.0.jar" >> "%~dp0backend-dev.out.log" 2>&1
