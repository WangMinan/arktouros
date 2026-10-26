FROM node:22-bookworm-slim AS frontend
WORKDIR /src
COPY arktouros-ui/package.json arktouros-ui/package-lock.json ./
# Retain locked versions and integrity while using the public registry.
RUN node -e "const fs=require('fs');const p='package-lock.json';fs.writeFileSync(p,fs.readFileSync(p,'utf8').replaceAll('https://registry.npmmirror.com/','https://registry.npmjs.org/'))" && npm ci --no-audit --no-fund
COPY arktouros-ui/ ./
RUN npm run build

FROM eclipse-temurin:21-jre-jammy
ENV TZ=Asia/Shanghai
RUN apt-get update && apt-get install -y --no-install-recommends nginx supervisor curl && rm -rf /var/lib/apt/lists/*
WORKDIR /usr/local/arktouros-apm-api
COPY build/arktouros-apm-api-leaves.tar.gz /tmp/apm.tar.gz
RUN tar -xzf /tmp/apm.tar.gz -C /usr/local && rm /tmp/apm.tar.gz && mkdir -p db logs input_logs
COPY arktouros-image/arktouros-apm-api/config/application.yaml ./config/application.yaml
COPY --from=frontend /src/dist/ /etc/nginx/html/arktouros-ui/
COPY arktouros-bigscreen/ /etc/nginx/html/arktouros-bigscreen/
COPY arktouros-image/arktouros-bigscreen/config.json /etc/nginx/html/arktouros-bigscreen/config.json
COPY arktouros-image/nginx.conf /etc/nginx/nginx.conf
COPY arktouros-image/supervisord.conf /etc/supervisor/supervisord.conf
LABEL org.opencontainers.image.title="Arktouros SQLite all-in-one" edu.npu.arktouros.storage="sqlite"
EXPOSE 80 50049 50050 50051 50052 50053
HEALTHCHECK --interval=10s --timeout=5s --start-period=60s --retries=6 CMD curl -fsS http://127.0.0.1:50050/api/v0/service/time-range >/dev/null && curl -fsS http://127.0.0.1:50052/ >/dev/null && curl -fsS http://127.0.0.1:50053/ >/dev/null || exit 1
ENTRYPOINT ["/usr/bin/supervisord", "-n", "-c", "/etc/supervisor/supervisord.conf"]