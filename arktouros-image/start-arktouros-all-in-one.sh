#!/bin/bash
DOCKER='' # DOCKER的可执行路径
DOCKER_COMPOSE='' # DOCKER_COMPOSE的可执行路径
LOCAL_IP=''
ES_JVM_OPTIONS=''

function print_help() {
    echo "该脚本将使用基于 Almalinux 9.4 的 docker 镜像安装 arktouros apm。所有参数均为可选项，不指定时指定默认安装。您可以根据需要修改后重启容器。"
    echo "用法:  [OPTION]"
    echo "-h, --help                    显示帮助信息"
    echo "-d, --delete-arktouros-index" 删除elasticsearch中所有arktouros索引
    echo "-i, --local-ip                宿主机的机器的IP，字符串形式的符合IPv4标准的地址。该IP将被用于定义 arktouros-ui 以及 arktouros-bigscreen 的跳转URL，默认127.0.0.1，不会从网卡读取。"
    echo "-j, --es-jvm-options          被配置到Elatisticsearch的JVM配置项，将被写入jvm.options文件，后续您也可以手动配置该文件。如果您对JVM及Elasticsearch的JVM初始配置不了解，我们建议您不要配置该选项。"
}

function delete_arktouros_index() {
    # 检查是否安装了 Docker
    if ! command -v docker &> /dev/null
    then
        echo "Docker 未安装，请安装 Docker 后再运行此脚本。" >&2
        exit 1
    fi
    # 获取 Docker 的执行路径并赋值给变量 DOCKER
    DOCKER=$(command -v docker)
    $DOCKER cp $(pwd)/delete-all-arktouros-index.sh arktouros-all-in-one:/usr/local/arktouros-apm-api
    $DOCKER exec -it arktouros-all-in-one /bin/bash /usr/local/arktouros-apm-api/delete-all-arktouros-index.sh
}

while [[ "$#" -gt 0 ]]; do
    case $1 in
        -h|--help)
            print_help
            exit 0
            ;;
        -d|--delete-arktouros-index)
            delete_arktouros_index
            exit 0
            ;;
        -i|--local-ip)
            LOCAL_IP="$2"
            shift # 跳过已处理的参数
            ;;
        -j|--es-jvm-options)
            ES_JVM_OPTIONS="$2"
            shift # 跳过已处理的参数
            ;;
        *)
            echo "未知参数: $1"
            print_help
            exit 1
            ;;
    esac
    shift
done

function init_arktouros() {
    TAR_FILE="./arktouros-all-in-one.tar"
    IMAGE_NAME="wangminan/arktouros-all-in-one"
    CONTAINER_NAME="arktouros-all-in-one"
    IMAGE_TAG="latest"

    # 检查是否存在名为 IMAGE_NAME 的容器
    EXISTING_CONTAINER_ID=$($DOCKER ps -a --filter "name=^/${CONTAINER_NAME}$" --format "{{.ID}}")

    if [ -n "$EXISTING_CONTAINER_ID" ]; then
        # 如果找到相同名称的容器，先停止并删除它
        echo "已存在容器 ${CONTAINER_NAME}，正在停止并删除..."
        $DOCKER stop "$EXISTING_CONTAINER_ID"
        $DOCKER rm "$EXISTING_CONTAINER_ID"

        if [ $? -ne 0 ]; then
            echo "删除容器 ${CONTAINER_NAME} 失败。" >&2
            exit 1
        fi
        echo "容器 ${CONTAINER_NAME} 已删除。"
    fi

    # 检查是否已经存在相同的 IMAGE_NAME:IMAGE_TAG
    EXISTING_IMAGE_ID=$($DOCKER images --format "{{.ID}}" "$IMAGE_NAME:$IMAGE_TAG")

    if [ -n "$EXISTING_IMAGE_ID" ]; then
        # 如果找到相同的镜像，先删除它
        echo "已存在镜像 ${IMAGE_NAME}:${IMAGE_TAG}，正在删除..."
        $DOCKER rmi "$IMAGE_NAME:$IMAGE_TAG"

        if [ $? -ne 0 ]; then
            echo "删除镜像 ${IMAGE_NAME}:${IMAGE_TAG} 失败。" >&2
            exit 1
        fi
        echo "镜像 ${IMAGE_NAME}:${IMAGE_TAG} 已删除。"
    fi

    echo "加载镜像 ${TAR_FILE}..."
    # 如果有pv命令 跑 pv "$TAR_FILE" | $DOCKER load -i 否则跑 $DOCKER load -i "$TAR_FILE"
    if command -v pv &> /dev/null; then
        pv "$TAR_FILE" | $DOCKER load
    else
        $DOCKER load -i "$TAR_FILE"
    fi

    # 检查加载是否成功
    if [ $? -ne 0 ]; then
        echo "加载镜像失败。" >&2
        exit 1
    fi

     # 检查是否已经存在相同的 IMAGE_NAME:IMAGE_TAG
    EXISTING_IMAGE_ID=$($DOCKER images --format "{{.ID}}" "$IMAGE_NAME:$IMAGE_TAG")

    if [ -n "$EXISTING_IMAGE_ID" ]; then
        echo "已存在镜像 ${IMAGE_NAME}:${IMAGE_TAG}，跳过打标。"
    else
        # 查找没有 REPOSITORY 和 TAG 的镜像，即 <none> 镜像
        UNTAGGED_IMAGE_ID=$($DOCKER images --filter "dangling=true" --format "{{.ID}}")
        # 检查是否找到未打标签的镜像
        if [ -z "$UNTAGGED_IMAGE_ID" ]; then
            echo "未找到未打标签的镜像。" >&2
            exit 1
        fi
        # 为找到的镜像打标签
        echo "为镜像 ID: $UNTAGGED_IMAGE_ID 打标签为 ${IMAGE_NAME}:${IMAGE_TAG}..."
        $DOCKER tag "$UNTAGGED_IMAGE_ID" "${IMAGE_NAME}:${IMAGE_TAG}"
    fi

    if [ -n "$DOCKER_COMPOSE" ]; then
        # 如果 DOCKER_COMPOSE 非空，使用 docker-compose 启动
        echo "使用 docker-compose 启动项目..."
        $DOCKER_COMPOSE up -d
    elif [ -n "$DOCKER" ]; then
        # 如果 DOCKER_COMPOSE 为空，使用 docker compose 启动
        echo "使用 docker compose 启动项目..."
        $DOCKER compose up -d
    fi

    # echo "启动 arktouros自带的elasticsearch和arktouros服务... 请等待20s"
    # # 等待5s 执行docker exec -it arktouros-all-in-one systemctl start elasticsearch 以及 docker exec -it arktouros-all-in-one systemctl start arktouros
    # sleep 5
    # # 删除锁
    # $DOCKER exec -it arktouros-all-in-one rm -rf /var/lib/elasticsearch/node.lock
    # $DOCKER exec -it arktouros-all-in-one systemctl start elasticsearch
    # sleep 15
    # $DOCKER exec -it arktouros-all-in-one systemctl start arktouros

    # 检查启动是否成功
    if [ $? -eq 0 ]; then
        echo "arktouros启动成功。"
    else
        echo "arktouros启动失败。" >&2
        exit 1
    fi
}

function prechange_config_files() {
    check_vm_options
    replace_ip
    set_es_jvm_options
}

function check_vm_options() {
    # 定义要检查的配置项
    CONFIG_KEY="vm.max_map_count"
    CONFIG_VALUE="262144"
    CONFIG_LINE="${CONFIG_KEY}=${CONFIG_VALUE}"

    # 检查 /etc/sysctl.conf 中是否已经有 vm.max_map_count 配置
    if grep -q "^${CONFIG_KEY}=" /etc/sysctl.conf; then
        # 如果存在，使用 sed 替换该行
        sed -i "s/^${CONFIG_KEY}=.*/${CONFIG_LINE}/" /etc/sysctl.conf
        echo "已更新 ${CONFIG_KEY} 为 ${CONFIG_VALUE}。"
    else
        # 如果不存在，追加到文件末尾
        echo "${CONFIG_LINE}" >> /etc/sysctl.conf
        echo "已添加 ${CONFIG_KEY} 为 ${CONFIG_VALUE}。"
    fi

    # 使新的设置立即生效
    sysctl -p
}

function replace_ip() {
    # 检查 LOCAL_IP 是否为空字符串
    if [ -z "$LOCAL_IP" ]; then
        echo "LOCAL_IP 为空，跳过 IP 修改操作。"
        return 0
    fi

    # 检查 LOCAL_IP 是否为合法的 IPv4 地址
    if [[ ! $LOCAL_IP =~ ^([0-9]{1,3}\.){3}[0-9]{1,3}$ ]]; then
        echo "错误: $LOCAL_IP 不是合法的 IPv4 地址。" >&2
        exit 1
    fi

    # 确保 IP 地址的每一部分在 0-255 之间
    IFS='.' read -r -a octets <<< "$LOCAL_IP"
    for octet in "${octets[@]}"; do
        if (( octet < 0 || octet > 255 )); then
            echo "错误: $LOCAL_IP 不是合法的 IPv4 地址。" >&2
            exit 1
        fi
    done

    # 替换 arktourosUiConfig.js 文件中的 127.0.0.1
    UI_CONFIG_FILE="./arktouros-ui/arktourosUiConfig.js"
    if [ -f "$UI_CONFIG_FILE" ]; then
        sed -i "s|http://127.0.0.1:50050|http://$LOCAL_IP:50050|g" "$UI_CONFIG_FILE"
        sed -i "s|http://127.0.0.1:50053|http://$LOCAL_IP:50053|g" "$UI_CONFIG_FILE"
        echo "$UI_CONFIG_FILE 文件已修改。"
    else
        echo "错误: 找不到 $UI_CONFIG_FILE 文件。" >&2
        exit 1
    fi

    # 替换 config.json 文件中的 127.0.0.1
    BIGSCREEN_CONFIG_FILE="./arktouros-bigscreen/config.json"
    if [ -f "$BIGSCREEN_CONFIG_FILE" ]; then
        sed -i "s|http://127.0.0.1:50050|http://$LOCAL_IP:50050|g" "$BIGSCREEN_CONFIG_FILE"
        sed -i "s|http://127.0.0.1:50052|http://$LOCAL_IP:50052|g" "$BIGSCREEN_CONFIG_FILE"
        echo "$BIGSCREEN_CONFIG_FILE 文件已修改。"
    else
        echo "错误: 找不到 $BIGSCREEN_CONFIG_FILE 文件。" >&2
        exit 1
    fi

    echo "IP 地址替换操作完成。"
}

function set_es_jvm_options() {
    if [ -n "$ES_JVM_OPTIONS" ]; then
        # 将 ES_JVM_OPTIONS 追加写入 jvm.options 文件
        echo "$ES_JVM_OPTIONS" >> ./elasticsearch/config/jvm.options
        echo "ES_JVM_OPTIONS 已追加到 jvm.options 文件中。"
    else
        echo "ES_JVM_OPTIONS 为空，跳过追加操作。"
    fi
}

function execute_prechecks() {
    check_root
    check_platform
}

function check_root() {
    if [ $(id -u) != "0" ]; then
        echo "Error: You must be root to run this script, please use root to install arktouros apm."
        exit 1
    fi
}

# 确认本机是否为 x86_64 平台
function check_platform() {
    platform=$(uname -m)
    if [[ $platform != "x86_64" ]]; then
        echo "Error: This script only supports x86_64 platform."
        exit 1
    fi
}

function set_docker() {
    # 检查是否安装了 Docker
    if ! command -v docker &> /dev/null
    then
        echo "Docker 未安装，请安装 Docker 后再运行此脚本。" >&2
        exit 1
    fi

    # 获取 Docker 的执行路径并赋值给变量 DOCKER
    DOCKER=$(command -v docker)

    # 输出 Docker 路径
    echo "Docker 已安装，路径为: $DOCKER"

    # 检查是否安装了 docker-compose 或支持 docker compose 命令
    if command -v docker-compose &> /dev/null
    then
        DOCKER_COMPOSE=$(command -v docker-compose)
        echo "docker-compose 已安装，路径为: $DOCKER_COMPOSE，安装将使用docker-compose。"
    elif $DOCKER compose version &> /dev/null
    then
        echo "docker-compose未安装，但支持 docker compose 插件，安装将使用 docker compose命令。"
    else
        echo "未安装 docker-compose，也不支持 docker compose 插件。您可以使用如下命令手动启动："
        echo "docker load -i arktouros-all-in-one.tar"
        echo "docker run -d --name arktouros-all-in-one --privileged --restart on-failure -p 50049:50049 -p 50050:50050 -p 50051:50051 -p 50052:50052 -p 50053:50053 -v \$(pwd)/arktouros-apm-api/config:/usr/local/arktouros-apm-api/config -v \$(pwd)/arktouros-apm-api/logs:/usr/local/arktouros-apm-api/logs -v \$(pwd)/arktouros-apm-api/input_logs:/usr/local/arktouros-apm-api/input_logs -v \$(pwd)/arktouros-ui/arktourosUiConfig.js:/etc/nginx/html/arktouros-ui/arktourosUiConfig.js -v \$(pwd)/arktouros-bigscreen/config.json:/etc/nginx/html/arktouros-bigscreen/config.json --memory=8g $IMAGE_NAME:$IMAGE_TAG /usr/sbin/init"
        echo "如若arktouros未正常启动，请执行 docker exec -it arktouros-all-in-one systemctl start arktouros"
        exit 1
    fi
}

# 先声明 再调用
execute_prechecks
prechange_config_files
set_docker
init_arktouros
