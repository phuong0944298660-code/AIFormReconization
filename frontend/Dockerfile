# 阶段1：构建 Vue 静态文件
FROM node:20-alpine AS builder
WORKDIR /app
COPY package*.json ./
RUN npm ci
COPY . .

# 生产构建时 API 通过 nginx 代理（同域），VITE_API_BASE 设为空
ENV VITE_API_BASE=""
RUN npm run build

# 阶段2：nginx 提供静态文件 + API 反向代理
FROM nginx:alpine

# 复制构建产物
COPY --from=builder /app/dist /usr/share/nginx/html

# 复制 nginx 配置
COPY nginx.conf /etc/nginx/conf.d/default.conf

EXPOSE 80

CMD ["nginx", "-g", "daemon off;"]
