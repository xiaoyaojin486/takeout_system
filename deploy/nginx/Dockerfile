# =====================================================================
#  网关镜像（多阶段：Node 构建两套前端 → Nginx 托管 + 反代）
#  构建（context 必须是项目根，因为要 COPY frontend-* 和 deploy/nginx/）：
#    docker build -t takeout-nginx:1.0.0 -f deploy/nginx/Dockerfile .
# =====================================================================

# ===== Stage 1：构建前端 =====
FROM node:20-alpine AS frontend
WORKDIR /build

# ---- H5 顾客端（端口 80）----
COPY frontend-h5/package*.json ./frontend-h5/
RUN cd frontend-h5 && npm ci --no-audit --no-fund
COPY frontend-h5/ ./frontend-h5/
RUN cd frontend-h5 && npm run build

# ---- Admin 商家/管理端（端口 81）----
COPY frontend-admin/package*.json ./frontend-admin/
RUN cd frontend-admin && npm ci --no-audit --no-fund
COPY frontend-admin/ ./frontend-admin/
RUN cd frontend-admin && npm run build

# ===== Stage 2：Nginx =====
FROM nginx:1.27-alpine

COPY deploy/nginx/nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=frontend /build/frontend-h5/dist    /usr/share/nginx/h5
COPY --from=frontend /build/frontend-admin/dist /usr/share/nginx/admin

EXPOSE 80 81