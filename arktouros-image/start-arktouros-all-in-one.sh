#!/bin/bash
set -euo pipefail
cd "$(dirname "$0")"
LOCAL_IP=''
WITH_EXAMPLE=false
CONTAINER_NAME=arktouros-all-in-one
IMAGE_NAME=arktouros-all-in-one:sqlite
while [[ $# -gt 0 ]]; do
    case "$1" in
        -h|--help)
            echo '用法: bash start-arktouros-all-in-one.sh [-i 宿主机IPv4] [--with-example]'
            echo '仅需 docker；校验并加载离线镜像，替换本应用旧容器，健康后清理未被使用的旧镜像。'
            exit 0 ;;
        -i|--local-ip) LOCAL_IP="${2:?缺少 IPv4 地址}"; shift 2 ;;
        --with-example) WITH_EXAMPLE=true; shift ;;
        *) echo "未知参数: $1" >&2; exit 1 ;;
    esac
done
command -v docker >/dev/null || { echo '请先安装并启动 Docker Engine。' >&2; exit 1; }
docker info >/dev/null
[[ "$(docker info --format '{{.OSType}}')" == linux ]] || { echo '需要 Linux 容器引擎。' >&2; exit 1; }
[[ "$(uname -m)" == x86_64 ]] || { echo '本离线镜像适用于 Linux x86_64/amd64。' >&2; exit 1; }
for file in arktouros-all-in-one.tar arktouros-all-in-one.tar.sha256 \
    arktouros-apm-api/config/application.yaml arktouros-ui/arktourosUiConfig.js arktouros-bigscreen/config.json; do
    [[ -f "$file" ]] || { echo "缺少文件: $file；请完整解压安装包。" >&2; exit 1; }
done
sha256sum -c arktouros-all-in-one.tar.sha256
if [[ -n "$LOCAL_IP" ]]; then
    [[ "$LOCAL_IP" =~ ^([0-9]{1,3}\.){3}[0-9]{1,3}$ ]] || { echo 'IPv4 地址无效' >&2; exit 1; }
    IFS='.' read -r -a octets <<< "$LOCAL_IP"
    for octet in "${octets[@]}"; do
        (( 10#$octet <= 255 )) || { echo 'IPv4 地址无效' >&2; exit 1; }
    done
    cat > arktouros-ui/arktourosUiConfig.js <<EOF
window.arktourosUiConfig = {
    VITE_BASE_URL: "http://$LOCAL_IP:50050/api/v0",
    VITE_BIG_SCREEN_URL: "http://$LOCAL_IP:50053"
}
EOF
    cat > arktouros-bigscreen/config.json <<EOF
{"backendUrl":"http://$LOCAL_IP:50050/api/v0","arktourosUiIp":"http://$LOCAL_IP:50052"}
EOF
fi
OLD_IMAGE=''
OLD_TAGS=()
for tag in arktouros-all-in-one:0.1 wangminan/arktouros-all-in-one:latest \
    wangminan/arktouros-all-in-one:legacy wangminan/arktouros-all-in-one:sqlite; do
    if docker image inspect "$tag" >/dev/null 2>&1; then OLD_TAGS+=("$tag"); fi
done
if docker container inspect "$CONTAINER_NAME" >/dev/null 2>&1; then
    OLD_IMAGE=$(docker inspect --format '{{.Image}}' "$CONTAINER_NAME")
fi
# Validate the replacement before stopping the existing application.
docker load -i arktouros-all-in-one.tar
[[ "$(docker image inspect --format '{{index .Config.Labels "edu.npu.arktouros.storage"}}' "$IMAGE_NAME")" == sqlite ]] || { echo '镜像不是 SQLite 交付版本。' >&2; exit 1; }
NEW_IMAGE=$(docker image inspect --format '{{.Id}}' "$IMAGE_NAME")
if [[ -n "$OLD_IMAGE" ]]; then
    BACKUP_DIR="backups/replaced-$(date +%Y%m%d-%H%M%S)-$$"
    mkdir -p "$BACKUP_DIR"
    docker inspect "$CONTAINER_NAME" > "$BACKUP_DIR/container-inspect.json"
    STORAGE=$(docker inspect --format '{{index .Config.Labels "edu.npu.arktouros.storage"}}' "$CONTAINER_NAME")
    DB_MOUNT=$(docker inspect --format '{{range .Mounts}}{{if eq .Destination "/usr/local/arktouros-apm-api/db"}}mounted{{end}}{{end}}' "$CONTAINER_NAME")
    docker stop --time 45 "$CONTAINER_NAME"
    if [[ "$STORAGE" != sqlite || "$DB_MOUNT" != mounted ]]; then
        echo "正在备份旧容器内部文件到 $BACKUP_DIR/container-rootfs.tar；宿主机挂载和 Docker volumes 均保留。"
        docker export --output "$BACKUP_DIR/container-rootfs.tar" "$CONTAINER_NAME"
    fi
    docker rm "$CONTAINER_NAME"
fi
mkdir -p arktouros-apm-api/db arktouros-apm-api/logs arktouros-apm-api/input_logs
INSTALL_DIR=$(pwd -P)
if command -v cygpath >/dev/null; then INSTALL_DIR=$(cygpath -m "$INSTALL_DIR"); fi
docker run -d --name "$CONTAINER_NAME" --restart on-failure --stop-timeout 45 \
    --label "edu.npu.arktouros.install-dir=$INSTALL_DIR" \
    -p 80:80 -p 50049:50049 -p 50050:50050 -p 50051:50051 -p 50052:50052 -p 50053:50053 \
    -v "$INSTALL_DIR/arktouros-apm-api/config:/usr/local/arktouros-apm-api/config" \
    -v "$INSTALL_DIR/arktouros-apm-api/logs:/usr/local/arktouros-apm-api/logs" \
    -v "$INSTALL_DIR/arktouros-apm-api/input_logs:/usr/local/arktouros-apm-api/input_logs" \
    -v "$INSTALL_DIR/arktouros-ui/arktourosUiConfig.js:/etc/nginx/html/arktouros-ui/arktourosUiConfig.js" \
    -v "$INSTALL_DIR/arktouros-bigscreen/config.json:/etc/nginx/html/arktouros-bigscreen/config.json" \
    -v "$INSTALL_DIR/arktouros-apm-api/db:/usr/local/arktouros-apm-api/db" "$IMAGE_NAME"
for (( attempt=0; attempt<90; attempt++ )); do
    HEALTH=$(docker inspect --format '{{.State.Health.Status}}' "$CONTAINER_NAME")
    if [[ "$HEALTH" == healthy ]]; then
        echo '前端、大屏与后端健康检查通过。'
        for old in "${OLD_TAGS[@]}" "$OLD_IMAGE"; do
            [[ -n "$old" ]] || continue
            ID=$(docker image inspect --format '{{.Id}}' "$old" 2>/dev/null) || continue
            [[ "$ID" != "$NEW_IMAGE" ]] || continue
            if [[ -z "$(docker ps -aq --filter "ancestor=$ID")" ]]; then
                docker image rm "$old" || echo "旧镜像仍有其他标签，保留: $old"
            else
                echo "旧镜像仍被其他容器使用，保留: $old"
            fi
        done
        if [[ "$WITH_EXAMPLE" == true ]]; then bash ./import-example.sh; fi
        echo "前端: http://${LOCAL_IP:-127.0.0.1}:50052/#/main/service/table"
        echo "大屏: http://${LOCAL_IP:-127.0.0.1}:50053/"
        exit 0
    fi
    sleep 2
done
echo '启动超时，请检查日志；旧镜像和数据备份未清理。' >&2
docker logs --tail 100 "$CONTAINER_NAME"
exit 1