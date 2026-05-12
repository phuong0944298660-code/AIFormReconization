# 阶段1：编译 Spring Boot 应用
FROM maven:3.9-eclipse-temurin-17 AS builder
WORKDIR /build
COPY pom.xml .
COPY src ./src
RUN mvn package -DskipTests

# 阶段2：运行环境 + Tesseract OCR
FROM eclipse-temurin:17-jdk

# 安装 Tesseract OCR 及中文语言包（Tess4j 5.x 需要 Tesseract 5.x）
RUN apt-get update && apt-get install -y --no-install-recommends \
    tesseract-ocr \
    tesseract-ocr-chi-sim \
    tesseract-ocr-chi-tra \
    && rm -rf /var/lib/apt/lists/*

# 验证 Tesseract 版本和语言包
RUN tesseract --version && tesseract --list-langs

WORKDIR /app
COPY --from=builder /build/target/*.jar app.jar

# 官方模板通过 docker-compose volume 挂载到 /app/official_templates
EXPOSE 8080

# 环境变量默认值（可在 docker-compose 中覆盖）
ENV OCR_ENABLED=true
ENV OCR_LANGUAGE=eng+chi_sim+chi_tra
ENV TESSDATA_PREFIX=/usr/share/tesseract-ocr/5/tessdata
ENV RAG_ENABLED=true
ENV RAG_BASE_URL=http://rag-service:8090

ENTRYPOINT ["java", "-jar", "app.jar"]
