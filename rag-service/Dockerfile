FROM python:3.11-slim

WORKDIR /app

# 安装依赖
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

# 复制应用代码
COPY app ./app
COPY data ./data

EXPOSE 8090

# 环境变量默认值
ENV EMBEDDING_ENABLED=false
ENV EMBEDDING_BASE_URL=https://apie.zhisuaninfo.com/v1
ENV EMBEDDING_MODEL=bge-m3
ENV EMBEDDING_API_KEY=""
ENV VECTOR_CACHE_PATH=data/rule-vectors-bge-m3.json

CMD ["python", "-m", "uvicorn", "app.main:app", "--host", "0.0.0.0", "--port", "8090"]
